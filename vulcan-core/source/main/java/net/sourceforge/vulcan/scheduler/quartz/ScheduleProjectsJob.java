/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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
package net.sourceforge.vulcan.scheduler.quartz;

import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.DependencyBuildPolicy;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.event.InfoEvent;
import net.sourceforge.vulcan.exception.AlreadyScheduledException;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.ProjectsLockedException;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class ScheduleProjectsJob extends QuartzJobBean implements Job {
	private BuildManager buildManager;
	private ProjectManager projectManager;
	private EventHandler eventHandler;
	
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
	
	public EventHandler getEventHandler() {
		return eventHandler;
	}

	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		executeInternal(context.getJobDetail().getName());
	}

	protected void executeInternal(final String name) {
		final ProjectConfigDto[] projects = projectManager.getProjectsForScheduler(name);
		
		if (projects.length == 0) {
			return;
		}
		
		try {
			final DependencyGroup dg = projectManager
				.buildDependencyGroup(
					projects,
					DependencyBuildPolicy.AS_NEEDED,
					null,
					false, false);
		
			if (dg.isEmpty()) {
				return;
			}
			
			dg.setName(name);
			buildManager.add(dg);
		} catch (ProjectsLockedException ignore) {
			// This is a very unlikely race condition.
		} catch (AlreadyScheduledException e) {
			eventHandler.reportEvent(new InfoEvent(
					this,
					"Scheduler.interval.too.short",
					new Object[] {name}));
		} catch (ConfigException e) {
			eventHandler.reportEvent(new ErrorEvent(
					this,
					e.getKey(),
					new Object[] {e.getArgs()},
					e));
		}
	}
}
