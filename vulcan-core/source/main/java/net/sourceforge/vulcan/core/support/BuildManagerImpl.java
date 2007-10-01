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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.DependencyException;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.BuildManagerConfigDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.Date;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
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
						new ErrorEvent(this, "messages.save.failure",
								new String[] {e.getMessage()}, e));
			}
		}
	}

	@ManagedOperation
	@ManagedOperationParameters({@ManagedOperationParameter(name="buildDaemonInfo", description="")})
	public ProjectConfigDto getTarget(BuildDaemonInfoDto buildDaemonInfo) {
		try {
			writeLock.lock();
			return getTarget(buildDaemonInfo, 0);
		} finally {
			writeLock.unlock();
		}
	}

	public void registerBuildStatus(BuildDaemonInfoDto info, ProjectConfigDto target, ProjectStatusDto buildStatus) {
		try {
			writeLock.lock();

			if (!activeDaemons.containsKey(info)) {
				throw new IllegalStateException(info + " is not building a project.");
			}
			
			projectsBeingBuilt.put(target.getName(), buildStatus);
		} finally {
			writeLock.unlock();
		}
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
		
		dg.initializeBuildResults(cache.getLatestOutcomes());
		
		try {
			writeLock.lock();
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
	public List<UUID> getAvailableStatusIdsInRange(Set<String> projectNames, java.util.Date begin, java.util.Date end) {
		final List<UUID> all = new ArrayList<UUID>();
		
		for (String projectName : projectNames) {
			final List<UUID> ids = getAvailableStatusIds(projectName);
			final long low = begin.getTime();
			final long high = end.getTime();
			
			for (Iterator<UUID> itr = ids.iterator(); itr.hasNext();) {
				final UUID id = itr.next();
				
				/* Time based UUIDs are measured as 100-nanosecond units since Oct. 15, 1582 UTC
				 * whereas java.util.Date.getTime() is measures as milliseconds since January 1st, 1970.
				 * Conversion necessary.
				 */
				final long timestamp = id.timestamp()/10000 - 12219292800000L;
				
				if (timestamp >= low && timestamp < high) {
					all.add(id);
				}
			}
		}
		return all;
	}
	@ManagedAttribute
	public Map<String, ProjectStatusDto> getProjectStatus() {
		return cache.getLatestOutcomes();
	}
	public Map<String, ProjectStatusDto> getProjectsBeingBuilt() {
		try {
			readLock.lock();
			return new HashMap<String, ProjectStatusDto>(projectsBeingBuilt);
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
			LOG.info("Project " + config.getName() + " completed build with status " + outcome.getStatus().name() + ".");
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

		boolean passed;
		
		switch (outcome.getStatus()) {
			case PASS:
				passed = true;
				break;
			case UP_TO_DATE:
				passed = getLatestStatus(projectName).getStatus().equals(Status.PASS);
				break;
			default:
				passed = false;
		}

		try {
			writeLock.lock();
			final Target target = activeDaemons.remove(info);
			
			target.targetCompleted(config, passed);
			
			projectsBeingBuilt.remove(projectName);
			
			if (!activeBuilds.remove(config)) {
				throw new IllegalStateException();
			}
		} finally {
			writeLock.unlock();
		}
		
		if (!outcome.getStatus().equals(Status.UP_TO_DATE)) {
			fireBuildCompleted(info, config, outcome);
		}
	}

	private void fireBuildCompleted(final BuildDaemonInfoDto info,
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
					new ErrorEvent(this, "messages.save.failure",
							new String[] {e.getMessage()}, e));
		}
	}

	// must obtain write lock before calling this method
	private ProjectConfigDto getTarget(BuildDaemonInfoDto buildDaemonInfo, int i) {
		if (activeDaemons.containsKey(buildDaemonInfo)) {
			throw new IllegalStateException();
		}
		
		if (queue.size() <= i) {
			return null;
		}
		final Target tgt = queue.get(i);
		
		final ProjectConfigDto config;

		try {
			boolean removed = false;
			
			config = tgt.next();
			if (!tgt.hasNext()) {
				removed = true;
				queue.remove(i);
			}
			if (config != null) {
				final String targetName = config.getName();
				if (projectsBeingBuilt.containsKey(targetName)) {
					warnDuplicateBuild(config, tgt);
					if (!removed) {
						queue.remove(i);
					}
					return getTarget(buildDaemonInfo, i);
				}
				
				if (LOG.isInfoEnabled()) {
					LOG.info("Assigned project " + targetName + " to builder "+ buildDaemonInfo.getName() + ".");
				}
				activeBuilds.add(config);
				activeDaemons.put(buildDaemonInfo, tgt);
				projectsBeingBuilt.put(targetName, new ProjectStatusDto());
			}
			return config;
		} catch (PendingDependencyException e) {
			return getTarget(buildDaemonInfo, i+1);
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
			eventHandler.reportEvent(new WarningEvent(this, "warnings.duplicate.target.removed",
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
		ProjectConfigDto next() throws PendingDependencyException;
		void targetCompleted(ProjectConfigDto config, boolean success);
		void appendPendingTargets(List<ProjectStatusDto> list);
		String getName();
	}
	private class DependencyTarget implements Target {
		final DependencyGroup group;
		
		DependencyTarget(DependencyGroup group) {
			this.group = group;
		}
		public boolean hasNext() {
			return !group.isEmpty();
		}
		public ProjectConfigDto next() throws PendingDependencyException {
			ProjectConfigDto current = null;
			try {
				current = group.getNextTarget();
			} catch (DependencyFailureException e) {
				fireBuildCompleted(null, e.getProjectConfig(), null, Status.SKIP, e, null);
			} catch (PendingDependencyException e) {
				throw e;
			} catch (DependencyException e) {
				fireBuildCompleted(null, null, null, Status.SKIP, e, null);
			}

			if (current != null) {
				current.setRequestedBy(getName());
				if (!group.isManualBuild()) {
					current.setScheduledBuild(true);
				}
			}
			
			return current;
		}
		public void targetCompleted(ProjectConfigDto config, boolean success) {
			group.targetCompleted(config, success);
		}
		public void appendPendingTargets(List<ProjectStatusDto> list) {
			list.addAll(Arrays.asList(group.getPendingTargets()));
		}
		public String getName() {
			return group.getName();
		}
	}
}
