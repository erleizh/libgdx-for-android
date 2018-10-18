package com.erlei.videorecorder.recorder;

import android.content.Context;
import android.opengl.EGLSurface;
import android.os.Environment;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.utils.Logger;
import com.erlei.gdx.widget.EGLCore;
import com.erlei.videorecorder.camera.CameraRender;
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

public class VideoRecorder implements CameraRender.Renderer, IVideoRecorder {


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
    private Size mCameraSize;

    private VideoRecorder(Config config) {
        mThreadExecutor = Executors.newSingleThreadExecutor();
        mConfig = config;
    }

    @Override
    public void create(EGLCore egl, GL20 gl) {
        mEGLCore = egl;
        mSpriteBatch = new SpriteBatch(1);
        startRecord();
    }

    @Override
    public void resize(Size viewSize, Size cameraSize) {
        mCameraSize = cameraSize;
    }

    @Override
    public void render(GL20 gl, FrameBuffer frameBuffer) {
        if (mWindowSurface != null && mVideoEncoder != null && isRecording() && mMuxerRunning) {
            mEGLCore.makeCurrent(mWindowSurface);
            mVideoEncoder.frameAvailableSoon();
            mSpriteBatch.begin();
            mSpriteBatch.draw(frameBuffer.getColorBufferTexture(), 0, 0, mCameraSize.getWidth(), mCameraSize.getHeight(), 0, 0,
                    frameBuffer.getColorBufferTexture().getWidth(),
                    frameBuffer.getColorBufferTexture().getHeight(), false, true);
            mSpriteBatch.end();
            mEGLCore.swapBuffers(mWindowSurface);
            mEGLCore.makeCurrent();
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {
        if (mWindowSurface != null) {
            mEGLCore.releaseSurface(mWindowSurface);
            mWindowSurface = null;
        }
        stopEncoder();
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
                mLogger.error("startEncoder:begin");
                synchronized (mSync) {
                    try {
                        mMuxer = new MediaMuxerWrapper(mOutputFile.getAbsolutePath(), mConfig.viewHandler);
                        mVideoEncoder = new MediaVideoEncoder(mMuxer, mConfig);
                        new MediaAudioEncoder(mMuxer, mConfig);
                        mMuxer.prepare();
                        mMuxer.startRecording();

                        mWindowSurface = mEGLCore.createWindowSurface(mVideoEncoder.getSurface());
                    } catch (Exception e) {
                        e.printStackTrace();
                        mLogger.error("startEncoder:" + e);
                    }
                    mMuxerRunning = true;
                    mRequestStart = false;
                }
                if (mConfig.viewHandler != null) {
                    mConfig.viewHandler.onCaptureStarted(mOutputFile.getAbsolutePath());
                }
            }
        });
    }


    private synchronized void stopEncoder() {
        mRecording = false;
        mRequestStop = true;
        mLogger.info("stopEncoder:begin");
        mThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (!mMuxerRunning) return;
                synchronized (mSync) {
                    mLogger.info("stopEncoder:begin");
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
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @NonNull
    private File getOutPut() {
        if (mConfig.mOutputFile != null) {
            return mConfig.mOutputFile;
        } else {
            File path = new File(mConfig.outputPath);
            if (!path.exists()) {
                path.mkdirs();
            }
            if (path.isFile()) path = path.getParentFile();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault());
            return new File(path, format.format(new Date()) + ".mp4");
        }
    }


    public static class Builder {

        private final Config mP;

        public Builder(Context context) {
            mP = new Config(context);
        }

        public Config getConfig() {
            return mP;
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
         * 设置视频大小
         * @param size 视频大小
         */
        public Builder setVideoSize(Size size) {
            mP.videoSize = size;
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
         * @param outputPath 输出文件夹 , 只有沒 setOutPutFile ,这个属性才会起作用, 每一次startRecord都会生成一个新的文件
         */
        public Builder setOutPutPath(String outputPath) {
            mP.outputPath = outputPath;
            return this;
        }


        public VideoRecorder build() {
            if (mP.context == null)
                throw new IllegalArgumentException("context cannot be null");

            if (mP.mOutputFile == null && mP.outputPath == null) {
                File filesDir = mP.context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                if (filesDir == null) filesDir = mP.context.getFilesDir();
                mP.outputPath = filesDir.getPath();
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
        boolean logFPS;
        File mOutputFile;
        int audioBitRate = 64000;
        int iFrameInterval = 5;
        int frameRate = 25;
        int audioSampleRate = 44100;
        int audioChannelCount = 1;
        int videoBitRate;
        Size videoSize;
        String outputPath;

        Config(Context context) {
            this.context = context;
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

        public Size getVideoSize() {
            return videoSize;
        }

        public String getOutputPath() {
            return outputPath;
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

        public void setVideoSize(Size videoSize) {
            this.videoSize = videoSize;
        }

        public void setAudioChannelCount(int audioChannelCount) {
            this.audioChannelCount = audioChannelCount;
        }

        public void setVideoBitRate(int videoBitRate) {
            this.videoBitRate = videoBitRate;
        }

        public void setOutputPath(String outputPath) {
            this.outputPath = outputPath;
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
