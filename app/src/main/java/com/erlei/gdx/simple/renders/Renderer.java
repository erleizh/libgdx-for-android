package com.erlei.gdx.simple.renders;


import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.utils.Logger;
import com.erlei.gdx.widget.BaseRender;
import com.erlei.gdx.widget.EGLCore;

public class Renderer extends BaseRender {

    Logger mLogger = new Logger("Renderer", Logger.DEBUG);
    private SpriteBatch mBatch;
    private Texture mTexture;
    private FrameBuffer mFrameBuffer;

    @Override
    public void create(EGLCore egl, GL20 gl) {
        super.create(egl, gl);
        mLogger.info("create");
        mBatch = new SpriteBatch();
        mTexture = new Texture(files.internal("593522e9ea624.png"));
    }

    @Override
    public void render(GL20 gl) {
        super.render(gl);
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
        if (mFrameBuffer != null) mFrameBuffer.dispose();
        mFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, getWidth(), getHeight(), false);
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
        super.dispose();
        mLogger.info("dispose");
        mTexture.dispose();
        mTexture = null;
        mFrameBuffer.dispose();
        mFrameBuffer = null;
        mBatch.dispose();
        mBatch = null;
    }

}