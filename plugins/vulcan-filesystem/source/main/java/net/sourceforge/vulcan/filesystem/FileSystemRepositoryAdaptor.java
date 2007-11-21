/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2007 Chris Eldredge
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
package net.sourceforge.vulcan.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.filesystem.dto.FileSystemProjectConfigDto;

import org.apache.commons.io.FileUtils;

public class FileSystemRepositoryAdaptor implements RepositoryAdaptor {
	public static final String WORKING_COPY_MARKER = ".vulcan-filesystem";
	private final FileSystemProjectConfigDto config;
	
	public FileSystemRepositoryAdaptor(FileSystemProjectConfigDto config) throws RepositoryException {
		this.config = config;
	}

	public void createWorkingCopy(File targetDir, BuildDetailCallback buildDetailCallback) throws RepositoryException {
		final File sourceDir = new File(config.getSourceDirectory());
		
		try {
			FileUtils.copyDirectory(sourceDir, targetDir);
			FileUtils.touch(new File(targetDir, WORKING_COPY_MARKER));
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}
	
	public void updateWorkingCopy(File targetDir, BuildDetailCallback buildDetailCallback) throws RepositoryException {
		// This will actually overwrite all files from source to target (even if they are the same)
		// so it's not as incremental as it should be.
		createWorkingCopy(targetDir, buildDetailCallback);
	}
	
	public boolean isWorkingCopy(File absolutePath) {
		if (new File(absolutePath, WORKING_COPY_MARKER).exists()) {
			return true;
		}
		
		return false;
	}
	
	public RevisionTokenDto getLatestRevision(RevisionTokenDto previousRevision) throws RepositoryException, InterruptedException {
		return new RevisionTokenDto(System.currentTimeMillis());
	}
	
	public String getRepositoryUrl() {
		return null;
	}
	
	public ChangeLogDto getChangeLog(RevisionTokenDto arg0, RevisionTokenDto arg1, OutputStream arg2) throws RepositoryException, InterruptedException {
		final ChangeLogDto empty = new ChangeLogDto();
		
		empty.setChangeSets(Collections.<ChangeSetDto>emptyList());
		
		return empty;
	}
	public List<RepositoryTagDto> getAvailableTags() throws RepositoryException {
		final RepositoryTagDto tag = new RepositoryTagDto();
		
		tag.setDescription("Not Available");
		tag.setName("N/A");
		
		return Collections.singletonList(tag);
	}
	
	public String getTagName() {
		return "N/A";
	}
	
	public void setTagName(String tagName) {
	}
}
