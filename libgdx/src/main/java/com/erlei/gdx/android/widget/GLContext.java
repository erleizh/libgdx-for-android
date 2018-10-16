package com.erlei.gdx.android.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.os.Debug;
import android.view.WindowManager;

import com.erlei.gdx.Files;
import com.erlei.gdx.LifecycleListener;
import com.erlei.gdx.Preferences;
import com.erlei.gdx.android.AndroidPreferences;
import com.erlei.gdx.files.AndroidFiles;
import com.erlei.gdx.graphics.Color;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.GL30;
import com.erlei.gdx.utils.FPSCounter;

import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;

public class GLContext implements IRenderView.Renderer {
    private static final ThreadLocal<GLContext> sThreadLocal = new ThreadLocal<>();
    protected final IRenderView mRenderView;
    protected final Context mContext;
    protected final Files files;
    protected final FPSCounter mFPSCounter;
    private String extensions;
    public GL20 gl;
    public GL30 gl30;

    public GLContext(IRenderView renderView) {
        mRenderView = renderView;
        mContext = renderView.getContext();
        files = new AndroidFiles(mContext.getAssets(), mContext.getFilesDir().getAbsolutePath());
        mFPSCounter = initFPSCounter();
    }

    protected FPSCounter initFPSCounter() {
        return new FPSCounter(new FPSCounter.FPSCounter2());
    }


    @Override
    public void create(EglHelper egl, GL20 gl) {
        setGL20(gl);
        sThreadLocal.set(this);
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void render(GL20 gl) {
        mFPSCounter.update();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {
        sThreadLocal.remove();
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

    public float getDeltaTime() {
        return mFPSCounter.getFPS();
    }


    public int getWidth() {
        return mRenderView.getSurfaceWidth();
    }

    public int getHeight() {
        return mRenderView.getSurfaceHeight();
    }

    public int getBackBufferWidth() {
        return mRenderView.getSurfaceWidth();
    }

    public int getBackBufferHeight() {
        return mRenderView.getSurfaceHeight();
    }

    public boolean supportsExtension(String extension) {
        if (extensions == null) extensions = gl.glGetString(GLES10.GL_EXTENSIONS);
        return extensions.contains(extension);
    }

    public long getJavaHeap() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    public long getNativeHeap() {
        return Debug.getNativeHeapAllocatedSize();
    }

    public Preferences getPreferences(String name) {
        return new AndroidPreferences(mContext.getSharedPreferences(name, Context.MODE_PRIVATE));
    }

    public boolean isGL30Available() {
        return gl instanceof GL30;

    }

    /**
     * @return activity is  landscape
     */
    public boolean isLandscape() {
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null && windowManager.getDefaultDisplay() != null) {
            int rotation = windowManager.getDefaultDisplay().getRotation();
            return rotation == ROTATION_90 || rotation == ROTATION_270;
        }
        return mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public void postRunnable(Runnable runnable) {
        mRenderView.queueEvent(runnable);
    }

    public Context getContext() {
        return mContext;
    }


    public IRenderView getRenderView() {
        return mRenderView;
    }

    public static GLContext getGLContext() {
        GLContext glContext = sThreadLocal.get();
        if (glContext == null) {
            throw new IllegalStateException("GLContext == null Cannot be called on a non GL thread = " + Thread.currentThread().getName());
        }
        return glContext;
    }

    public static GL20 getGL20() {
        return getGLContext().gl;
    }

    public static GL30 getGL30() {
        return getGLContext().gl30;
    }


    public static Files getFiles() {
        return getGLContext().files;
    }

    public void addLifecycleListener(LifecycleListener listener) {

    }

    public void removeLifecycleListener(LifecycleListener lifecycleListener) {
    }

    public void setGL30(GL30 gl30) {
        this.gl = gl30;
        this.gl30 = gl30;
    }

    public void setGL20(GL20 gl20) {
        this.gl = gl20;
        this.gl30 = isGL30Available() ? (GL30) gl20 : null;
    }
}
