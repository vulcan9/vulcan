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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.Date;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

public class SubversionRepositoryAdaptor extends PluginSupport implements RepositoryAdaptor {
	static {
		// Prefer Basic in case server offers to use NTLM first.
		System.setProperty("svnkit.http.methods", "Basic");

		// Enable support for various protocols.
		FSRepositoryFactory.setup();
		DAVRepositoryFactory.setup();
		SVNRepositoryFactoryImpl.setup();
	}
	
	private final Log log = LogFactory.getLog(getClass());
	private final SubversionProjectConfigDto config;
	private final SubversionRepositoryProfileDto profile;
	private final SVNRepository svnRepository;
	private final EventHandler eventHandler = new EventHandler();
	private final String projectName;
	private final Map<String, Long> byteCounters;
	
	final DefaultSVNOptions options;
	final LineOfDevelopment lineOfDevelopment = new LineOfDevelopment();

	private long revision = -1;
	private long diffStartRevision = -1;
	
	private final StateManager stateManager;
	
	public SubversionRepositoryAdaptor(SubversionConfigDto repoConfig, ProjectConfigDto projectConfig, SubversionProjectConfigDto config, StateManager stateManager) throws ConfigException {
		this(repoConfig, projectConfig, config, stateManager, true);
	}

	SubversionRepositoryAdaptor(SubversionConfigDto repoConfig, ProjectConfigDto projectConfig, SubversionProjectConfigDto config, StateManager stateManager, boolean init) throws ConfigException {
		this(
			repoConfig,
			projectConfig,
			config,
			stateManager,
			createRepository(getSelectedEnvironment(
									repoConfig.getProfiles(),
									config.getRepositoryProfile(),
									"svn.profile.missing"),
									init));
	}

	SubversionRepositoryAdaptor(SubversionConfigDto repoConfig, ProjectConfigDto projectConfig, SubversionProjectConfigDto config, StateManager stateManager, SVNRepository svnRepository) throws ConfigException {
		this.config = config;
		this.stateManager = stateManager;
		this.projectName = projectConfig.getName();
		this.byteCounters = repoConfig.getWorkingCopyByteCounts();
		this.profile = getSelectedEnvironment(
				repoConfig.getProfiles(),
				config.getRepositoryProfile(),
				"svn.profile.missing");
		
		this.svnRepository = svnRepository;
		
		if (svnRepository == null) {
			this.options = null;
			return;
		}
		
		if (StringUtils.isNotBlank(profile.getUsername())) {
			svnRepository.setAuthenticationManager(
					new BasicAuthenticationManager(
							profile.getUsername(),
							profile.getPassword()));
		}
		
		this.options = new DefaultSVNOptions();
		this.options.setAuthStorageEnabled(false);
		
		lineOfDevelopment.setPath(config.getPath());
		lineOfDevelopment.setRepositoryRoot(profile.getRootUrl());
		lineOfDevelopment.setTagFolderNames(new HashSet<String>(Arrays.asList(repoConfig.getTagFolderNames())));
	}

	public RevisionTokenDto getLatestRevision() throws RepositoryException {
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
		
		revision = info.getRevision();
		return new RevisionTokenDto(revision, "r" + revision);
	}

	public void createWorkingCopy(File absolutePath, BuildDetailCallback buildDetailCallback) throws RepositoryException {
		synchronized (byteCounters) {
			if (byteCounters.containsKey(projectName)) {
				eventHandler.setPreviousByteCount(byteCounters.get(projectName).longValue());
			}
		}
		
		eventHandler.setBuildDetailCallback(buildDetailCallback);
		
		final SVNUpdateClient client = new SVNUpdateClient(svnRepository.getAuthenticationManager(), options);
		client.setEventHandler(eventHandler);
		
		try {
			final SVNRevision svnRev = SVNRevision.create(revision);
			client.doCheckout(
					getCompleteSVNURL(),
					absolutePath,
					svnRev,
					svnRev,
					config.isRecursive());
			
			synchronized (byteCounters) {
				byteCounters.put(projectName, eventHandler.getByteCount());
			}
			
			configureBugtraqIfNecessary(absolutePath);
		} catch (SVNCancelException e) {
		} catch (SVNException e) {
			throw new RepositoryException(e);
		}
	}
	public void updateWorkingCopy(File absolutePath, BuildDetailCallback buildDetailCallback) throws RepositoryException {
		final SVNUpdateClient client = new SVNUpdateClient(svnRepository.getAuthenticationManager(), options);
		client.setEventHandler(eventHandler);
		
		try {
			final SVNRevision svnRev = SVNRevision.create(revision);
			client.doUpdate(absolutePath, svnRev, config.isRecursive());
			
			configureBugtraqIfNecessary(absolutePath);
		} catch (SVNCancelException e) {
		} catch (SVNException e) {
			throw new RepositoryException(e);
		}
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
	
	List<ChangeSetDto> fetchChangeSets(final SVNRevision r1, final SVNRevision r2) throws RepositoryException {
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
					/* The log message for r1 is in the previous build report.  Don't include it twice */ 
					return;
				}
				
				final ChangeSetDto changeSet = new ChangeSetDto();
				
				changeSet.setRevision(new RevisionTokenDto(logEntryRevision, "r" + logEntryRevision));
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

	boolean isFatal(SVNException e) {
		final String message = e.getMessage();
		
		if (isNotBlank(message) && message.indexOf("does not exist in the repository or refers to an unrelated object") > 0) {
			log.error("Got non-fatal subversion exception with code " + e.getErrorMessage().getErrorCode(), e);
			return false;
		}
		
		return true;
	}

	void fetchDifferences(final SVNRevision r1, final SVNRevision r2, OutputStream os) throws RepositoryException {
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
	
	SVNURL getCompleteSVNURL() throws SVNException {
		return SVNURL.parseURIEncoded(lineOfDevelopment.getAbsoluteUrl());
	}
	
	String combinePatterns(String logRegex, String messagePattern) {
		final StringBuilder sb = new StringBuilder();
		
		if (isNotBlank(logRegex)) {
			sb.append(logRegex);
		}
		
		if (isNotBlank(messagePattern)) {
			if (sb.length() > 0) {
				sb.append('|');
			}
			
			sb.append(messagePattern.replaceAll("%BUGID%", "(\\\\d+)"));
		}
		
		return sb.toString();
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
		final ProjectConfigDto projectConfig = (ProjectConfigDto) orig.copy();
		
		final SVNWCClient client = new SVNWCClient(svnRepository.getAuthenticationManager(), options);
		
		SVNPropertyData prop;
		String logRegex = "";
		String messagePattern = "";
		
		prop = client.doGetProperty(absolutePath, "bugtraq:url", SVNRevision.BASE, null, false);
		if (prop != null) {
			logRegex = prop.getValue();
		}
		
		prop = client.doGetProperty(absolutePath, "bugtraq:message", SVNRevision.BASE, null, false);
		if (prop != null) {
			messagePattern = prop.getValue();
		}
		
		projectConfig.setBugtraqUrl(combinePatterns(logRegex, messagePattern));
		
		prop = client.doGetProperty(absolutePath, "bugtraq:logregex", SVNRevision.BASE, null, false);
		if (prop != null) {
			final String value = prop.getValue().replaceAll("\r", "");
			final String[] patterns = value.split("\n");
			
			projectConfig.setBugtraqLogRegex1(patterns[0]);
			
			if (patterns.length > 1) {
				projectConfig.setBugtraqLogRegex2(patterns[1]);
			}
		}
		
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
	private static SVNRepository createRepository(SubversionRepositoryProfileDto profile, boolean init) throws ConfigException {
		if (!init) {
			return null;
		}
		
		if (profile == null) {
			throw new ConfigException("svn.profile.not.selected", null);
		}
		try {
			return SVNRepositoryFactory.create(SVNURL.parseURIEncoded(profile.getRootUrl()));
		} catch (Exception e) {
			throw new RepositoryException(e);
		}
	}
	
	private static class EventHandler implements ISVNEventHandler {
		private long previousByteCount = -1;
		private long byteCount = 0;
		private BuildDetailCallback buildDetailCallback;
		private File previousFile;
		
		public void handleEvent(SVNEvent event, double progress) throws SVNException {
			final SVNEventAction action = event.getAction();
			if (action == SVNEventAction.UPDATE_ADD || action == SVNEventAction.UPDATE_COMPLETED) {
				if (previousFile != null) {
					byteCount += previousFile.length();
					previousFile = null;
				}
			}
			
			if (action == SVNEventAction.UPDATE_ADD) {
				if (event.getNodeKind() == SVNNodeKind.DIR) {
					byteCount += 1024;
				} else if (event.getNodeKind() == SVNNodeKind.FILE) {
					previousFile = event.getFile();
				}
				
				PluginSupport.setWorkingCopyProgress(buildDetailCallback, byteCount, previousByteCount);
			}
		}
		public void checkCancelled() throws SVNCancelException {
			if (Thread.interrupted()) {
				throw new SVNCancelException();
			}
		}
		void setBuildDetailCallback(BuildDetailCallback buildDetailCallback) {
			this.buildDetailCallback = buildDetailCallback;
		}
		long getByteCount() {
			return byteCount;
		}
		void setPreviousByteCount(long previousByteCount) {
			this.previousByteCount = previousByteCount;
		}
	}
}
