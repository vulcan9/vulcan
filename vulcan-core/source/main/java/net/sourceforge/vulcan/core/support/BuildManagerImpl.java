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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.BuildTarget;
import net.sourceforge.vulcan.core.DependencyException;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.BuildManagerConfigDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.event.BrokenBuildClaimedEvent;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.BuildStartingEvent;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.event.InfoEvent;
import net.sourceforge.vulcan.event.Message;
import net.sourceforge.vulcan.event.WarningEvent;
import net.sourceforge.vulcan.exception.AlreadyScheduledException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;


@SvnRevision(id="$Id$", url="$HeadURL$")
@ManagedResource(objectName="vulcan:name=buildManager")
public class BuildManagerImpl implements BuildManager {
	private final static Log LOG = LogFactory.getLog(BuildManager.class);
	
	EventHandler eventHandler;
	
	BuildManagerConfigDto buildManagerConfig;
	BuildOutcomeCache cache;
	
	final Lock readLock;
	final Lock writeLock;
	final List<Target> queue = new ArrayList<Target>();
	final List<ProjectConfigDto> activeBuilds = new ArrayList<ProjectConfigDto>();
	final Map<BuildDaemonInfoDto, Target> activeDaemons = new HashMap<BuildDaemonInfoDto, Target>();
	final Map<String, ProjectBuilder> projectBuilders = new HashMap<String, ProjectBuilder>();
	final Map<String, ProjectStatusDto> projectsBeingBuilt = new HashMap<String, ProjectStatusDto>();
	
	public BuildManagerImpl() {
		final ReadWriteLock lock = new ReentrantReadWriteLock();
		this.readLock = lock.readLock();
		this.writeLock = lock.writeLock();
	}
	
	@SuppressWarnings("deprecation")
	public void init(BuildManagerConfigDto buildManagerConfig) {
		this.buildManagerConfig = buildManagerConfig;
		
		final Map<String, ProjectStatusDto> old = buildManagerConfig.getProjectStatus();
		
		if (old != null) {
			try {
				cache.mergeOutcomes(old);
				buildManagerConfig.setProjectStatus(null);
			} catch (StoreException e) {
				eventHandler.reportEvent(
						new ErrorEvent(this, "messages.save.failure.build",
								new String[] {"", e.getMessage()}, e));
			}
		}
	}

	@ManagedOperation
	@ManagedOperationParameters({@ManagedOperationParameter(name="buildDaemonInfo", description="")})
	public BuildTarget getTarget(BuildDaemonInfoDto buildDaemonInfo) {
		try {
			writeLock.lock();
			
			final Set<String> dependenciesNeededByActiveBuilds = new HashSet<String>();
			for (ProjectConfigDto project : activeBuilds) {
				dependenciesNeededByActiveBuilds.addAll(Arrays.asList(project.getDependencies()));
			}

			return getTarget(buildDaemonInfo, dependenciesNeededByActiveBuilds, 0);
		} finally {
			writeLock.unlock();
		}
	}

	public void registerBuildStatus(BuildDaemonInfoDto info, ProjectBuilder builder, ProjectConfigDto target, ProjectStatusDto buildStatus) {
		try {
			writeLock.lock();

			if (!activeDaemons.containsKey(info)) {
				throw new IllegalStateException(info + " is not building a project.");
			}
			
			projectsBeingBuilt.put(target.getName(), buildStatus);
			projectBuilders.put(target.getName(), builder);
		} finally {
			writeLock.unlock();
		}
		
		eventHandler.reportEvent(new BuildStartingEvent(this, info, target, buildStatus));
	}
	
	public void add(DependencyGroup dg) throws AlreadyScheduledException {
		if (dg.isEmpty()) {
			throw new IllegalArgumentException("DepenedencyGroup has no targets.");
		}
		
		if (!dg.isManualBuild()) {
			final String name = dg.getName();
			if (name != null) {
				try {
					readLock.lock();
					for (Target tgt : queue) {
						if (name.equals(tgt.getName())) {
							throw new AlreadyScheduledException(name);
						}
					}
				} finally {
					readLock.unlock();
				}
			}
		}
		
		try {
			writeLock.lock();
			
			dg.initializeBuildResults(cache.getLatestOutcomes());
			queue.add(new DependencyTarget(dg));
		} finally {
			writeLock.unlock();
		}
	}
	
	@ManagedOperation
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="info", description=""),
		@ManagedOperationParameter(name="config", description=""),
		@ManagedOperationParameter(name="outcome", description="")})
	public void targetCompleted(BuildDaemonInfoDto info, ProjectConfigDto config, ProjectStatusDto outcome) {
		if (outcome.getStatus().equals(Status.PASS) && outcome.getMessageKey() == null) {
			outcome.setMessageKey("messages.build.success");
			outcome.setMessageArgs(new String[] {config.getName()});
		}
		
		processTargetComplete(info, config, outcome);
	}
	
	public boolean claimBrokenBuild(String projectName, int buildNumber, String claimUser) {
		final ProjectStatusDto outcome;
		
		try {
			writeLock.lock();
			
			outcome = getStatusByBuildNumber(projectName, buildNumber);
			
			if (outcome == null) {
				throw new IllegalArgumentException("Build " + buildNumber + " for project " + projectName + " not found.");
			} else if (isNotBlank(outcome.getBrokenBy())) {
				return false;
			}
			
			cache.claimBrokenBuild(outcome, claimUser);
		} finally {
			writeLock.unlock();
		}
		
		eventHandler.reportEvent(new BrokenBuildClaimedEvent(this, outcome));
		
		return true;
	}
	
	public boolean isBuildingOrInQueue(String... name) {
		final Set<String> names = new HashSet<String>(Arrays.asList(name));
		try {
			readLock.lock();
			for (Iterator<ProjectConfigDto> itr = activeBuilds.iterator(); itr.hasNext();) {
				if (names.contains(itr.next().getName())) {
					return true;
				}
			}
			
			for (Iterator<Target> itr = queue.iterator(); itr.hasNext();) {
				if (itr.next().containsAny(names)) {
					return true;
				}
			}
			
			return false;
		} finally {
			readLock.unlock();
		}
	}
	public ProjectStatusDto[] getPendingTargets() {
		final List<ProjectStatusDto> targets = new ArrayList<ProjectStatusDto>();
		
		try {
			readLock.lock();
			for (Iterator<ProjectConfigDto> itr = activeBuilds.iterator(); itr.hasNext();) {
				final ProjectConfigDto project = itr.next();
				targets.add(createProjectStatusDto(project.getName(), null, Status.BUILDING, null));
			}
			
			for (Iterator<Target> itr = queue.iterator(); itr.hasNext();) {
				final Target target = itr.next();
				target.appendPendingTargets(targets);
			}
		} finally {
			readLock.unlock();
		}

		return targets.toArray(new ProjectStatusDto[targets.size()]);
	}

	public void clear() {
		try {
			writeLock.lock();
			queue.clear();
		} finally {
			writeLock.unlock();
		}
	}
	public ProjectStatusDto getLatestStatus(String projectName) {
		return cache.getLatestOutcome(projectName);
	}
	public ProjectStatusDto getStatus(UUID id) {
		return cache.getOutcome(id);
	}
	public ProjectStatusDto getStatusByBuildNumber(String projectName, int buildNumber) {
		ProjectStatusDto status = cache.getOutcomeByBuildNumber(projectName, buildNumber);
		
		if (status != null) {
			return status;
		}
		
		status = getProjectsBeingBuilt().get(projectName);
		
		if (status != null && status.getBuildNumber() == buildNumber) {
			return status;
		}
		
		return null;
	}
	public List<UUID> getAvailableStatusIds(String projectName) {
		final List<UUID> outcomeIds = cache.getOutcomeIds(projectName);
		if (outcomeIds == null) {
			return null;
		}
		return Collections.unmodifiableList(outcomeIds);
	}
	@ManagedAttribute
	public Map<String, ProjectStatusDto> getProjectStatus() {
		return cache.getLatestOutcomes();
	}

	public Integer getMostRecentBuildNumberByWorkDir(String workDir) {
		return cache.getMostRecentBuildNumberByWorkDir(workDir);
	}

	public ProjectStatusDto getMostRecentBuildByWorkDir(String projectName, String workDir) {
		return cache.getOutcomeByBuildNumber(projectName, getMostRecentBuildNumberByWorkDir(workDir));
	}

	public Map<String, ProjectStatusDto> getProjectsBeingBuilt() {
		try {
			readLock.lock();
			return new HashMap<String, ProjectStatusDto>(projectsBeingBuilt);
		} finally {
			readLock.unlock();
		}
	}
	public ProjectBuilder getProjectBuilder(String projectName) {
		try {
			readLock.lock();
			return projectBuilders.get(projectName);
		} finally {
			readLock.unlock();
		}
	}
	public EventHandler getEventHandler() {
		return eventHandler;
	}
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	public BuildOutcomeCache getBuildOutcomeCache() {
		return cache;
	}
	public void setBuildOutcomeCache(BuildOutcomeCache cache) {
		this.cache = cache;
	}

	void add(ProjectConfigDto project) {
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(project);
	
		try {
			writeLock.lock();
			queue.add(new DependencyTarget(dg));
		} finally {
			writeLock.unlock();
		}
	}
	private void processTargetComplete(BuildDaemonInfoDto info, ProjectConfigDto config,
			ProjectStatusDto outcome) {
		
		final String projectName = config.getName();
		
		if (LOG.isInfoEnabled()) {
			LOG.info("Project " + config.getName() + " completed build " + outcome.getBuildNumber() + " with status " + outcome.getStatus().name() + ".");
		}

		try {
			readLock.lock();
			if (!activeDaemons.containsKey(info)) {
				throw new IllegalStateException("Builder " + info.getName() + " does not have an active build.");
			} else if (!projectsBeingBuilt.containsKey(projectName)) {
				throw new IllegalStateException("Project " + projectName + " is not currently being built.");
			}
		} finally {
			readLock.unlock();
		}

		boolean passed = false;
		
		try {
			passed = outcome.getStatus() == Status.PASS;
			fireBuildCompleted(info, config, outcome);
		} finally {		
			try {
				writeLock.lock();
				final Target target = activeDaemons.remove(info);
				
				target.targetCompleted(config, passed);
				
				projectsBeingBuilt.remove(projectName);
				projectBuilders.remove(projectName);
				
				if (!activeBuilds.remove(config)) {
					throw new IllegalStateException();
				}
			} finally {
				writeLock.unlock();
			}
		}
	}

	private void fireFailedDependencyCompleted(final BuildDaemonInfoDto info,
			final ProjectConfigDto config, final RevisionTokenDto revision,
			final Status status, final Message message, ChangeLogDto changeLog) {
		
		final ProjectStatusDto prevStatusDto = getLatestStatus(config.getName());
		
		if (prevStatusDto != null && prevStatusDto.getStatus() == Status.SKIP) {
			return;
		}
		
		final ProjectStatusDto statusDto = createProjectStatusDto(config, revision, status, message, changeLog);
		
		fireBuildCompleted(info, config, statusDto);
	}

	private void fireBuildCompleted(final BuildDaemonInfoDto info, final ProjectConfigDto config, final ProjectStatusDto outcome) {
		addStatusInfo(config, outcome);

		eventHandler.reportEvent(new BuildCompletedEvent(this, info, config, outcome));

		try {
			cache.store(outcome);
		} catch (StoreException e) {
			eventHandler.reportEvent(
					new ErrorEvent(this, "messages.save.failure.build",
							new String[] {outcome.getName(), e.getMessage()}, e));
		}
	}

	// must obtain write lock before calling this method
	private BuildTarget getTarget(BuildDaemonInfoDto buildDaemonInfo, Set<String> dependenciesNeededByActiveBuilds, int i) {
		if (activeDaemons.containsKey(buildDaemonInfo)) {
			throw new IllegalStateException();
		}
		
		if (queue.size() <= i) {
			return null;
		}
		final Target tgt = queue.get(i);
		
		final BuildTarget buildTarget;

		try {
			boolean removed = false;
			
			// adding this behavior to avoid putting build queue in permanent illegal state
			if (!tgt.hasNext()) {
				LOG.error("Target was empty and should have been removed by now.");
				queue.remove(i);
				
				return null;
			}
			
			buildTarget = tgt.next();
			
			if (buildTarget != null && dependenciesNeededByActiveBuilds.contains(buildTarget.getProjectName())) {
				// put it back for later.
				tgt.push(buildTarget);
				throw new PendingDependencyException();
			}
			
			if (!tgt.hasNext()) {
				removed = true;
				queue.remove(i);
			}
			if (buildTarget != null) {
				final String targetName = buildTarget.getProjectName();
				if (projectsBeingBuilt.containsKey(targetName)) {
					warnDuplicateBuild(buildTarget.getProjectConfig(), tgt);
					if (!removed) {
						queue.remove(i);
					}
					return getTarget(buildDaemonInfo, dependenciesNeededByActiveBuilds, i);
				}
				
				if (LOG.isInfoEnabled()) {
					LOG.info("Assigned project " + targetName + " to builder "+ buildDaemonInfo.getName() + ".");
				}
				activeBuilds.add(buildTarget.getProjectConfig());
				activeDaemons.put(buildDaemonInfo, tgt);
				projectsBeingBuilt.put(targetName, new ProjectStatusDto());
			}
			return buildTarget;
		} catch (PendingDependencyException e) {
			return getTarget(buildDaemonInfo, dependenciesNeededByActiveBuilds, i+1);
		}
	}

	private void addStatusInfo(ProjectConfigDto config, ProjectStatusDto statusDto) {
		final ProjectStatusDto prev = getLatestStatus(statusDto.getName());
		final Date now = new Date();

		statusDto.setCompletionDate(now);

		if (statusDto.getStartDate() == null) {
			// SKIP outcomes won't have a start date set.
			statusDto.setStartDate(now);
		}
		
		if (statusDto.getBuildNumber() == null) {
			if (prev == null) {
				statusDto.setBuildNumber(0);
			} else {
				statusDto.setBuildNumber(prev.getBuildNumber() + 1);
			}
		}
		
		if (Status.PASS.equals(statusDto.getStatus())) {
			statusDto.setLastGoodBuildNumber(statusDto.getBuildNumber());
		} else {
			if (prev != null) {
				statusDto.setLastGoodBuildNumber(prev.getLastGoodBuildNumber());
			}
		}
		
		if (statusDto.getRevision() != null) {
			statusDto.setLastKnownRevision(statusDto.getRevision());
		} else if (prev != null) {
			statusDto.setLastKnownRevision(prev.getLastKnownRevision());
		}
		
		final Map<String, UUID> devIds = new HashMap<String, UUID>();
		
		for (String depName : config.getDependencies()) {
			final UUID id = cache.getLatestOutcomeId(depName);
			if (id != null) {
				devIds.put(depName, id);
			}
		}
		
		statusDto.setDependencyIds(devIds);
	}

	private void warnDuplicateBuild(ProjectConfigDto config, Target tgt) {
		final List<ProjectStatusDto> others = new ArrayList<ProjectStatusDto>();
		
		tgt.appendPendingTargets(others);

		if (others.isEmpty()) {
			eventHandler.reportEvent(new InfoEvent(this, "warnings.duplicate.target.removed",
					new String[] {config.getName() }));
		} else {
			final List<String> names = new ArrayList<String>(others.size());
			
			for (ProjectStatusDto target : others) {
				names.add(target.getName());
			}
			
			eventHandler.reportEvent(new WarningEvent(this, "warnings.group.removed",
					new String[] {config.getName(), StringUtils.join(names.iterator(), ", ")}));
		}
	}
	private static ProjectStatusDto createProjectStatusDto(ProjectConfigDto config,
			RevisionTokenDto revision, Status status, Message message, ChangeLogDto changeLog) {
		final ProjectStatusDto ps = createProjectStatusDto(config.getName(), revision, status, message);

		ps.setChangeLog(changeLog);
		
		return ps;
	}
	private static ProjectStatusDto createProjectStatusDto(String projectName,
			RevisionTokenDto revision, Status status, Message message) {
		
		final ProjectStatusDto statusDto = new ProjectStatusDto();
		
		statusDto.setName(projectName);
		statusDto.setStatus(status);
		statusDto.setRevision(revision);
		
		if (message != null) {
			statusDto.setMessageKey(message.getKey());
			statusDto.setMessageArgs(message.getArgs());
		}
		
		return statusDto;
	}
	private interface Target {
		boolean hasNext();
		boolean containsAny(Collection<String> projectNames);
		BuildTarget next() throws PendingDependencyException;
		void push(BuildTarget target);
		void targetCompleted(ProjectConfigDto config, boolean success);
		void appendPendingTargets(List<ProjectStatusDto> list);
		String getName();
	}
	private class DependencyTarget implements Target {
		final DependencyGroup group;
		BuildTarget pushed;
		
		DependencyTarget(DependencyGroup group) {
			this.group = group;
		}
		public boolean hasNext() {
			return pushed != null || !group.isEmpty();
		}
		public BuildTarget next() throws PendingDependencyException {
			BuildTarget current = null;
			
			if (pushed != null) {
				current = pushed;
				pushed = null;
				return current;
			}
			
			try {
				current = group.getNextTarget();
			} catch (DependencyFailureException e) {
				fireFailedDependencyCompleted(null, e.getProjectConfig(), null, Status.SKIP, e, null);
			} catch (PendingDependencyException e) {
				throw e;
			} catch (DependencyException e) {
				fireFailedDependencyCompleted(null, null, null, Status.SKIP, e, null);
			}

			if (current != null) {
				// TODO: scheduler should set these properties
				current.getProjectConfig().setRequestedBy(getName());
				if (!group.isManualBuild()) {
					current.getProjectConfig().setScheduledBuild(true);
				}
			}
			
			return current;
		}
		public void push(BuildTarget target) {
			pushed = target;
		}
		public void targetCompleted(ProjectConfigDto config, boolean success) {
			group.targetCompleted(config, success);
		}
		public void appendPendingTargets(List<ProjectStatusDto> list) {
			if (pushed != null) {
				final ProjectStatusDto pushedStatus = new ProjectStatusDto();
				pushedStatus.setName(pushed.getProjectName());
				pushedStatus.setStatus(Status.IN_QUEUE);
				list.add(pushedStatus);
			}
			list.addAll(group.getPendingTargets());
		}
		public boolean containsAny(Collection<String> projectNames) {
			for (ProjectConfigDto target : group.getPendingProjects()) {
				if (projectNames.contains(target.getName())) {
					return true;
				}
			}
			return false;
		}
		public String getName() {
			return group.getName();
		}
	}
}
