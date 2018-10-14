package com.erlei.camera;


import android.graphics.SurfaceTexture;

import com.erlei.gdx.android.widget.GLContext;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.TextureData;
import com.erlei.gdx.utils.GdxRuntimeException;

/**
 * Created by lll on 2018/9/29
 * Email : lllemail@foxmail.com
 * Describe :
 */
public class CameraTexture extends Texture {

    private SurfaceTexture mSurfaceTexture;

    public CameraTexture(int glTarget, CameraTextureData data) {
        super(glTarget, GLContext.getGL20().glGenTexture(), data);
        mSurfaceTexture = new SurfaceTexture(getTextureObjectHandle());
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    protected void reload() {

    }


    @Override
    public void dispose() {
        super.dispose();
        mSurfaceTexture.release();
        mSurfaceTexture = null;
    }

    public static class CameraTextureData implements TextureData {

        public CameraTextureData() {
        }

        @Override
        public TextureDataType getType() {
            return TextureDataType.Custom;
        }

        @Override
        public boolean isPrepared() {
            return true;
        }

        @Override
        public void prepare() {

        }

        @Override
        public Pixmap consumePixmap() {
            throw new GdxRuntimeException("This CameraTextureData implementation does not return a Pixmap");
        }

        @Override
        public boolean disposePixmap() {
            throw new GdxRuntimeException("This CameraTextureData implementation does not return a Pixmap");
        }

        @Override
        public void consumeCustomData(int target) {
            if (!GLContext.getGLContext().supportsExtension("OES_texture_float"))
                throw new GdxRuntimeException("Extension OES_texture_float not supported!");
        }


        @Override
        public int getWidth() {
            throw new GdxRuntimeException("This CameraTextureData implementation does not support getWidth");
        }

        @Override
        public int getHeight() {
            throw new GdxRuntimeException("This CameraTextureData implementation does not support getHeight");
        }

        @Override
        public Pixmap.Format getFormat() {
            return Pixmap.Format.RGBA8888; // it's not true, but FloatTextureData.getFormat() isn't used anywhere
        }

        @Override
        public boolean useMipMaps() {
            return false;
        }
    }
}
