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

import static net.sourceforge.vulcan.dto.ProjectStatusDto.Status.ERROR;
import static net.sourceforge.vulcan.dto.ProjectStatusDto.Status.FAIL;
import static net.sourceforge.vulcan.dto.ProjectStatusDto.Status.PASS;
import static net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType.Full;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
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
import net.sourceforge.vulcan.core.BuildTarget;
import net.sourceforge.vulcan.core.ConfigurationStore;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class BuildContext {
	private final BuildTarget target;
	
	private ProjectStatusDto lastBuild;
	private ProjectStatusDto lastBuildFromSameTag;
	private ProjectStatusDto lastBuildInSameWorkDir;

	private RepositoryAdaptor repositoryAdaptor;

	public BuildContext(BuildTarget target) {
		this.target = target;
	}
	
	public ProjectConfigDto getConfig() { 
		return target.getProjectConfig();
	}
	
	public ProjectStatusDto getCurrentStatus() {
		return target.getStatus();
	}
	
	public ProjectStatusDto getLastBuild() {
		return lastBuild;
	}
	
	public void setLastBuild(ProjectStatusDto lastBuild) {
		this.lastBuild = lastBuild;
	}
	
	public ProjectStatusDto getLastBuildFromSameTag() {
		return lastBuildFromSameTag;
	}
	
	public void setLastBuildFromSameTag(ProjectStatusDto lastBuildFromSameTag) {
		this.lastBuildFromSameTag = lastBuildFromSameTag;
	}
	
	public ProjectStatusDto getLastBuildInSameWorkDir() {
		return lastBuildInSameWorkDir;
	}
	
	public void setLastBuildInSameWorkDir(ProjectStatusDto lastBuildInSameWorkDir) {
		this.lastBuildInSameWorkDir = lastBuildInSameWorkDir;
	}

	public RepositoryAdaptor getRepositoryAdaptor() {
		return repositoryAdaptor;
	}
	
	public void setRepositoryAdatpor(RepositoryAdaptor repositoryAdaptor) {
		this.repositoryAdaptor = repositoryAdaptor;
	}

	public UpdateType getUpdateType() {
		return target.getStatus().getUpdateType();
	}
	
	public void setUpdateType(UpdateType updateType) {
		target.getStatus().setUpdateType(updateType);
	}

	public String getProjectName() {
		return target.getProjectName();
	}

	public RevisionTokenDto getRevision() {
		return target.getStatus().getRevision();
	}

	public void setResult(Status result) {
		setResult(result, null);
	}

	public void setResult(Status result, String messageKey, Object... args) {
		final ProjectStatusDto status = target.getStatus();
		
		status.setStatus(result);
		status.setMessageKey(messageKey);
		status.setMessageArgs(args);
		
		if (getLastBuild() == null) {
			status.setStatusChanged(true);
		} else {
			status.setStatusChanged(!getLastBuild().getStatus().equals(result));
		}
	}
}

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ProjectBuilderImpl implements ProjectBuilder {
	protected Log log = LogFactory.getLog(ProjectBuilder.class);
	
	private WorkingCopyUpdateExpert workingCopyUpdateExpert = new WorkingCopyUpdateExpert();
	
	/* Injected configuration fields */
	private ConfigurationStore configurationStore;
	private BuildOutcomeStore buildOutcomeStore;
	private ProjectManager projectManager;
	private BuildManager buildManager;
	private boolean diffsEnabled;
	
	/* State */
	protected boolean building;
	protected boolean killing;
	protected boolean timeout;
	
	protected String killedBy;
	
	DelegatingBuildDetailCallback buildDetailCallback;
	
	private Thread buildThread;
	
	public void init() {
	}
	
	public final void build(BuildDaemonInfoDto info, BuildTarget currentTarget, BuildDetailCallback buildDetailCallback) {
		initializeThreadState();

		try {
			buildInternal(info, currentTarget, buildDetailCallback);
		} finally {
			synchronized(this) {
				buildThread = null;
				building = false;
				killing = false;
				timeout = false;
			
				notifyAll();
			}
		}
	}

	protected void buildInternal(BuildDaemonInfoDto info, BuildTarget currentTarget, BuildDetailCallback buildDetailCallback) {
		initializeBuildDetailCallback(currentTarget, buildDetailCallback);
		BuildContext buildContext = null;
		
		try {
			buildContext = initializeBuildStatus(currentTarget);
			
			if (log.isDebugEnabled() && buildContext.getLastBuild() != null) {
				log.debug("Project " + currentTarget.getProjectName() + " previous build " + buildContext.getLastBuild().getBuildNumber() + " completed " + buildContext.getLastBuild().getCompletionDate());
			}
			
			//TODO: should take target, not config + status. consider moving to BuildDaemonImpl.
			buildManager.registerBuildStatus(info, this, currentTarget.getProjectConfig(), currentTarget.getStatus());
			
			executeBuildPhases(buildContext);
			
			buildContext.setResult(PASS);
		} catch (ConfigException e) {
			buildContext.setResult(ERROR, e.getKey(), e.getArgs());
		} catch (TimeoutException e) {
			if (timeout) {
				buildContext.setResult(ERROR, "messages.build.timeout");
			} else {
				buildContext.setResult(ERROR, "messages.build.killed", killedBy);
			}
		} catch (BuildFailedException e) {
			if (e.getTarget() != null) {
				buildContext.setResult(FAIL, "messages.build.failure.during.target", e.getMessage(), e.getTarget());
			} else {
				buildContext.setResult(FAIL, "messages.build.failure", e.getMessage());
			}
		} catch (Throwable e) {
			log.error("unexpected error", e);
			
			BuildPhase currentPhase = this.buildDetailCallback.getCurrentPhase();
			String phaseName = currentPhase != null ? currentPhase.name() : "(none)";
			
			buildContext.setResult(ERROR, "messages.build.uncaught.exception", buildContext.getProjectName(), e.getMessage(), phaseName);
		} finally {
			this.buildDetailCallback.setPhase(BuildPhase.Publish);
			// TODO: targetCompleted should take currentTarget, not config + status
			buildManager.targetCompleted(info, currentTarget.getProjectConfig(), currentTarget.getStatus());

			this.buildDetailCallback.clearPhase();
		}
	}

	protected synchronized void initializeThreadState() {
		buildThread = Thread.currentThread();
		
		building = true;
		killing = false;
		timeout = false;
		
		notifyAll();
	}

	void initializeBuildDetailCallback(BuildTarget target, BuildDetailCallback buildDetailCallback) {
		this.buildDetailCallback = new DelegatingBuildDetailCallback(
				target.getStatus(),
				buildDetailCallback,
				this,
				target.getProjectConfig().isSuppressErrors(),
				target.getProjectConfig().isSuppressWarnings());
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
		buildDetailCallback.addListener(listener);
	}
	
	public boolean removeBuildStatusListener(BuildStatusListener listener) {
		return buildDetailCallback.removeListener(listener);
	}
	
	public void setConfigurationStore(ConfigurationStore store) {
		this.configurationStore = store;
	}
	
	public void setWorkingCopyUpdateExpert(WorkingCopyUpdateExpert workingCopyUpdateExpert) {
		this.workingCopyUpdateExpert = workingCopyUpdateExpert;
	}
	
	public void setBuildOutcomeStore(BuildOutcomeStore buildOutcomeStore) {
		this.buildOutcomeStore = buildOutcomeStore;
	}
	
	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
	
	public void setProjectManager(ProjectManager projectManager) {
		this.projectManager = projectManager;
	}

	public void setDiffsEnabled(boolean diffsEnabled) {
		this.diffsEnabled = diffsEnabled;
	}
	
	protected BuildContext initializeBuildStatus(BuildTarget currentTarget) throws StoreException, ConfigException {
		BuildContext context = new BuildContext(currentTarget);
		
		context.setRepositoryAdatpor(projectManager.getRepositoryAdaptor(currentTarget.getProjectConfig()));
		
		ProjectStatusDto lastBuild = buildManager.getLatestStatus(currentTarget.getProjectName());
		context.setLastBuild(lastBuild);
		
		ProjectStatusDto lastBuildInSameWorkDir = lastBuild;
		if (lastBuild != null && !lastBuild.getWorkDir().equals(context.getConfig().getWorkDir())) {
			lastBuildInSameWorkDir = buildOutcomeStore.loadMostRecentBuildOutcomeByWorkDir(context.getConfig().getName(), context.getConfig().getWorkDir());
		}
		
		context.setLastBuildInSameWorkDir(lastBuildInSameWorkDir);
		
		if (StringUtils.isNotBlank(currentTarget.getProjectConfig().getRepositoryTagName())) {
			context.getRepositoryAdaptor().setTagOrBranch(currentTarget.getProjectConfig().getRepositoryTagName());
		}
		
		final String tagName = context.getRepositoryAdaptor().getTagOrBranch();
		
		// TODO: experts use this to compare with previous build. they should get it from current status instead. 
		context.getConfig().setRepositoryTagName(tagName);
		
		ProjectStatusDto lastBuildFromSameTag = lastBuild;
		if (lastBuild != null && !lastBuild.getTagName().equals(tagName)) {
			lastBuildFromSameTag = buildOutcomeStore.loadMostRecentBuildOutcomeByTagName(currentTarget.getProjectName(), tagName);
		}
		
		context.setLastBuildFromSameTag(lastBuildFromSameTag);			
		
		ProjectStatusDto buildStatus = currentTarget.getStatus();
		
		generateUniqueBuildId(buildStatus);
		
		if (context.getLastBuild() == null || context.getLastBuild().getBuildNumber() == null) {
			buildStatus.setBuildNumber(0);
		} else {
			buildStatus.setBuildNumber(context.getLastBuild().getBuildNumber() + 1);
		}
		
		buildStatus.setStartDate(new Date());
		
		//TODO: law of Demeter
		buildStatus.setRequestedBy(currentTarget.getProjectConfig().getRequestedBy());
		buildStatus.setScheduledBuild(currentTarget.getProjectConfig().isScheduledBuild());
		buildStatus.setWorkDir(currentTarget.getProjectConfig().getWorkDir());
		buildStatus.setStatus(Status.BUILDING);
		buildStatus.setTagName(tagName);
		
		return context;
	}
	
	protected void generateUniqueBuildId(ProjectStatusDto status) {
		final UUID id = UUIDUtils.generateTimeBasedUUID();
		
		status.setId(id);
		status.setBuildLogId(id);
		status.setDiffId(id);
	}
	
	protected RunnablePhaseImpl prepareRepository = new RunnablePhaseImpl(BuildPhase.PrepareRepository) {
		protected void executePhase(BuildContext buildContext) throws Exception {
			prepareRepository(buildContext);
			determineUpdateType(buildContext);
			setEstimatedBuildDuration(buildContext);
		}
		
		private RevisionTokenDto prepareRepository(BuildContext buildContext) throws InterruptedException, StoreException, ConfigException {
			final RepositoryAdaptor repository = buildContext.getRepositoryAdaptor();
			
			repository.prepareRepository(buildDetailCallback);
			
			final RevisionTokenDto currentRev = repository.getLatestRevision(null);
			
			// TODO law of demeter
			ProjectStatusDto buildStatus = buildContext.getCurrentStatus();
			buildStatus.setRevision(currentRev);
			buildStatus.setRepositoryUrl(repository.getRepositoryUrl());
			
			return currentRev;
		}

		private void determineUpdateType(BuildContext buildContext) throws StoreException {
			ProjectStatusDto previousStatusForWorkDir = buildContext.getLastBuildInSameWorkDir();
			
			buildContext.setUpdateType(workingCopyUpdateExpert.determineUpdateStrategy(buildContext.getConfig(), previousStatusForWorkDir));
		}

		private void setEstimatedBuildDuration(BuildContext buildContext) {
			buildContext.getCurrentStatus().setEstimatedBuildTimeMillis(buildOutcomeStore.loadAverageBuildTimeMillis(buildContext.getProjectName(), buildContext.getUpdateType()));
		}
	};
	
	protected RunnablePhaseImpl createWorkingCopy = new RunnablePhaseImpl(BuildPhase.CheckoutWorkingCopy) {
		protected void executePhase(BuildContext buildContext) throws Exception {
			if (buildContext.getUpdateType() == Full) {
				buildContext.getRepositoryAdaptor().createPristineWorkingCopy(buildDetailCallback);
			} else {
				buildContext.getRepositoryAdaptor().updateWorkingCopy(buildDetailCallback);
			}
			
			// We made it this far, so the next build can be incremental
			// even if this build results in ERROR because the working
			// copy should be in a known state to the RepositoryAdaptor.
			buildContext.getCurrentStatus().setWorkDirSupportsIncrementalUpdate(true);
		}
	};

	protected RunnablePhaseImpl getChangeLog = new RunnablePhaseImpl(BuildPhase.GetChangeLog) {
		@Override
		protected void executePhase(BuildContext buildContext) throws Exception {
			if (buildContext.getLastBuildFromSameTag() == null
				|| buildContext.getLastBuildFromSameTag().getLastKnownRevision() == null) {
				return;
			}
			
			final RevisionTokenDto previousRevision = buildContext.getLastBuildFromSameTag().getLastKnownRevision();
			
			if (previousRevision.equals(buildContext.getRevision())) {
				return;
			}
			
			OutputStream diffOutputStream = null;
			
			if (diffsEnabled) {
				diffOutputStream = new FileOutputStream(configurationStore.getChangeLog(buildContext.getProjectName(), buildContext.getCurrentStatus().getDiffId()));
			}
			
			try {
				buildContext.getCurrentStatus().setChangeLog(buildContext.getRepositoryAdaptor().getChangeLog(previousRevision, buildContext.getRevision(), diffOutputStream));
			} finally {
				if (diffOutputStream != null) {
					diffOutputStream.close();
				}
			}
			
			if (diffOutputStream == null) {
				buildContext.getCurrentStatus().setDiffId(null);
			}
		}
	};
	
	protected RunnablePhaseImpl invokeBuildTool = new RunnablePhaseImpl(BuildPhase.Build) {
		@Override
		protected void executePhase(BuildContext buildContext) throws Exception {
			final File logFile = configurationStore.getBuildLog(buildContext.getProjectName(), buildContext.getCurrentStatus().getBuildLogId());
			
			final BuildTool tool = projectManager.getBuildTool(buildContext.getConfig());
			
			tool.buildProject(buildContext.getConfig(), (ProjectStatusDto) buildContext.getCurrentStatus().copy(), logFile, buildDetailCallback);
		}
	};
	
	protected List<? extends RunnablePhase> phases = Arrays.asList(prepareRepository, createWorkingCopy, getChangeLog, invokeBuildTool);
	
	protected void executeBuildPhases(BuildContext buildContext) throws Exception {
		final ProjectConfigDto project = buildContext.getConfig();
		
		if (StringUtils.isBlank(project.getWorkDir())) {
			throw new ConfigException("messages.build.null.work.dir");
		}
		
		for (RunnablePhase phase : phases) {
			phase.execute(buildContext);
		}
	}
	
	protected interface RunnablePhase {
		void execute(BuildContext buildContext) throws Exception;
	}
	
	protected abstract class RunnablePhaseImpl implements RunnablePhase {
		private final BuildPhase phase;
		
		protected RunnablePhaseImpl(BuildPhase phase) {
			this.phase = phase;
		}
		
		public final void execute(BuildContext buildContext) throws Exception {
			buildDetailCallback.setPhase(phase);
			
			try {
				executePhase(buildContext);
			} catch (InterruptedException e) {
			} finally {
				buildDetailCallback.clearPhase();
			}

			//clear interrupt flag if present
			Thread.interrupted();
			
			if (killing || timeout) {
				throw new TimeoutException();
			}
		}
		
		protected abstract void executePhase(BuildContext buildContext) throws Exception;
	}
	
	protected static final class KilledException extends Exception {}
}
