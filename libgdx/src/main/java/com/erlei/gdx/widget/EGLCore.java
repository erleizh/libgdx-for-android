package com.erlei.gdx.widget;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;

import com.erlei.gdx.graphics.AndroidGL20;
import com.erlei.gdx.graphics.AndroidGL30;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.utils.Logger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGL11;


/**
 * An EGL helper class.
 */
public class EGLCore {
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



    private Logger mLogger = new Logger("EGLCore", Logger.INFO);
    private final WeakReference<IRenderView> mWeakReference;
    private IRenderView.EGLConfigChooser mEGLConfigChooser;
    private IRenderView.EGLContextFactory mEGLContextFactory;
    private IRenderView.EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private IRenderView.GLWrapper mGLWrapper;
    private EGLDisplay mEglDisplay;
    private EGLSurface mEglSurface;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;
    private static int GL_VERSION = -1;

    public EGLCore(WeakReference<IRenderView> weakReference) {
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


    public EGLDisplay getEglDisplay() {
        return mEglDisplay;
    }

    public EGLSurface getEglSurface() {
        return mEglSurface;
    }

    public EGLConfig getEglConfig() {
        return mEglConfig;
    }

    public EGLContext getEglContext() {
        return mEglContext;
    }

    public static int getGlVersion() {
        return GL_VERSION;
    }

    /**
     * Initialize EGL for a given configuration spec.
     */
    void start() {
        mLogger.info("start() tid=" + Thread.currentThread().getId());

        /*
         * Get to the default display.
         */
        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        /*
         * We can now initialize EGL for that display
         */
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("eglInitialize failed");
        }
        mEglConfig = mEGLConfigChooser.chooseConfig(mEglDisplay);

        /*
         * Create an EGL context. We want to do this as rarely as we can, because an
         * EGL context is a somewhat heavy object.
         */
        mEglContext = mEGLContextFactory.createContext(GL_VERSION, mEglDisplay, mEglConfig);
        if (mEglContext == null || mEglContext == EGL14.EGL_NO_CONTEXT) {
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
    boolean createSurface() {
        mLogger.info("createSurface()  tid=" + Thread.currentThread().getId());

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
            mEglSurface = mEGLWindowSurfaceFactory.createWindowSurface(mEglDisplay, mEglConfig, view.getSurface());
        } else {
            mEglSurface = null;
        }

        if (mEglSurface == null || mEglSurface == EGL14.EGL_NO_SURFACE) {
            int error = EGL14.eglGetError();
            if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
                mLogger.error("createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
            }
            return false;
        }

        /*
         * Before we can issue GL commands, we need to make sure
         * the context is current and bound to a surface.
         */
        if (!EGL14.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            /*
             * Could not make the context current, probably because the underlying
             * SurfaceView surface has been destroyed.
             */
            logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", EGL14.eglGetError());
            return false;
        }

        return true;
    }

    /**
     * Makes no context current.
     */
    public void makeNothingCurrent() {
        if (!EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    public void setPresentationTime(EGLSurface eglSurface, long nsecs) {
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, eglSurface, nsecs);
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     *
     * @return false on failure
     */
    public boolean swapBuffers(EGLSurface eglSurface) {
        return EGL14.eglSwapBuffers(mEglDisplay, eglSurface);
    }

    /**
     * Makes our EGL context current,
     */
    public void makeCurrent() {
        makeCurrent(mEglSurface);
    }

    /**
     * Makes our EGL context current, using the supplied surface for both "draw" and "read".
     */
    public void makeCurrent(EGLSurface eglSurface) {
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            // called makeCurrent() before create?
            mLogger.debug("NOTE: makeCurrent w/o display");
        }
        if (!EGL14.eglMakeCurrent(mEglDisplay, eglSurface, eglSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    /**
     * Returns true if our context and the specified surface are current.
     */
    public boolean isCurrent(EGLSurface eglSurface) {
        return mEglContext.equals(EGL14.eglGetCurrentContext()) &&
                eglSurface.equals(EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW));
    }

    /**
     * Performs a simple surface query.
     */
    public int querySurface(EGLSurface eglSurface, int what) {
        int[] value = new int[1];
        EGL14.eglQuerySurface(mEglDisplay, eglSurface, what, value, 0);
        return value[0];
    }

    /**
     * Destroys the specified surface.  Note the EGLSurface won't actually be destroyed if it's
     * still current in a context.
     */
    public void releaseSurface(EGLSurface eglSurface) {
        EGL14.eglDestroySurface(mEglDisplay, eglSurface);
    }
    /**
     * Creates an EGL surface associated with a Surface.
     * <p>
     * If this is destined for MediaCodec, the EGLConfig should have the "recordable" attribute.
     */
    public EGLSurface createWindowSurface(Object surface) {
        if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
            throw new RuntimeException("invalid surface: " + surface);
        }

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface,
                surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;
    }


    /**
     * Creates an EGL surface associated with an offscreen buffer.
     */
    public EGLSurface createOffscreenSurface(int width, int height) {
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, width,
                EGL14.EGL_HEIGHT, height,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(mEglDisplay, mEglConfig,
                surfaceAttribs, 0);
        checkEglError("eglCreatePbufferSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;
    }


    /**
     * Queries a string value.
     */
    public String queryString(int what) {
        return EGL14.eglQueryString(mEglDisplay, what);
    }
    /**
     * Makes our EGL context current, using the supplied "draw" and "read" surfaces.
     */
    public void makeCurrent(EGLSurface drawSurface, EGLSurface readSurface) {
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            /*
             * Could not make the context current, probably because the underlying
             * SurfaceView surface has been destroyed.
             */
            mLogger.debug("NOTE: makeCurrent w/o display");
        }
        if (!EGL14.eglMakeCurrent(mEglDisplay, drawSurface, readSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent(draw,read) failed : " + EGL14.eglGetError());
        }
    }

    /**
     * Create a GL object for the current EGL context.
     *
     * @return GL20
     * @see GL20
     */
    GL20 createGL() {
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
    int swap() {
        if (!EGL14.eglSwapBuffers(mEglDisplay, mEglSurface)) {
            return EGL14.eglGetError();
        }
        return EGL14.EGL_SUCCESS;
    }

    void destroySurface() {
        mLogger.info("destroySurface()  tid=" + Thread.currentThread().getId());
        destroySurfaceImp();
    }

    private void destroySurfaceImp() {
        if (mEglSurface != null && mEglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            mEGLWindowSurfaceFactory.destroySurface(mEglDisplay, mEglSurface);
            mEglSurface = null;
        }
    }

    void finish() {
        if (mEglContext != null) {
            mLogger.info("destroyContext() tid=" + Thread.currentThread().getId());
            mEGLContextFactory.destroyContext(mEglDisplay, mEglContext);
            mEglContext = null;
        }
        if (mEglDisplay != null) {
            EGL14.eglTerminate(mEglDisplay);
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
        throwEglException(function, EGL14.eglGetError());
    }

    public static void throwEglException(String function, int error) {
        String message = formatEglError(function, error);

        Logger.error("EGLCore", "throwEglException tid=" + Thread.currentThread().getId() + " " + message);
        throw new RuntimeException(message);
    }

    public static void logEglErrorAsWarning(String tag, String function, int error) {
        Logger.warn(tag, formatEglError(function, error));
    }

    public static String formatEglError(String function, int error) {
        return function + " failed: " + getErrorString(error);
    }
    /**
     * Checks for EGL errors.  Throws an exception if an error has been raised.
     */
    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + formatEglError(msg,error));
        }
    }

    public static String getHex(int value) {
        return "0x" + Integer.toHexString(value);
    }

    public static String getErrorString(int error) {
        switch (error) {
            case EGL14.EGL_SUCCESS:
                return "EGL_SUCCESS";
            case EGL14.EGL_NOT_INITIALIZED:
                return "EGL_NOT_INITIALIZED";
            case EGL14.EGL_BAD_ACCESS:
                return "EGL_BAD_ACCESS";
            case EGL14.EGL_BAD_ALLOC:
                return "EGL_BAD_ALLOC";
            case EGL14.EGL_BAD_ATTRIBUTE:
                return "EGL_BAD_ATTRIBUTE";
            case EGL14.EGL_BAD_CONFIG:
                return "EGL_BAD_CONFIG";
            case EGL14.EGL_BAD_CONTEXT:
                return "EGL_BAD_CONTEXT";
            case EGL14.EGL_BAD_CURRENT_SURFACE:
                return "EGL_BAD_CURRENT_SURFACE";
            case EGL14.EGL_BAD_DISPLAY:
                return "EGL_BAD_DISPLAY";
            case EGL14.EGL_BAD_MATCH:
                return "EGL_BAD_MATCH";
            case EGL14.EGL_BAD_NATIVE_PIXMAP:
                return "EGL_BAD_NATIVE_PIXMAP";
            case EGL14.EGL_BAD_NATIVE_WINDOW:
                return "EGL_BAD_NATIVE_WINDOW";
            case EGL14.EGL_BAD_PARAMETER:
                return "EGL_BAD_PARAMETER";
            case EGL14.EGL_BAD_SURFACE:
                return "EGL_BAD_SURFACE";
            case EGL11.EGL_CONTEXT_LOST:
                return "EGL_CONTEXT_LOST";
            default:
                return getHex(error);
        }
    }

    public int getGLESVersion() {
        return GL_VERSION;
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
        public BaseConfigChooser(int flags, int[] configSpec) {
            mFlags = flags;
            mConfigSpec = configSpec;
        }

        @Override
        public EGLConfig chooseConfig(EGLDisplay display) {
            EGLConfig eglConfig = null;
            // Try to get a GLES3 context, if requested.
            if ((mFlags & FLAG_TRY_GLES3) != 0) {
                eglConfig = chooseConfig(3, display);
                if (eglConfig != null) GL_VERSION = 3;
            }

            if (eglConfig == null) {
                eglConfig = chooseConfig(2, display);
                if (eglConfig != null) GL_VERSION = 2;
            }

            if (eglConfig == null) {
                throw new RuntimeException("Unable to find a suitable EGLConfig");
            }
            return eglConfig;
        }

        private EGLConfig chooseConfig(int glVersion, EGLDisplay display) {
            int[] configSpec = filterConfigSpec(glVersion, mConfigSpec);
            int[] num_config = new int[1];
            if (!EGL14.eglChooseConfig(display, configSpec, 0, null, 0, 0, num_config, 0)) {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }

            int numConfigs = num_config[0];

            if (numConfigs <= 0) {
                throw new IllegalArgumentException("No configs match configSpec");
            }

            EGLConfig[] configs = new EGLConfig[numConfigs];
            if (!EGL14.eglChooseConfig(display, configSpec, 0, configs, 0, configs.length, num_config, 0)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            EGLConfig config = chooseConfig(display, configs);
            if (config == null) {
                mLogger.warn("unable to find configSpec / " + glVersion + " EGLConfig  " + Arrays.toString(configSpec));
                return null;
            }
            return config;
        }

        abstract EGLConfig chooseConfig(EGLDisplay display,
                                        EGLConfig[] configs);


        private int[] filterConfigSpec(int version, int[] configSpec) {


            int[] attr;
            if ((mFlags & FLAG_RECORDABLE) != 0) {
                attr = new int[]{
                        EGL14.EGL_RENDERABLE_TYPE, version == 2 ? EGL14.EGL_OPENGL_ES2_BIT : EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                        EGL_RECORDABLE_ANDROID, 1,
                        EGL14.EGL_NONE
                };

            } else {
                attr = new int[]{
                        EGL14.EGL_RENDERABLE_TYPE, version == 2 ? EGL14.EGL_OPENGL_ES2_BIT : EGLExt.EGL_OPENGL_ES3_BIT_KHR,
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
                    EGL14.EGL_RED_SIZE, redSize,
                    EGL14.EGL_GREEN_SIZE, greenSize,
                    EGL14.EGL_BLUE_SIZE, blueSize,
                    EGL14.EGL_ALPHA_SIZE, alphaSize,
                    EGL14.EGL_DEPTH_SIZE, depthSize,
                    EGL14.EGL_STENCIL_SIZE, stencilSize,
                    EGL14.EGL_NONE});
            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
        }

        @Override
        public EGLConfig chooseConfig(EGLDisplay display,
                                      EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(display, config,
                        EGL14.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(display, config,
                        EGL14.EGL_STENCIL_SIZE, 0);
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    int r = findConfigAttrib(display, config,
                            EGL14.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(display, config,
                            EGL14.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(display, config,
                            EGL14.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(display, config,
                            EGL14.EGL_ALPHA_SIZE, 0);
                    if ((r == mRedSize) && (g == mGreenSize)
                            && (b == mBlueSize) && (a == mAlphaSize)) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGLDisplay display,
                                     EGLConfig config, int attribute, int defaultValue) {

            if (EGL14.eglGetConfigAttrib(display, config, attribute, mValue, 0)) {
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

        public EGLSurface createWindowSurface(EGLDisplay display,
                                              EGLConfig config, Object nativeWindow) {
            EGLSurface result = null;
            try {
                result = EGL14.eglCreateWindowSurface(display, config, nativeWindow, new int[]{EGL14.EGL_NONE}, 0);
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

        public void destroySurface(EGLDisplay display,
                                   EGLSurface surface) {
            EGL14.eglDestroySurface(display, surface);
        }
    }

    class DefaultContextFactory implements IRenderView.EGLContextFactory {
        private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        public EGLContext createContext(int glVersion, EGLDisplay display, EGLConfig config) {
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, glVersion, EGL14.EGL_NONE};

            return EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, attrib_list, 0);
        }

        public void destroyContext(EGLDisplay display,
                                   EGLContext context) {
            if (!EGL14.eglDestroyContext(display, context)) {
                mLogger.error("display:" + display + " context: " + context);
                mLogger.info("tid=" + Thread.currentThread().getId());
                EGLCore.throwEglException("eglDestroyContext", EGL14.eglGetError());
            }
        }
    }
}