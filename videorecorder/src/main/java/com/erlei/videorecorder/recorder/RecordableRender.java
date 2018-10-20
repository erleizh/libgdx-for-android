package com.erlei.videorecorder.recorder;

import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.widget.BaseRender;
import com.erlei.gdx.widget.EGLCore;
import com.erlei.gdx.widget.IRenderView;


/**
 * Created by lll on 2018/10/19
 * Email : lllemail@foxmail.com
 * Describe : 一个易于记录的渲染器 ，它将数据渲染到FrameBuffer中 , 而不是直接渲染到屏幕上,
 */
public class RecordableRender extends BaseRender {

    private final Recorder mRenderer;
    protected FrameBuffer mFrameBuffer;
    protected SpriteBatch mSpriteBatch;

    /**
     * @param renderer 需要Recorder对象返回一个
     * @see Recorder
     */
    public RecordableRender(Recorder renderer) {
        mRenderer = renderer;
    }

    @Override
    public void create(EGLCore egl, GL20 gl) {
        super.create(egl, gl);
        mSpriteBatch = new SpriteBatch();
        mRenderer.create(egl, gl);
        initFrameBuffer();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mRenderer.resize(width, height);
    }

    @Override
    public void resume() {
        super.resume();
        mRenderer.resume();
    }

    @Override
    public void dispose() {
        super.dispose();
        mRenderer.dispose();
    }

    @Override
    public void render(GL20 gl) {
        super.render(gl);
        mFrameBuffer.begin();
        mRenderer.render(gl);
        mFrameBuffer.end();

        mSpriteBatch.begin();
        mSpriteBatch.draw(mFrameBuffer.getColorBufferTexture(),
                0, 0, mFrameBuffer.getColorBufferTexture().getWidth(), mFrameBuffer.getColorBufferTexture().getHeight(),
                0, 0, getWidth(), getHeight(),
                false, true);
        mSpriteBatch.end();
    }

    protected void initFrameBuffer() {
        mFrameBuffer = mRenderer.generateFrameBuffer();
        if (mFrameBuffer == null) {
            throw new IllegalStateException("generateFrameBuffer can not return null");
        }
    }


    public FrameBuffer getFrameBuffer() {
        return mFrameBuffer;
    }


    /**
     * 返回一个FrameBuffer ， 用于存储每一次渲染的帧数据
     */
    public interface Recorder extends IRenderView.Renderer {

        FrameBuffer generateFrameBuffer();

    }
}
