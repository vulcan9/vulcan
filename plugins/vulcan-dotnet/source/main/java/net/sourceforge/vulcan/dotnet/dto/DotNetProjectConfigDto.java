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
package net.sourceforge.vulcan.dotnet.dto;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.vulcan.ant.AntProjectConfig;
import net.sourceforge.vulcan.dotnet.DotNetBuildPlugin;
import net.sourceforge.vulcan.integration.ConfigChoice;

public class DotNetProjectConfigDto extends AntProjectConfig {
	public static enum BuildConfiguration {
		Inherit,
		Unspecified,
		Debug,
		Release
	}

	private BuildConfiguration buildConfiguration = BuildConfiguration.Unspecified;
	private String buildEnvironment;
	
	public DotNetProjectConfigDto() {
		setBuildScript("");
	}
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "buildScript", "DotNetProjectConfigDto.project.file.name",
				"DotNetProjectConfigDto.project.file.text", locale);

		addProperty(pds, "targets", "DotNetProjectConfigDto.targets.name",
				"DotNetProjectConfigDto.targets.text", locale);
		
		final List<String> availChoices = new ArrayList<String>();
		
		final DotNetBuildEnvironmentDto[] envs = getPlugin(DotNetBuildPlugin.class)
			.getConfiguration().getBuildEnvironments();
		
		for (DotNetBuildEnvironmentDto env : envs) {
			availChoices.add(env.getDescription());
		}
		
		final Map<String, Object> props = new HashMap<String, Object>();
		props.put(ATTR_CHOICE_TYPE, ConfigChoice.INLINE);
		props.put(ATTR_AVAILABLE_CHOICES, availChoices);
		
		addProperty(pds, "buildEnvironment", "DotNetProjectConfigDto.buildEnvironment.name",
				"DotNetProjectConfigDto.buildEnvironment.text", locale, props);
		
		addProperty(pds, "buildConfiguration", "DotNetProjectConfigDto.buildConfiguration.name",
				"DotNetProjectConfigDto.buildConfiguration.text", locale);

		addProperty(pds, "antProperties", "DotNetProjectConfigDto.properties.name",
				"DotNetProjectConfigDto.properties.text", locale);
		
		return pds;
	}
	
	@Override
	public String getPluginId() {
		return DotNetBuildPlugin.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return DotNetBuildPlugin.PLUGIN_NAME;
	}
	@Override
	public String getHelpTopic() {
		return "DotNetProjectConfiguration";
	}

	public BuildConfiguration getBuildConfiguration() {
		return buildConfiguration;
	}

	public void setBuildConfiguration(BuildConfiguration buildConfiguration) {
		this.buildConfiguration = buildConfiguration;
	}
	
	public String getBuildEnvironment() {
		return buildEnvironment;
	}
	
	public void setBuildEnvironment(String buildEnvironment) {
		this.buildEnvironment = buildEnvironment;
	}
}
