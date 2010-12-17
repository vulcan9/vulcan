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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.core.support.FileSystem;
import net.sourceforge.vulcan.core.support.FileSystemImpl;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.RepositoryException;

import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MercurialRepository implements RepositoryAdaptor {
	protected static enum Command {
		clone(true),
		pull(true),
		incoming(true),
		log(false),
		diff(false),
		update(false),
		purge(false),
		branches(false),
		tags(false),
		heads(false);
		
		private final boolean remote;
 
		Command(boolean remote) {
			this.remote = remote;
		}
		
		public boolean isRemote() {
			return remote;
		}
	}
	
	private static final Log LOG = LogFactory.getLog(MercurialRepository.class);
	
	private final static Pattern tagWithRevisionPattern = Pattern.compile("^(.*)\\s+(\\d+:\\w+)$", Pattern.MULTILINE);
	
	private final ProjectConfigDto projectConfig;
	private final MercurialProjectConfig settings;
	private final MercurialConfig globals;
	
	private FileSystem fileSystem = new FileSystemImpl();
	
	public MercurialRepository(ProjectConfigDto projectConfig, MercurialConfig globals) {
		this.projectConfig = projectConfig;
		this.settings = (MercurialProjectConfig)projectConfig.getRepositoryAdaptorConfig().copy();
		this.globals = globals;
	}

	public boolean hasIncomingChanges(ProjectStatusDto mostRecentBuildInSameWorkDir) throws RepositoryException {
		try {
			final RevisionTokenDto latestRevision = getLatestRevision(mostRecentBuildInSameWorkDir.getRevision());
			
			if (!latestRevision.equals(mostRecentBuildInSameWorkDir.getRevision())) {
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
			clone(workDir, buildDetailCallback);
		} else {
			pull(buildDetailCallback);
		}
	}

	public void clone(File workDir, BuildDetailCallback buildDetailCallback) throws RepositoryException {
		if (!isRemoteRepositoryConfigured()) {
			throw new RepositoryException("hg.errors.no.repo.and.no.remote", null, workDir);
		}

		buildDetailCallback.setDetailMessage("hg.activity.clone", null);
		
		List<String> args = new ArrayList<String>();
		if (settings.isCloneWithPullProtocol()) {
			args.add("--pull");
		}
		if (settings.isUncompressed()) {
			args.add("--uncompressed");
		}
		args.add("--noupdate");
		args.add(settings.getRemoteRepositoryUrl());
		args.add(".");

		tryInvoke(Command.clone, args.toArray(new String[args.size()]));
	}
	
	public void pull(BuildDetailCallback buildDetailCallback) throws RepositoryException {
		if (!isRemoteRepositoryConfigured()) {
			return;
		}

		buildDetailCallback.setDetailMessage("hg.activity.pull", null);
		
		tryInvoke(Command.pull, settings.getRemoteRepositoryUrl());
	}
	
	public boolean isWorkingCopy() throws RepositoryException {
		if (!fileSystem.directoryExists(getLocalRepositoryPath())) {
			return false;
		}
		
		try {
			String[] args = { "--quiet" };
			final Invoker invoker = createInvoker();
			InvocationResult result = invoker.invoke("summary", getLocalRepositoryPath(), args);
			return result.isSuccess();
		} catch (IOException ignore) {
			return false;
		}
	}

	public RevisionTokenDto getLatestRevision(RevisionTokenDto previousRevision) throws RepositoryException, InterruptedException {
		InvocationResult result = tryInvoke(Command.log, "--rev", getSelectedBranch(), "--limit", "1", "--quiet");
		
		final String output = result.getOutput().trim();
		String[] parts = output.split(":", 2);
		
		if (parts.length != 2) {
			throw new RepositoryException("Expected log output: " + output, (Throwable)null);
		}
		
		return new RevisionTokenDto(Long.parseLong(parts[0]), output);
	}
	
	boolean hasIncomingChangesFromRemote() throws RepositoryException, InterruptedException {
		if (!isRemoteRepositoryConfigured()) {
			return false;
		}
		
		return tryInvoke(Command.incoming, "--rev", getSelectedBranch(), "--limit", "1", "--quiet", settings.getRemoteRepositoryUrl()).isSuccess();
	}

	boolean isRemoteRepositoryConfigured() {
		return !StringUtils.isBlank(settings.getRemoteRepositoryUrl());
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
	
	public void createPristineWorkingCopy(BuildDetailCallback buildDetailCallback) throws RepositoryException, InterruptedException {
		if (globals.isPurgeEnabled()) {
			buildDetailCallback.setDetailMessage("hg.activity.update", null);
			tryInvoke(Command.update, "--clean", getSelectedBranch());
			
			buildDetailCallback.setDetailMessage("hg.activity.purge", null);
			tryInvoke(Command.purge, "--all", "--config", "extensions.purge=");
		} else {
			buildDetailCallback.setDetailMessage("hg.activity.remove.working.copy", null);
			tryInvoke(Command.update, "--clean", "null");
			
			buildDetailCallback.setDetailMessage("hg.activity.purge", null);
			
			try {
				fileSystem.cleanDirectory(getLocalRepositoryPath(), new NameFileFilter(".hg"));
			} catch (IOException e) {
				throw new RepositoryException("hg.errors.delete.files", e);
			}
			
			buildDetailCallback.setDetailMessage("hg.activity.update", null);
			tryInvoke(Command.update, getSelectedBranch());
		}
	}

	public void updateWorkingCopy(BuildDetailCallback buildDetailCallback) throws RepositoryException {
		tryInvoke(Command.update, getSelectedBranch());
	}

	public ChangeLogDto getChangeLog(RevisionTokenDto previousRevision,	RevisionTokenDto currentRevision, OutputStream diffOutputStream) throws RepositoryException, InterruptedException {
		final String revisionRange = MessageFormat.format("{0}:{1}", previousRevision.getRevision()+1, currentRevision.getRevision());
		
		getDiff(revisionRange, diffOutputStream);
		
		return getChangeSets(revisionRange);
	}

	private void getDiff(String revisionRange, OutputStream diffOutputStream) throws RepositoryException {
		if (diffOutputStream == null) {
			return;
		}
		
		try {
			tryInvokeWithStream(Command.diff, diffOutputStream, "-r", revisionRange);
		} finally {
			try {
				diffOutputStream.close();
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}
	}

	private ChangeLogDto getChangeSets(final String revisionRange) throws RepositoryException {
		final InvocationResult result = tryInvoke(Command.log, "--style", "xml", "--verbose", "-r", revisionRange);
		
		final CommitLogParser parser = new CommitLogParser();
		
		try {
			return parser.parse(result.getOutput());
		} catch (Exception e) {
			throw new RepositoryException("hg.errors.parse.xml", e, e.getMessage());
		}
	}
	
	public List<RepositoryTagDto> getAvailableTagsAndBranches() throws RepositoryException {
		final List<RepositoryTagDto> results = new ArrayList<RepositoryTagDto>();
		final List<String> namedRevisions = new ArrayList<String>();
		
		addAvailableTags(Command.branches, results, namedRevisions);
		addAvailableTags(Command.tags, results, namedRevisions);
		
		addAnonymousHeads(results, namedRevisions);
		
		return results;
	}

	private void addAvailableTags(Command command, List<RepositoryTagDto> results, List<String> namedRevisions) throws RepositoryException {
		String[] args = new String[0];
		if (command == Command.branches) {
			args = new String[] {"--active"};
		}
		
		final InvocationResult result = tryInvoke(command, args);
		
		final Matcher matcher = tagWithRevisionPattern.matcher(result.getOutput());
		
		while (matcher.find()) {
			final String name = matcher.group(1).trim();
			final String revision = matcher.group(2);
			
			results.add(new RepositoryTagDto(name, name));
			namedRevisions.add(revision);
		}
	}

	private void addAnonymousHeads(List<RepositoryTagDto> results, List<String> namedRevisions) throws RepositoryException {
		final InvocationResult result = tryInvoke(Command.heads, "--quiet", "--topo");
		
		for (String s : result.getOutput().split("\n")) {
			s = s.trim();
			if (s.equals("") || namedRevisions.contains(s)) {
				continue;
			}
			
			results.add(new RepositoryTagDto(s, s));
		}
	}
	
	protected InvocationResult tryInvoke(Command command, String... args) throws RepositoryException {
		return tryInvokeWithStream(command, null, args);
	}
	
	protected InvocationResult tryInvokeWithStream(Command command, OutputStream output, String... args) throws RepositoryException {
		final File workDir = getLocalRepositoryPath();
		
		if (!fileSystem.directoryExists(workDir)) {
			try {
				fileSystem.createDirectory(workDir);
			} catch (IOException e) {
				throw new RepositoryException("hg.errors.mkdir", e, workDir);
			}
		}

		final Invoker invoker = createInvoker();
		
		if (output != null) {
			invoker.setOutputStream(output);
		}
		
		if (command.isRemote()) {
			args = addRemoteFlags(args);
		}
		
		try {
			return invoker.invoke(command.name(), workDir, args);
		} catch (IOException e) {
			final String errorText = invoker.getErrorText();
			LOG.error("Unexpected exception invoking hg: " + errorText, e);
			throw new RepositoryException("hg.errors.invocation", e, errorText, invoker.getExitCode());
		}
	}

	private String[] addRemoteFlags(String[] args) {
		List<String> list = new ArrayList<String>();
		
		addIfSpecified(list, "--ssh", settings.getSshCommand());
		addIfSpecified(list, "--remotecmd", settings.getRemoteCommand());
		
		list.addAll(Arrays.asList(args));
		
		return list.toArray(new String[list.size()]);
	}

	private void addIfSpecified(List<String> args, String flag, String value) {
		if (isNotBlank(value)) {
			args.add(flag);
			args.add(value);
		}
	}

	protected Invoker createInvoker() {
		final ProcessInvoker invoker = new ProcessInvoker();
		
		invoker.setExecutable(globals.getExecutable());
		
		return invoker;
	}

	public String getRepositoryUrl() {
		return settings.getRemoteRepositoryUrl();
	}

	public String getTagOrBranch() {
		return getSelectedBranch();
	}

	public void setTagOrBranch(String tagName) {
		settings.setBranch(tagName);
	}
	
	public void setFileSystem(FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}
	
	protected ProjectConfigDto getProjectConfig() {
		return projectConfig;
	}
	
	protected MercurialProjectConfig getSettings() {
		return settings;
	}
	
	protected MercurialConfig getGlobals() {
		return globals;
	}
}
