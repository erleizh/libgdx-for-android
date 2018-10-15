
package com.erlei.gdx.assets.loaders.resolvers;

import com.erlei.gdx.android.widget.GLContext;
import com.erlei.gdx.assets.loaders.FileHandleResolver;
import com.erlei.gdx.files.FileHandle;

public class LocalFileHandleResolver implements FileHandleResolver {
	@Override
	public FileHandle resolve (String fileName) {
		return GLContext.getFiles().local(fileName);
	}
}
