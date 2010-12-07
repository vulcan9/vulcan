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
package net.sourceforge.vulcan.mercurial;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.exception.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MercurialRepository implements RepositoryAdaptor {
	private static final Log LOG = LogFactory.getLog(MercurialRepository.class);
	
	private final ProjectConfigDto projectConfig;
	private final MercurialProjectConfig settings;
	
	public MercurialRepository(ProjectConfigDto projectConfig) {
		this.projectConfig = projectConfig;
		this.settings = (MercurialProjectConfig)projectConfig.getRepositoryAdaptorConfig().copy();
	}

	public boolean hasIncomingChanges(ProjectStatusDto mostRecentBuildInSameWorkDir) throws RepositoryException {
		try {
			if (!getLatestRevision(mostRecentBuildInSameWorkDir.getRevision()).equals(mostRecentBuildInSameWorkDir.getRevision())) {
				return true;
			}
			
			return hasIncomingChangesFromRemote();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void prepareRepository(BuildDetailCallback buildDetailCallback) throws RepositoryException, InterruptedException {
		final File workDir = getLocalRepositoryPath();
		
		if (!isWorkingCopy()) {
			if (!isDirectoryPresent(workDir)) {
				if (!workDir.mkdirs()) {
					throw new RepositoryException("hg.errors.mkdir", new Object[] {workDir}, null);
				}
			}
			buildDetailCallback.setDetailMessage("hg.activity.clone", null);
			//TODO: add flags for --pull, --uncompressed
			tryInvoke("clone", "--noupdate", settings.getRemoteRepositoryUrl(), ".");
		} else {
			buildDetailCallback.setDetailMessage("hg.activity.pull", null);
			tryInvoke("pull", settings.getRemoteRepositoryUrl());
		}
	}
	
	public boolean isWorkingCopy() throws RepositoryException {
		if (!isDirectoryPresent(getLocalRepositoryPath())) {
			return false;
		}
		
		try {
			InvocationResult result = invoke("summary", "--quiet");
			return result.isSuccess();
		} catch (IOException ignore) {
			return false;
		}
	}

	protected boolean isDirectoryPresent(File absolutePath) {
		return absolutePath.isDirectory();
	}

	public RevisionTokenDto getLatestRevision(RevisionTokenDto previousRevision) throws RepositoryException, InterruptedException {
		InvocationResult result = tryInvoke("log", "--rev", getSelectedBranch(), "--limit", "1", "--quiet");
		
		final String output = result.getOutput().trim();
		String[] parts = output.split(":", 2);
		
		if (parts.length != 2) {
			throw new RepositoryException("Expected log output: " + output, (Throwable)null);
		}
		
		return new RevisionTokenDto(Long.parseLong(parts[0]), output);
	}
	
	boolean hasIncomingChangesFromRemote() throws RepositoryException, InterruptedException {
		if (StringUtils.isBlank(settings.getRemoteRepositoryUrl())) {
			return false;
		}
		
		return tryInvoke("incoming", "--rev", getSelectedBranch(), "--limit", "1", "--quiet", settings.getRemoteRepositoryUrl()).isSuccess();
	}

	String getSelectedBranch() {
		if (StringUtils.isBlank(settings.getBranch())) {
			return "default";
		}
		
		return settings.getBranch();
	}

	File getLocalRepositoryPath() {
		return new File(projectConfig.getWorkDir());
	}

	private InvocationResult tryInvoke(String command, String... args) throws RepositoryException {
		final Invoker invoker = createInvoker();

		try {
			return invoker.invoke(command, getLocalRepositoryPath(), args);
		} catch (IOException e) {
			LOG.error("Unexpected exception invoking hg: " + invoker.getErrorText(), e);
			throw new RepositoryException("hg.errors.invocation", new Object[] {invoker.getErrorText(), invoker.getExitCode()}, e);
		}
	}

	private InvocationResult invoke(String command, String... args) throws IOException {
		final Invoker invoker = createInvoker();
		return invoker.invoke(command, getLocalRepositoryPath(), args);
	}

	protected Invoker createInvoker() {
		return new ProcessInvoker();
	}
	
	public void createPristineWorkingCopy(UpdateType updateType, BuildDetailCallback buildDetailCallback) throws RepositoryException, InterruptedException {
	}

	public void updateWorkingCopy(BuildDetailCallback buildDetailCallback) throws RepositoryException {
	}

	public ChangeLogDto getChangeLog(RevisionTokenDto previousRevision,	RevisionTokenDto currentRevision, OutputStream diffOutputStream) throws RepositoryException, InterruptedException {
		return null;
	}
	
	public List<RepositoryTagDto> getAvailableTags() throws RepositoryException {
		return null;
	}

	public String getRepositoryUrl() {
		return null;
	}

	public String getTagName() {
		return getSelectedBranch();
	}

	public void setTagName(String tagName) {
		settings.setBranch(tagName);
	}
}
