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

import java.util.Collection;
import java.util.Map;

import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class ConfigUpdatesDto extends BaseDto {
	private Collection<ProjectConfigDto> newProjectConfigs;
	private Map<String, ? extends PluginConfigDto> modifiedPluginConfigs;
	
	public Collection<ProjectConfigDto> getNewProjectConfigs() {
		return newProjectConfigs;
	}
	public void setNewProjectConfigs(Collection<ProjectConfigDto> newProjectConfigs) {
		this.newProjectConfigs = newProjectConfigs;
	}
	public Map<String, ? extends PluginConfigDto> getModifiedPluginConfigs() {
		return modifiedPluginConfigs;
	}
	public void setModifiedPluginConfigs(Map<String, ? extends PluginConfigDto> pluginConfigs) {
		this.modifiedPluginConfigs = pluginConfigs;
	}
}
