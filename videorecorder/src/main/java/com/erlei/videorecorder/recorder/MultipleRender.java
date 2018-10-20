package com.erlei.videorecorder.recorder;

import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.glutils.FrameBuffer;
import com.erlei.gdx.widget.EGLCore;
import com.erlei.gdx.widget.IRenderView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by lll on 2018/10/19
 * Email : lllemail@foxmail.com
 * Describe : 包装多个Render
 */
public class MultipleRender implements IRenderView.Renderer, RecordableRender.Recorder {

    private List<IRenderView.Renderer> mRendererList = new ArrayList<>();
    private boolean mCreated;
    private boolean mDispose;
    private boolean mResized;
    private boolean mPaused;
    private boolean mResumed;
    private EGLCore mEgl;
    private GL20 mGl;
    private int mWidth;
    private int mHeight;

    public MultipleRender(IRenderView.Renderer... renders) {
        mRendererList.addAll(Arrays.asList(renders));
    }

    @Override
    public void create(EGLCore egl, GL20 gl) {
        mEgl = egl;
        mGl = gl;
        mCreated = true;
        for (IRenderView.Renderer renderer : mRendererList) {
            renderer.create(egl, gl);
        }
    }

    @Override
    public void resize(int width, int height) {
        mResized = true;
        mWidth = width;
        mHeight = height;
        for (IRenderView.Renderer renderer : mRendererList) {
            renderer.resize(width, height);
        }
    }

    @Override
    public void render(GL20 gl) {
        for (IRenderView.Renderer renderer : mRendererList) {
            renderer.render(gl);
        }
    }

    @Override
    public void pause() {
        mPaused = true;
        for (IRenderView.Renderer renderer : mRendererList) {
            renderer.pause();
        }
    }

    @Override
    public void resume() {
        mResumed = true;
        for (IRenderView.Renderer renderer : mRendererList) {
            renderer.resume();
        }
    }

    @Override
    public void dispose() {
        mDispose = true;
        for (IRenderView.Renderer renderer : mRendererList) {
            renderer.dispose();
        }
    }

    public int size() {
        return mRendererList.size();
    }

    public void add(IRenderView.Renderer renderer) {
        if (mCreated) {
            renderer.create(mEgl, mGl);
        }
        if (mResized) {
            renderer.resize(mWidth, mHeight);
        }
        if (mPaused) {
            renderer.pause();
        }
        mRendererList.add(renderer);
    }

    public boolean contains(IRenderView.Renderer renderer) {
        return mRendererList.contains(renderer);
    }

    public void remove(IRenderView.Renderer renderer) {
        boolean remove = mRendererList.remove(renderer);
        if (remove && !mDispose) renderer.dispose();

    }

    public void remove(int index) {
        if (index >= mRendererList.size()) return;
        IRenderView.Renderer renderer = mRendererList.remove(index);
        if (!mDispose) renderer.dispose();
    }


    @Override
    public FrameBuffer generateFrameBuffer() {
        for (IRenderView.Renderer renderer : mRendererList) {
            if (renderer instanceof RecordableRender.Recorder) {
                return ((RecordableRender.Recorder) renderer).generateFrameBuffer();
            }
        }
        return null;
    }
}
