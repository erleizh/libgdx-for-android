package com.erlei.videorecorder.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaVideoEncoder.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 */

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.view.Surface;

import com.erlei.gdx.utils.Logger;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.recorder.VideoRecorder;

import java.io.IOException;
import java.util.Locale;

public class MediaVideoEncoder extends MediaEncoder {
    private static final String TAG = "MediaVideoEncoder";

    private static final String MIME_TYPE = "video/avc";
    // parameters for recording
    private static final int FRAME_RATE = 30;
    private Logger mLogger = new Logger(TAG);
    private final Size mVideoSize;
    private final int mBitRate;
    private final int mIFrameInterval;
    private final int mFrameRate;
    private Surface mSurface;

    public MediaVideoEncoder(MediaMuxerWrapper muxer, VideoRecorder.Config config) {
        super(muxer);
        mVideoSize = config.getVideoSize();
        mIFrameInterval = config.getIFrameInterval();
        mBitRate = config.getVideoBitRate() <= 0 ? calcBitRate() : config.getVideoBitRate();
        mFrameRate = config.getFrameRate() <= 0 ? FRAME_RATE : config.getFrameRate();
    }

    @Override
    protected void prepare() throws IOException {
        mLogger.info("prepare: ");
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;

        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
        if (videoCodecInfo == null) {
            mLogger.error("Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        mLogger.info("selected codec: " + videoCodecInfo.getName());

        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mVideoSize.getWidth(), mVideoSize.getHeight());
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);    // API >= 18
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);
        mLogger.info("format: " + format);

        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // get Surface for encoder input
        // this method only can call between #configure and #start
        mSurface = mMediaCodec.createInputSurface();    // API >= 18
        mMediaCodec.start();
        mLogger.info("prepare finishing");

    }

    public Surface getSurface() {
        return mSurface;
    }

    @Override
    public boolean frameAvailableSoon() {
        return super.frameAvailableSoon();
    }

    @Override
    protected void release() {
        mLogger.info("release:");
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        super.release();
    }

    private int calcBitRate() {
        final int bitrate = mVideoSize.getWidth() * mVideoSize.getHeight() * 3 * 4;
        mLogger.info(String.format(Locale.getDefault(), "bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return null if no codec matched
     */
    protected static final MediaCodecInfo selectVideoCodec(final String mimeType) {
        Logger.info(TAG, "selectVideoCodec:");

        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    Logger.debug(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
                    final int format = selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * select color format available on specific codec and we can use.
     *
     * @return 0 if no colorFormat is matched
     */
    protected static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
        Logger.info(TAG, "selectColorFormat: ");
        int result = 0;
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
            colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
                if (result == 0)
                    result = colorFormat;
                break;
            }
        }
        if (result == 0)
            Logger.error(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return result;
    }

    /**
     * color formats that we can use in this class
     */
    protected static int[] recognizedFormats;

    static {
        recognizedFormats = new int[]{
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
        };
    }

    private static final boolean isRecognizedViewoFormat(final int colorFormat) {
        Logger.info(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void signalEndOfInputStream() {
        mLogger.debug("sending EOS to encoder");
        mMediaCodec.signalEndOfInputStream();    // API >= 18
        mIsEOS = true;
    }

}
