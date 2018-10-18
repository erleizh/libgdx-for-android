package com.erlei.videorecorder.camera;

import com.erlei.gdx.widget.EGLCore;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.graphics.glutils.FrameBuffer;

import java.util.ArrayList;
import java.util.List;

public class PreviewEffectsManager implements CameraRender.Renderer {

    private Size mViewSize;
    private SpriteBatch mSpriteBatch;
    private List<CameraRender.Renderer> mEffects = new ArrayList<>();

    @Override
    public void create(EGLCore egl, final GL20 gl) {
        mSpriteBatch = new SpriteBatch(10);
        forEach(renderer -> renderer.create(egl, gl));
    }


    public void addEffect(CameraRender.Renderer renderer) {
        mEffects.add(renderer);
    }

    public boolean removeEffect(CameraRender.Renderer renderer) {
        return mEffects.remove(renderer);
    }

    @Override
    public void resize(Size viewSize, Size cameraSize) {
        mViewSize = viewSize;
        forEach(renderer -> renderer.resize(viewSize, cameraSize));
    }

    @Override
    public void render(GL20 gl, FrameBuffer frameBuffer) {
        forEach(renderer -> renderer.render(gl, frameBuffer));
        mSpriteBatch.begin();
        mSpriteBatch.draw(frameBuffer.getColorBufferTexture(), 0, 0, mViewSize.getWidth(), mViewSize.getHeight(), 0, 0,
                frameBuffer.getColorBufferTexture().getWidth(),
                frameBuffer.getColorBufferTexture().getHeight(), false, true);
        mSpriteBatch.end();
    }

    @Override
    public void pause() {
        forEach(CameraRender.Renderer::pause);
    }

    @Override
    public void resume() {
        forEach(CameraRender.Renderer::resume);
    }

    @Override
    public void dispose() {
        forEach(CameraRender.Renderer::dispose);
        mSpriteBatch.dispose();
        mEffects.clear();
    }


    private interface Consumer<T> {
        void accept(T t);
    }

    private void forEach(Consumer<CameraRender.Renderer> consumer) {
        for (CameraRender.Renderer renderer : mEffects) {
            consumer.accept(renderer);
        }
    }

}
