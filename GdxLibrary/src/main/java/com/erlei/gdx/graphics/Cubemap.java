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

import com.erlei.gdx.android.widget.GLContext;
import com.erlei.gdx.files.FileHandle;
import com.erlei.gdx.graphics.Pixmap.Format;
import com.erlei.gdx.graphics.Texture.TextureFilter;
import com.erlei.gdx.graphics.Texture.TextureWrap;
import com.erlei.gdx.graphics.glutils.FacedCubemapData;
import com.erlei.gdx.graphics.glutils.PixmapTextureData;
import com.erlei.gdx.math.Vector3;

/**
 * Wraps a standard OpenGL ES Cubemap. Must be disposed when it is no longer used.
 *
 * @author Xoppa
 */
public class Cubemap extends GLTexture {
    /**
     * Enum to identify each side of a Cubemap
     */
    public enum CubemapSide {
        /**
         * The positive X and first side of the cubemap
         */
        PositiveX(0, GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, -1, 0, 1, 0, 0),
        /**
         * The negative X and second side of the cubemap
         */
        NegativeX(1, GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, -1, 0, -1, 0, 0),
        /**
         * The positive Y and third side of the cubemap
         */
        PositiveY(2, GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, 0, 1, 0, 1, 0),
        /**
         * The negative Y and fourth side of the cubemap
         */
        NegativeY(3, GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, 0, -1, 0, -1, 0),
        /**
         * The positive Z and fifth side of the cubemap
         */
        PositiveZ(4, GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, -1, 0, 0, 0, 1),
        /**
         * The negative Z and sixth side of the cubemap
         */
        NegativeZ(5, GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, -1, 0, 0, 0, -1);

        /**
         * The zero based index of the side in the cubemap
         */
        public final int index;
        /**
         * The OpenGL target (used for glTexImage2D) of the side.
         */
        public final int glEnum;
        /**
         * The up vector to target the side.
         */
        public final Vector3 up;
        /**
         * The direction vector to target the side.
         */
        public final Vector3 direction;

        CubemapSide(int index, int glEnum, float upX, float upY, float upZ, float directionX, float directionY, float directionZ) {
            this.index = index;
            this.glEnum = glEnum;
            this.up = new Vector3(upX, upY, upZ);
            this.direction = new Vector3(directionX, directionY, directionZ);
        }

        /**
         * @return The OpenGL target (used for glTexImage2D) of the side.
         */
        public int getGLEnum() {
            return glEnum;
        }

        /**
         * @return The up vector of the side.
         */
        public Vector3 getUp(Vector3 out) {
            return out.set(up);
        }

        /**
         * @return The direction vector of the side.
         */
        public Vector3 getDirection(Vector3 out) {
            return out.set(direction);
        }
    }

    protected CubemapData data;

    /**
     * Construct a Cubemap based on the given CubemapData.
     */
    public Cubemap(CubemapData data) {
        super(GL20.GL_TEXTURE_CUBE_MAP);
        this.data = data;
        load(data);
    }

    /**
     * Construct a Cubemap with the specified texture files for the sides, does not generate mipmaps.
     */
    public Cubemap(FileHandle positiveX, FileHandle negativeX, FileHandle positiveY, FileHandle negativeY, FileHandle positiveZ,
                   FileHandle negativeZ) {
        this(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ, false);
    }

    /**
     * Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps.
     */
    public Cubemap(FileHandle positiveX, FileHandle negativeX, FileHandle positiveY, FileHandle negativeY, FileHandle positiveZ,
                   FileHandle negativeZ, boolean useMipMaps) {
        this(TextureData.Factory.loadFromFile(positiveX, useMipMaps), TextureData.Factory.loadFromFile(negativeX, useMipMaps),
                TextureData.Factory.loadFromFile(positiveY, useMipMaps), TextureData.Factory.loadFromFile(negativeY, useMipMaps),
                TextureData.Factory.loadFromFile(positiveZ, useMipMaps), TextureData.Factory.loadFromFile(negativeZ, useMipMaps));
    }

    /**
     * Construct a Cubemap with the specified {@link Pixmap}s for the sides, does not generate mipmaps.
     */
    public Cubemap(Pixmap positiveX, Pixmap negativeX, Pixmap positiveY, Pixmap negativeY, Pixmap positiveZ, Pixmap negativeZ) {
        this(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ, false);
    }

    /**
     * Construct a Cubemap with the specified {@link Pixmap}s for the sides, optionally generating mipmaps.
     */
    public Cubemap(Pixmap positiveX, Pixmap negativeX, Pixmap positiveY, Pixmap negativeY, Pixmap positiveZ, Pixmap negativeZ,
                   boolean useMipMaps) {
        this(positiveX == null ? null : new PixmapTextureData(positiveX, null, useMipMaps, false), negativeX == null ? null
                        : new PixmapTextureData(negativeX, null, useMipMaps, false), positiveY == null ? null : new PixmapTextureData(positiveY,
                        null, useMipMaps, false), negativeY == null ? null : new PixmapTextureData(negativeY, null, useMipMaps, false),
                positiveZ == null ? null : new PixmapTextureData(positiveZ, null, useMipMaps, false), negativeZ == null ? null
                        : new PixmapTextureData(negativeZ, null, useMipMaps, false));
    }

    /**
     * Construct a Cubemap with {@link Pixmap}s for each side of the specified size.
     */
    public Cubemap(int width, int height, int depth, Format format) {
        this(new PixmapTextureData(new Pixmap(depth, height, format), null, false, true), new PixmapTextureData(new Pixmap(depth,
                        height, format), null, false, true), new PixmapTextureData(new Pixmap(width, depth, format), null, false, true),
                new PixmapTextureData(new Pixmap(width, depth, format), null, false, true), new PixmapTextureData(new Pixmap(width,
                        height, format), null, false, true), new PixmapTextureData(new Pixmap(width, height, format), null, false, true));
    }

    /**
     * Construct a Cubemap with the specified {@link TextureData}'s for the sides
     */
    public Cubemap(TextureData positiveX, TextureData negativeX, TextureData positiveY, TextureData negativeY,
                   TextureData positiveZ, TextureData negativeZ) {
        super(GL20.GL_TEXTURE_CUBE_MAP);
        minFilter = TextureFilter.Nearest;
        magFilter = TextureFilter.Nearest;
        uWrap = TextureWrap.ClampToEdge;
        vWrap = TextureWrap.ClampToEdge;
        data = new FacedCubemapData(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ);
        load(data);
    }

    /**
     * Sets the sides of this cubemap to the specified {@link CubemapData}.
     */
    public void load(CubemapData data) {
        if (!data.isPrepared()) data.prepare();
        bind();
        unsafeSetFilter(minFilter, magFilter, true);
        unsafeSetWrap(uWrap, vWrap, true);
        data.consumeCubemapData();
        GLContext.getGL20().glBindTexture(glTarget, 0);
    }

    public CubemapData getCubemapData() {
        return data;
    }

    @Override
    protected void reload() {
        glHandle = GLContext.getGL20().glGenTexture();
        load(data);
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

    /**
     * Disposes all resources associated with the cubemap
     */
    @Override
    public void dispose() {
        // this is a hack. reason: we have to set the glHandle to 0 for textures that are
        // reloaded through the asset manager as we first remove (and thus dispose) the texture
        // and then reload it. the glHandle is set to 0 in invalidateAllTextures prior to
        // removal from the asset manager.
        if (glHandle == 0) return;
        delete();
    }
}
