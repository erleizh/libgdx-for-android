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

import com.erlei.gdx.files.FileHandle;
import com.erlei.gdx.utils.GdxRuntimeException;

/**
 * Open GLES wrapper for TextureArray
 *
 * @author Tomski
 */
public class TextureArray extends GLTexture {

    private TextureArrayData data;

    public TextureArray(String... internalPaths) {
        this(getInternalHandles(internalPaths));
    }

    public TextureArray(FileHandle... files) {
        this(false, files);
    }

    public TextureArray(boolean useMipMaps, FileHandle... files) {
        this(useMipMaps, Pixmap.Format.RGBA8888, files);
    }

    public TextureArray(boolean useMipMaps, Pixmap.Format format, FileHandle... files) {
        this(TextureArrayData.Factory.loadFromFiles(format, useMipMaps, files));
    }

    public TextureArray(TextureArrayData data) {
        super(GL30.GL_TEXTURE_2D_ARRAY, Gdx.gl.glGenTexture());

        if (Gdx.gl30 == null) {
            throw new GdxRuntimeException("TextureArray requires a device running with GLES 3.0 compatibilty");
        }

        load(data);
    }

    private static FileHandle[] getInternalHandles(String... internalPaths) {
        FileHandle[] handles = new FileHandle[internalPaths.length];
        for (int i = 0; i < internalPaths.length; i++) {
            handles[i] = AndroidFiles.getInstance().internal(internalPaths[i]);
        }
        return handles;
    }

    private void load(TextureArrayData data) {
        this.data = data;

        bind();
        Gdx.gl30.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, data.getInternalFormat(), data.getWidth(), data.getHeight(), data.getDepth(), 0, data.getInternalFormat(), data.getGLType(), null);

        if (!data.isPrepared()) data.prepare();

        data.consumeTextureArrayData();

        setFilter(minFilter, magFilter);
        setWrap(uWrap, vWrap);
        Gdx.gl.glBindTexture(glTarget, 0);
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
        return data.getDepth();
    }

    @Override
    protected void reload() {
        glHandle = Gdx.gl.glGenTexture();
        load(data);
    }
}
