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
package net.sourceforge.vulcan;

import java.util.Date;
import java.util.List;

import net.sourceforge.vulcan.core.DependencyBuildPolicy;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.core.WorkingCopyUpdateStrategy;
import net.sourceforge.vulcan.dto.BuildArtifactLocationDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.exception.ProjectNeedsDependencyException;
import net.sourceforge.vulcan.exception.ProjectsLockedException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public interface ProjectManager {

	public ProjectConfigDto getProjectConfig(String name) throws NoSuchProjectException;

	public void addProjectConfig(ProjectConfigDto... configs)
			throws DuplicateNameException, StoreException;

	public void updateProjectConfig(String oldName,
			ProjectConfigDto updatedConfig, boolean setLastModifiedDate) throws DuplicateNameException, NoSuchProjectException, StoreException;

	public void deleteProjectConfig(String... names)
			throws ProjectNeedsDependencyException, NoSuchProjectException, StoreException;

	public List<BuildArtifactLocationDto> getArtifactLocations();
	
	/**
	 * @return Array of projects assigned to a scheduler, excluding locked projects.
	 */
	public ProjectConfigDto[] getProjectsForScheduler(String schedulerName);

	public RepositoryAdaptor getRepositoryAdaptor(ProjectConfigDto projectConfig) throws ConfigException;
	
	public BuildTool getBuildTool(ProjectConfigDto target) throws ConfigException;
	
	public DependencyGroup buildDependencyGroup(ProjectConfigDto[] projects,
			DependencyBuildPolicy policy, WorkingCopyUpdateStrategy updateStrategyOverride,
			boolean buildOnDependencyFailureOverride, boolean buildOnNoUpdatesOverride) throws ProjectsLockedException;

	public Date getPluginModificationDate(String pluginId);
}