/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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
import java.io.IOException;

import org.apache.commons.io.filefilter.IOFileFilter;

/**
 * Abstraction of file system operations to facilitate unit testing.
 */
public interface FileSystem {
	/**
	 * Remove any files and directories in the specified directory except if
	 * they match excludeFilter.
	 */
	void cleanDirectory(File directory, IOFileFilter excludeFilter) throws IOException;

	/**
	 * Create directory and any parent directories that are not already present.
	 * @throws IOException if directory or parents could not be created.
	 */
	void createDirectory(File directory) throws IOException;

	boolean directoryExists(File directory);

	File[] listFiles(File directory) throws IOException;
}