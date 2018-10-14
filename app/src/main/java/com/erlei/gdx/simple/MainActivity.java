package com.erlei.gdx.simple;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.erlei.camera.Camera;
import com.erlei.camera.CameraRender;
import com.erlei.camera.Size;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.utils.Logger;

public class MainActivity extends AppCompatActivity {

    private IRenderView mRenderView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRenderView = findViewById(R.id.renderView);
//        mRenderView.setRenderer(new Renderer(mRenderView));
//        mRenderView.setRenderer(new ShaderMultiTextureTest(mRenderView));
        mRenderView.setRenderer(new CameraRender(mRenderView, new CameraRender.CameraControl() {

            private Camera mCamera;

            @Override
            public Size getPreviewSize() {
                return mCamera.getPreviewSize();
            }

            @Override
            public void open(SurfaceTexture surfaceTexture) {
                if (mCamera == null || !mCamera.isOpen()) {
                    mCamera = new Camera.CameraBuilder(getApplicationContext()).useDefaultConfig().setSurfaceTexture(surfaceTexture).build().open();
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
    protected void onPause() {
        super.onPause();
        Logger.debug("Activity", "onPause");
        mRenderView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.debug("Activity", "onResume");
        mRenderView.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.debug("Activity", "onDestroy");
        mRenderView.onDestroy();
    }
}
