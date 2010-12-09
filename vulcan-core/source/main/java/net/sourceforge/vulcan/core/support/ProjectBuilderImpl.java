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
import net.sourceforge.vulcan.core.BuildTarget;
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
}

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ProjectBuilderImpl implements ProjectBuilder {
	private final Log log = LogFactory.getLog(ProjectBuilder.class);
	
	private WorkingCopyUpdateExpert workingCopyUpdateExpert = new WorkingCopyUpdateExpert();
	
	/* Injected configuration fields */
	private ConfigurationStore configurationStore;
	private BuildOutcomeStore buildOutcomeStore;
	private ProjectManager projectManager;
	private BuildManager buildManager;
	private boolean diffsEnabled;
	
	/* State */
	protected UpdateType updateType;
	
	protected boolean building;
	protected boolean killing;
	protected boolean timeout;
	
	protected RevisionTokenDto previousRevision;
	protected ProjectStatusDto buildStatus;
	protected BuildContext buildContext;
	protected ProjectStatusDto.Status previousOutcome;
	protected String killedBy;
	
	DelegatingBuildDetailCallback buildDetailCallback;
	
	private Thread buildThread;
	
	public void init() {
	}
	
	public final void build(BuildDaemonInfoDto info, BuildTarget currentTarget, BuildDetailCallback buildDetailCallback) {
		
		synchronized(this) {
			buildThread = Thread.currentThread();
			
			building = true;
			killing = false;
			timeout = false;
			
			notifyAll();
		}

		initializeBuildDetailCallback(currentTarget, buildDetailCallback);
		
		try {
			buildStatus = currentTarget.getStatus();
			buildContext = initializeBuildStatus(currentTarget);
			
			if (log.isDebugEnabled() && buildContext.getLastBuild() != null) {
				log.debug("Project " + currentTarget.getProjectName() + " previous build " + buildContext.getLastBuild().getBuildNumber() + " completed " + buildContext.getLastBuild().getCompletionDate());
			}
			
			//TODO: should take target, not config + status
			buildManager.registerBuildStatus(info, this, currentTarget.getProjectConfig(), buildStatus);
			
			buildProject(currentTarget.getProjectConfig());
			
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
		} catch (Throwable e) {
			log.error("unexpected error", e);
			
			buildStatus.setStatus(Status.ERROR);
			buildStatus.setMessageKey("messages.build.uncaught.exception");
			
			String phaseName = "(none)";
			
			BuildPhase currentPhase = this.buildDetailCallback.getCurrentPhase();
			
			if (currentPhase != null) {
				phaseName = currentPhase.name();
			}
			
			buildStatus.setMessageArgs(new String[] {currentTarget.getProjectName(), e.getMessage(), phaseName});
		} finally {
			if (previousOutcome == null) {
				buildStatus.setStatusChanged(true);
			} else {
				buildStatus.setStatusChanged(!previousOutcome.equals(buildStatus.getStatus()));
			}
			
			buildDetailCallback.setPhaseMessageKey(BuildPhase.Publish.getMessageKey());
			// TODO: targetCompleted should take currentTarget, not config + status
			buildManager.targetCompleted(info, currentTarget.getProjectConfig(), buildStatus);

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

	public boolean isDiffsEnabled() {
		return diffsEnabled;
	}
	
	public void setDiffsEnabled(boolean diffsEnabled) {
		this.diffsEnabled = diffsEnabled;
	}
	
	protected BuildContext initializeBuildStatus(BuildTarget currentTarget) throws StoreException, ConfigException {
		BuildContext context = new BuildContext(currentTarget);
		
		context.setRepositoryAdatpor(projectManager.getRepositoryAdaptor(currentTarget.getProjectConfig()));
		
		ProjectStatusDto lastBuild = buildManager.getLatestStatus(currentTarget.getProjectName());
		context.setLastBuild(lastBuild);
		
		if (lastBuild != null && !lastBuild.getWorkDir().equals(context.getConfig().getWorkDir())) {
			context.setLastBuildInSameWorkDir(buildOutcomeStore.loadMostRecentBuildOutcomeByWorkDir(context.getConfig().getName(), context.getConfig().getWorkDir()));
		} else {
			context.setLastBuildInSameWorkDir(lastBuild);
		}
		
		if (StringUtils.isNotBlank(currentTarget.getProjectConfig().getRepositoryTagName())) {
			context.getRepositoryAdaptor().setTagOrBranch(currentTarget.getProjectConfig().getRepositoryTagName());
		}
		
		if (lastBuild != null && !lastBuild.getTagName().equals(context.getRepositoryAdaptor().getTagOrBranch())) {
			
		} else {
			context.setLastBuildFromSameTag(lastBuild);			
		}
		
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

		// TODO: move this.buildStatus into this.buildContext.
		return context;
	}
	
	protected void generateUniqueBuildId(ProjectStatusDto status) {
		final UUID id = UUIDUtils.generateTimeBasedUUID();
		
		status.setId(id);
		status.setBuildLogId(id);
		status.setDiffId(id);
	}
	
	protected void buildProject(final ProjectConfigDto currentTarget) throws Exception {
		if (StringUtils.isBlank(currentTarget.getWorkDir())) {
			throw new ConfigException("messages.build.null.work.dir", null);
		}
		
		final RepositoryAdaptor ra = projectManager.getRepositoryAdaptor(currentTarget);

		doPhase(new RunnablePhase(BuildPhase.PrepareRepository) {
			@Override
			protected void executePhase() throws Exception {
				prepareRepository(ra, currentTarget);
				determineUpdateType(currentTarget);
				buildStatus.setEstimatedBuildTimeMillis(buildOutcomeStore.loadAverageBuildTimeMillis(currentTarget.getName(), updateType));
			}
		});
		
		if (updateType == Full) {
			doPhase(new RunnablePhase(BuildPhase.CheckoutWorkingCopy) {
				@Override
				protected void executePhase() throws Exception {
					createWorkingCopy(ra, currentTarget.getWorkDir());
				};
			});
		} else {
			doPhase(new RunnablePhase(BuildPhase.UpdateWorkingCopy) {
				@Override
				protected void executePhase() throws Exception {
					updateWorkingCopy(ra, currentTarget.getWorkDir());
				};
			});
		}
		
		doPhase(new RunnablePhase(BuildPhase.GetChangeLog) {
			@Override
			protected void executePhase() throws Exception {
				OutputStream diffOutputStream = null;
				
				if (previousRevision != null &&
						!previousRevision.equals(buildStatus.getRevision())) {
					diffOutputStream = diffsEnabled ? 
						new FileOutputStream(configurationStore.getChangeLog(currentTarget.getName(), buildStatus.getDiffId())) : null;
					
					try {
						buildStatus.setChangeLog(ra.getChangeLog(previousRevision, buildStatus.getRevision(), diffOutputStream));
					} finally {
						if (diffOutputStream != null) {
							diffOutputStream.close();
						}
					}
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
		
		doPhase(new RunnablePhase(BuildPhase.Build) {
			@Override
			protected void executePhase() throws Exception {
				invokeBuilder(currentTarget);
			}
		});
	}
	
	protected RevisionTokenDto prepareRepository(final RepositoryAdaptor repository, final ProjectConfigDto currentTarget) throws InterruptedException, StoreException, ConfigException {
		String tagName = currentTarget.getRepositoryTagName();
		
		if (!StringUtils.isBlank(tagName)) {
			repository.setTagOrBranch(tagName);
		} else {
			tagName = repository.getTagOrBranch();
			currentTarget.setRepositoryTagName(tagName);
		}
		
		repository.prepareRepository(buildDetailCallback);
		
		ProjectStatusDto prevStatusByTag = buildContext.getLastBuildFromSameTag();
		
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
		
		final RevisionTokenDto currentRev = repository.getLatestRevision(previousRevision);
		
		buildStatus.setRevision(currentRev);
		buildStatus.setTagName(tagName);
		buildStatus.setRepositoryUrl(repository.getRepositoryUrl());
		
		return currentRev;
	}

	protected void determineUpdateType(ProjectConfigDto currentTarget) throws StoreException {
		ProjectStatusDto previousStatusForWorkDir = buildContext.getLastBuildInSameWorkDir();
		
		updateType = workingCopyUpdateExpert.determineUpdateStrategy(currentTarget, previousStatusForWorkDir);
	}
	
	protected void createWorkingCopy(RepositoryAdaptor repository, String workDir) throws RepositoryException, IOException, ConfigException, InterruptedException {
		buildStatus.setUpdateType(Full);
		repository.createPristineWorkingCopy(buildDetailCallback);
	}
	
	protected void updateWorkingCopy(RepositoryAdaptor repository, String workDir) throws RepositoryException, IOException, ConfigException, InterruptedException {
		buildStatus.setUpdateType(Incremental);
		repository.updateWorkingCopy(buildDetailCallback);
	}
	
	protected void invokeBuilder(final ProjectConfigDto target) throws TimeoutException, KilledException, BuildFailedException, ConfigException, IOException, StoreException {
		final File logFile = configurationStore.getBuildLog(target.getName(), buildStatus.getBuildLogId());
		
		final BuildTool tool = projectManager.getBuildTool(target);
		
		tool.buildProject(target, (ProjectStatusDto) buildStatus.copy(), logFile, buildDetailCallback);
	}
	
	protected void doPhase(RunnablePhase runnable) throws Exception {
		runnable.execute();
	}
	
	protected abstract class RunnablePhase {
		private final BuildPhase phase;
		
		protected RunnablePhase(BuildPhase phase) {
			this.phase = phase;
		}
		
		public void execute() throws Exception {
			buildDetailCallback.setPhase(phase);
			try {
				executePhase();
			} catch (InterruptedException e) {
			} finally {
				buildDetailCallback.clearPhase();
			}

			//clear interrupt flag if present
			Thread.interrupted();
			
			if (timeout) {
				throw new TimeoutException();
			}
			if (killing) {
				throw new KilledException();
			}		}
		
		protected abstract void executePhase() throws Exception;
	}
	
	protected static final class KilledException extends Exception {}
	
	protected static class DelegatingBuildDetailCallback implements BuildDetailCallback {
		private final ProjectStatusDto buildStatus;
		private final BuildDetailCallback delegate;
		private final Object eventSource;
		private final boolean suppressErrors;
		private final boolean suppressWarnings;

		private BuildPhase currentPhase;
		
		private final Object listenerLock = new Object();
		private List<BuildStatusListener> buildListeners;
		
		interface BuildStatusListenerVisitor {
			void visit(BuildStatusListener node);
		}
		
		public DelegatingBuildDetailCallback(ProjectStatusDto buildStatus, BuildDetailCallback delegate, Object eventSource, boolean suppressErrors, boolean suppressWarnings) {
			this.buildStatus = buildStatus;
			this.delegate = delegate;
			this.eventSource = eventSource;
			this.suppressErrors = suppressErrors;
			this.suppressWarnings = suppressWarnings;
		}

		public BuildPhase getCurrentPhase() {
			return currentPhase;
		}

		public void setPhase(final BuildPhase phase) {
			currentPhase = phase;
			
			setPhaseMessageKey(phase.getMessageKey());
			
			raise(new BuildStatusListenerVisitor() {
				public void visit(BuildStatusListener listener) {
					listener.onBuildPhaseChanged(this, phase);
				}
			});
		}
		
		public void clearPhase() {
			currentPhase = null;
			setPhaseMessageKey(null);
			setDetail(null);
		}

		public void reportError(String message, String file, Integer lineNumber, String code) {
			if (suppressErrors) {
				return;
			}
			
			delegate.reportError(message, file, lineNumber, code);
			final BuildMessageDto error = new BuildMessageDto(message, file, lineNumber, code);
			buildStatus.addError(error);
			
			raise(new BuildStatusListenerVisitor() {
				public void visit(BuildStatusListener listener) {
					listener.onErrorLogged(eventSource, error);
				}
			});
		}

		public void reportWarning(String message, String file, Integer lineNumber, String code) {
			if (suppressWarnings) {
				return;
			}
			
			delegate.reportWarning(message, file, lineNumber, code);
			final BuildMessageDto warning = new BuildMessageDto(message, file, lineNumber, code);
			buildStatus.addWarning(warning);
			
			raise(new BuildStatusListenerVisitor() {
				public void visit(BuildStatusListener listener) {
					listener.onWarningLogged(eventSource, warning);
				}
			});
		}
		
		public void addMetric(MetricDto metric) {
			buildStatus.addMetric(metric);
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

		public void addListener(BuildStatusListener listener) {
			synchronized(listenerLock) {
				List<BuildStatusListener> newList = new ArrayList<BuildStatusListener>();
				if (buildListeners != null) {
					newList.addAll(buildListeners);
				}
				newList.add(listener);
				buildListeners = newList;
			}
		}

		public boolean removeListener(BuildStatusListener listener) {
			synchronized(listenerLock) {
				if (buildListeners == null) {
					return false;
				}
				
				List<BuildStatusListener> newList = new ArrayList<BuildStatusListener>(buildListeners);
				boolean flag = newList.remove(listener);
				buildListeners = newList.isEmpty() ? null : newList;
				return flag;
			}
		}

		private void raise(BuildStatusListenerVisitor visitor) {
			// copy to local variable to avoid concurrent modification
			final List<BuildStatusListener> listeners = buildListeners;

			if (listeners == null) {
				return;
			}
			
			for (BuildStatusListener listener : listeners) {
				visitor.visit(listener);
			}
		}
	}
}
