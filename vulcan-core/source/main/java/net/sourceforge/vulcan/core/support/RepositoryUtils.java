/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2006 Chris Eldredge
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

import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.exception.RepositoryException;

public class RepositoryUtils {

	private final FileSystem fileSystem;
	
	public RepositoryUtils() {
		this(new FileSystemImpl());
	}
	
	public RepositoryUtils(FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	public void createOrCleanWorkingCopy(File workDir, BuildDetailCallback cb) throws RepositoryException {
		if (!fileSystem.directoryExists(workDir)) {
			try {
				fileSystem.createDirectory(workDir);
			} catch (IOException e) {
				throw new RepositoryException("errors.cannot.create.dir", e, new Object[] {workDir, e.getMessage()});
			}
			return;
		}
		
		try {
			cb.setDetail("build.messages.clean");
			fileSystem.cleanDirectory(workDir, null);
		} catch (IOException e) {
			throw new RepositoryException("messages.build.cannot.clean.work.dir", e, new Object[] {workDir, e.getMessage()});
		}

	}
	
}
