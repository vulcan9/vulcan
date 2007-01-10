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
package net.sourceforge.vulcan.scheduler.thread;

import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.DependencyBuildPolicy;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.event.WarningEvent;
import net.sourceforge.vulcan.exception.AlreadyScheduledException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.scheduler.ProjectScheduler;


@SvnRevision(id="$Id$", url="$HeadURL$")
public final class ProjectSchedulerImpl extends AbstractScheduler implements ProjectScheduler {
	private ProjectManager projectManager;
	private BuildManager buildManager;

	@Override
	protected void execute() throws Exception {
		final ProjectConfigDto[] projects = projectManager.getProjectsForScheduler(getConfig().getName());
		
		final DependencyGroup dg = projectManager
				.buildDependencyGroup(
						projects,
						DependencyBuildPolicy.AS_NEEDED,
						null,
						false, false);
		
		dg.setName(getName());
		
		try {
			buildManager.add(dg);
		} catch (AlreadyScheduledException e) {
			eventHandler.reportEvent(new WarningEvent(
					this,
					"Scheduler.interval.too.short",
					new Object[] {getName()}));
		}
	}
	
	public ProjectManager getProjectManager() {
		return projectManager;
	}
	public void setProjectManager(ProjectManager projectManager) {
		this.projectManager = projectManager;
	}
	public BuildManager getBuildManager() {
		return buildManager;
	}
	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
}
