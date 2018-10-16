package com.erlei.gdx.android.widget;

import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.support.annotation.IntDef;

import com.erlei.gdx.graphics.AndroidGL20;
import com.erlei.gdx.graphics.AndroidGL30;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.utils.Logger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * An EGL helper class.
 */
public class EglHelper {
    /**
     * Constructor flag: surface must be recordable.  This discourages EGL from using a
     * pixel format that cannot be converted efficiently to something usable by the video
     * encoder.
     */
    public static final int FLAG_RECORDABLE = 0x01;

    /**
     * Constructor flag: ask for GLES3, fall back to GLES2 if not available.  Without this
     * flag, GLES2 is used.
     */
    public static final int FLAG_TRY_GLES3 = 0x02;

    @IntDef({
            FLAG_RECORDABLE,
            FLAG_TRY_GLES3})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flag {
    }


    private Logger mLogger = new Logger("EglHelper", Logger.INFO);
    private final WeakReference<IRenderView> mWeakReference;
    private IRenderView.EGLConfigChooser mEGLConfigChooser;
    private IRenderView.EGLContextFactory mEGLContextFactory;
    private IRenderView.EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private IRenderView.GLWrapper mGLWrapper;
    private EGL10 mEgl;
    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;
    private static int GL_VERSION = -1;

    public EglHelper(WeakReference<IRenderView> weakReference) {
        mWeakReference = weakReference;
        IRenderView iRenderView = weakReference.get();
        if (iRenderView != null) {
            mEGLConfigChooser = iRenderView.getEGLConfigChooser();
            mEGLContextFactory = iRenderView.getEGLContextFactory();
            mEGLWindowSurfaceFactory = iRenderView.getEGLWindowSurfaceFactory();
            mGLWrapper = iRenderView.getGLWrapper();
        }

        if (mEGLConfigChooser == null)
            mEGLConfigChooser = new SimpleEGLConfigChooser(FLAG_TRY_GLES3 | FLAG_RECORDABLE, false);
        if (mEGLContextFactory == null)
            mEGLContextFactory = new DefaultContextFactory();
        if (mEGLWindowSurfaceFactory == null)
            mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
    }

    public static int getGlVersion() {
        return GL_VERSION;
    }

    /**
     * Initialize EGL for a given configuration spec.
     */
    public void start() {
        mLogger.info("start() tid=" + Thread.currentThread().getId());
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
        mEglContext = mEGLContextFactory.createContext(GL_VERSION, mEgl, mEglDisplay, mEglConfig);
        if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT) {
            mEglContext = null;
            throwEglException("createContext");
        }

        mLogger.info("createContext " + mEglContext + " tid=" + Thread.currentThread().getId());

        mEglSurface = null;
    }

    /**
     * Create an egl surface for the current SurfaceHolder surface. If a surface
     * already exists, destroy it before creating the new surface.
     *
     * @return true if the surface was created successfully.
     */
    public boolean createSurface() {
        mLogger.info("createSurface()  tid=" + Thread.currentThread().getId());
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
        IRenderView view = mWeakReference.get();
        if (view != null) {
            mEglSurface = mEGLWindowSurfaceFactory.createWindowSurface(mEgl,
                    mEglDisplay, mEglConfig, view.getSurface());
        } else {
            mEglSurface = null;
        }

        if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
            int error = mEgl.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                mLogger.error("createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
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
     * Create a GL object for the current EGL context.
     *
     * @return GL20
     * @see GL20
     */
    public GL20 createGL() {
        AndroidGL20.init();
        GL20 gl = GL_VERSION >= 3 ? new AndroidGL30() : new AndroidGL20();
        if (mGLWrapper != null) {
            gl = mGLWrapper.wrap(gl);
        }
        return gl;
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
        mLogger.info("destroySurface()  tid=" + Thread.currentThread().getId());
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
        if (mEglContext != null) {
            mLogger.info("destroyContext() tid=" + Thread.currentThread().getId());
            mEGLContextFactory.destroyContext(mEgl, mEglDisplay, mEglContext);
            mEglContext = null;
        }
        if (mEglDisplay != null) {
            mEgl.eglTerminate(mEglDisplay);
            mEglDisplay = null;
        }
        mEglConfig = null;
        mEGLConfigChooser = null;
        mEGLContextFactory = null;
        mEGLWindowSurfaceFactory = null;
        mGLWrapper = null;
        mWeakReference.clear();
        mLogger.info("finish() tid=" + Thread.currentThread().getId());
    }

    private void throwEglException(String function) {
        throwEglException(function, mEgl.eglGetError());
    }

    public static void throwEglException(String function, int error) {
        String message = formatEglError(function, error);

        Logger.error("EglHelper", "throwEglException tid=" + Thread.currentThread().getId() + " " + message);
        throw new RuntimeException(message);
    }

    public static void logEglErrorAsWarning(String tag, String function, int error) {
        Logger.warn(tag, formatEglError(function, error));
    }

    public static String formatEglError(String function, int error) {
        return function + " failed: " + getErrorString(error);
    }

    public static String getHex(int value) {
        return "0x" + Integer.toHexString(value);
    }

    public static String getErrorString(int error) {
        switch (error) {
            case EGL10.EGL_SUCCESS:
                return "EGL_SUCCESS";
            case EGL10.EGL_NOT_INITIALIZED:
                return "EGL_NOT_INITIALIZED";
            case EGL10.EGL_BAD_ACCESS:
                return "EGL_BAD_ACCESS";
            case EGL10.EGL_BAD_ALLOC:
                return "EGL_BAD_ALLOC";
            case EGL10.EGL_BAD_ATTRIBUTE:
                return "EGL_BAD_ATTRIBUTE";
            case EGL10.EGL_BAD_CONFIG:
                return "EGL_BAD_CONFIG";
            case EGL10.EGL_BAD_CONTEXT:
                return "EGL_BAD_CONTEXT";
            case EGL10.EGL_BAD_CURRENT_SURFACE:
                return "EGL_BAD_CURRENT_SURFACE";
            case EGL10.EGL_BAD_DISPLAY:
                return "EGL_BAD_DISPLAY";
            case EGL10.EGL_BAD_MATCH:
                return "EGL_BAD_MATCH";
            case EGL10.EGL_BAD_NATIVE_PIXMAP:
                return "EGL_BAD_NATIVE_PIXMAP";
            case EGL10.EGL_BAD_NATIVE_WINDOW:
                return "EGL_BAD_NATIVE_WINDOW";
            case EGL10.EGL_BAD_PARAMETER:
                return "EGL_BAD_PARAMETER";
            case EGL10.EGL_BAD_SURFACE:
                return "EGL_BAD_SURFACE";
            case EGL11.EGL_CONTEXT_LOST:
                return "EGL_CONTEXT_LOST";
            default:
                return getHex(error);
        }
    }

    public int getGLESVersion() {
        return 3;
    }

    abstract class BaseConfigChooser
            implements IRenderView.EGLConfigChooser {

        private final int mFlags;
        protected int[] mConfigSpec;
        private static final int EGL_RECORDABLE_ANDROID = 0x3142;

        /**
         * @param flags
         * @param configSpec
         * @see #FLAG_RECORDABLE
         * @see #FLAG_TRY_GLES3
         */
        public BaseConfigChooser(@Flag int flags, int[] configSpec) {
            mFlags = flags;
            mConfigSpec = configSpec;
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            EGLConfig eglConfig = null;
            // Try to get a GLES3 context, if requested.
            if ((mFlags & FLAG_TRY_GLES3) != 0) {
                eglConfig = chooseConfig(3, egl, display);
                if (eglConfig != null) GL_VERSION = 3;
            }

            if (eglConfig == null) {
                eglConfig = chooseConfig(2, egl, display);
                if (eglConfig != null) GL_VERSION = 2;
            }

            if (eglConfig == null) {
                throw new RuntimeException("Unable to find a suitable EGLConfig");
            }
            return eglConfig;
        }

        private EGLConfig chooseConfig(int glVersion, EGL10 egl, EGLDisplay display) {
            int[] configSpec = filterConfigSpec(glVersion, mConfigSpec);
            int[] num_config = new int[1];
            if (!egl.eglChooseConfig(display, configSpec, null, 0, num_config)) {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException("No configs match configSpec");
            }

            EGLConfig[] configs = new EGLConfig[numConfigs];
            if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs,
                    num_config)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            EGLConfig config = chooseConfig(egl, display, configs);
            if (config == null) {
                mLogger.warn("unable to find configSpec / " + glVersion + " EGLConfig  " + Arrays.toString(configSpec));
                return null;
            }
            return config;
        }

        abstract EGLConfig chooseConfig(EGL10 egl, EGLDisplay display,
                                        EGLConfig[] configs);


        private int[] filterConfigSpec(int version, int[] configSpec) {


            int[] attr;
            if ((mFlags & FLAG_RECORDABLE) != 0) {
                attr = new int[]{
                        EGL10.EGL_RENDERABLE_TYPE, version == 2 ? EGL14.EGL_OPENGL_ES2_BIT : EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                        EGL_RECORDABLE_ANDROID, 1,
                        EGL14.EGL_NONE
                };

            } else {
                attr = new int[]{
                        EGL10.EGL_RENDERABLE_TYPE, version == 2 ? EGL14.EGL_OPENGL_ES2_BIT : EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                        EGL14.EGL_NONE
                };
            }

            int[] newConfigSpec = new int[configSpec.length + attr.length - 1];

            System.arraycopy(configSpec, 0, newConfigSpec, 0, configSpec.length - 1);
            System.arraycopy(attr, 0, newConfigSpec, configSpec.length - 1, attr.length);
            return newConfigSpec;
        }
    }

    /**
     * Choose a configuration with exactly the specified r,g,b,a sizes,
     * and at least the specified depth and stencil sizes.
     */
    class ComponentSizeChooser extends BaseConfigChooser {


        /**
         * @see #FLAG_RECORDABLE
         * @see #FLAG_TRY_GLES3
         */
        public ComponentSizeChooser(int flags, int redSize, int greenSize, int blueSize,
                                    int alphaSize, int depthSize, int stencilSize) {
            super(flags, new int[]{
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
     * This class will choose a RGBA_8888 surface with
     * or without a depth buffer.
     */
    class SimpleEGLConfigChooser extends ComponentSizeChooser {
        /**
         * @see #FLAG_RECORDABLE
         * @see #FLAG_TRY_GLES3
         */
        public SimpleEGLConfigChooser(int flags, boolean withDepthBuffer) {
            super(flags, 8, 8, 8, 8, withDepthBuffer ? 16 : 0, 0);
        }
    }


    class DefaultWindowSurfaceFactory implements IRenderView.EGLWindowSurfaceFactory {

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
                mLogger.error("eglCreateWindowSurface", e);
            }
            return result;
        }

        public void destroySurface(EGL10 egl, EGLDisplay display,
                                   EGLSurface surface) {
            egl.eglDestroySurface(display, surface);
        }
    }

    class DefaultContextFactory implements IRenderView.EGLContextFactory {
        private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        public EGLContext createContext(int glVersion, EGL10 egl, EGLDisplay display, EGLConfig config) {
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, glVersion, EGL10.EGL_NONE};

            return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attrib_list);
        }

        public void destroyContext(EGL10 egl, EGLDisplay display,
                                   EGLContext context) {
            if (!egl.eglDestroyContext(display, context)) {
                mLogger.error("display:" + display + " context: " + context);
                mLogger.info("tid=" + Thread.currentThread().getId());
                EglHelper.throwEglException("eglDestroyContext", egl.eglGetError());
            }
        }
    }
}