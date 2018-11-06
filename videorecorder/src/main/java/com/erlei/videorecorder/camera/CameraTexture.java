package com.erlei.videorecorder.camera;


import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;

import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.widget.GLContext;

/**
 * Created by lll on 2018/9/29
 * Email : lllemail@foxmail.com
 * Describe :
 */
public class CameraTexture extends Texture {


    public CameraTexture(CameraTextureData data) {
        super(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLContext.getGL20().glGenTexture(), data);
        data.setTextureId(getTextureObjectHandle());
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
    }

    @Override
    protected void reload() {
        glHandle = GLContext.getGL20().glGenTexture();
        ((CameraTextureData) getTextureData()).setTextureId(getTextureObjectHandle());
        load(getTextureData());
    }

    public SurfaceTexture getSurfaceTexture() {
        return ((CameraTextureData) getTextureData()).getSurfaceTexture();
    }
}
