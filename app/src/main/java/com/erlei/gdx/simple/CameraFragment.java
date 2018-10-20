package com.erlei.gdx.simple;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.erlei.gdx.utils.Logger;
import com.erlei.gdx.widget.GLSurfaceView;
import com.erlei.gdx.widget.IRenderView;
import com.erlei.videorecorder.camera.CameraRender;
import com.erlei.videorecorder.camera.DefaultCameraControl;
import com.erlei.videorecorder.camera.Size;
import com.erlei.videorecorder.recorder.MultipleRender;
import com.erlei.videorecorder.recorder.VideoRecorder;
import com.erlei.videorecorder.recorder.VideoRecorderHandler;

import java.io.File;

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

//        mRenderView.setRenderer(new RecordableRender(new MultipleRender(new CameraRender(new DefaultCameraControl(mRenderView)))));
        mRenderView.setRenderer(new MultipleRender(new CameraRender(new DefaultCameraControl(mRenderView))));
        mRenderView.setRenderMode(IRenderView.RenderMode.WHEN_DIRTY);
    }

    private IRenderView.Renderer initVideoRecorder() {
        VideoRecorder.Builder builder = new VideoRecorder.Builder(getContext())
                .setCallbackHandler(new CallbackHandler())
                .setOutPutDir(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), File.separator + "VideoRecorder").getAbsolutePath())
                .setFrameRate(30)
                .setVideoSize(new Size(1920, 1080))
                .setChannelCount(1);
        return builder.build();
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

    private class CallbackHandler extends VideoRecorderHandler {
    }
}
