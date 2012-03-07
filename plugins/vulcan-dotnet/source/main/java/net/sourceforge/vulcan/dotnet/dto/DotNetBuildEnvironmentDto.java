/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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
package net.sourceforge.vulcan.dotnet.dto;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.vulcan.dotnet.DotNetBuildPlugin;
import net.sourceforge.vulcan.dto.PluginProfileDto;
import net.sourceforge.vulcan.integration.ConfigChoice;

public class DotNetBuildEnvironmentDto extends PluginProfileDto {
	public static enum DotNetEnvironmentType {
		MSBuild, NAnt
	}
	
	public static List<String> availableToolsVersions =
		Arrays.asList(new String[] {"Unspecified", "2.0", "3.5", "4.0"});
	
	private String description;
	private String location;
	private DotNetEnvironmentType type;
	private String toolsVersion = "Unspecified";
	private String maxJobs;
	private boolean nodeReuseEnabled = true;
	
	@Override
	public String getPluginId() {
		return DotNetBuildPlugin.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return DotNetBuildPlugin.PLUGIN_NAME;
	}
	@Override
	public String getProjectConfigProfilePropertyName() {
		return "buildEnvironment";
	}
	@Override
	public String getHelpTopic() {
		return "DotNetBuildEnvironmentConfiguration";
	}
	public boolean isNodeReuseEnabled() {
		return nodeReuseEnabled;
	}
	public void setNodeReuseEnabled(boolean nodeReuseEnabled) {
		this.nodeReuseEnabled = nodeReuseEnabled;
	}

	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "description", "DotNetBuildEnvironmentDto.description.name",
				"DotNetBuildEnvironmentDto.description.text", locale);
		
		addProperty(pds, "location", "DotNetBuildEnvironmentDto.location.name",
				"DotNetBuildEnvironmentDto.location.text", locale);
		
		addProperty(pds, "type", "DotNetBuildEnvironmentDto.type.name",
				"DotNetBuildEnvironmentDto.type.text", locale);
		
		Map<String, Object> metadata = new HashMap<String, Object>();
		metadata.put(ATTR_CHOICE_TYPE, ConfigChoice.INLINE);
		metadata.put(ATTR_AVAILABLE_CHOICES, availableToolsVersions);

		addProperty(pds, "toolsVersion", "DotNetBuildEnvironmentDto.toolsVersion.name",
				"DotNetBuildEnvironmentDto.toolsVersion.text", locale, metadata);
		
		addProperty(pds, "maxJobs", "DotNetBuildEnvironmentDto.maxJobs.name",
				"DotNetBuildEnvironmentDto.maxJobs.text", locale);
		
		addProperty(pds, "nodeReuseEnabled", "DotNetBuildEnvironmentDto.nodeReuseEnabled.name",
				"DotNetBuildEnvironmentDto.nodeReuseEnabled.text", locale);
		
		return pds;
	}

	@Override
	public String toString() {
		return description;
	}

	@Override
	public String getName() {
		return description;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public DotNetEnvironmentType getType() {
		return type;
	}

	public void setType(DotNetEnvironmentType type) {
		this.type = type;
	}
	
	public String getToolsVersion() {
		return toolsVersion;
	}
	
	public void setToolsVersion(String toolsVersion) {
		this.toolsVersion = toolsVersion;
	}
	
	public String getMaxJobs() {
		return maxJobs;
	}
	
	public void setMaxJobs(String maxJobs) {
		this.maxJobs = maxJobs;
	}
}
