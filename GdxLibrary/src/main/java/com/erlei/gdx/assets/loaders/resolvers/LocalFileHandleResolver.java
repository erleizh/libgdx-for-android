
package com.erlei.gdx.assets.loaders.resolvers;

import com.erlei.gdx.assets.loaders.FileHandleResolver;
import com.erlei.gdx.files.AndroidFiles;
import com.erlei.gdx.files.FileHandle;

public class LocalFileHandleResolver implements FileHandleResolver {
	@Override
	public FileHandle resolve (String fileName) {
		return AndroidFiles.getInstance().local(fileName);
	}
}
