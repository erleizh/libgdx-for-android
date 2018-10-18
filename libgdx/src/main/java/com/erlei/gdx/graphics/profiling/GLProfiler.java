/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.erlei.gdx.graphics.profiling;

import com.erlei.gdx.widget.GLContext;
import com.erlei.gdx.graphics.GL30;
import com.erlei.gdx.math.FloatCounter;

/**
 * When enabled, collects statistics about GL calls and checks for GL errors.
 * Enabling will wrap Gdx.gl* instances with delegate classes which provide described functionality
 * and route GL calls to the actual GL instances.
 *
 * @author Daniel Holderbaum
 * @author Jan Polák
 * @see GL20Interceptor
 * @see GL30Interceptor
 */
public class GLProfiler {

    private final GLContext glContext;
    private GLInterceptor glInterceptor;
    private GLErrorListener listener;
    private boolean enabled = false;

    /**
     * Create a new instance of GLProfiler to monitor a {@link com.erlei.gdx.widget.GLContext} instance's gl calls
     *
     * @param glContext
     */
    public GLProfiler(GLContext glContext) {
        this.glContext = glContext;
        GL30 gl30 = glContext.gl30;
        if (gl30 != null) {
            glInterceptor = new GL30Interceptor(this, glContext.gl30);
        } else {
            glInterceptor = new GL20Interceptor(this, glContext.gl);
        }
        listener = GLErrorListener.LOGGING_LISTENER;
    }

    /**
     * Enables profiling by replacing the {@code GL20} and {@code GL30} instances with profiling ones.
     */
    public void enable() {
        if (enabled) return;

        GL30 gl30 = glContext.gl30;
        if (gl30 != null) {
            glContext.setGL30((GL30) glInterceptor);
        } else {
            glContext.setGL20(glInterceptor);
        }

        enabled = true;
    }

    /**
     * Disables profiling by resetting the {@code GL20} and {@code GL30} instances with the original ones.
     */
    public void disable() {
        if (!enabled) return;

        GL30 gl30 = glContext.gl30;
        if (gl30 != null) glContext.setGL30(((GL30Interceptor) glContext.gl30).gl30);
        else glContext.setGL20(((GL20Interceptor) glContext.gl).gl20);

        enabled = false;
    }

    /**
     * Set the current listener for the {@link GLProfiler} to {@code errorListener}
     */
    public void setListener(GLErrorListener errorListener) {
        this.listener = errorListener;
    }

    /**
     * @return the current {@link GLErrorListener}
     */
    public GLErrorListener getListener() {
        return listener;
    }

    /**
     * @return true if the GLProfiler is currently profiling
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return the total gl calls made since the last reset
     */
    public int getCalls() {
        return glInterceptor.getCalls();
    }

    /**
     * @return the total amount of texture bindings made since the last reset
     */
    public int getTextureBindings() {
        return glInterceptor.getTextureBindings();
    }

    /**
     * @return the total amount of draw calls made since the last reset
     */
    public int getDrawCalls() {
        return glInterceptor.getDrawCalls();
    }

    /**
     * @return the total amount of shader switches made since the last reset
     */
    public int getShaderSwitches() {
        return glInterceptor.getShaderSwitches();
    }

    /**
     * @return {@link FloatCounter} containing information about rendered vertices since the last reset
     */
    public FloatCounter getVertexCount() {
        return glInterceptor.getVertexCount();
    }

    /**
     * Will reset the statistical information which has been collected so far. This should be called after every frame.
     * Error listener is kept as it is.
     */
    public void reset() {
        glInterceptor.reset();
    }

}
