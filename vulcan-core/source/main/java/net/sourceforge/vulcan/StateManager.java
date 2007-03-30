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

import java.util.List;
import java.util.Set;

import net.sourceforge.vulcan.dto.PluginProfileDto;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.exception.PluginNotFoundException;
import net.sourceforge.vulcan.exception.ProjectNeedsDependencyException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.scheduler.BuildDaemon;
import net.sourceforge.vulcan.scheduler.ProjectScheduler;

@SvnRevision(id="$Id$", url="$HeadURL$")
public interface StateManager {
	@Deprecated
	public StateManagerConfigDto getConfig();
	
	public List<String> getProjectConfigNames();
	public ProjectConfigDto getProjectConfig(String name) throws NoSuchProjectException;
	public void addProjectConfig(ProjectConfigDto... configs) throws DuplicateNameException, StoreException;
	public void updateProjectConfig(String oldName, ProjectConfigDto updatedConfig, boolean setLastModifiedDate)	throws DuplicateNameException, NoSuchProjectException, StoreException;
	public void deleteProjectConfig(String name) throws ProjectNeedsDependencyException, StoreException;
	
	public SchedulerConfigDto getSchedulerConfig(String name);
	public ProjectConfigDto[] getProjectsForScheduler(String schedulerName);
	public void addSchedulerConfig(SchedulerConfigDto config) throws DuplicateNameException, StoreException;
	public void updateSchedulerConfig(String oldName, SchedulerConfigDto updatedConfig) throws DuplicateNameException, StoreException;
	public void deleteSchedulerConfig(String name);
	
	public SchedulerConfigDto getBuildDaemonConfig(String name);
	public void addBuildDaemonConfig(SchedulerConfigDto scheduler) throws DuplicateNameException, StoreException;
	public void updateBuildDaemonConfig(String oldName, SchedulerConfigDto updatedConfig) throws DuplicateNameException, StoreException;
	public void deleteBuildDaemonConfig(String name) throws StoreException;
	
	public List<ProjectScheduler> getSchedulers();
	public List<BuildDaemon> getBuildDaemons();
	public BuildDaemon getBuildDaemon(String name);
	
	public void flushBuildQueue();
	
	public boolean isRunning();

	public PluginManager getPluginManager();

	public void updatePluginConfig(PluginConfigDto pluginConfig, Set<PluginProfileDto> renamedProfiles) throws PluginNotFoundException, StoreException;
	public void removePlugin(String pluginId) throws StoreException, PluginNotFoundException;

	public void save() throws StoreException;

	public void start() throws StoreException;
	public void shutdown() throws StoreException;
}
