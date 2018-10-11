package com.erlei.gdx.simple;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.erlei.camera.Camera;
import com.erlei.camera.CameraRender;
import com.erlei.camera.Size;
import com.erlei.gdx.android.widget.GLSurfaceView;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.simple.renders.Renderer;
import com.erlei.gdx.simple.renders.ShaderMultiTextureTest;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = findViewById(R.id.surfaceView);
//        mSurfaceView.setRenderer(new Renderer( mSurfaceView));
//        mSurfaceView.setRenderer(new ShaderMultiTextureTest( mSurfaceView));
        mSurfaceView.setRenderer(new CameraRender(mSurfaceView, new CameraRender.CameraControl() {

            private Camera mCamera;

            @Override
            public Size getPreviewSize() {
                return mCamera.getPreviewSize();
            }

            @Override
            public void open(SurfaceTexture surfaceTexture) {
                mCamera = new Camera.CameraBuilder(getBaseContext()).useDefaultConfig().setSurfaceTexture(surfaceTexture).build().open();
            }

            @Override
            public void close() {
                mCamera.close();
            }
        }));
        mSurfaceView.setRenderMode(IRenderView.RenderMode.WHEN_DIRTY);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSurfaceView.onDestroy();
    }
}
