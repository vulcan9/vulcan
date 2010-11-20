/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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

import static net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType.Full;
import static net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType.Incremental;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.BuildOutcomeStore;
import net.sourceforge.vulcan.core.BuildPhase;
import net.sourceforge.vulcan.core.BuildStatusListener;
import net.sourceforge.vulcan.core.ConfigurationStore;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ProjectBuilderImpl implements ProjectBuilder {
	private final Log log = LogFactory.getLog(ProjectBuilder.class);
	
	private WorkingCopyUpdateExpert workingCopyUpdateExpert = new WorkingCopyUpdateExpert();
	
	/* Injected configuration fields */
	private ConfigurationStore configurationStore;
	private BuildOutcomeStore buildOutcomeStore;
	private ProjectManager projectManager;
	private BuildManager buildManager;
	private int deleteDirectoryAttempts = 1;
	private long deleteFailureSleepTime;
	private boolean diffsEnabled;
	
	/* State */
	private BuildPhase currentPhase;
	protected UpdateType updateType;
	
	private List<BuildStatusListener> buildListeners = new ArrayList<BuildStatusListener>();
	
	private List<BuildMessageDto> errors = new ArrayList<BuildMessageDto>();
	private List<BuildMessageDto> warnings = new ArrayList<BuildMessageDto>();
	private List<MetricDto> metrics = new ArrayList<MetricDto>();
	
	protected boolean building;
	protected boolean killing;
	protected boolean timeout;
	
	protected RevisionTokenDto previousRevision;
	protected ProjectStatusDto buildStatus;
	protected ProjectStatusDto previousStatus;
	protected ProjectStatusDto.Status previousOutcome;
	protected String killedBy;
	
	BuildDetailCallback buildDetailCallback;
	
	private Thread buildThread;
	
	public void init() {
	}
	
	public final void build(BuildDaemonInfoDto info, ProjectConfigDto currentTarget, BuildDetailCallback buildDetailCallback) {
		
		synchronized(this) {
			buildThread = Thread.currentThread();
			
			building = true;
			killing = false;
			timeout = false;
			
			notifyAll();
		}

		this.buildDetailCallback = new DelegatingBuildDetailCallback(
				buildDetailCallback, currentTarget.isSuppressErrors(),
				currentTarget.isSuppressWarnings());
		
		previousStatus = buildManager.getLatestStatus(currentTarget.getName());
		
		if (log.isDebugEnabled() && previousStatus != null) {
			log.debug("Project " + currentTarget.getName() + " previous build " + previousStatus.getBuildNumber() + " completed " + previousStatus.getCompletionDate());
		}
		
		try {
			buildStatus = createBuildStatus(currentTarget);
			
			buildManager.registerBuildStatus(info, this, currentTarget, buildStatus);
			
			buildProject(currentTarget);
			buildStatus.setStatus(Status.PASS);
		} catch (ConfigException e) {
			buildStatus.setStatus(Status.ERROR);
			buildStatus.setMessageKey(e.getKey());
			buildStatus.setMessageArgs(e.getArgs());
		} catch (TimeoutException e) {
			buildStatus.setStatus(Status.ERROR);
			buildStatus.setMessageKey("messages.build.timeout");
		} catch (KilledException e) {
			buildStatus.setStatus(Status.ERROR);
			buildStatus.setMessageKey("messages.build.killed");
			buildStatus.setMessageArgs(new String[] {killedBy});
		} catch (BuildFailedException e) {
			final String target = e.getTarget();
			buildStatus.setStatus(Status.FAIL);
			
			if (target != null) {
				buildStatus.setMessageKey("messages.build.failure.during.target");
				buildStatus.setMessageArgs(new String[] {e.getMessage(), target});
			} else {
				buildStatus.setMessageKey("messages.build.failure");
				buildStatus.setMessageArgs(new String[] {e.getMessage()});
			}
		} catch (ProjectUpToDateException e) {
			buildStatus.setWorkDirSupportsIncrementalUpdate(true);
			buildStatus.setStatus(Status.UP_TO_DATE);
		} catch (Throwable e) {
			log.error("unexpected error", e);
			
			buildStatus.setStatus(Status.ERROR);
			buildStatus.setMessageKey("messages.build.uncaught.exception");
			
			String phaseName = "(none)";
			
			if (currentPhase != null) {
				phaseName = currentPhase.name();
			}
			
			buildStatus.setMessageArgs(new String[] {currentTarget.getName(), e.getMessage(), phaseName});
		} finally {
			if (previousOutcome == null) {
				buildStatus.setStatusChanged(true);
			} else if (!buildStatus.getStatus().equals(Status.UP_TO_DATE)) {
				buildStatus.setStatusChanged(!previousOutcome.equals(buildStatus.getStatus()));
			}
			
			buildDetailCallback.setPhaseMessageKey(BuildPhase.Publish.getMessageKey());
			buildManager.targetCompleted(info, currentTarget, buildStatus);

			previousRevision = null;
			
			this.buildDetailCallback.setPhaseMessageKey(null);

			synchronized(this) {
					buildThread = null;
					building = false;
					killing = false;
					timeout = false;
				
				notifyAll();
			}
		}
	}
	public synchronized void abortCurrentBuild(boolean timeout, String requestUsername) {
		killedBy = requestUsername;
		
		if (timeout && !this.killing) {
			this.timeout = true;
		}
		if (building && buildThread != null) {
			killing = true;
			buildThread.interrupt();
		}
	}
	public synchronized boolean isBuilding() {
		return building;
	}
	public synchronized boolean isKilling() {
		return killing;
	}
	public void addBuildStatusListener(BuildStatusListener listener) {
		synchronized(buildListeners) {
			buildListeners.add(listener);
		}
	}
	public boolean removeBuildStatusListener(BuildStatusListener listener) {
		synchronized(buildListeners) {
			return buildListeners.remove(listener);
		}
	}
	public ConfigurationStore getConfigurationStore() {
		return configurationStore;
	}
	public void setConfigurationStore(ConfigurationStore store) {
		this.configurationStore = store;
	}
	public void setWorkingCopyUpdateExpert(WorkingCopyUpdateExpert workingCopyUpdateExpert) {
		this.workingCopyUpdateExpert = workingCopyUpdateExpert;
	}
	public BuildOutcomeStore getBuildOutcomeStore() {
		return buildOutcomeStore;
	}
	public void setBuildOutcomeStore(BuildOutcomeStore buildOutcomeStore) {
		this.buildOutcomeStore = buildOutcomeStore;
	}
	public BuildManager getBuildManager() {
		return buildManager;
	}
	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
	public ProjectManager getProjectManager() {
		return projectManager;
	}
	public void setProjectManager(ProjectManager projectManager) {
		this.projectManager = projectManager;
	}
	public void setDeleteDirectoryAttempts(int deleteDirectoryAttempts) {
		this.deleteDirectoryAttempts = deleteDirectoryAttempts;
	}
	public void setDeleteFailureSleepTime(long deleteFailureSleepTime) {
		this.deleteFailureSleepTime = deleteFailureSleepTime;
	}
	public boolean isDiffsEnabled() {
		return diffsEnabled;
	}
	public void setDiffsEnabled(boolean diffsEnabled) {
		this.diffsEnabled = diffsEnabled;
	}
	protected ProjectStatusDto createBuildStatus(ProjectConfigDto currentTarget) {
		final ProjectStatusDto status = createBuildOutcome(currentTarget.getName());
		
		if (previousStatus == null || previousStatus.getBuildNumber() == null) {
			status.setBuildNumber(0);
		} else {
			status.setBuildNumber(previousStatus.getBuildNumber() + 1);
		}
		
		status.setStartDate(new Date());
		status.setRequestedBy(currentTarget.getRequestedBy());
		status.setScheduledBuild(currentTarget.isScheduledBuild());
		status.setWorkDir(currentTarget.getWorkDir());
		status.setStatus(Status.BUILDING);
		status.setErrors(errors);
		status.setWarnings(warnings);
		status.setMetrics(metrics);
		
		return status;
	}
	protected ProjectStatusDto createBuildOutcome(String projectName) {
		final ProjectStatusDto dto = new ProjectStatusDto();
		final UUID id = UUIDUtils.generateTimeBasedUUID();
		
		dto.setName(projectName);
		dto.setId(id);
		dto.setBuildLogId(id);
		dto.setDiffId(id);
		
		return dto;
	}
	protected void buildProject(final ProjectConfigDto currentTarget) throws Exception {
		if (StringUtils.isBlank(currentTarget.getWorkDir())) {
			throw new ConfigException("messages.build.null.work.dir", null);
		}
		
		final RepositoryAdaptor ra = projectManager.getRepositoryAdaptor(currentTarget);

		doPhase(BuildPhase.CheckForUpdates, new PhaseCallback() {
			public void execute() throws Exception {
				checkBuildNeccessary(ra, currentTarget);
				determineUpdateType(currentTarget);
				buildStatus.setEstimatedBuildTimeMillis(buildOutcomeStore.loadAverageBuildTimeMillis(currentTarget.getName(), updateType));
			}
		});
		
		if (updateType == Full) {
			doPhase(BuildPhase.CleanWorkingCopy, new PhaseCallback() {
				public void execute() throws Exception {
					cleanWorkingDirectory(currentTarget.getWorkDir(), ra);
				};
			});
			
			doPhase(BuildPhase.CheckoutWorkingCopy, new PhaseCallback() {
				public void execute() throws Exception {
					createWorkingCopy(ra, currentTarget.getWorkDir());
				};
			});
		} else {
			doPhase(BuildPhase.UpdateWorkingCopy, new PhaseCallback() {
				public void execute() throws Exception {
					updateWorkingCopy(ra, currentTarget.getWorkDir());
				};
			});
		}
		
		doPhase(BuildPhase.GetChangeLog, new PhaseCallback() {
			public void execute() throws Exception {
				OutputStream diffOutputStream = null;
				
				if (previousRevision != null &&
						!previousRevision.equals(buildStatus.getRevision())) {
					diffOutputStream = diffsEnabled ? 
						new FileOutputStream(configurationStore.getChangeLog(currentTarget.getName(), buildStatus.getDiffId())) : null;
					
					// RepositoryAdaptor instance will close the OutputStream so it
					// is not closed here in a finally block.
					buildStatus.setChangeLog(ra.getChangeLog(previousRevision, buildStatus.getRevision(), diffOutputStream));
				}
				
				if (diffOutputStream == null) {
					buildStatus.setDiffId(null);
				}
			}
		});

		// We made it this far, so the next build can be incremental
		// even if this build results in ERROR because the working
		// copy should be in a known state to the RepositoryAdaptor.
		buildStatus.setWorkDirSupportsIncrementalUpdate(true);
		
		doPhase(BuildPhase.Build, new PhaseCallback() {
			public void execute() throws Exception {
				invokeBuilder(currentTarget);
			}
		});
	}
	protected File cleanWorkingDirectory(String workDir, RepositoryAdaptor ra) throws ConfigException, IOException {
		final File path = new File(workDir).getCanonicalFile();
		
		if (path.exists() && !ra.isWorkingCopy(path)) {
			throw new ConfigException(
					"errors.wont.delete.non.working.copy",
					new Object[] {path.toString()});
		}
		
		if (!createWorkingDirectories(path)) {
			throw new ConfigException(
					"errors.cannot.create.dir",
					new Object[] {path.toString()});
		}
		
		return path;
	}
	/**
	 * Create working directory and parent directories if they don't exist.
	 * @return success flag (false if directories were not created).
	 */
	protected boolean createWorkingDirectories(File path) throws ConfigException {
		if (!deleteWorkingDirectory(path)) {
			return false;
		}
		
		return path.mkdirs();
	}

	private boolean deleteWorkingDirectory(File path) throws ConfigException {
		int tries = 0;
		
		try {
			while (path.exists()) {
				tries++;
				try {
					FileUtils.deleteDirectory(path);
					break;
				} catch (IOException e) {
					if (tries >= deleteDirectoryAttempts) {
						throw e;
					}
					try {
						Thread.sleep(deleteFailureSleepTime);
					} catch (InterruptedException e1) {
						return false;
					}
				}
			}
		} catch (IOException e) {
				throw new ConfigException(
						"messages.build.cannot.delete.work.dir",
						new Object[] {
							path.getPath(),
							e.getMessage()});
		}
		
		return true;
	}
	
	protected RevisionTokenDto checkBuildNeccessary(final RepositoryAdaptor ra, final ProjectConfigDto currentTarget) throws ProjectUpToDateException, InterruptedException, StoreException, ConfigException {
		String tagName = currentTarget.getRepositoryTagName();
		
		if (!StringUtils.isBlank(tagName)) {
			ra.setTagName(tagName);
		} else {
			tagName = ra.getTagName();
			currentTarget.setRepositoryTagName(tagName);
		}
		
		ProjectStatusDto prevStatusByTag = previousStatus;
		
		if (previousStatus != null && !tagName.equals(previousStatus.getTagName())) {
			// Look up the most recent build with the same tag name.
			prevStatusByTag = buildOutcomeStore.loadMostRecentBuildOutcomeByTagName(currentTarget.getName(), tagName);
		}
		
		if (prevStatusByTag != null) {
			previousRevision = prevStatusByTag.getRevision();
			if (previousRevision == null) {
				previousRevision = prevStatusByTag.getLastKnownRevision();
			}
			previousOutcome = prevStatusByTag.getStatus();
		} else {
			previousRevision = null;
			previousOutcome = null;
		}
		
		final RevisionTokenDto currentRev = ra.getLatestRevision(previousRevision);
		
		buildStatus.setRevision(currentRev);
		buildStatus.setTagName(tagName);
		buildStatus.setRepositoryUrl(ra.getRepositoryUrl());
		
		determineBuildReason(currentTarget);
		
		return currentRev;
	}

	protected void determineBuildReason(final ProjectConfigDto currentTarget) throws ConfigException,	ProjectUpToDateException {
		ProjectRebuildExpert expert = new ProjectRebuildExpert();
		expert.setBuildManager(buildManager);
		expert.setProjectManager(projectManager);
		expert.setWorkingCopyUpdateExpert(workingCopyUpdateExpert);

		if (!expert.shouldBuild(currentTarget, previousStatus)) {
			throw new ProjectUpToDateException();
		}
		
		buildStatus.setBuildReasonKey(expert.getMessageKey());
		buildStatus.setBuildReasonArgs(expert.getMessageArgs());
	}

	protected void determineUpdateType(ProjectConfigDto currentTarget) throws StoreException {
		ProjectStatusDto previousStatusForWorkDir = previousStatus;
		
		if (previousStatus != null && !currentTarget.getWorkDir().equals(previousStatus.getWorkDir())) {
			// Look up the most recent build in the same work dir.
			previousStatusForWorkDir = buildOutcomeStore.loadMostRecentBuildOutcomeByWorkDir(currentTarget.getName(), currentTarget.getWorkDir());
		}
		
		updateType = workingCopyUpdateExpert.determineUpdateStrategy(currentTarget, previousStatusForWorkDir);
	}
	protected void createWorkingCopy(RepositoryAdaptor ra, String workDir) throws RepositoryException, IOException, ConfigException, InterruptedException {
		buildStatus.setUpdateType(Full);
		ra.createWorkingCopy(new File(workDir).getCanonicalFile(), buildDetailCallback);
	}
	protected void updateWorkingCopy(RepositoryAdaptor ra, String workDir) throws RepositoryException, IOException, ConfigException, InterruptedException {
		buildStatus.setUpdateType(Incremental);
		ra.updateWorkingCopy(new File(workDir).getCanonicalFile(), buildDetailCallback);
	}
	protected void invokeBuilder(final ProjectConfigDto target) throws TimeoutException, KilledException, BuildFailedException, ConfigException, IOException, StoreException {
		final File logFile = configurationStore.getBuildLog(target.getName(), buildStatus.getBuildLogId());
		
		final BuildTool tool = projectManager.getBuildTool(target);
		
		tool.buildProject(target, (ProjectStatusDto) buildStatus.copy(), logFile, buildDetailCallback);
	}
	protected final void doPhase(BuildPhase phase, PhaseCallback callback) throws Exception {
		currentPhase = phase;
		buildDetailCallback.setPhaseMessageKey(phase.getMessageKey());
		for (BuildStatusListener listener : getCurrentBuildListeners()) {
			listener.onBuildPhaseChanged(this, phase);
		}
		try {
			callback.execute();
		} catch (InterruptedException e) {
		} finally {
			this.buildDetailCallback.setPhaseMessageKey(null);
			this.buildDetailCallback.setDetail(null);
		}

		//clear interrupt flag if present
		Thread.interrupted();
		
		if (timeout) {
			throw new TimeoutException();
		}
		if (killing) {
			throw new KilledException();
		}
	}
	
	private List<BuildStatusListener> getCurrentBuildListeners() {
		synchronized(buildListeners) {
			return new ArrayList<BuildStatusListener>(buildListeners);
		}
	}

	protected interface PhaseCallback {
		void execute() throws Exception;
	}
	protected static final class KilledException extends Exception {}
	protected static final class ProjectUpToDateException extends Exception {}
	
	private class DelegatingBuildDetailCallback implements BuildDetailCallback {
		private final BuildDetailCallback delegate;
		private final boolean suppressErrors;
		private final boolean suppressWarnings;
		
		public DelegatingBuildDetailCallback(BuildDetailCallback delegate,
				boolean suppressErrors, boolean suppressWarnings) {
			this.delegate = delegate;
			this.suppressErrors = suppressErrors;
			this.suppressWarnings = suppressWarnings;
		}

		public void reportError(String message, String file, Integer lineNumber, String code) {
			if (!suppressErrors) {
				delegate.reportError(message, file, lineNumber, code);
				final BuildMessageDto error = new BuildMessageDto(message, file, lineNumber, code);
				errors.add(error);
				for (BuildStatusListener listener : getCurrentBuildListeners()) {
					listener.onErrorLogged(ProjectBuilderImpl.this, error);
				}
			}
		}

		public void reportWarning(String message, String file, Integer lineNumber, String code) {
			if (!suppressWarnings) {
				delegate.reportWarning(message, file, lineNumber, code);
				final BuildMessageDto warning = new BuildMessageDto(message, file, lineNumber, code);
				warnings.add(warning);
				for (BuildStatusListener listener : getCurrentBuildListeners()) {
					listener.onWarningLogged(ProjectBuilderImpl.this, warning);
				}
			}
		}

		public void addMetric(MetricDto metric) {
			metrics.add(metric);
			delegate.addMetric(metric);
		}
		
		public void setDetail(String detail) {
			delegate.setDetail(detail);
		}

		public void setDetailMessage(String messageKey, Object[] args) {
			delegate.setDetailMessage(messageKey, args);
		}
		
		public void setPhaseMessageKey(String key) {
			delegate.setPhaseMessageKey(key);
		}
	}
}
