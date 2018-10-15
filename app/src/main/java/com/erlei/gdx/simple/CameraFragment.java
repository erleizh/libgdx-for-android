package com.erlei.gdx.simple;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.erlei.camera.Camera;
import com.erlei.camera.CameraRender;
import com.erlei.camera.Size;
import com.erlei.gdx.android.widget.GLSurfaceView;
import com.erlei.gdx.android.widget.IRenderView;
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
        mRenderView.setRenderer(new CameraRender(mRenderView, new CameraRender.CameraControl() {

            private Camera mCamera;

            @Override
            public Size getPreviewSize() {
                return mCamera.getPreviewSize();
            }

            @Override
            public void open(SurfaceTexture surfaceTexture) {
                if (mCamera == null || !mCamera.isOpen()) {
                    mCamera = new Camera.CameraBuilder(getContext()).useDefaultConfig().setSurfaceTexture(surfaceTexture).build().open();
                }
            }

            @Override
            public void close() {
                if (mCamera != null && mCamera.isOpen()) {
                    mCamera.close();
                    mCamera = null;
                }
            }
        }));
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
