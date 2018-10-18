package com.erlei.videorecorder.camera;


import android.graphics.SurfaceTexture;

import com.erlei.gdx.widget.GLContext;
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

        private Size mSize;

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
            return mSize == null ? 0 : mSize.getWidth();
        }

        @Override
        public int getHeight() {
            return mSize == null ? 0 : mSize.getHeight();
        }

        @Override
        public Pixmap.Format getFormat() {
            return Pixmap.Format.RGBA8888; // it's not true, but FloatTextureData.getFormat() isn't used anywhere
        }

        @Override
        public boolean useMipMaps() {
            return false;
        }

        protected void setTextureSize(Size cameraSize) {
            mSize = cameraSize;
        }
    }
}
