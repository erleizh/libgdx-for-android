package com.erlei.gdx.simple.renders;


import com.erlei.gdx.Gdx;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.utils.Logger;

public class Renderer extends Gdx {

    Logger mLogger = new Logger("Renderer", Logger.DEBUG);
    private SpriteBatch mBatch;
    private Texture mTexture;
    private FrameBuffer mFrameBuffer;

    public Renderer(IRenderView renderView) {
        super(renderView);
    }


    @Override
    public void create(EglCore egl, EglSurfaceBase eglSurface) {
        super.create(egl, eglSurface);
        mLogger.info("create");
        mFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, getWidth(), getHeight(), false);
        mBatch = new SpriteBatch();
        mTexture = new Texture(files.internal("593522e9ea624.png"));
    }

    @Override
    public void render() {
        super.render();
        mLogger.info("render");

        clear();

        //绘制到frameBuffer
        mFrameBuffer.begin();
        mBatch.begin();
        mBatch.draw(mTexture, 0, 0, getWidth(), getHeight());
        mBatch.end();
        mFrameBuffer.end();

        //绘制到屏幕
        mBatch.begin();
        mBatch.draw(mFrameBuffer.getColorBufferTexture(), 0, 0, getWidth(), getHeight(), 0, 0,
                mFrameBuffer.getColorBufferTexture().getWidth(),
                mFrameBuffer.getColorBufferTexture().getHeight(), false, true);
        mBatch.end();

    }


    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mLogger.info("resize = " + width + "*" + height);
    }

    @Override
    public void resume() {
        super.resume();
        mLogger.info("resume");
    }

    @Override
    public void pause() {
        super.pause();
        mLogger.info("pause");
    }

    @Override
    public void dispose() {
        //before super.dispose();
        mLogger.info("dispose");
        mTexture.dispose();
        mBatch.dispose();
        super.dispose();
    }

}