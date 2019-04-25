package com.erlei.videorecorder.recorder;

import android.content.Context;
import android.opengl.EGLSurface;
import android.os.Environment;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.utils.Logger;
import com.erlei.gdx.utils.ScreenUtils;
import com.erlei.gdx.widget.BaseRender;
import com.erlei.gdx.widget.EGLCore;
import com.erlei.videorecorder.camera.CameraControl;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.encoder.MediaAudioEncoder;
import com.erlei.videorecorder.encoder.MediaMuxerWrapper;
import com.erlei.videorecorder.encoder.MediaVideoEncoder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class VideoRecorder extends BaseRender implements IVideoRecorder, RecordableRender.Recorder {


    private Logger mLogger = new Logger("VideoRecorder");
    private final Executor mThreadExecutor;
    private volatile boolean mRecording, mRequestStart, mMuxerRunning, mRequestStop;
    private final Object mSync = new Object();
    private final Config mConfig;
    private File mOutputFile;
    private MediaMuxerWrapper mMuxer;
    private MediaVideoEncoder mVideoEncoder;
    private EGLCore mEGLCore;
    private EGLSurface mWindowSurface;
    private SpriteBatch mSpriteBatch;
    private Size mSize;
    private FrameBuffer mFrameBuffer;
    private FrameBuffer mRecordFrameBuffer;

    private VideoRecorder(Config config) {
        mThreadExecutor = Executors.newSingleThreadExecutor();
        mConfig = config;
    }

    @Override
    public void create(EGLCore egl, GL20 gl) {
        super.create(egl, gl);
        mEGLCore = egl;
        mSpriteBatch = new SpriteBatch();
        startRecord();
    }


    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void render(GL20 gl) {
        super.render(gl);
        if (mWindowSurface != null && mVideoEncoder != null && isRecording() && mMuxerRunning) {
            mEGLCore.makeCurrent(mWindowSurface);
            mRecordFrameBuffer.begin();
            mSpriteBatch.begin();
            Texture texture = mFrameBuffer.getColorBufferTexture();
            mSpriteBatch.draw(texture,
                    0, 0,
                    texture.getWidth(), texture.getHeight(),
                    0, 0,
                    mRecordFrameBuffer.getWidth(),
                    mRecordFrameBuffer.getHeight(), false, false);
            mSpriteBatch.end();
            mRecordFrameBuffer.end();

            mSpriteBatch.begin();
            texture = mRecordFrameBuffer.getColorBufferTexture();
            mSpriteBatch.draw(texture,
                    0, 0,
                    texture.getWidth(),
                    texture.getHeight(),
                    0, 0,
                    texture.getWidth(),
                    texture.getHeight(), false, false);
            mSpriteBatch.end();
            mVideoEncoder.frameAvailableSoon();

            mEGLCore.swapBuffers(mWindowSurface);
            mEGLCore.makeCurrent();
        }
    }

    @Override
    public void dispose() {
        stopRecord();
        if (mWindowSurface != null) {
            mEGLCore.releaseSurface(mWindowSurface);
            mWindowSurface = null;
        }
        mRecordFrameBuffer.dispose();
        mSpriteBatch.dispose();
    }

    @Override
    public void startRecord() {
        setRecording(true);
    }

    @Override
    public void stopRecord() {
        setRecording(false);
    }

    @Override
    public boolean isRecording() {
        return mRecording;
    }

    @Override
    public void takePicture(TakePictureCallback callback) {

    }

    public synchronized void setRecording(boolean enable) {
        if (isRecording() == enable) {
            mLogger.warn("setRecording isRecording() == enable");
            return;
        }
        if (mEGLCore == null) {
            mLogger.warn("GLThread not running");
            return;
        }
        if (enable) {
            startEncoder();
        } else {
            stopEncoder();
        }
    }


    private synchronized void startEncoder() {
        mRecording = true;
        mRequestStart = true;
        mLogger.info("startEncoder:begin");
        mOutputFile = getOutPut();
        mThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mLogger.info("startEncoder:begin");
                synchronized (mSync) {
                    try {
                        mMuxer = new MediaMuxerWrapper(mOutputFile.getAbsolutePath(), mConfig.viewHandler);
                        mVideoEncoder = new MediaVideoEncoder(mMuxer, mSize, mConfig.getIFrameInterval(), mConfig.getVideoBitRate(), mConfig.getFrameRate());
                        new MediaAudioEncoder(mMuxer, mConfig.getAudioSampleRate(), mConfig.getAudioBitRate(), mConfig.getAudioChannelCount());
                        mMuxer.prepare();
                        mMuxer.startRecording();

                        mWindowSurface = mEGLCore.createWindowSurface(mVideoEncoder.getSurface());
                    } catch (Exception e) {
                        e.printStackTrace();
                        mLogger.error("startEncoder:", e);
                    }
                    mMuxerRunning = true;
                    mRequestStart = false;
                }
                if (mConfig.viewHandler != null) {
                    mConfig.viewHandler.onCaptureStarted(mOutputFile.getAbsolutePath());
                }
                mLogger.info("startEncoder:end");
            }
        });
    }


    private synchronized void stopEncoder() {
        mRecording = false;
        mRequestStop = true;
        mLogger.info("stopEncoder:begin tid=" + Thread.currentThread().getId());
        mThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mMuxerRunning) return;
                synchronized (mSync) {
                    mLogger.info("stopEncoder:begin tid=" + Thread.currentThread().getId());
                    mMuxerRunning = false;
                    try {
                        if (mMuxer != null) {
                            mMuxer.stopRecording();
                            mMuxer = null;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        mLogger.error("stopEncoder:", e);
                    }
                    mRequestStop = false;
                }
                if (mConfig.viewHandler != null) {
                    mConfig.viewHandler.onCaptureStopped(mOutputFile.getAbsolutePath());
                }
                mLogger.info("stopEncoder:end tid=" + Thread.currentThread().getId());
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @NonNull
    private File getOutPut() {
        if (mConfig.mOutputFile != null) {
            return mConfig.mOutputFile;
        } else {
            File path = new File(mConfig.outputDir);
            if (!path.exists()) {
                path.mkdirs();
            }
            if (path.isFile()) path = path.getParentFile();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault());
            return new File(path, format.format(new Date()) + ".mp4");
        }
    }

    @Override
    public FrameBuffer generateFrameBuffer() {
        mSize = mConfig.cameraControl.getCameraSize();
        mLogger.info("generateFrameBuffer : " + mSize.toString());
        mFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, mSize.getWidth(), mSize.getHeight(), false);
        mRecordFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, mSize.getWidth(), mSize.getHeight(), false);
        return mFrameBuffer;
    }

    public static class Builder {

        private final Config mP;

        public Builder(CameraControl control) {
            mP = new Config(control);
        }

        public Config getConfig() {
            return mP;
        }

        /**
         * 设置期望的帧率
         * 默认为25
         */
        public Builder setCameraControl(CameraControl control) {
            mP.cameraControl = control;
            return this;
        }


        /**
         * 设置期望的帧率
         * 默认为25
         */
        public Builder setFrameRate(int frameRate) {
            mP.frameRate = frameRate;
            return this;
        }

        /**
         * 设置声道数
         */
        public Builder setChannelCount(@IntRange(from = 1, to = 2) int channelCount) {
            mP.audioChannelCount = channelCount;
            return this;
        }

        /**
         * 设置音频采样率
         * 默认为 44100
         */
        public Builder setAudioSampleRate(int sampleRate) {
            mP.audioSampleRate = sampleRate;
            return this;
        }

        /**
         * @param bitRate 设置视频比特率
         *                默认为 width * height *  3 * 4
         */
        public Builder setVideoBitRate(int bitRate) {
            mP.videoBitRate = bitRate;
            return this;
        }

        /**
         * @param bitRate 设置音频比特率
         *                默认为 64000
         */
        public Builder setAudioBitRate(int bitRate) {
            mP.audioBitRate = bitRate;
            return this;
        }

        /**
         * 设置关键帧间隔
         */
        public Builder setIFrameInterval(int interval) {
            mP.iFrameInterval = interval;
            return this;
        }

        /**
         * @param file 设置输出文件 , 无论一个 VideoRecorder实例开启几次录制 , 只有最后一次的录制文件会保存
         */
        public Builder setOutPutFile(File file) {
            mP.mOutputFile = file;
            return this;
        }

        /**
         * @param outputDir 输出文件夹 , 只有沒 setOutPutFile ,这个属性才会起作用, 每一次startRecord都会生成一个新的文件
         */
        public Builder setOutPutDir(String outputDir) {
            mP.outputDir = outputDir;
            return this;
        }


        public VideoRecorder build() {
            if (mP.context == null)
                throw new IllegalArgumentException("context cannot be null");

            if (mP.mOutputFile == null && mP.outputDir == null) {
                File filesDir = mP.context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                if (filesDir == null) filesDir = mP.context.getFilesDir();
                mP.outputDir = filesDir.getPath();
            }
            return new VideoRecorder(mP.clone());
        }

        public Builder setCallbackHandler(VideoRecorderHandler viewHandler) {
            mP.viewHandler = viewHandler;
            return this;
        }
    }

    public static class Config implements Cloneable {
        Context context;
        VideoRecorderHandler viewHandler;
        CameraControl cameraControl;
        boolean logFPS;
        File mOutputFile;
        int audioBitRate = 64000;
        int iFrameInterval = 5;
        int frameRate = 25;
        int audioSampleRate = 44100;
        int audioChannelCount = 1;
        int videoBitRate;
        String outputDir;


        public Config(CameraControl control) {
            context = control.getContext();
            cameraControl = control;
        }

        public Context getContext() {
            return context;
        }

        public VideoRecorderHandler getViewHandler() {
            return viewHandler;
        }

        public boolean isLogFPS() {
            return logFPS;
        }

        public File getOutputFile() {
            return mOutputFile;
        }

        public int getAudioBitRate() {
            return audioBitRate;
        }

        public int getIFrameInterval() {
            return iFrameInterval;
        }

        public int getFrameRate() {
            return frameRate;
        }

        public int getAudioSampleRate() {
            return audioSampleRate;
        }

        public int getAudioChannelCount() {
            return audioChannelCount;
        }

        public int getVideoBitRate() {
            return videoBitRate;
        }

        public String getOutputDir() {
            return outputDir;
        }

        public void setContext(Context context) {
            this.context = context;
        }

        public void setViewHandler(VideoRecorderHandler viewHandler) {
            this.viewHandler = viewHandler;
        }

        public void setLogFPS(boolean logFPS) {
            this.logFPS = logFPS;
        }

        public void setOutputFile(File outputFile) {
            mOutputFile = outputFile;
        }

        public void setAudioBitRate(int audioBitRate) {
            this.audioBitRate = audioBitRate;
        }

        public void setIFrameInterval(int iFrameInterval) {
            this.iFrameInterval = iFrameInterval;
        }

        public void setFrameRate(int frameRate) {
            this.frameRate = frameRate;
        }

        public void setAudioSampleRate(int audioSampleRate) {
            this.audioSampleRate = audioSampleRate;
        }

        public void setAudioChannelCount(int audioChannelCount) {
            this.audioChannelCount = audioChannelCount;
        }

        public void setVideoBitRate(int videoBitRate) {
            this.videoBitRate = videoBitRate;
        }

        public void setOutputDir(String outputDir) {
            this.outputDir = outputDir;
        }

        public CameraControl getCameraControl() {
            return cameraControl;
        }

        public void setCameraControl(CameraControl cameraControl) {
            this.cameraControl = cameraControl;
        }

        @Override
        public Config clone() {
            try {
                return (Config) super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


}
