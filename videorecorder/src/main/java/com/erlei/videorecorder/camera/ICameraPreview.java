package com.erlei.videorecorder.camera;

import android.content.Context;

import com.erlei.gdx.widget.EGLCore;


public interface ICameraPreview {

    Size getSurfaceSize();

    Context getContext();

    /**
     * @return object
     * if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
     *  throw new RuntimeException("invalid surface: " + surface);
     * }
     * @param eglCore
     */
    Object getSurface(EGLCore eglCore);
}
