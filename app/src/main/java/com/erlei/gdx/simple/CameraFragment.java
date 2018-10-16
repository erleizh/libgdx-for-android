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
import com.erlei.gdx.android.widget.GLContext;
import com.erlei.gdx.android.widget.GLSurfaceView;
import com.erlei.gdx.android.widget.IRenderView;
import com.erlei.gdx.graphics.Color;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.g2d.BitmapFont;
import com.erlei.gdx.graphics.g2d.SpriteBatch;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.utils.Align;
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
                    mCamera = new Camera.CameraBuilder(getContext()).useDefaultConfig().setPreviewSize(new Size(2048, 1536)).setSurfaceTexture(surfaceTexture).build().open();
                }
            }

            @Override
            public void close() {
                if (mCamera != null && mCamera.isOpen()) {
                    mCamera.close();
                    mCamera = null;
                }
            }
        }, new CameraRender.Renderer() {

            private BitmapFont mBitmapFont;
            private Texture mTexture;
            private Size mViewSize;
            private SpriteBatch mSpriteBatch;

            @Override
            public void create(GL20 gl) {
                mSpriteBatch = new SpriteBatch(10);
                mBitmapFont = new BitmapFont();
                mBitmapFont.setColor(Color.WHITE);
            }

            @Override
            public void resize(Size viewSize, Size cameraSize) {
                mViewSize = viewSize;
                Pixmap pixmap = new Pixmap(cameraSize.getWidth(), cameraSize.getHeight(), Pixmap.Format.RGBA8888);
                pixmap.setColor(Color.RED);
                pixmap.drawCircle(pixmap.getWidth() / 2, pixmap.getHeight() / 2, pixmap.getWidth() / 4);
                mTexture = new Texture(pixmap);
            }

            @Override
            public void render(GL20 gl, FrameBuffer frameBuffer) {
                mSpriteBatch.begin();
                mSpriteBatch.draw(frameBuffer.getColorBufferTexture(), 0, 0, mViewSize.getWidth(), mViewSize.getHeight(), 0, 0,
                        frameBuffer.getColorBufferTexture().getWidth(),
                        frameBuffer.getColorBufferTexture().getHeight(), false, true);
                mBitmapFont.draw(mSpriteBatch, "fps : " + GLContext.getGLContext().getDeltaTime(), mViewSize.getWidth() / 2, mViewSize.getHeight() / 2,
                        0f, Align.left, true
                );
                mSpriteBatch.draw(mTexture, 0, 0, mViewSize.getWidth(), mViewSize.getHeight(), 0, 0,
                        mTexture.getWidth(), mTexture.getHeight(), false, false);
                mSpriteBatch.end();
            }

            @Override
            public void pause() {

            }

            @Override
            public void resume() {

            }

            @Override
            public void dispose() {
                mBitmapFont.dispose();
                mSpriteBatch.dispose();
                mTexture.dispose();
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
