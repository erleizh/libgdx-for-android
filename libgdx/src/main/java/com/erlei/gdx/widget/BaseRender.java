package com.erlei.gdx.widget;

import android.opengl.GLES20;

import com.erlei.gdx.Files;
import com.erlei.gdx.graphics.Color;
import com.erlei.gdx.graphics.GL20;

public abstract class BaseRender implements IRenderView.Renderer {

    protected Files files;
    protected GL20 gl;
    protected IRenderView mRenderView;

    @Override
    public void create(EGLCore egl, GL20 gl) {
        this.gl = gl;
        files = GLContext.getFiles();
        mRenderView = GLContext.getGLContext().getRenderView();
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void render(GL20 gl) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {

    }

    public int getWidth() {
        return mRenderView.getSurfaceWidth();
    }

    public int getHeight() {
        return mRenderView.getSurfaceHeight();
    }

    /**
     * 使用黑色清除屏幕
     */
    protected void clearColor(Color color) {
        gl.glClearColor(color.r, color.g, color.b, color.a);
    }

    /**
     * 使用黑色清除屏幕
     */
    protected void clearColor() {
        clearColor(Color.BLACK);
    }

    /**
     * 使用黑色清除屏幕
     */
    protected void clear() {
        clearColor();
        clearBuffers();
    }


    /**
     * 清除颜色缓冲区，深度缓冲区
     */
    protected void clearBuffers() {
        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * 清除颜色缓冲区
     */
    protected void clearColorBuffer() {
        gl.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

    /**
     * 清除深度缓冲区
     */
    protected void clearDepthBuffer() {
        gl.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
    }
}
