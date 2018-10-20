package com.erlei.gdx.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.os.Debug;
import android.view.WindowManager;

import com.erlei.gdx.AndroidPreferences;
import com.erlei.gdx.Files;
import com.erlei.gdx.LifecycleListener;
import com.erlei.gdx.Preferences;
import com.erlei.gdx.files.AndroidFiles;
import com.erlei.gdx.graphics.Color;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.GL30;
import com.erlei.gdx.utils.FPSCounter;
import com.erlei.gdx.utils.SnapshotArray;

import java.lang.ref.WeakReference;

import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;

public final class GLContext {
    private static final ThreadLocal<GLContext> sThreadLocal = new ThreadLocal<>();
    protected final WeakReference<IRenderView> mReference;
    protected final Context mContext;
    protected final Files files;
    protected final FPSCounter mFPSCounter;
    private final SnapshotArray<LifecycleListener> mListeners = new SnapshotArray<>();
    private String extensions;
    public GL20 gl;
    public GL30 gl30;

    public GLContext(WeakReference<IRenderView> reference) {
        mReference = reference;
        if (reference == null || reference.get() == null)
            throw new IllegalArgumentException("IRenderView can not be null");
        mContext = reference.get().getContext();
        files = new AndroidFiles(mContext.getAssets(), mContext.getFilesDir().getAbsolutePath());
        mFPSCounter = initFPSCounter();
    }

    protected FPSCounter initFPSCounter() {
        return new FPSCounter(new FPSCounter.FPSCounter2());
    }


    void create(EGLCore egl, GL20 gl) {
        setGL20(gl);
        sThreadLocal.set(this);
    }

    void render(GL20 gl) {
        mFPSCounter.update();
    }

    void pause() {

    }

    void resume() {

    }

    void dispose() {
        sThreadLocal.remove();
    }



    public float getDeltaTime() {
        return mFPSCounter.getFPS();
    }


    public int getWidth() {
        return getRenderView().getSurfaceWidth();
    }

    public int getHeight() {
        return getRenderView().getSurfaceHeight();
    }

    public int getBackBufferWidth() {
        return getRenderView().getSurfaceWidth();
    }

    public int getBackBufferHeight() {
        return getRenderView().getSurfaceHeight();
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
        IRenderView iRenderView = mReference.get();
        if (iRenderView != null) {
            iRenderView.queueEvent(runnable);
        }
    }

    public Context getContext() {
        return mContext;
    }


    public IRenderView getRenderView() {
        return mReference.get();
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
        synchronized (mListeners) {
            mListeners.add(listener);
        }
    }

    public void removeLifecycleListener(LifecycleListener lifecycleListener) {
        synchronized (mListeners) {
            mListeners.removeValue(lifecycleListener, true);
        }
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
