
package com.erlei.gdx.assets.loaders.resolvers;

import com.erlei.gdx.Files;
import com.erlei.gdx.assets.loaders.FileHandleResolver;
import com.erlei.gdx.files.AndroidFiles;
import com.erlei.gdx.files.FileHandle;

import javax.inject.Inject;

public class AbsoluteFileHandleResolver implements FileHandleResolver {

	@Override
	public FileHandle resolve (String fileName) {
		return AndroidFiles.getInstance().absolute(fileName);
	}
}
