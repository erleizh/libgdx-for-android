package com.erlei.gdx.android.widget;


import android.content.Context;
import android.text.TextUtils;

import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.utils.Logger;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public interface IRenderView {

    Logger logger = new Logger("IRenderView", Logger.INFO);


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

        void create(EglHelper egl, GL20 gl);

        void resize(int width, int height);

        void render();

        void pause();

        void resume();

        void dispose();

    }

    void setGLWrapper(GLWrapper wrapper);

    /**
     * An interface used to wrap a GL interface.
     * <p>Typically
     * used for implementing debugging and tracing on top of the default
     * GL interface. You would typically use this by creating your own class
     * that implemented all the GL methods by delegating to another GL instance.
     * Then you could add your own behavior before or after calling the
     * delegate. All the GLWrapper would do was instantiate and return the
     * wrapper GL instance:
     * <pre class="prettyprint">
     * class MyGLWrapper implements GLWrapper {
     * GL20 wrap(GL20 gl) {
     * return new MyGLImplementation(gl);
     * }
     * static class MyGLImplementation implements GL20,GL30{
     * ...
     * }
     * }
     * </pre>
     *
     * @see IRenderView#setGLWrapper(IRenderView.GLWrapper)
     */
    interface GLWrapper {
        /**
         * Wraps a gl interface in another gl interface.
         *
         * @param gl a GL interface that is to be wrapped.
         * @return either the input argument or another GL object that wraps the input argument.
         */
        GL20 wrap(GL20 gl);
    }

    /**
     * Install a custom EGLContextFactory.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(IRenderView.Renderer)}
     * is called.
     * <p>
     * If this method is not called, then by default
     * a context will be created with no shared context and
     * with a null attribute list.
     */
    void setEGLContextFactory(EGLContextFactory factory);


    /**
     * An interface for customizing the eglCreateContext and eglDestroyContext calls.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link IRenderView#setEGLContextFactory(IRenderView.EGLContextFactory)}
     */
    interface EGLContextFactory {
        EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig);

        void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context);
    }

    /**
     * Install a custom EGLWindowSurfaceFactory.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If this method is not called, then by default
     * a window surface will be created with a null attribute list.
     */
    void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory);

    /**
     * An interface for customizing the eglCreateWindowSurface and eglDestroySurface calls.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link #setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory)}
     */
    interface EGLWindowSurfaceFactory {
        /**
         * @return null if the surface cannot be constructed.
         */
        EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config,
                                       Object nativeWindow);

        void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface);
    }


    /**
     * Install a custom EGLConfigChooser.
     * <p>If this method is
     * called, it must be called before {@link #setRenderer(Renderer)}
     * is called.
     * <p>
     * If no setEGLConfigChooser method is called, then by default the
     * view will choose an EGLConfig that is compatible with the current
     * android.view.Surface, with a depth buffer depth of
     * at least 16 bits.
     *
     * @param configChooser configChooser
     */
    void setEGLConfigChooser(EGLConfigChooser configChooser);

    /**
     * An interface for choosing an EGLConfig configuration from a list of
     * potential configurations.
     * <p>
     * This interface must be implemented by clients wishing to call
     * {@link #setEGLConfigChooser(EGLConfigChooser)}
     */
    interface EGLConfigChooser {
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


    GLWrapper getGLWrapper();

    EGLWindowSurfaceFactory getEGLWindowSurfaceFactory();

    EGLContextFactory getEGLContextFactory();

    EGLConfigChooser getEGLConfigChooser();


}
