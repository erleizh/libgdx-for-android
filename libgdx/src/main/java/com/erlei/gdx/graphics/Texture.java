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

package com.erlei.gdx.graphics;

import com.erlei.gdx.widget.GLContext;
import com.erlei.gdx.files.FileHandle;
import com.erlei.gdx.graphics.Pixmap.Format;
import com.erlei.gdx.graphics.glutils.PixmapTextureData;

/**
 * A Texture wraps a standard OpenGL ES texture.
 * A Texture has to be bound via the {@link Texture#bind()} method in order for it to be applied to geometry. The texture will be
 * bound to the currently active texture unit specified via {@link GL20#glActiveTexture(int)}.
 * <p>
 * You can draw {@link Pixmap}s to a texture at any time. The changes will be automatically uploaded to texture memory. This is of
 * course not extremely fast so use it with care. It also only works with unmanaged textures.
 * <p>
 * A Texture must be disposed when it is no longer used
 *
 * @author badlogicgames@gmail.com
 */
public class Texture extends GLTexture {

    public enum TextureFilter {
        /**
         * Fetch the nearest texel that best maps to the pixel on screen.
         */
        Nearest(GL20.GL_NEAREST),

        /**
         * Fetch four nearest texels that best maps to the pixel on screen.
         */
        Linear(GL20.GL_LINEAR),

        /**
         * @see TextureFilter#MipMapLinearLinear
         */
        MipMap(GL20.GL_LINEAR_MIPMAP_LINEAR),

        /**
         * Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a
         * nearest filter.
         */
        MipMapNearestNearest(GL20.GL_NEAREST_MIPMAP_NEAREST),

        /**
         * Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a
         * linear filter.
         */
        MipMapLinearNearest(GL20.GL_LINEAR_MIPMAP_NEAREST),

        /**
         * Fetch the two best fitting images from the mip map chain and then sample the nearest texel from each of the two images,
         * combining them to the final output pixel.
         */
        MipMapNearestLinear(GL20.GL_NEAREST_MIPMAP_LINEAR),

        /**
         * Fetch the two best fitting images from the mip map chain and then sample the four nearest texels from each of the two
         * images, combining them to the final output pixel.
         */
        MipMapLinearLinear(GL20.GL_LINEAR_MIPMAP_LINEAR);

        final int glEnum;

        TextureFilter(int glEnum) {
            this.glEnum = glEnum;
        }

        public boolean isMipMap() {
            return glEnum != GL20.GL_NEAREST && glEnum != GL20.GL_LINEAR;
        }

        public int getGLEnum() {
            return glEnum;
        }
    }

    public enum TextureWrap {
        MirroredRepeat(GL20.GL_MIRRORED_REPEAT), ClampToEdge(GL20.GL_CLAMP_TO_EDGE), Repeat(GL20.GL_REPEAT);

        final int glEnum;

        TextureWrap(int glEnum) {
            this.glEnum = glEnum;
        }

        public int getGLEnum() {
            return glEnum;
        }
    }

    TextureData data;

    public Texture(String internalPath) {
        this(GLContext.getFiles().internal(internalPath));
    }

    public Texture(FileHandle file) {
        this(file, null, false);
    }

    public Texture(FileHandle file, boolean useMipMaps) {
        this(file, null, useMipMaps);
    }

    public Texture(FileHandle file, Format format, boolean useMipMaps) {
        this(TextureData.Factory.loadFromFile(file, format, useMipMaps));
    }

    public Texture(Pixmap pixmap) {
        this(new PixmapTextureData(pixmap, null, false, false));
    }

    public Texture(Pixmap pixmap, boolean useMipMaps) {
        this(new PixmapTextureData(pixmap, null, useMipMaps, false));
    }

    public Texture(Pixmap pixmap, Format format, boolean useMipMaps) {
        this(new PixmapTextureData(pixmap, format, useMipMaps, false));
    }

    public Texture(int width, int height, Format format) {
        this(new PixmapTextureData(new Pixmap(width, height, format), null, false, true));
    }

    public Texture(TextureData data) {
        this(GL20.GL_TEXTURE_2D, GLContext.getGL20().glGenTexture(), data);
    }

    protected Texture(int glTarget, int glHandle, TextureData data) {
        super(glTarget, glHandle);
        load(data);
    }

    public void load(TextureData data) {
        this.data = data;

        if (!data.isPrepared()) data.prepare();

        bind();
        uploadImageData(GL20.GL_TEXTURE_2D, data);

        unsafeSetFilter(minFilter, magFilter, true);
        unsafeSetWrap(uWrap, vWrap, true);
        GLContext.getGL20().glBindTexture(glTarget, 0);
    }

    /**
     * Used internally to reload after context loss. Creates a new GL handle then calls {@link #load(TextureData)}. Use this only
     * if you know what you do!
     */
    @Override
    protected void reload() {
        glHandle = GLContext.getGL20().glGenTexture();
        load(data);
    }

    /**
     * Draws the given {@link Pixmap} to the texture at position x, y. No clipping is performed so you have to make sure that you
     * draw only inside the texture region. Note that this will only draw to mipmap level 0!
     *
     * @param pixmap The Pixmap
     * @param x      The x coordinate in pixels
     * @param y      The y coordinate in pixels
     */
    public void draw(Pixmap pixmap, int x, int y) {
        bind();
        GLContext.getGL20().glTexSubImage2D(glTarget, 0, x, y, pixmap.getWidth(), pixmap.getHeight(), pixmap.getGLFormat(), pixmap.getGLType(),
                pixmap.getPixels());
    }

    @Override
    public int getWidth() {
        return data.getWidth();
    }

    @Override
    public int getHeight() {
        return data.getHeight();
    }

    @Override
    public int getDepth() {
        return 0;
    }

    public TextureData getTextureData() {
        return data;
    }


    /**
     * Disposes all resources associated with the texture
     */
    public void dispose() {
        // this is a hack. reason: we have to set the glHandle to 0 for textures that are
        // reloaded through the asset manager as we first remove (and thus dispose) the texture
        // and then reload it. the glHandle is set to 0 in invalidateAllTextures prior to
        // removal from the asset manager.
        if (glHandle == 0) return;
        delete();
    }
}
