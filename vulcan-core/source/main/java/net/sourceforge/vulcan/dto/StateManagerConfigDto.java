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
package net.sourceforge.vulcan.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class StateManagerConfigDto extends BaseDto {
	private BuildManagerConfigDto buildManagerConfig;
	private ProjectConfigDto[] projects = {};
	private SchedulerConfigDto[] schedulers = {};
	private SchedulerConfigDto[] buildDaemons = {};
	private List<BuildArtifactLocationDto> artifactLocations;
	private Map<String, PluginConfigDto> pluginConfigs = new HashMap<String, PluginConfigDto>();
	
	public BuildManagerConfigDto getBuildManagerConfig() {
		return buildManagerConfig;
	}
	public void setBuildManagerConfig(BuildManagerConfigDto buildManagerConfig) {
		this.buildManagerConfig = buildManagerConfig;
	}
	public ProjectConfigDto[] getProjects() {
		return projects;
	}
	public void setProjects(ProjectConfigDto[] projects) {
		ProjectConfigDto[] oldProjects = this.projects;
		this.projects = projects;
		propertyChangeSupport.firePropertyChange("projects", oldProjects, projects);
	}
	public SchedulerConfigDto[] getSchedulers() {
		return schedulers;
	}
	public void setSchedulers(SchedulerConfigDto[] schedules) {
		this.schedulers = schedules;
	}
	public SchedulerConfigDto[] getBuildDaemons() {
		return buildDaemons;
	}
	public void setBuildDaemons(SchedulerConfigDto[] buildDaemons) {
		this.buildDaemons = buildDaemons;
	}
	public Map<String, PluginConfigDto> getPluginConfigs() {
		return pluginConfigs;
	}
	public void setPluginConfigs(Map<String, PluginConfigDto> pluginConfigs) {
		this.pluginConfigs = pluginConfigs;
	}
	public List<BuildArtifactLocationDto> getArtifactLocations() {
		return artifactLocations;
	}
	public void setArtifactLocations(List<BuildArtifactLocationDto> artifactLocations) {
		this.artifactLocations = artifactLocations;
	}
}