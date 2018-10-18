package com.erlei.gdx.widget;


import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.utils.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;

/**
 * A generic GL Thread. Takes care of initializing EGL and GL. Delegates
 * to a Renderer instance to do the actual drawing. Can be configured to
 * render continuously or on request.
 * <p>
 * All potentially blocking synchronization is done through the
 * sGLThreadManager object. This avoids multiple-lock ordering issues.
 */
class GLThread extends Thread {
    private Logger mLogger = new Logger("GLThread", Logger.INFO);
    private final GLThreadManager sGLThreadManager = new GLThreadManager();

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
    private IRenderView.RenderMode mRenderMode;
    private boolean mRequestRender;
    private boolean mWantRenderNotification;
    private boolean mRenderComplete;
    private ArrayList<Runnable> mEventQueue = new ArrayList<>();
    private boolean mSizeChanged = true;
    private Runnable mFinishDrawingRunnable = null;

    // End of member variables protected by the sGLThreadManager monitor.

    private EGLCore mEGLCore;

    /**
     * Set once at thread construction time, nulled out when the parent view is garbage
     * called. This weak reference allows the GLSurfaceView to be garbage collected while
     * the GLThread is still alive.
     */
    private WeakReference<IRenderView> mWeakReference;

    public int getGLESVersion() {
        return mEGLCore.getGLESVersion();
    }

    GLThread(IRenderView iRenderView) {
        super();
        mWidth = 0;
        mHeight = 0;
        mRequestRender = true;
        mRenderMode = IRenderView.RenderMode.CONTINUOUSLY;
        mWantRenderNotification = false;
        mWeakReference = new WeakReference<>(iRenderView);
    }

    @Override
    public void run() {
        setName("GLThread " + getId());
        mLogger.info("starting tid=" + getId());

        try {
            guardedRun();
        } catch (InterruptedException e) {
            // fall thru and exit normally
        } finally {
            sGLThreadManager.threadExiting(this);
            mEGLCore = null;
        }
        mLogger.info("finish tid=" + getId());
    }

    /*
     * This private method should only be called inside a
     * synchronized(sGLThreadManager) block.
     */
    private void stopEglSurfaceLocked() {
        if (mHaveEglSurface) {
            mHaveEglSurface = false;
            mEGLCore.destroySurface();
        }
    }

    /*
     * This private method should only be called inside a
     * synchronized(sGLThreadManager) block.
     */
    private void stopEglContextLocked() {
        if (mHaveEglContext) {
            mEGLCore.finish();
            mHaveEglContext = false;
            sGLThreadManager.releaseEglContextLocked(this);
        }
    }

    private void guardedRun() throws InterruptedException {
        mEGLCore = new EGLCore(mWeakReference);
        mHaveEglContext = false;
        mHaveEglSurface = false;
        mWantRenderNotification = false;

        try {
            GL20 gl = null;
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
                            if (mRequestPaused) {
                                handlePause();
                            } else {
                                handleResume();
                            }
                            pausing = mRequestPaused;
                            mPaused = mRequestPaused;
                            sGLThreadManager.notifyAll();
                            mLogger.info("mPaused is now " + mPaused + " tid=" + getId());
                        }

                        // Do we need to give up the EGL context?
                        if (mShouldReleaseEglContext) {
                            mLogger.info("releasing EGL context because asked to tid=" + getId());
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
                            mLogger.info("releasing EGL surface because paused tid=" + getId());
                            stopEglSurfaceLocked();
                        }

                        // When pausing, optionally release the EGL Context:
                        if (pausing && mHaveEglContext) {
                            IRenderView view = mWeakReference.get();
                            boolean preserveEglContextOnPause = view != null && view.getPreserveEGLContextOnPause();
                            if (!preserveEglContextOnPause) {
                                stopEglContextLocked();
                                mLogger.info("releasing EGL context because paused tid=" + getId());
                            }
                        }

                        // Have we lost the SurfaceView surface?
                        if ((!mHasSurface) && (!mWaitingForSurface)) {
                            mLogger.info("noticed surfaceView surface lost tid=" + getId());
                            if (mHaveEglSurface) {
                                stopEglSurfaceLocked();
                            }
                            mWaitingForSurface = true;
                            mSurfaceIsBad = false;
                            sGLThreadManager.notifyAll();
                        }

                        // Have we acquired the surface view surface?
                        if (mHasSurface && mWaitingForSurface) {
                            mLogger.info("noticed surfaceView surface acquired tid=" + getId());
                            mWaitingForSurface = false;
                            sGLThreadManager.notifyAll();
                        }

                        if (doRenderNotification) {
                            mLogger.info("sending render notification tid=" + getId());
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
                                        mEGLCore.start();
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
                                    mLogger.info("noticing that we want render notification tid=" + getId());

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
                                mLogger.warn("Warning, !readyToDraw() but waiting for " +
                                        "draw finished! Early reporting draw finished.");
                                finishDrawingRunnable.run();
                                finishDrawingRunnable = null;
                            }
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
                    mLogger.info("egl createSurface");
                    if (mEGLCore.createSurface()) {
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

                if (createGlInterface) {
                    gl = mEGLCore.createGL();

                    createGlInterface = false;
                }

                if (createEglContext) {
                    mLogger.info("onSurfaceCreated");
                    IRenderView view = mWeakReference.get();
                    if (view != null) {
                        try {
                            view.getRenderer().create(mEGLCore, gl);
                        } catch (Exception e) {
                            mLogger.error("render create error ", e);
                        }
                    }
                    createEglContext = false;
                }

                if (sizeChanged) {
                    mLogger.info("onSurfaceChanged(" + w + ", " + h + ")");
                    IRenderView view = mWeakReference.get();
                    if (view != null) {
                        try {
                            view.getRenderer().resize(w, h);
                        } catch (Exception e) {
                            mLogger.error("render resize error ", e);
                        }
                    }
                    sizeChanged = false;
                }

                {
                    IRenderView view = mWeakReference.get();
                    if (view != null) {
                        try {
                            view.getRenderer().render(gl);
                            if (finishDrawingRunnable != null) {
                                finishDrawingRunnable.run();
                                finishDrawingRunnable = null;
                            }
                        } catch (Exception e) {
                            mLogger.error("render error ", e);
                        }
                    }
                }
                int swapError = mEGLCore.swap();
                switch (swapError) {
                    case EGL10.EGL_SUCCESS:
                        break;
                    case EGL11.EGL_CONTEXT_LOST:
                        mLogger.info("egl context lost tid=" + getId());
                        lostEglContext = true;
                        break;
                    default:
                        // Other errors typically mean that the current surface is bad,
                        // probably because the SurfaceView surface has been destroyed,
                        // but we haven't been notified yet.
                        // Log the error to help developers understand why rendering stopped.
                        EGLCore.logEglErrorAsWarning("GLThread", "eglSwapBuffers", swapError);

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
            IRenderView iRenderView = mWeakReference.get();
            if (iRenderView != null) {
                try {
                    mLogger.info("dispose() tid=" + Thread.currentThread().getId());
                    iRenderView.getRenderer().dispose();
                } catch (Exception e) {
                    mLogger.error("dispose ", e);
                }
            }
            /*
             * clean-up everything...
             */
            synchronized (sGLThreadManager) {
                stopEglSurfaceLocked();
                stopEglContextLocked();
            }
        }
    }

    private void handlePause() {
        mLogger.info("pause");
        IRenderView view = mWeakReference.get();
        if (view != null) {
            try {
                view.getRenderer().pause();
            } catch (Exception e) {
                mLogger.error("pause error ", e);
            }
        }
    }

    private void handleResume() {
        mLogger.info("resume");
        IRenderView view = mWeakReference.get();
        if (view != null) {
            try {
                view.getRenderer().resume();
            } catch (Exception e) {
                mLogger.error("resume error ", e);
            }
        }
    }

    public boolean ableToDraw() {
        return mHaveEglContext && mHaveEglSurface && readyToDraw();
    }

    private boolean readyToDraw() {
        return (!mPaused) && mHasSurface && (!mSurfaceIsBad)
                && (mWidth > 0) && (mHeight > 0)
                && (mRequestRender || (mRenderMode == IRenderView.RenderMode.CONTINUOUSLY));
    }

    public void setRenderMode(IRenderView.RenderMode renderMode) {
        synchronized (sGLThreadManager) {
            mRenderMode = renderMode;
            sGLThreadManager.notifyAll();
        }
    }

    public IRenderView.RenderMode getRenderMode() {
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

            mLogger.info("surfaceCreated tid=" + getId());
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
            mLogger.info("surfaceDestroyed tid=" + getId());
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
            mLogger.info("onPause tid=" + getId());
            mRequestPaused = true;
            sGLThreadManager.notifyAll();
            while ((!mExited) && (!mPaused)) {
                mLogger.info("onPause waiting for mPaused.");
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
            mLogger.info("onResume tid=" + getId());
            mRequestPaused = false;
            mRequestRender = true;
            mRenderComplete = false;
            sGLThreadManager.notifyAll();
            while ((!mExited) && mPaused && (!mRenderComplete)) {
                mLogger.info("onResume waiting for !mPaused.");
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
                mLogger.info("onWindowResize waiting for render complete from tid=" + getId());
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

    static class GLThreadManager {
        synchronized void threadExiting(GLThread thread) {
            Logger.info("GLThreadManager", "exiting tid=" + thread.getId());
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
}
