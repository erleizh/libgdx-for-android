package com.erlei.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;

public class DefaultCameraControl implements CameraRender.CameraControl {

    private final Context mContext;

    public DefaultCameraControl(Context context) {
        mContext = context;
    }

    private Camera mCamera;

    @Override
    public Size getPreviewSize() {
        return mCamera.getPreviewSize();
    }

    @Override
    public void open(SurfaceTexture surfaceTexture) {
        if (mCamera == null || !mCamera.isOpen()) {
            mCamera = new Camera.CameraBuilder(mContext).useDefaultConfig().setPreviewSize(new Size(2048, 1536)).setSurfaceTexture(surfaceTexture).build().open();
        }
    }

    @Override
    public void close() {
        if (mCamera != null && mCamera.isOpen()) {
            mCamera.close();
            mCamera = null;
        }
    }
}
