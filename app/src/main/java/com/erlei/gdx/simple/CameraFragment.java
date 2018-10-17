package com.erlei.gdx.simple;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.erlei.camera.CameraRender;
import com.erlei.camera.DefaultCameraControl;
import com.erlei.camera.Size;
import com.erlei.camera.PreviewEffectsManager;
import com.erlei.gdx.android.widget.EGLCore;
import com.erlei.gdx.android.widget.GLSurfaceView;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.utils.Logger;

public class CameraFragment extends Fragment {

    private IRenderView mRenderView;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return (View) (mRenderView = new GLSurfaceView(getContext()));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.debug("CameraFragment", "onViewCreated");
        PreviewEffectsManager effectsManager = new PreviewEffectsManager();
        effectsManager.addEffect(new CameraRender.Renderer() {
            @Override
            public void create(EGLCore egl, GL20 gl) {

            }

            @Override
            public void resize(Size viewSize, Size cameraSize) {

            }

            @Override
            public void render(GL20 gl, FrameBuffer frameBuffer) {

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
        });
        mRenderView.setRenderer(new CameraRender(mRenderView, new DefaultCameraControl(getContext()), effectsManager));
        mRenderView.setRenderMode(IRenderView.RenderMode.WHEN_DIRTY);
    }

    @Override
    public void onPause() {
        super.onPause();
        Logger.debug("CameraFragment", "onPause");
        mRenderView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.debug("CameraFragment", "onResume");
        mRenderView.onResume();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.debug("CameraFragment", "onDestroy");
        mRenderView.onDestroy();
    }

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }
}
