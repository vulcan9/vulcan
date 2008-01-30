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
package net.sourceforge.vulcan.subversion;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.integration.support.PluginSupport;
import net.sourceforge.vulcan.subversion.dto.SubversionConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionProjectConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionRepositoryProfileDto;

import org.apache.commons.lang.StringUtils;
import org.tigris.subversion.javahl.ClientException;
import org.tigris.subversion.javahl.Notify2;
import org.tigris.subversion.javahl.NotifyAction;
import org.tigris.subversion.javahl.NotifyInformation;
import org.tigris.subversion.javahl.PromptUserPassword;
import org.tigris.subversion.javahl.Revision;
import org.tigris.subversion.javahl.SVNClient;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

public class SubversionRepositoryAdaptor extends SubversionSupport implements RepositoryAdaptor {
	private final EventHandler eventHandler = new EventHandler();
	private final String projectName;
	private final Map<String, Long> byteCounters;
	
	final LineOfDevelopment lineOfDevelopment = new LineOfDevelopment();
	final SVNClient client = new SVNClient();
	
	private long revision = -1;
	private long diffStartRevision = -1;
	
	private final StateManager stateManager;
	
	boolean canceling = false;
	
	public SubversionRepositoryAdaptor(SubversionConfigDto globalConfig, ProjectConfigDto projectConfig, SubversionProjectConfigDto config, StateManager stateManager) throws ConfigException {
		this(globalConfig, projectConfig, config, stateManager, true);
	}

	protected SubversionRepositoryAdaptor(SubversionConfigDto globalConfig, ProjectConfigDto projectConfig, SubversionProjectConfigDto config, StateManager stateManager, boolean init) throws ConfigException {
		this(
			globalConfig,
			projectConfig,
			config,
			stateManager,
			init,
			getSelectedEnvironment(
					globalConfig.getProfiles(),
					config.getRepositoryProfile(),
					"svn.profile.missing"));
	}

	protected SubversionRepositoryAdaptor(SubversionConfigDto globalConfig, ProjectConfigDto projectConfig, SubversionProjectConfigDto config, StateManager stateManager, boolean init, SubversionRepositoryProfileDto profile) throws ConfigException {
		this(
			globalConfig,
			projectConfig,
			config,
			stateManager,
			profile,
			createRepository(profile, init));
	}

	protected SubversionRepositoryAdaptor(SubversionConfigDto globalConfig, ProjectConfigDto projectConfig, SubversionProjectConfigDto config, StateManager stateManager, final SubversionRepositoryProfileDto profile, SVNRepository svnRepository) throws ConfigException {
		super(config, profile, svnRepository);
		
		this.stateManager = stateManager;
		this.projectName = projectConfig.getName();
		
		if (globalConfig != null) {
			this.byteCounters = globalConfig.getWorkingCopyByteCounts();
		} else {
			this.byteCounters = Collections.emptyMap();
		}

		lineOfDevelopment.setPath(config.getPath());
		lineOfDevelopment.setRepositoryRoot(profile.getRootUrl());
		lineOfDevelopment.setTagFolderNames(new HashSet<String>(Arrays.asList(globalConfig.getTagFolderNames())));
		
		client.notification2(eventHandler);
		
		if (StringUtils.isNotBlank(profile.getUsername())) {
			client.setPrompt(new PromptUserPassword() {
				public String getUsername() {
					return profile.getUsername();
				}
				public String getPassword() {
					return profile.getPassword();
				}
				public String askQuestion(String arg0, String arg1, boolean arg2) {
					throw new UnsupportedOperationException();
				}
				public boolean askYesNo(String arg0, String arg1, boolean arg2) {
					return true;
				}
				public boolean prompt(String arg0, String arg1) {
					return true;
				}
			});
		}
	}

	public RevisionTokenDto getLatestRevision(RevisionTokenDto previousRevision) throws RepositoryException {
		final String path = lineOfDevelopment.getComputedRelativePath();
		final SVNDirEntry info;
		
		try {
			info = svnRepository.info(path, revision);
		} catch (SVNException e) {
			throw new RepositoryException(e);
		}
		
		if (info == null) {
			throw new RepositoryException("svn.path.not.exist",
					new String[] {path}, null);
		}
		
		final long lastChangedRevision = info.getRevision();
		
		/*
		 *  Get the revision of the newest log entry for this path.
		 *  See Issue 95 (http://code.google.com/p/vulcan/issues/detail?id=95).
		 */
		final SVNLogClient logClient = new SVNLogClient(
				svnRepository.getAuthenticationManager(), options);
		
		final ISVNLogEntryHandler handler = new ISVNLogEntryHandler() {
			public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
				revision = logEntry.getRevision();
			}
		};
		
		try {
			logClient.doLog(SVNURL.parseURIEncoded(profile.getRootUrl()),
					new String[] {lineOfDevelopment.getComputedRelativePath()},
					SVNRevision.HEAD, SVNRevision.HEAD, SVNRevision.create(lastChangedRevision),
					true, false, 1, handler);
		} catch (SVNException e) {
			throw new RepositoryException(e);
		}
		
		// If for some reason there were zero log entries, default to Last Changed Revision.
		if (revision < 0) {
			revision = lastChangedRevision;
		}
		
		return new RevisionTokenDto(revision, "r" + revision);
	}

	public void createWorkingCopy(File absolutePath, BuildDetailCallback buildDetailCallback) throws RepositoryException {
		synchronized (byteCounters) {
			if (byteCounters.containsKey(projectName)) {
				eventHandler.setPreviousFileCount(byteCounters.get(projectName).longValue());
			}
		}
		
		eventHandler.setBuildDetailCallback(buildDetailCallback);
		
		try {
			client.checkout(
					getCompleteSVNURL().toString(),
					absolutePath.toString(),
					Revision.getInstance(revision),
					config.isRecursive());
			
			synchronized (byteCounters) {
				byteCounters.put(projectName, eventHandler.getFileCount());
			}
			
			configureBugtraqIfNecessary(absolutePath);
		} catch (ClientException e) {
			if (!canceling) {
				throw new RepositoryException(e);
			}
		} catch (SVNException e) {
			throw new RepositoryException(e);
		}
	}
	
	public void updateWorkingCopy(File absolutePath, BuildDetailCallback buildDetailCallback) throws RepositoryException {
		try {
			client.update(absolutePath.toString(), Revision.getInstance(revision), config.isRecursive());
		} catch (ClientException e) {
			if (!canceling) {
				throw new RepositoryException(e);
			}
			throw new RepositoryException(e);
		}
	}
	
	public boolean isWorkingCopy(File path) {
		try {
			if (client.info(path.getAbsolutePath()) != null) {
				return true;
			}
		} catch (ClientException ignore) {
		}
		return false;
	}
	
	public ChangeLogDto getChangeLog(RevisionTokenDto first, RevisionTokenDto last, OutputStream diffOutputStream) throws RepositoryException {
		final SVNRevision r1 = SVNRevision.create(first.getRevision().longValue());
		final SVNRevision r2 = SVNRevision.create(revision);
		
		final List<ChangeSetDto> changeSets = fetchChangeSets(r1, r2);
		fetchDifferences(SVNRevision.create(diffStartRevision), r2, diffOutputStream);
		
		final ChangeLogDto changeLog = new ChangeLogDto();
		
		changeLog.setChangeSets(changeSets);
		
		return changeLog;
	}

	@SuppressWarnings("unchecked")
	public List<RepositoryTagDto> getAvailableTags() throws RepositoryException {
		final String projectRoot = lineOfDevelopment.getComputedTagRoot();
		
		final List<RepositoryTagDto> tags = new ArrayList<RepositoryTagDto>();
		
		final RepositoryTagDto trunkTag = new RepositoryTagDto();
		trunkTag.setDescription("trunk");
		trunkTag.setName("trunk");
		tags.add(trunkTag);
		
		try {
			final Collection<SVNDirEntry> entries = svnRepository.getDir(projectRoot, -1, null, (Collection) null);
			
			for (SVNDirEntry entry : entries) {
				final String folderName = entry.getName();
				if (entry.getKind() == SVNNodeKind.DIR && lineOfDevelopment.isTag(folderName)) {
					addTags(projectRoot, folderName, tags);
				}
			}
		} catch (SVNException e) {
			throw new RepositoryException(e);
		}
		
		Collections.sort(tags, new Comparator<RepositoryTagDto>() {
			public int compare(RepositoryTagDto t1, RepositoryTagDto t2) {
				return t1.getName().compareTo(t2.getName());
			}
		});
		
		return tags;
	}
	
	public String getRepositoryUrl() {
		try {
			return getCompleteSVNURL().toString();
		} catch (SVNException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getTagName() {
		return lineOfDevelopment.getComputedTagName();
	}

	public void setTagName(String tagName) {
		lineOfDevelopment.setAlternateTagName(tagName);
	}
	
	protected List<ChangeSetDto> fetchChangeSets(final SVNRevision r1, final SVNRevision r2) throws RepositoryException {
		final SVNLogClient logClient = new SVNLogClient(svnRepository.getAuthenticationManager(), options);
		logClient.setEventHandler(eventHandler);
		
		final List<ChangeSetDto> changeSets = new ArrayList<ChangeSetDto>();
		
		diffStartRevision = r2.getNumber();
		
		final ISVNLogEntryHandler handler = new ISVNLogEntryHandler() {
			@SuppressWarnings("unchecked")
			public void handleLogEntry(SVNLogEntry logEntry) {
				final long logEntryRevision = logEntry.getRevision();
				if (diffStartRevision > logEntryRevision) {
					diffStartRevision = logEntryRevision;
				}
				
				if (logEntryRevision == r1.getNumber()) {
					/* The log message for r1 is in the previous build report.  Don't include it twice. */ 
					return;
				}
				
				final ChangeSetDto changeSet = new ChangeSetDto();
				
				changeSet.setRevisionLabel("r" + logEntryRevision);
				changeSet.setAuthor(logEntry.getAuthor());
				changeSet.setMessage(logEntry.getMessage());
				changeSet.setTimestamp(new Date(logEntry.getDate().getTime()));
				
				final Set<String> paths = logEntry.getChangedPaths().keySet();
				
				changeSet.setModifiedPaths(paths.toArray(new String[paths.size()]));
				
				changeSets.add(changeSet);
			}
		};
		
		try {
			logClient.doLog(
					SVNURL.parseURIEncoded(profile.getRootUrl()),
					new String[] {lineOfDevelopment.getComputedRelativePath()},
					r1, r1, r2,
					true,
					true,
					0,
					handler);
		} catch (SVNCancelException e) {
		} catch (SVNException e) {
			if (isFatal(e)) {
				throw new RepositoryException(e);
			}
		}
		return changeSets;
	}

	protected void fetchDifferences(final SVNRevision r1, final SVNRevision r2, OutputStream os) throws RepositoryException {
		final SVNDiffClient diffClient = new SVNDiffClient(svnRepository.getAuthenticationManager(), options);

		diffClient.setEventHandler(eventHandler);
		
		try {
			diffClient.doDiff(getCompleteSVNURL(), r1, r1, r2, true, true, os);
			os.close();
		} catch (SVNCancelException e) {
		} catch (SVNException e) {
			if (e.getErrorMessage().getErrorCode() == SVNErrorCode.RA_DAV_PATH_NOT_FOUND) {
				// This usually happens when building from a different branch or tag that 
				// does not share ancestry with the previous build.
				log.info("Failed to obtain diff of revisions r"
						+ r1.getNumber() + ":" + r2.getNumber(), e);
			} else {
				throw new RepositoryException(e);
			}
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
	}
	
	protected SVNURL getCompleteSVNURL() throws SVNException {
		return SVNURL.parseURIEncoded(lineOfDevelopment.getAbsoluteUrl());
	}

	@SuppressWarnings("unchecked")
	private void addTags(String projectRoot, String folderName, List<RepositoryTagDto> tags) throws SVNException {
		final String path = projectRoot + "/" + folderName;
		
		final Collection<SVNDirEntry> entries = svnRepository.getDir(path, -1, null, (Collection) null);
		
		for (SVNDirEntry entry : entries) {
			final String tagName = entry.getName();
			if (entry.getKind() == SVNNodeKind.DIR) {
				RepositoryTagDto tag = new RepositoryTagDto();
				tag.setName(folderName + "/" + tagName);
				tag.setDescription(tag.getName());
				tags.add(tag);
			}
		}
	}
	private void configureBugtraqIfNecessary(File absolutePath) throws SVNException {
		if (!this.config.isObtainBugtraqProperties()) {
			return;
		}
		
		final ProjectConfigDto orig = stateManager.getProjectConfig(projectName);
		
		final SVNWCClient client = new SVNWCClient(svnRepository.getAuthenticationManager(), options);
		
		final Map<String, String> bugtraqProps = new HashMap<String, String>();
		
		getWorkingCopyProperty(client, absolutePath, BUGTRAQ_URL, bugtraqProps);
		getWorkingCopyProperty(client, absolutePath, BUGTRAQ_MESSAGE, bugtraqProps);
		getWorkingCopyProperty(client, absolutePath, BUGTRAQ_LOGREGEX, bugtraqProps);
		
		final ProjectConfigDto projectConfig = (ProjectConfigDto) orig.copy();
		
		configureBugtraq(projectConfig, bugtraqProps);
		
		if (!orig.equals(projectConfig)) {
			try {
				log.info("Updating bugtraq information for project " + projectName);
				stateManager.updateProjectConfig(projectName, projectConfig, false);
			} catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException(e);
			}
		}
	}

	private void getWorkingCopyProperty(final SVNWCClient client, File absolutePath, String propName, final Map<String, String> bugtraqProps) throws SVNException {
		SVNPropertyData prop;
		prop = client.doGetProperty(absolutePath, propName, SVNRevision.BASE, null, false);
		bugtraqProps.put(propName, getValueIfNotNull(prop));
	}

	private String getValueIfNotNull(SVNPropertyData prop) {
		if (prop != null) {
			return prop.getValue();
		}
		return StringUtils.EMPTY;
	}

	private class EventHandler implements ISVNEventHandler, Notify2 {
		private long previousFileCount = -1;
		private long fileCount = 0;
		private BuildDetailCallback buildDetailCallback;
		
		public void onNotify(NotifyInformation info) {
			if (info.getAction() == NotifyAction.update_add) {
				fileCount++;
				PluginSupport.setWorkingCopyProgress(buildDetailCallback, fileCount, previousFileCount, ProgressUnit.Files);
			}
			
			if (Thread.interrupted()) {
				try {
					client.cancelOperation();
					canceling = true;
				} catch (ClientException e) {
					log.error("Error canceling svn operation", e);
				}
			}
		}
		
		public void handleEvent(SVNEvent event, double progress) throws SVNException {
		}
		
		public void checkCancelled() throws SVNCancelException {
			if (Thread.interrupted()) {
				throw new SVNCancelException();
			}
		}
		void setBuildDetailCallback(BuildDetailCallback buildDetailCallback) {
			this.buildDetailCallback = buildDetailCallback;
		}
		long getFileCount() {
			return fileCount;
		}
		void setPreviousFileCount(long previousByteCount) {
			this.previousFileCount = previousByteCount;
		}
	}
}
