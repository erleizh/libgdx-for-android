package com.erlei.camera;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;
import com.erlei.gdx.android.widget.IRenderView;

/**
 * Created by lll on 2018/10/8
 * Email : lllemail@foxmail.com
 * Describe : 使用相机的数据作为纹理渲染到renderView
 */
public class CameraRender extends Gdx {

    private final CameraControl mControl;

    public CameraRender(IRenderView renderView, CameraControl cameraControl) {
        super(renderView);
        mControl = cameraControl;
        renderView.setRenderer(this);
        renderView.setRenderMode(IRenderView.RenderMode.WHEN_DIRTY);
    }

    @Override
    public void create(EglCore egl, EglSurfaceBase eglSurface) {
        super.create(egl, eglSurface);
    }

    @Override
    public void resume() {
        super.resume();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();

    }

    interface CameraControl {

        void open(IRenderView renderView);

        void close();

    }
}
