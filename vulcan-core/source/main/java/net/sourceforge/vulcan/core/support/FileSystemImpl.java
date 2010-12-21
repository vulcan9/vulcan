/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2010 Chris Eldredge
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sourceforge.vulcan.core.support;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

public class FileSystemImpl implements FileSystem {

	public void cleanDirectory(File directory, IOFileFilter excludeFilter) throws IOException {
		if (!directory.exists() || !directory.isDirectory()) {
			throw new IOException("Not a directory");
		}
		
		final FileFilter filter = excludeFilter == null ? null : FileFilterUtils.notFileFilter(excludeFilter);
		
		final File[] files = directory.listFiles(filter);
		
		for (File file : files) {
			if (file.isDirectory()) {
				FileUtils.deleteDirectory(file);
			}
			else {
				FileUtils.forceDelete(file);
			}
		}
	}

	public boolean directoryExists(File directory) {
		return directory.exists();
	}
	
	public void createDirectory(File path) throws IOException {
		FileUtils.forceMkdir(path);
	}
	
	public File[] listFiles(File directory) throws IOException {
		final File[] files = directory.listFiles();
		
		if (files == null) {
			throw new IOException("Failed to list files.");
		}

		return files;
	}
}