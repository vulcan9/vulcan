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

import static net.sourceforge.vulcan.core.WorkingCopyUpdateStrategy.Full;
import static net.sourceforge.vulcan.core.WorkingCopyUpdateStrategy.Incremental;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.DependencyBuildPolicy;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.core.WorkingCopyUpdateStrategy;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto.UpdateStrategy;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.ProjectsLockedException;
import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
class DependencyGroupBuilder {
	final ProjectManager projectManager;
	final BuildManager buildManager;
	final DependencyBuildPolicy policy;
	final boolean buildOnDependencyFailureOverride;
	final boolean buildOnNoUpdatesOverride;
	
	final Set<String> added = new HashSet<String>();
	final DependencyGroup dg = new DependencyGroupImpl();
	final WorkingCopyUpdateStrategy updateStrategyOverride;
	final List<String> locked = new ArrayList<String>();
	
	private DependencyGroupBuilder(ProjectManager projectManager, BuildManager buildManager, DependencyBuildPolicy policy, WorkingCopyUpdateStrategy updateStrategyOverride, boolean buildOnDependencyFailureOverride, boolean buildOnNoUpdatesOverride) {
		this.projectManager = projectManager;
		this.buildManager = buildManager;
		this.policy = policy;
		this.updateStrategyOverride = updateStrategyOverride;
		this.buildOnDependencyFailureOverride = buildOnDependencyFailureOverride;
		this.buildOnNoUpdatesOverride = buildOnNoUpdatesOverride;
	}
	
	static DependencyGroup buildDependencyGroup(ProjectConfigDto[] projects,
			ProjectManager projectManager, BuildManager buildManager,
			DependencyBuildPolicy policy, WorkingCopyUpdateStrategy updateStrategyOverride, boolean buildOnDependencyFailureOverride, boolean buildOnNoUpdatesOverride)
		throws ProjectsLockedException, ConfigException {
		return new DependencyGroupBuilder(projectManager, buildManager,
				policy,
				updateStrategyOverride, buildOnDependencyFailureOverride, buildOnNoUpdatesOverride).buildDependencyGroup(projects);
	}
	
	private DependencyGroup buildDependencyGroup(ProjectConfigDto[] projects) throws ProjectsLockedException, ConfigException {
		for (ProjectConfigDto project : projects) {
			addOnce(project, false);
		}
		
		if (locked.size() > 0) {
			throw new ProjectsLockedException(locked);
		}
			
		return dg;
	}
	
	private void addOnce(ProjectConfigDto projectConfig, boolean isDependency) throws ConfigException {
		if (projectConfig.isLocked()) {
			if (isDependency && DependencyBuildPolicy.AS_NEEDED.equals(policy)) {
				// exclude locked projects that have been included "as needed"
				return;
			}
			
			// fail otherwise
			locked.add(projectConfig.getName());
			return;
		}
		
		if (!added.contains(projectConfig.getName())) {
			applyOverridesAndAdd(projectConfig);
			
			added.add(projectConfig.getName());
			
			if (projectConfig.isAutoIncludeDependencies() && !DependencyBuildPolicy.NONE.equals(policy)) {
				for (String depName : projectConfig.getDependencies()) {
					ProjectConfigDto copy = (ProjectConfigDto)projectManager.getProjectConfig(depName).copy();
					if (DependencyBuildPolicy.FORCE.equals(policy)) {
						copy.setBuildOnDependencyFailure(true);
						copy.setBuildOnNoUpdates(true);
					}
									
					addOnce(copy, true);
				}
			}
		}
	}
	
	private void applyOverridesAndAdd(ProjectConfigDto projectConfig) throws ConfigException {
		ProjectConfigDto copy = (ProjectConfigDto) projectConfig.copy();
		
		if (buildOnDependencyFailureOverride) {
			copy.setBuildOnDependencyFailure(true);
		}
		
		if (buildOnNoUpdatesOverride) {
			copy.setBuildOnNoUpdates(true);
		}
		
		if (Full == updateStrategyOverride) {
			copy.setUpdateStrategy(UpdateStrategy.CleanAlways);
		} else if (Incremental == updateStrategyOverride) {
			copy.setUpdateStrategy(UpdateStrategy.IncrementalAlways);
		}
		
		addIfNeedToRebuild(copy);
	}

	private void addIfNeedToRebuild(ProjectConfigDto project) throws ConfigException {
		ProjectRebuildExpert expert = new ProjectRebuildExpert();
		expert.setProjectManager(projectManager);
		expert.setBuildManager(buildManager);
		expert.setWorkingCopyUpdateExpert(new WorkingCopyUpdateExpert());
		
		if (expert.shouldBuild(project, buildManager.getLatestStatus(project.getName()))) {
			final ProjectStatusDto buildStatus = new ProjectStatusDto();
			buildStatus.setBuildReasonKey(expert.getMessageKey());
			buildStatus.setBuildReasonArgs(expert.getMessageArgs());
			
			dg.addTarget(project, buildStatus);
		}
	}
}
