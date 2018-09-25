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

package com.erlei.gdx;

import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.GL30;
import com.erlei.gdx.graphics.glutils.GLVersion;


public interface Graphics {
    /**
     * Class describing the bits per pixel, depth buffer precision, stencil precision and number of MSAA samples.
     */
    public static class BufferFormat {
        /* number of bits per color channel */
        public final int r, g, b, a;
        /* number of bits for depth and stencil buffer */
        public final int depth, stencil;
        /**
         * number of samples for multi-sample anti-aliasing (MSAA)
         **/
        public final int samples;
        /**
         * whether coverage sampling anti-aliasing is used. in that case you have to clear the coverage buffer as well!
         */
        public final boolean coverageSampling;

        public BufferFormat(int r, int g, int b, int a, int depth, int stencil, int samples, boolean coverageSampling) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.depth = depth;
            this.stencil = stencil;
            this.samples = samples;
            this.coverageSampling = coverageSampling;
        }

        public String toString() {
            return "r: " + r + ", g: " + g + ", b: " + b + ", a: " + a + ", depth: " + depth + ", stencil: " + stencil
                    + ", num samples: " + samples + ", coverage sampling: " + coverageSampling;
        }
    }

    /**
     * Returns whether OpenGL ES 3.0 is available. If it is you can get an instance of {@link GL30} via {@link #getGL30()} to
     * access OpenGL ES 3.0 functionality. Note that this functionality will only be available if you instructed the
     * {@link Application} instance to use OpenGL ES 3.0!
     *
     * @return whether OpenGL ES 3.0 is available
     */
    public boolean isGL30Available();

    /**
     * @return the {@link GL20} instance
     */
    public GL20 getGL20();

    /**
     * @return the {@link GL30} instance or null if not supported
     */
    public GL30 getGL30();

    /**
     * Set the GL20 instance
     **/
    public void setGL20(GL20 gl20);

    /**
     * Set the GL30 instance
     **/
    public void setGL30(GL30 gl30);

    /**
     * @return the width of the client area in logical pixels.
     */
    public int getWidth();

    /**
     * @return the height of the client area in logical pixels
     */
    public int getHeight();

    /**
     * @return the width of the framebuffer in physical pixels
     */
    public int getBackBufferWidth();

    /**
     * @return the height of the framebuffer in physical pixels
     */
    public int getBackBufferHeight();

    /**
     * Returns the id of the current frame. The general contract of this method is that the id is incremented only when the
     * application is in the running state right before calling the {@link ApplicationListener#render()} method. Also, the id of
     * the first frame is 0; the id of subsequent frames is guaranteed to take increasing values for 2<sup>63</sup>-1 rendering
     * cycles.
     *
     * @return the id of the current frame
     */
    public long getFrameId();

    /**
     * @return the time span between the current frame and the last frame in seconds. Might be smoothed over n frames.
     */
    public float getDeltaTime();

    /**
     * @return the time span between the current frame and the last frame in seconds, without smoothing
     **/
    public float getRawDeltaTime();

    /**
     * @return the average number of frames per second
     */
    public int getFramesPerSecond();

    /**
     * @return the {@link GLVersion} of this Graphics instance
     */
    public GLVersion getGLVersion();

    /**
     * @return the pixels per inch on the x-axis
     */
    public float getPpiX();

    /**
     * @return the pixels per inch on the y-axis
     */
    public float getPpiY();

    /**
     * @return the pixels per centimeter on the x-axis
     */
    public float getPpcX();

    /**
     * @return the pixels per centimeter on the y-axis.
     */
    public float getPpcY();

    /**
     * This is a scaling factor for the Density Independent Pixel unit, following the same conventions as
     * android.util.DisplayMetrics#density, where one DIP is one pixel on an approximately 160 dpi screen. Thus on a 160dpi screen
     * this density value will be 1; on a 120 dpi screen it would be .75; etc.
     *
     * @return the logical density of the Display.
     */
    public float getDensity();

    /**
     * @return the format of the color, depth and stencil buffer in a {@link BufferFormat} instance
     */
    public BufferFormat getBufferFormat();

    /**
     * @param extension the extension name
     * @return whether the extension is supported
     */
    public boolean supportsExtension(String extension);
}
