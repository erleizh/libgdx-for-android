package com.erlei.videorecorder.camera;

import android.graphics.SurfaceTexture;
import android.os.Looper;

import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.TextureData;
import com.erlei.gdx.utils.GdxRuntimeException;
import com.erlei.gdx.widget.GLContext;
import com.erlei.gdx.widget.IRenderView;

public class CameraTextureData implements TextureData, SurfaceTexture.OnFrameAvailableListener {

    private final CameraControl mControl;
    private Size mSize;
    private SurfaceTexture mSurfaceTexture;
    private int mTextureId;
    private IRenderView mRenderView;

    public CameraTextureData(CameraControl control) {
        mControl = control;
    }

    @Override
    public TextureDataType getType() {
        return TextureDataType.Custom;
    }

    @Override
    public boolean isPrepared() {
        return mControl.isOpen();
    }

    @Override
    public void prepare() {
        if (mControl.isOpen()) throw new GdxRuntimeException("Already prepared");
        if (mSurfaceTexture == null) return;
        mControl.open(mSurfaceTexture);
        mSize = mControl.getCameraSize();
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
        if (!GLContext.get().supportsExtension("OES_texture_float"))
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

    public void setTextureId(int textureId) {
        mTextureId = textureId;
        mRenderView = GLContext.get().getRenderView();
        if (mSurfaceTexture != null) mSurfaceTexture.release();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        prepare();
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mRenderView.requestRender();
    }
}