package com.erlei.gdx.android.widget.trash;


import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.text.TextUtils;
import android.util.Log;

import com.erlei.gdx.android.EglCore;
import com.erlei.gdx.android.EglSurfaceBase;

import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

public interface IRenderView {

    Context getContext();

    enum RenderMode {
        CONTINUOUSLY("CONTINUOUSLY"), WHEN_DIRTY("WHEN_DIRTY");

        private final String name;

        RenderMode(String name) {
            this.name = name;
        }

        static RenderMode from(String name) {
            for (RenderMode f : values()) {
                if (TextUtils.equals(f.name, name)) return f;
            }
            throw new IllegalArgumentException();
        }
    }

    enum ViewType {

        SurfaceView("SurfaceView"), TextureView("TextureView");

        private final String name;

        ViewType(String name) {
            this.name = name;
        }

        static ViewType from(String name) {
            for (ViewType f : values()) {
                if (TextUtils.equals(f.name, name)) return f;
            }
            throw new IllegalArgumentException();
        }
    }


    interface SurfaceSizeChangeListener {
        /**
         * @param w - The new width of the surface.
         * @param h - The new height of the surface.
         */
        void onSizeChanged(int w, int h);
    }

    void addSurfaceSizeChangeListener(SurfaceSizeChangeListener listener);

    void removeSurfaceSizeChangeListener(SurfaceSizeChangeListener listener);


    /**
     * Get the view type
     *
     * @return the view type
     * @see ViewType#TextureView
     * @see ViewType#SurfaceView
     */
    ViewType getViewType();

    /**
     * Get the current rendering mode. May be called
     * from any thread. Must not be called before a renderer has been set.
     *
     * @return the current rendering mode.
     * @see RenderMode#WHEN_DIRTY
     * @see RenderMode#CONTINUOUSLY
     */
    RenderMode getRenderMode();

    /**
     * Must not be called before a Renderer#create(EglCore, EglSurfaceBase) has been set.
     *
     * @return GLES version code
     */
    int getGLESVersion();

    /**
     * Request that the renderer render a frame.
     * This method is typically used when the render mode has been set to
     * {@link RenderMode#WHEN_DIRTY}, so that frames are only rendered on demand.
     * May be called
     * from any thread. Must not be called before a renderer has been set.
     */
    void requestRender();


    /**
     * Set the rendering mode. When renderMode is
     * RenderMode#CONTINUOUSLY, the renderer is called
     * repeatedly to re-render the scene. When renderMode
     * is RenderMode#WHEN_DIRTY, the renderer only rendered when the surface
     * is created, or when {@link #requestRender} is called. Defaults to RenderMode#CONTINUOUSLY.
     * <p>
     * Using RenderMode#WHEN_DIRTY can improve battery life and overall system performance
     * by allowing the GPU and CPU to idle when the view does not need to be updated.
     * <p>
     * This method can only be called after {@link #setRenderer(IRenderView.Renderer)}
     *
     * @param renderMode one of the RenderMode enum
     * @see RenderMode#CONTINUOUSLY
     * @see RenderMode#WHEN_DIRTY
     */
    void setRenderMode(RenderMode renderMode);


    /**
     * Set the renderer associated with this view. Also starts the thread that
     * will call the renderer, which in turn causes the rendering to start.
     * <p>This method should be called once and only once in the life-cycle of
     * a GLSurfaceView.
     * <p>The following IRenderView methods can only be called <em>before</em>
     * setRenderer is called:
     * <p>
     * The following GLSurfaceView methods can only be called <em>after</em>
     * setRenderer is called:
     * <ul>
     * <li>{@link #getRenderMode()}
     * <li>{@link #onPause()}
     * <li>{@link #onResume()}
     * <li>{@link #queueEvent(Runnable)}
     * <li>{@link #requestRender()}
     * <li>{@link #setRenderMode(RenderMode)}}
     * </ul>
     *
     * @param renderer the renderer to use to perform OpenGL drawing.
     */
    void setRenderer(Renderer renderer);

    /**
     * Pause the rendering thread, optionally tearing down the EGL context
     * depending upon the value of {@link #setPreserveEGLContextOnPause(boolean)}.
     * <p>
     * This method should be called when it is no longer desirable for the
     * IRenderView to continue rendering, such as in response to
     * {@link android.app.Activity#onStop Activity.onStop}.
     * <p>
     * Must not be called before a renderer has been set.
     */
    void onPause();

    /**
     * Resumes the rendering thread, re-creating the OpenGL context if necessary. It
     * is the counterpart to {@link #onPause()}.
     * <p>
     * This method should typically be called in
     * {@link android.app.Activity#onStart Activity.onStart}.
     * <p>
     * Must not be called before a renderer has been set.
     */
    void onResume();


    void onDestroy();

    /**
     * Queue a runnable to be run on the GL rendering thread. This can be used
     * to communicate with the Renderer on the rendering thread.
     * Must not be called before a renderer has been set.
     *
     * @param r the runnable to be run on the GL rendering thread.
     */
    void queueEvent(Runnable r);


    int getSurfaceWidth();

    int getSurfaceHeight();


    Renderer getRenderer();


    /**
     * Control whether the EGL context is preserved when the IRenderView is paused and
     * resumed.
     * <p>
     * If set to true, then the EGL context may be preserved when the IRenderView is paused.
     * <p>
     * Prior to API level 11, whether the EGL context is actually preserved or not
     * depends upon whether the Android device can support an arbitrary number of
     * EGL contexts or not. Devices that can only support a limited number of EGL
     * contexts must release the EGL context in order to allow multiple applications
     * to share the GPU.
     * <p>
     * If set to false, the EGL context will be released when the IRenderView is paused,
     * and recreated when the IRenderView is resumed.
     * <p>
     * <p>
     * The default is true.
     *
     * @param preserveOnPause preserve the EGL context when paused
     */
    void setPreserveEGLContextOnPause(boolean preserveOnPause);

    /**
     * @return true if the EGL context will be preserved when paused
     */
    boolean getPreserveEGLContextOnPause();

    Object getSurface();

    interface Renderer {

        void create(EglCore egl, EglSurfaceBase eglSurface);

        void resize(int width, int height);

        void render();

        void pause();

        void resume();

        void dispose();

    }

    /**
     * An interface for customizing the eglCreateContext and eglDestroyContext calls.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link android.opengl.GLSurfaceView#setEGLContextFactory(android.opengl.GLSurfaceView.EGLContextFactory)}
     */
    public interface EGLContextFactory {
        EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig);

        void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context);
    }

    public class DefaultContextFactory implements android.opengl.GLSurfaceView.EGLContextFactory {
        private static final boolean LOG_THREADS = true;
        private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        private int mEGLContextClientVersion = 3;

        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
                    EGL10.EGL_NONE};

            return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT,
                    mEGLContextClientVersion != 0 ? attrib_list : null);
        }

        public void destroyContext(EGL10 egl, EGLDisplay display,
                                   EGLContext context) {
            if (!egl.eglDestroyContext(display, context)) {
                Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
                if (LOG_THREADS) {
                    Log.i("DefaultContextFactory", "tid=" + Thread.currentThread().getId());
                }
                EglHelper.throwEglException("eglDestroyContex", egl.eglGetError());
            }
        }
    }

    /**
     * An interface for customizing the eglCreateWindowSurface and eglDestroySurface calls.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link android.opengl.GLSurfaceView#setEGLWindowSurfaceFactory(android.opengl.GLSurfaceView.EGLWindowSurfaceFactory)}
     */
    public interface EGLWindowSurfaceFactory {
        /**
         * @return null if the surface cannot be constructed.
         */
        EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config,
                                       Object nativeWindow);

        void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface);
    }

    class DefaultWindowSurfaceFactory implements android.opengl.GLSurfaceView.EGLWindowSurfaceFactory {

        private static final String TAG = "WindowSurfaceFactory";

        public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display,
                                              EGLConfig config, Object nativeWindow) {
            EGLSurface result = null;
            try {
                result = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
            } catch (IllegalArgumentException e) {
                // This exception indicates that the surface flinger surface
                // is not valid. This can happen if the surface flinger surface has
                // been torn down, but the application has not yet been
                // notified via SurfaceHolder.Callback.surfaceDestroyed.
                // In theory the application should be notified first,
                // but in practice sometimes it is not. See b/4588890
                Log.e(TAG, "eglCreateWindowSurface", e);
            }
            return result;
        }

        public void destroySurface(EGL10 egl, EGLDisplay display,
                                   EGLSurface surface) {
            egl.eglDestroySurface(display, surface);
        }
    }

    /**
     * An interface for choosing an EGLConfig configuration from a list of
     * potential configurations.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link android.opengl.GLSurfaceView#setEGLConfigChooser(android.opengl.GLSurfaceView.EGLConfigChooser)}
     */
    public interface EGLConfigChooser {
        /**
         * Choose a configuration from the list. Implementors typically
         * implement this method by calling
         * {@link EGL10#eglChooseConfig} and iterating through the results. Please consult the
         * EGL specification available from The Khronos Group to learn how to call eglChooseConfig.
         *
         * @param egl     the EGL10 for the current display.
         * @param display the current display.
         * @return the chosen configuration.
         */
        EGLConfig chooseConfig(EGL10 egl, EGLDisplay display);
    }

    public abstract class BaseConfigChooser
            implements android.opengl.GLSurfaceView.EGLConfigChooser {
        private int mEGLContextClientVersion;

        public BaseConfigChooser(int[] configSpec) {
            mConfigSpec = filterConfigSpec(configSpec);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            if (!egl.eglChooseConfig(display, mConfigSpec, null, 0,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException(
                        "No configs match configSpec");
            }

            EGLConfig[] configs = new EGLConfig[numConfigs];
            if (!egl.eglChooseConfig(display, mConfigSpec, configs, numConfigs,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                throw new IllegalArgumentException("No config chosen");
            }
            return config;
        }

        abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                                        EGLConfig[] configs);

        protected int[] mConfigSpec;

        private int[] filterConfigSpec(int[] configSpec) {
            if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) {
                return configSpec;
            }
            /* We know none of the subclasses define EGL_RENDERABLE_TYPE.
             * And we know the configSpec is well formed.
             */
            int len = configSpec.length;
            int[] newConfigSpec = new int[len + 2];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
            newConfigSpec[len - 1] = EGL10.EGL_RENDERABLE_TYPE;
            if (mEGLContextClientVersion == 2) {
                newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT;  /* EGL_OPENGL_ES2_BIT */
            } else {
                newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR; /* EGL_OPENGL_ES3_BIT_KHR */
            }
            newConfigSpec[len + 1] = EGL10.EGL_NONE;
            return newConfigSpec;
        }
    }

    /**
     * Choose a configuration with exactly the specified r,g,b,a sizes,
     * and at least the specified depth and stencil sizes.
     */
    public class ComponentSizeChooser extends BaseConfigChooser {
        public ComponentSizeChooser(int redSize, int greenSize, int blueSize,
                                    int alphaSize, int depthSize, int stencilSize) {
            super(new int[]{
                    EGL10.EGL_RED_SIZE, redSize,
                    EGL10.EGL_GREEN_SIZE, greenSize,
                    EGL10.EGL_BLUE_SIZE, blueSize,
                    EGL10.EGL_ALPHA_SIZE, alphaSize,
                    EGL10.EGL_DEPTH_SIZE, depthSize,
                    EGL10.EGL_STENCIL_SIZE, stencilSize,
                    EGL10.EGL_NONE});
            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                                      EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config,
                        EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl, display, config,
                        EGL10.EGL_STENCIL_SIZE, 0);
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    int r = findConfigAttrib(egl, display, config,
                            EGL10.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(egl, display, config,
                            EGL10.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(egl, display, config,
                            EGL10.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(egl, display, config,
                            EGL10.EGL_ALPHA_SIZE, 0);
                    if ((r == mRedSize) && (g == mGreenSize)
                            && (b == mBlueSize) && (a == mAlphaSize)) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display,
                                     EGLConfig config, int attribute, int defaultValue) {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }

        private int[] mValue;
        // Subclasses can adjust these values:
        protected int mRedSize;
        protected int mGreenSize;
        protected int mBlueSize;
        protected int mAlphaSize;
        protected int mDepthSize;
        protected int mStencilSize;
    }

    /**
     * This class will choose a RGB_888 surface with
     * or without a depth buffer.
     */
    public class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser(boolean withDepthBuffer) {
            super(8, 8, 8, 0, withDepthBuffer ? 16 : 0, 0);
        }
    }

    /**
     * An EGL helper class.
     */

    public static class EglHelper {
        private static final boolean LOG_EGL = true;
        private static final boolean LOG_THREADS = true;
        private DefaultContextFactory mEGLContextFactory = new DefaultContextFactory();
        private BaseConfigChooser mEGLConfigChooser = new SimpleEGLConfigChooser(false);
        private DefaultWindowSurfaceFactory mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
        ;

        public EglHelper(WeakReference<IRenderView> glSurfaceViewWeakRef) {
            mGLSurfaceViewWeakRef = glSurfaceViewWeakRef;
        }

        /**
         * Initialize EGL for a given configuration spec.
         */
        public void start() {
            if (LOG_EGL) {
                Log.w("EglHelper", "start() tid=" + Thread.currentThread().getId());
            }
            /*
             * Get an EGL instance
             */
            mEgl = (EGL10) EGLContext.getEGL();

            /*
             * Get to the default display.
             */
            mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

            if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed");
            }

            /*
             * We can now initialize EGL for that display
             */
            int[] version = new int[2];
            if (!mEgl.eglInitialize(mEglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed");
            }
            mEglConfig = mEGLConfigChooser.chooseConfig(mEgl, mEglDisplay);

            /*
             * Create an EGL context. We want to do this as rarely as we can, because an
             * EGL context is a somewhat heavy object.
             */
            mEglContext = mEGLContextFactory.createContext(mEgl, mEglDisplay, mEglConfig);
            if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT) {
                mEglContext = null;
                throwEglException("createContext");
            }
            if (LOG_EGL) {
                Log.w("EglHelper", "createContext " + mEglContext + " tid=" + Thread.currentThread().getId());
            }

            mEglSurface = null;
        }

        /**
         * Create an egl surface for the current SurfaceHolder surface. If a surface
         * already exists, destroy it before creating the new surface.
         *
         * @return true if the surface was created successfully.
         */
        public boolean createSurface() {
            if (LOG_EGL) {
                Log.w("EglHelper", "createSurface()  tid=" + Thread.currentThread().getId());
            }
            /*
             * Check preconditions.
             */
            if (mEgl == null) {
                throw new RuntimeException("egl not initialized");
            }
            if (mEglDisplay == null) {
                throw new RuntimeException("eglDisplay not initialized");
            }
            if (mEglConfig == null) {
                throw new RuntimeException("mEglConfig not initialized");
            }

            /*
             *  The window size has changed, so we need to create a new
             *  surface.
             */
            destroySurfaceImp();

            /*
             * Create an EGL surface we can render into.
             */
            IRenderView view = mGLSurfaceViewWeakRef.get();
            mEglSurface = mEGLWindowSurfaceFactory.createWindowSurface(mEgl,
                    mEglDisplay, mEglConfig, view.getSurface());

            if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
                int error = mEgl.eglGetError();
                if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                    Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                }
                return false;
            }

            /*
             * Before we can issue GL commands, we need to make sure
             * the context is current and bound to a surface.
             */
            if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                /*
                 * Could not make the context current, probably because the underlying
                 * SurfaceView surface has been destroyed.
                 */
                logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", mEgl.eglGetError());
                return false;
            }

            return true;
        }

        /**
         * Display the current render surface.
         *
         * @return the EGL error code from eglSwapBuffers.
         */
        public int swap() {
            if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                return mEgl.eglGetError();
            }
            return EGL10.EGL_SUCCESS;
        }

        public void destroySurface() {
            if (LOG_EGL) {
                Log.w("EglHelper", "destroySurface()  tid=" + Thread.currentThread().getId());
            }
            destroySurfaceImp();
        }

        private void destroySurfaceImp() {
            if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {
                mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT);
                mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
                mEglSurface = null;
            }
        }

        public void finish() {
            if (LOG_EGL) {
                Log.w("EglHelper", "finish() tid=" + Thread.currentThread().getId());
            }
            if (mEglContext != null) {
                mEGLContextFactory.destroyContext(mEgl, mEglDisplay, mEglContext);
                mEglContext = null;
            }
            if (mEglDisplay != null) {
                mEgl.eglTerminate(mEglDisplay);
                mEglDisplay = null;
            }
        }

        private void throwEglException(String function) {
            throwEglException(function, mEgl.eglGetError());
        }

        public static void throwEglException(String function, int error) {
            String message = formatEglError(function, error);
            if (LOG_THREADS) {
                Log.e("EglHelper", "throwEglException tid=" + Thread.currentThread().getId() + " "
                        + message);
            }
            throw new RuntimeException(message);
        }

        public static void logEglErrorAsWarning(String tag, String function, int error) {
            Log.w(tag, formatEglError(function, error));

        }

        public static String formatEglError(String function, int error) {
            return function + " failed: " + error;
        }

        private WeakReference<IRenderView> mGLSurfaceViewWeakRef;
        EGL10 mEgl;
        EGLDisplay mEglDisplay;
        EGLSurface mEglSurface;
        EGLConfig mEglConfig;
        EGLContext mEglContext;

    }

    /**
     * A generic GL Thread. Takes care of initializing EGL and GL. Delegates
     * to a Renderer instance to do the actual drawing. Can be configured to
     * render continuously or on request.
     * <p>
     * All potentially blocking synchronization is done through the
     * sGLThreadManager object. This avoids multiple-lock ordering issues.
     */
    static class GLThread extends Thread {
        private static final boolean LOG_THREADS = true;
        private static final boolean LOG_PAUSE_RESUME = true;
        private static final boolean LOG_SURFACE = true;
        private static final boolean LOG_RENDERER = true;
        private static final String TAG = "GLThread";

        GLThread(IRenderView renderView) {
            super();
            mWidth = 0;
            mHeight = 0;
            mRequestRender = true;
            mRenderMode = RenderMode.CONTINUOUSLY;
            mWantRenderNotification = false;
            mGLSurfaceViewWeakRef = new WeakReference<>(renderView);
        }

        @Override
        public void run() {
            setName("GLThread " + getId());
            if (LOG_THREADS) {
                Log.i("GLThread", "starting tid=" + getId());
            }

            try {
                guardedRun();
            } catch (InterruptedException e) {
                // fall thru and exit normally
            } finally {
                sGLThreadManager.threadExiting(this);
            }
        }

        /*
         * This private method should only be called inside a
         * synchronized(sGLThreadManager) block.
         */
        private void stopEglSurfaceLocked() {
            if (mHaveEglSurface) {
                mHaveEglSurface = false;
                mEglHelper.destroySurface();
            }
        }

        /*
         * This private method should only be called inside a
         * synchronized(sGLThreadManager) block.
         */
        private void stopEglContextLocked() {
            if (mHaveEglContext) {
                mEglHelper.finish();
                mHaveEglContext = false;
                sGLThreadManager.releaseEglContextLocked(this);
            }
        }

        private void guardedRun() throws InterruptedException {
            mEglHelper = new EglHelper(mGLSurfaceViewWeakRef);
            mHaveEglContext = false;
            mHaveEglSurface = false;
            mWantRenderNotification = false;

            try {
                GL10 gl = null;
                boolean createEglContext = false;
                boolean createEglSurface = false;
                boolean createGlInterface = false;
                boolean lostEglContext = false;
                boolean sizeChanged = false;
                boolean wantRenderNotification = false;
                boolean doRenderNotification = false;
                boolean askedToReleaseEglContext = false;
                int w = 0;
                int h = 0;
                Runnable event = null;
                Runnable finishDrawingRunnable = null;

                while (true) {
                    synchronized (sGLThreadManager) {
                        while (true) {
                            if (mShouldExit) {
                                return;
                            }

                            if (!mEventQueue.isEmpty()) {
                                event = mEventQueue.remove(0);
                                break;
                            }

                            // Update the pause state.
                            boolean pausing = false;
                            if (mPaused != mRequestPaused) {
                                pausing = mRequestPaused;
                                mPaused = mRequestPaused;
                                sGLThreadManager.notifyAll();
                                if (LOG_PAUSE_RESUME) {
                                    Log.i("GLThread", "mPaused is now " + mPaused + " tid=" + getId());
                                }
                            }

                            // Do we need to give up the EGL context?
                            if (mShouldReleaseEglContext) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "releasing EGL context because asked to tid=" + getId());
                                }
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                mShouldReleaseEglContext = false;
                                askedToReleaseEglContext = true;
                            }

                            // Have we lost the EGL context?
                            if (lostEglContext) {
                                stopEglSurfaceLocked();
                                stopEglContextLocked();
                                lostEglContext = false;
                            }

                            // When pausing, release the EGL surface:
                            if (pausing && mHaveEglSurface) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "releasing EGL surface because paused tid=" + getId());
                                }
                                stopEglSurfaceLocked();
                            }

                            // When pausing, optionally release the EGL Context:
                            if (pausing && mHaveEglContext) {
                                IRenderView view = mGLSurfaceViewWeakRef.get();
                                boolean preserveEglContextOnPause = view == null ?
                                        false : view.getPreserveEGLContextOnPause();
                                if (!preserveEglContextOnPause) {
                                    stopEglContextLocked();
                                    if (LOG_SURFACE) {
                                        Log.i("GLThread", "releasing EGL context because paused tid=" + getId());
                                    }
                                }
                            }

                            // Have we lost the SurfaceView surface?
                            if ((!mHasSurface) && (!mWaitingForSurface)) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "noticed surfaceView surface lost tid=" + getId());
                                }
                                if (mHaveEglSurface) {
                                    stopEglSurfaceLocked();
                                }
                                mWaitingForSurface = true;
                                mSurfaceIsBad = false;
                                sGLThreadManager.notifyAll();
                            }

                            // Have we acquired the surface view surface?
                            if (mHasSurface && mWaitingForSurface) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "noticed surfaceView surface acquired tid=" + getId());
                                }
                                mWaitingForSurface = false;
                                sGLThreadManager.notifyAll();
                            }

                            if (doRenderNotification) {
                                if (LOG_SURFACE) {
                                    Log.i("GLThread", "sending render notification tid=" + getId());
                                }
                                mWantRenderNotification = false;
                                doRenderNotification = false;
                                mRenderComplete = true;
                                sGLThreadManager.notifyAll();
                            }

                            if (mFinishDrawingRunnable != null) {
                                finishDrawingRunnable = mFinishDrawingRunnable;
                                mFinishDrawingRunnable = null;
                            }

                            // Ready to draw?
                            if (readyToDraw()) {

                                // If we don't have an EGL context, try to acquire one.
                                if (!mHaveEglContext) {
                                    if (askedToReleaseEglContext) {
                                        askedToReleaseEglContext = false;
                                    } else {
                                        try {
                                            mEglHelper.start();
                                        } catch (RuntimeException t) {
                                            sGLThreadManager.releaseEglContextLocked(this);
                                            throw t;
                                        }
                                        mHaveEglContext = true;
                                        createEglContext = true;

                                        sGLThreadManager.notifyAll();
                                    }
                                }

                                if (mHaveEglContext && !mHaveEglSurface) {
                                    mHaveEglSurface = true;
                                    createEglSurface = true;
                                    createGlInterface = true;
                                    sizeChanged = true;
                                }

                                if (mHaveEglSurface) {
                                    if (mSizeChanged) {
                                        sizeChanged = true;
                                        w = mWidth;
                                        h = mHeight;
                                        mWantRenderNotification = true;
                                        if (LOG_SURFACE) {
                                            Log.i("GLThread",
                                                    "noticing that we want render notification tid="
                                                            + getId());
                                        }

                                        // Destroy and recreate the EGL surface.
                                        createEglSurface = true;

                                        mSizeChanged = false;
                                    }
                                    mRequestRender = false;
                                    sGLThreadManager.notifyAll();
                                    if (mWantRenderNotification) {
                                        wantRenderNotification = true;
                                    }
                                    break;
                                }
                            } else {
                                if (finishDrawingRunnable != null) {
                                    Log.w(TAG, "Warning, !readyToDraw() but waiting for " +
                                            "draw finished! Early reporting draw finished.");
                                    finishDrawingRunnable.run();
                                    finishDrawingRunnable = null;
                                }
                            }
                            // By design, this is the only place in a GLThread thread where we wait().
                            if (LOG_THREADS) {
                                Log.i("GLThread", "waiting tid=" + getId()
                                        + " mHaveEglContext: " + mHaveEglContext
                                        + " mHaveEglSurface: " + mHaveEglSurface
                                        + " mFinishedCreatingEglSurface: " + mFinishedCreatingEglSurface
                                        + " mPaused: " + mPaused
                                        + " mHasSurface: " + mHasSurface
                                        + " mSurfaceIsBad: " + mSurfaceIsBad
                                        + " mWaitingForSurface: " + mWaitingForSurface
                                        + " mWidth: " + mWidth
                                        + " mHeight: " + mHeight
                                        + " mRequestRender: " + mRequestRender
                                        + " mRenderMode: " + mRenderMode);
                            }
                            sGLThreadManager.wait();
                        }
                    } // end of synchronized(sGLThreadManager)

                    if (event != null) {
                        event.run();
                        event = null;
                        continue;
                    }

                    if (createEglSurface) {
                        if (LOG_SURFACE) {
                            Log.w("GLThread", "egl createSurface");
                        }
                        if (mEglHelper.createSurface()) {
                            synchronized (sGLThreadManager) {
                                mFinishedCreatingEglSurface = true;
                                sGLThreadManager.notifyAll();
                            }
                        } else {
                            synchronized (sGLThreadManager) {
                                mFinishedCreatingEglSurface = true;
                                mSurfaceIsBad = true;
                                sGLThreadManager.notifyAll();
                            }
                            continue;
                        }
                        createEglSurface = false;
                    }

                    if (createEglContext) {
                        if (LOG_RENDERER) {
                            Log.w("GLThread", "onSurfaceCreated");
                        }
                        IRenderView view = mGLSurfaceViewWeakRef.get();
                        if (view != null) {
                            try {
                                view.getRenderer().create(null, null);
                            } finally {
                            }
                        }
                        createEglContext = false;
                    }

                    if (sizeChanged) {
                        if (LOG_RENDERER) {
                            Log.w("GLThread", "onSurfaceChanged(" + w + ", " + h + ")");
                        }
                        IRenderView view = mGLSurfaceViewWeakRef.get();
                        if (view != null) {
                            try {
                                view.getRenderer().resize(w, h);
                            } finally {
                            }
                        }
                        sizeChanged = false;
                    }

                    Log.w("GLThread", "onDrawFrame tid=" + getId());
                    {
                        IRenderView view = mGLSurfaceViewWeakRef.get();
                        if (view != null) {
                            try {
                                view.getRenderer().render();
                                if (finishDrawingRunnable != null) {
                                    finishDrawingRunnable.run();
                                    finishDrawingRunnable = null;
                                }
                            } finally {
                            }
                        }
                    }
                    int swapError = mEglHelper.swap();
                    switch (swapError) {
                        case EGL10.EGL_SUCCESS:
                            break;
                        case EGL11.EGL_CONTEXT_LOST:
                            if (LOG_SURFACE) {
                                Log.i("GLThread", "egl context lost tid=" + getId());
                            }
                            lostEglContext = true;
                            break;
                        default:
                            // Other errors typically mean that the current surface is bad,
                            // probably because the SurfaceView surface has been destroyed,
                            // but we haven't been notified yet.
                            // Log the error to help developers understand why rendering stopped.
                            EglHelper.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError);

                            synchronized (sGLThreadManager) {
                                mSurfaceIsBad = true;
                                sGLThreadManager.notifyAll();
                            }
                            break;
                    }

                    if (wantRenderNotification) {
                        doRenderNotification = true;
                        wantRenderNotification = false;
                    }
                }

            } finally {
                /*
                 * clean-up everything...
                 */
                synchronized (sGLThreadManager) {
                    stopEglSurfaceLocked();
                    stopEglContextLocked();
                }
            }
        }

        public boolean ableToDraw() {
            return mHaveEglContext && mHaveEglSurface && readyToDraw();
        }

        private boolean readyToDraw() {
            return (!mPaused) && mHasSurface && (!mSurfaceIsBad)
                    && (mWidth > 0) && (mHeight > 0)
                    && (mRequestRender || (mRenderMode == RenderMode.CONTINUOUSLY));
        }

        public void setRenderMode(RenderMode renderMode) {
            synchronized (sGLThreadManager) {
                mRenderMode = renderMode;
                sGLThreadManager.notifyAll();
            }
        }

        public RenderMode getRenderMode() {
            synchronized (sGLThreadManager) {
                return mRenderMode;
            }
        }

        public void requestRender() {
            synchronized (sGLThreadManager) {
                mRequestRender = true;
                sGLThreadManager.notifyAll();
            }
        }

        public void requestRenderAndNotify(Runnable finishDrawing) {
            synchronized (sGLThreadManager) {
                // If we are already on the GL thread, this means a client callback
                // has caused reentrancy, for example via updating the SurfaceView parameters.
                // We will return to the client rendering code, so here we don't need to
                // do anything.
                if (Thread.currentThread() == this) {
                    return;
                }

                mWantRenderNotification = true;
                mRequestRender = true;
                mRenderComplete = false;
                mFinishDrawingRunnable = finishDrawing;

                sGLThreadManager.notifyAll();
            }
        }

        public void surfaceCreated() {
            synchronized (sGLThreadManager) {
                if (LOG_THREADS) {
                    Log.i("GLThread", "surfaceCreated tid=" + getId());
                }
                mHasSurface = true;
                mFinishedCreatingEglSurface = false;
                sGLThreadManager.notifyAll();
                while (mWaitingForSurface
                        && !mFinishedCreatingEglSurface
                        && !mExited) {
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void surfaceDestroyed() {
            synchronized (sGLThreadManager) {
                if (LOG_THREADS) {
                    Log.i("GLThread", "surfaceDestroyed tid=" + getId());
                }
                mHasSurface = false;
                sGLThreadManager.notifyAll();
                while ((!mWaitingForSurface) && (!mExited)) {
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onPause() {
            synchronized (sGLThreadManager) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onPause tid=" + getId());
                }
                mRequestPaused = true;
                sGLThreadManager.notifyAll();
                while ((!mExited) && (!mPaused)) {
                    if (LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onPause waiting for mPaused.");
                    }
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onResume() {
            synchronized (sGLThreadManager) {
                if (LOG_PAUSE_RESUME) {
                    Log.i("GLThread", "onResume tid=" + getId());
                }
                mRequestPaused = false;
                mRequestRender = true;
                mRenderComplete = false;
                sGLThreadManager.notifyAll();
                while ((!mExited) && mPaused && (!mRenderComplete)) {
                    if (LOG_PAUSE_RESUME) {
                        Log.i("Main thread", "onResume waiting for !mPaused.");
                    }
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onWindowResize(int w, int h) {
            synchronized (sGLThreadManager) {
                mWidth = w;
                mHeight = h;
                mSizeChanged = true;
                mRequestRender = true;
                mRenderComplete = false;

                // If we are already on the GL thread, this means a client callback
                // has caused reentrancy, for example via updating the SurfaceView parameters.
                // We need to process the size change eventually though and update our EGLSurface.
                // So we set the parameters and return so they can be processed on our
                // next iteration.
                if (Thread.currentThread() == this) {
                    return;
                }

                sGLThreadManager.notifyAll();

                // Wait for thread to react to resize and render a frame
                while (!mExited && !mPaused && !mRenderComplete
                        && ableToDraw()) {
                    if (LOG_SURFACE) {
                        Log.i("Main thread", "onWindowResize waiting for render complete from tid=" + getId());
                    }
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestExitAndWait() {
            // don't call this from GLThread thread or it is a guaranteed
            // deadlock!
            synchronized (sGLThreadManager) {
                mShouldExit = true;
                sGLThreadManager.notifyAll();
                while (!mExited) {
                    try {
                        sGLThreadManager.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestReleaseEglContextLocked() {
            mShouldReleaseEglContext = true;
            sGLThreadManager.notifyAll();
        }

        /**
         * Queue an "event" to be run on the GL rendering thread.
         *
         * @param r the runnable to be run on the GL rendering thread.
         */
        public void queueEvent(Runnable r) {
            if (r == null) {
                throw new IllegalArgumentException("r must not be null");
            }
            synchronized (sGLThreadManager) {
                mEventQueue.add(r);
                sGLThreadManager.notifyAll();
            }
        }

        // Once the thread is started, all accesses to the following member
        // variables are protected by the sGLThreadManager monitor
        private boolean mShouldExit;
        private boolean mExited;
        private boolean mRequestPaused;
        private boolean mPaused;
        private boolean mHasSurface;
        private boolean mSurfaceIsBad;
        private boolean mWaitingForSurface;
        private boolean mHaveEglContext;
        private boolean mHaveEglSurface;
        private boolean mFinishedCreatingEglSurface;
        private boolean mShouldReleaseEglContext;
        private int mWidth;
        private int mHeight;
        private RenderMode mRenderMode;
        private boolean mRequestRender;
        private boolean mWantRenderNotification;
        private boolean mRenderComplete;
        private ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
        private boolean mSizeChanged = true;
        private Runnable mFinishDrawingRunnable = null;

        // End of member variables protected by the sGLThreadManager monitor.

        private EglHelper mEglHelper;

        /**
         * Set once at thread construction time, nulled out when the parent view is garbage
         * called. This weak reference allows the GLSurfaceView to be garbage collected while
         * the GLThread is still alive.
         */
        private WeakReference<IRenderView> mGLSurfaceViewWeakRef;

        public int getGLESVersion() {
            return 3;
        }
    }

    class LogWriter extends Writer {

        @Override
        public void close() {
            flushBuilder();
        }

        @Override
        public void flush() {
            flushBuilder();
        }

        @Override
        public void write(char[] buf, int offset, int count) {
            for (int i = 0; i < count; i++) {
                char c = buf[offset + i];
                if (c == '\n') {
                    flushBuilder();
                } else {
                    mBuilder.append(c);
                }
            }
        }

        private void flushBuilder() {
            if (mBuilder.length() > 0) {
                Log.v("GLSurfaceView", mBuilder.toString());
                mBuilder.delete(0, mBuilder.length());
            }
        }

        private StringBuilder mBuilder = new StringBuilder();
    }


    class GLThreadManager {
        private static String TAG = "GLThreadManager";

        public synchronized void threadExiting(GLThread thread) {
            thread.mExited = true;
            notifyAll();
        }

        /*
         * Releases the EGL context. Requires that we are already in the
         * sGLThreadManager monitor when this is called.
         */
        public void releaseEglContextLocked(GLThread thread) {
            notifyAll();
        }
    }

    GLThreadManager sGLThreadManager = new GLThreadManager();

//    GLThreadManager sGLThreadManager = new GLThreadManager();
//
//    /**
//     * A generic GL Thread. Takes care of initializing EGL and GL. Delegates
//     * to a Renderer instance to do the actual drawing. Can be configured to
//     * render continuously or on request.
//     * <p>
//     * All potentially blocking synchronization is done through the
//     * sGLThreadManager object. This avoids multiple-lock ordering issues.
//     */
//    class GLThread extends Thread {
//        private static final String TAG = "GLThread";
//        private Logger mLogger = new Logger(TAG, Logger.INFO);
//
//        // Once the thread is started, all accesses to the following member
//        // variables are protected by the sGLThreadManager monitor
//        private boolean mShouldExit;
//        private boolean mExited;
//        private boolean mRequestPaused;
//        private boolean mPaused;
//        private boolean mHasSurface;
//        private boolean mSurfaceIsBad;
//        private boolean mWaitingForSurface;
//        private boolean mHaveEglContext;
//        private boolean mHaveEglSurface;
//        private boolean mLostEglContext;
//        private boolean mFinishedCreatingEglSurface;
//        private boolean mShouldReleaseEglContext;
//        private int mWidth;
//        private int mHeight;
//        private RenderMode mRenderMode;
//        private boolean mRequestRender;
//        private boolean mWantRenderNotification;
//        private boolean mRenderComplete;
//        private ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
//        private boolean mSizeChanged = true;
//        private Runnable mFinishDrawingRunnable = null;
//
//        /**
//         * Set once at thread construction time, nulled out when the parent view is garbage
//         * called. This weak reference allows the GLSurfaceView to be garbage collected while
//         * the GLThread is still alive.
//         */
//        private WeakReference<IRenderView> mRenderViewWeakRef;
//        private EglCore mEglCore;
//        private EglSurfaceBase mWindowSurface;
//
//        GLThread(IRenderView IRenderView) {
//            super();
//            mWidth = 0;
//            mHeight = 0;
//            mRequestRender = true;
//            mRenderMode = RenderMode.CONTINUOUSLY;
//            mWantRenderNotification = false;
//            mRenderViewWeakRef = new WeakReference<>(IRenderView);
//        }
//
//        @Override
//        public void run() {
//            setName("GLThread " + getId());
//            mLogger.info("starting tid=" + getId());
//
//            try {
//                guardedRun();
//            } catch (InterruptedException e) {
//                // fall thru and exit normally
//            } finally {
//                sGLThreadManager.threadExiting(this);
//            }
//        }
//
//        /*
//         * This private method should only be called inside a
//         * synchronized(sGLThreadManager) block.
//         */
//        private void stopEglSurfaceLocked() {
//            if (mHaveEglSurface) {
//                mHaveEglSurface = false;
//                mEglCore.makeNothingCurrent();
//                EglCore.logCurrent("stopEglSurfaceLocked");
//                mWindowSurface.release();
//                mWindowSurface = null;
//            }
//        }
//
//        /*
//         * This private method should only be called inside a
//         * synchronized(sGLThreadManager) block.
//         */
//        private void stopEglContextLocked() {
//            if (mHaveEglContext) {
//                IRenderView view = mRenderViewWeakRef.get();
//                if (view != null) {
//                    Renderer renderer = view.getRenderer();
//                    if (renderer != null) {
//                        try {
//                            renderer.dispose();
//                        } catch (Exception e) {
//                            mLogger.error("renderer dispose error", e);
//                        }
//                    }
//                }
//                mEglCore.release();
//                mHaveEglContext = false;
//                sGLThreadManager.releaseEglContextLocked(this);
//            }
//        }
//
//        private Object getSurface() {
//            IRenderView renderView = mRenderViewWeakRef.get();
//            if (renderView == null) {
//                throw new RuntimeException("renderView can not be null");
//            }
//            Object surface = renderView.getSurface();
//            if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
//                throw new RuntimeException("invalid surface: " + surface);
//            }
//            return surface;
//        }
//
//        private void guardedRun() throws InterruptedException {
//            mHaveEglContext = false;
//            mHaveEglSurface = false;
//            mWantRenderNotification = false;
//
//            try {
//                boolean createEglContext = false;
//                boolean createEglSurface = false;
//                boolean sizeChanged = false;
//                boolean wantRenderNotification = false;
//                boolean doRenderNotification = false;
//                boolean askedToReleaseEglContext = false;
//                int w = 0;
//                int h = 0;
//                Runnable event = null;
//                Runnable finishDrawingRunnable = null;
//
//                while (true) {
//                    synchronized (sGLThreadManager) {
//                        while (true) {
//                            if (mShouldExit) {
//                                return;
//                            }
//
//                            if (!mEventQueue.isEmpty()) {
//                                event = mEventQueue.remove(0);
//                                break;
//                            }
//
//                            // Update the pause state.
//                            boolean pausing = false;
//                            if (mPaused != mRequestPaused) {
//                                IRenderView view = mRenderViewWeakRef.get();
//                                if (view != null) {
//                                    if (mRequestPaused) {
//                                        view.getRenderer().pause();
//                                    } else {
//                                        view.getRenderer().resume();
//                                    }
//                                }
//
//                                pausing = mRequestPaused;
//                                mPaused = mRequestPaused;
//                                sGLThreadManager.notifyAll();
//                                mLogger.info("mPaused is now " + mPaused + " tid=" + getId());
//                            }
//
//                            // Do we need to give up the EGL context?
//                            if (mShouldReleaseEglContext) {
//                                mLogger.info("releasing EGL context because asked to tid=" + getId());
//                                stopEglSurfaceLocked();
//                                stopEglContextLocked();
//                                mShouldReleaseEglContext = false;
//                                askedToReleaseEglContext = true;
//                            }
//
//                            // Have we lost the EGL context?
//                            if (mLostEglContext) {
//                                stopEglSurfaceLocked();
//                                stopEglContextLocked();
//                                mLostEglContext = false;
//                            }
//
//                            // When pausing, release the EGL surface:
//                            if (pausing && mHaveEglSurface) {
//                                mLogger.info("releasing EGL surface because paused tid=" + getId());
//                                stopEglSurfaceLocked();
//                            }
//
//                            // When pausing, optionally release the EGL Context:
//                            if (pausing && mHaveEglContext) {
//                                IRenderView view = mRenderViewWeakRef.get();
//                                boolean preserveEglContextOnPause = view != null && view.getPreserveEGLContextOnPause();
//                                if (!preserveEglContextOnPause) {
//                                    stopEglContextLocked();
//                                    mLogger.info("releasing EGL context because paused tid=" + getId());
//                                }
//                            }
//
//                            // Have we lost the SurfaceView surface?
//                            if ((!mHasSurface) && (!mWaitingForSurface)) {
//                                mLogger.info("noticed surfaceView surface lost tid=" + getId());
//                                if (mHaveEglSurface) {
//                                    stopEglSurfaceLocked();
//                                }
//                                mWaitingForSurface = true;
//                                mSurfaceIsBad = false;
//                                sGLThreadManager.notifyAll();
//                            }
//
//                            // Have we acquired the surface view surface?
//                            if (mHasSurface && mWaitingForSurface) {
//                                mLogger.info("noticed surfaceView surface acquired tid=" + getId());
//                                mWaitingForSurface = false;
//                                sGLThreadManager.notifyAll();
//                            }
//
//                            if (doRenderNotification) {
//                                mLogger.info("sending render notification tid=" + getId());
//                                mWantRenderNotification = false;
//                                doRenderNotification = false;
//                                mRenderComplete = true;
//                                sGLThreadManager.notifyAll();
//                            }
//
//                            if (mFinishDrawingRunnable != null) {
//                                finishDrawingRunnable = mFinishDrawingRunnable;
//                                mFinishDrawingRunnable = null;
//                            }
//
//                            // Ready to draw?
//                            if (readyToDraw()) {
//
//                                // If we don't have an EGL context, try to acquire one.
//                                if (!mHaveEglContext) {
//                                    if (askedToReleaseEglContext) {
//                                        askedToReleaseEglContext = false;
//                                    } else {
//                                        try {
//                                            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
//                                        } catch (RuntimeException t) {
//                                            sGLThreadManager.releaseEglContextLocked(this);
//                                            throw t;
//                                        }
//                                        mHaveEglContext = true;
//                                        createEglContext = true;
//
//                                        sGLThreadManager.notifyAll();
//                                    }
//                                }
//
//                                if (mHaveEglContext && !mHaveEglSurface) {
//                                    mHaveEglSurface = true;
//                                    createEglSurface = true;
//                                    sizeChanged = true;
//                                }
//
//                                if (mHaveEglSurface) {
//                                    if (mSizeChanged) {
//                                        sizeChanged = true;
//                                        w = mWidth;
//                                        h = mHeight;
//                                        mWantRenderNotification = true;
//                                        mLogger.info("noticing that we want render notification tid="
//                                                + getId());
//
//                                        // Destroy and recreate the EGL surface.
//                                        createEglSurface = true;
//
//                                        mSizeChanged = false;
//                                    }
//                                    mRequestRender = false;
//                                    sGLThreadManager.notifyAll();
//                                    if (mWantRenderNotification) {
//                                        wantRenderNotification = true;
//                                    }
//                                    break;
//                                }
//                            } else {
//                                if (finishDrawingRunnable != null) {
//                                    mLogger.debug("Warning, !readyToDraw() but waiting for " +
//                                            "draw finished! Early reporting draw finished.");
//                                    finishDrawingRunnable.run();
//                                    finishDrawingRunnable = null;
//                                }
//                            }
//                            // By design, this is the only place in a GLThread thread where we wait().
//                            if (mLogger.getLevel() >= Logger.DEBUG) {
//                                mLogger.debug("waiting tid=" + getId()
//                                        + " mHaveEglContext: " + mHaveEglContext
//                                        + " mHaveEglSurface: " + mHaveEglSurface
//                                        + " mFinishedCreatingEglSurface: " + mFinishedCreatingEglSurface
//                                        + " mPaused: " + mPaused
//                                        + " mHasSurface: " + mHasSurface
//                                        + " mSurfaceIsBad: " + mSurfaceIsBad
//                                        + " mWaitingForSurface: " + mWaitingForSurface
//                                        + " mWidth: " + mWidth
//                                        + " mHeight: " + mHeight
//                                        + " mRequestRender: " + mRequestRender
//                                        + " mRenderMode: " + mRenderMode);
//                            }
//                            sGLThreadManager.wait();
//                        }
//                    } // end of synchronized(sGLThreadManager)
//
//                    if (event != null) {
//                        event.run();
//                        event = null;
//                        continue;
//                    }
//
//                    if (createEglSurface) {
//                        mLogger.debug("egl createSurface");
//                        try {
//                            mWindowSurface = new WindowSurface(mEglCore, getSurface(), false);
//                            mWindowSurface.makeCurrent();
//                            synchronized (sGLThreadManager) {
//                                mFinishedCreatingEglSurface = true;
//                                sGLThreadManager.notifyAll();
//                            }
//                        } catch (Exception e) {
//                            synchronized (sGLThreadManager) {
//                                mFinishedCreatingEglSurface = true;
//                                mSurfaceIsBad = true;
//                                sGLThreadManager.notifyAll();
//                            }
//                            continue;
//                        }
//                        createEglSurface = false;
//                    }
//
//                    if (createEglContext) {
//                        mLogger.debug("onSurfaceCreated");
//                        IRenderView view = mRenderViewWeakRef.get();
//                        if (view != null) {
//                            Renderer renderer = view.getRenderer();
//                            if (renderer != null) {
//                                try {
//                                    renderer.create(mEglCore, mWindowSurface);
//                                } catch (Exception e) {
//                                    mLogger.error("renderer onSurfaceCreated error", e);
//                                }
//                            }
//                        }
//                        createEglContext = false;
//                    }
//
//                    if (sizeChanged) {
//                        mLogger.debug("onSurfaceChanged(" + w + ", " + h + ")");
//                        IRenderView view = mRenderViewWeakRef.get();
//                        if (view != null) {
//                            Renderer renderer = view.getRenderer();
//                            if (renderer != null) {
//                                try {
//                                    renderer.resize(w, h);
//                                } catch (Exception e) {
//                                    mLogger.error("renderer onSurfaceChanged error", e);
//                                }
//                            }
//                        }
//                        sizeChanged = false;
//                    }
//
//                    mLogger.debug("onDrawFrame tid=" + getId());
//                    {
//                        IRenderView view = mRenderViewWeakRef.get();
//                        if (view != null) {
//                            Renderer renderer = view.getRenderer();
//                            if (renderer != null) {
//                                try {
//                                    renderer.render();
//                                    if (finishDrawingRunnable != null) {
//                                        finishDrawingRunnable.run();
//                                        finishDrawingRunnable = null;
//                                    }
//                                } catch (Exception e) {
//                                    mLogger.error("renderer onDrawFrame error", e);
//                                }
//                            }
//                        }
//                    }
//
//                    boolean swapResult = mWindowSurface.swapBuffers();
//                    if (!swapResult) {
//                        mLogger.error("egl context lost tid=" + getId());
//                        mLostEglContext = true;
//                        synchronized (sGLThreadManager) {
//                            mSurfaceIsBad = true;
//                            sGLThreadManager.notifyAll();
//                        }
//                    }
//
//                    if (wantRenderNotification) {
//                        doRenderNotification = true;
//                        wantRenderNotification = false;
//                    }
//                }
//
//            } finally {
//                /*
//                 * clean-up everything...
//                 */
//                synchronized (sGLThreadManager) {
//                    stopEglSurfaceLocked();
//                    stopEglContextLocked();
//                }
//            }
//        }
//
//        public boolean ableToDraw() {
//            return mHaveEglContext && mHaveEglSurface && readyToDraw();
//        }
//
//        private boolean readyToDraw() {
//            return (!mPaused) && mHasSurface && (!mSurfaceIsBad)
//                    && (mWidth > 0) && (mHeight > 0)
//                    && (mRequestRender || (mRenderMode == RenderMode.CONTINUOUSLY));
//        }
//
//        public void setRenderMode(RenderMode renderMode) {
//            synchronized (sGLThreadManager) {
//                mRenderMode = renderMode;
//                sGLThreadManager.notifyAll();
//            }
//        }
//
//        public RenderMode getRenderMode() {
//            synchronized (sGLThreadManager) {
//                return mRenderMode;
//            }
//        }
//
//        public void requestRender() {
//            synchronized (sGLThreadManager) {
//                mRequestRender = true;
//                sGLThreadManager.notifyAll();
//            }
//        }
//
//        public void requestRenderAndNotify(Runnable finishDrawing) {
//            synchronized (sGLThreadManager) {
//                // If we are already on the GL thread, this means a client callback
//                // has caused reentrancy, for example via updating the SurfaceView parameters.
//                // We will return to the client rendering code, so here we don't need to
//                // do anything.
//                if (Thread.currentThread() == this) {
//                    return;
//                }
//
//                mWantRenderNotification = true;
//                mRequestRender = true;
//                mRenderComplete = false;
//                mFinishDrawingRunnable = finishDrawing;
//
//                sGLThreadManager.notifyAll();
//            }
//        }
//
//        public void surfaceCreated() {
//            synchronized (sGLThreadManager) {
//                mLogger.info("surfaceCreated tid=" + getId());
//                mHasSurface = true;
//                mFinishedCreatingEglSurface = false;
//                sGLThreadManager.notifyAll();
//                while (mWaitingForSurface
//                        && !mFinishedCreatingEglSurface
//                        && !mExited) {
//                    try {
//                        sGLThreadManager.wait();
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            }
//        }
//
//        public void surfaceDestroyed() {
//            synchronized (sGLThreadManager) {
//                mLogger.info("surfaceDestroyed tid=" + getId());
//                mHasSurface = false;
//                sGLThreadManager.notifyAll();
//                while ((!mWaitingForSurface) && (!mExited)) {
//                    try {
//                        sGLThreadManager.wait();
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            }
//        }
//
//        public void onPause() {
//            synchronized (sGLThreadManager) {
//                mLogger.info("onPause tid=" + getId());
//                mRequestPaused = true;
//                sGLThreadManager.notifyAll();
//                while ((!mExited) && (!mPaused)) {
//                    Logger.info("Main thread", "onPause waiting for mPaused.");
//                    try {
//                        sGLThreadManager.wait();
//                    } catch (InterruptedException ex) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            }
//        }
//
//        public void onResume() {
//            synchronized (sGLThreadManager) {
//                mLogger.info("onResume tid=" + getId());
//                mRequestPaused = false;
//                mRequestRender = true;
//                mRenderComplete = false;
//                sGLThreadManager.notifyAll();
//                while ((!mExited) && mPaused && (!mRenderComplete)) {
//                    Logger.info("Main thread", "onResume waiting for !mPaused.");
//                    try {
//                        sGLThreadManager.wait();
//                    } catch (InterruptedException ex) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            }
//        }
//
//        public void onWindowResize(int w, int h) {
//            synchronized (sGLThreadManager) {
//                mWidth = w;
//                mHeight = h;
//                mSizeChanged = true;
//                mRequestRender = true;
//                mRenderComplete = false;
//
//                // If we are already on the GL thread, this means a client callback
//                // has caused reentrancy, for example via updating the SurfaceView parameters.
//                // We need to process the size change eventually though and update our EGLSurface.
//                // So we set the parameters and return so they can be processed on our
//                // next iteration.
//                if (Thread.currentThread() == this) {
//                    return;
//                }
//
//                sGLThreadManager.notifyAll();
//
//                // Wait for thread to react to resize and render a frame
//                while (!mExited && !mPaused && !mRenderComplete
//                        && ableToDraw()) {
//                    Logger.info("Main thread", "onWindowResize waiting for render complete from tid=" + getId());
//                    try {
//                        sGLThreadManager.wait();
//                    } catch (InterruptedException ex) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            }
//        }
//
//        public void requestExitAndWait() {
//            // don't call this from GLThread thread or it is a guaranteed
//            // deadlock!
//            synchronized (sGLThreadManager) {
//                mShouldExit = true;
//                sGLThreadManager.notifyAll();
//                while (!mExited) {
//                    try {
//                        sGLThreadManager.wait();
//                    } catch (InterruptedException ex) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//            }
//        }
//
//        public void requestReleaseEglContextLocked() {
//            mShouldReleaseEglContext = true;
//            sGLThreadManager.notifyAll();
//        }
//
//        /**
//         * Queue an "event" to be run on the GL rendering thread.
//         *
//         * @param r the runnable to be run on the GL rendering thread.
//         */
//        public void queueEvent(Runnable r) {
//            if (r == null) {
//                throw new IllegalArgumentException("r must not be null");
//            }
//            synchronized (sGLThreadManager) {
//                mEventQueue.add(r);
//                sGLThreadManager.notifyAll();
//            }
//        }
//
//
//        public int getGLESVersion() {
//            return mEglCore.getGLVersion();
//        }
//    }
//
//    class GLThreadManager {
//        private static String TAG = "GLThreadManager";
//
//        public synchronized void threadExiting(GLThread thread) {
//            Logger.info(TAG, "exiting tid=" + thread.getId());
//            thread.mExited = true;
//            notifyAll();
//        }
//
//        /*
//         * Releases the EGL context. Requires that we are already in the
//         * sGLThreadManager monitor when this is called.
//         */
//        public void releaseEglContextLocked(GLThread thread) {
//            notifyAll();
//        }
//    }

}
