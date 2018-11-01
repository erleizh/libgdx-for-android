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

package com.erlei.gdx.graphics.glutils;

import com.erlei.gdx.widget.GLContext;
import com.erlei.gdx.files.FileHandle;
import com.erlei.gdx.graphics.GL20;
import com.erlei.gdx.graphics.Pixmap;
import com.erlei.gdx.graphics.Pixmap.Format;
import com.erlei.gdx.graphics.TextureData;
import com.erlei.gdx.graphics.glutils.ETC1.ETC1Data;
import com.erlei.gdx.utils.GdxRuntimeException;

public class ETC1TextureData implements TextureData {
	FileHandle file;
	ETC1Data data;
	boolean useMipMaps;
	int width = 0;
	int height = 0;
	boolean isPrepared = false;

	public ETC1TextureData (FileHandle file) {
		this(file, false);
	}

	public ETC1TextureData (FileHandle file, boolean useMipMaps) {
		this.file = file;
		this.useMipMaps = useMipMaps;
	}

	public ETC1TextureData (ETC1Data encodedImage, boolean useMipMaps) {
		this.data = encodedImage;
		this.useMipMaps = useMipMaps;
	}

	@Override
	public TextureDataType getType () {
		return TextureDataType.Custom;
	}

	@Override
	public boolean isPrepared () {
		return isPrepared;
	}

	@Override
	public void prepare () {
		if (isPrepared) throw new GdxRuntimeException("Already prepared");
		if (file == null && data == null) throw new GdxRuntimeException("Can only load once from ETC1Data");
		if (file != null) {
			data = new ETC1Data(file);
		}
		width = data.width;
		height = data.height;
		isPrepared = true;
	}

	@Override
	public void consumeCustomData (int target) {
		if (!isPrepared) throw new GdxRuntimeException("Call prepare() before calling consumeCompressedData()");

		if (!GLContext.get().supportsExtension("GL_OES_compressed_ETC1_RGB8_texture")) {
			Pixmap pixmap = ETC1.decodeImage(data, Format.RGB565);
			GLContext.getGL20().glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0,
				pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
			if (useMipMaps) MipMapGenerator.generateMipMap(target, pixmap, pixmap.getWidth(), pixmap.getHeight());
			pixmap.dispose();
			useMipMaps = false;
		} else {
			GLContext.getGL20().glCompressedTexImage2D(target, 0, ETC1.ETC1_RGB8_OES, width, height, 0, data.compressedData.capacity()
				- data.dataOffset, data.compressedData);
			if (useMipMaps()) GLContext.getGL20().glGenerateMipmap(GL20.GL_TEXTURE_2D);
		}
		data.dispose();
		data = null;
		isPrepared = false;
	}

	@Override
	public Pixmap consumePixmap () {
		throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
	}

	@Override
	public boolean disposePixmap () {
		throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
	}

	@Override
	public int getWidth () {
		return width;
	}

	@Override
	public int getHeight () {
		return height;
	}

	@Override
	public Format getFormat () {
		return Format.RGB565;
	}

	@Override
	public boolean useMipMaps () {
		return useMipMaps;
	}
}
