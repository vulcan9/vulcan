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
package net.sourceforge.vulcan.dotnet;

import java.io.File;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.dotnet.dto.DotNetBuildEnvironmentDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetGlobalConfigDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetProjectConfigDto;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.PluginNotFoundException;
import net.sourceforge.vulcan.integration.BuildToolPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.support.PluginSupport;

import org.apache.commons.lang.StringUtils;

public class DotNetBuildPlugin extends PluginSupport implements BuildToolPlugin, ConfigurablePlugin {
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.dotnet";
	public static final String PLUGIN_NAME = ".NET";

	private PluginManager pluginManager;
	
	private DotNetGlobalConfigDto globalConfig = new DotNetGlobalConfigDto();
	
	public BuildTool createInstance(BuildToolConfigDto config) throws ConfigException {
		final DotNetProjectConfigDto dotNetProjectConfig = (DotNetProjectConfigDto) config;

		final DotNetBuildEnvironmentDto buildEnv = getSelectedEnvironment(
				globalConfig.getBuildEnvironments(),
				dotNetProjectConfig.getBuildEnvironment(),
				"dotnet.config.environment.not.available");
		
		if (buildEnv == null) {
			throw new ConfigException("dotnet.config.no.environment", null);
		}
		
		final File dir;
		
		try {
			dir = pluginManager.getPluginDirectory(PLUGIN_ID);
			
		} catch (PluginNotFoundException e) {
			throw new RuntimeException(e);
		}

		if (DotNetBuildEnvironmentDto.DotNetEnvironmentType.MSBuild.equals(buildEnv.getType())) {
			return new MSBuildTool(globalConfig, dotNetProjectConfig, buildEnv, dir);
		}
		
		throw new ConfigException("dotnet.config.nant.unsupported", null);
	}

	public ProjectBuildConfigurator createProjectConfigurator(File buildSpecFile) throws ConfigException {
		// Not supported.
		return null;
	}
	
	public DotNetGlobalConfigDto getConfiguration() {
		return globalConfig;
	}
	
	public void setConfiguration(PluginConfigDto bean) {
		this.globalConfig = (DotNetGlobalConfigDto) bean;
	}
	
	public BuildToolConfigDto getDefaultConfig() {
		return new DotNetProjectConfigDto();
	}

	public String getId() {
		return PLUGIN_ID;
	}

	public String getName() {
		return PLUGIN_NAME;
	}

	public void setPluginManager(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}
	
	DotNetBuildEnvironmentDto getSelectedBuildEnvironment(DotNetProjectConfigDto config) throws ConfigException {
		final String desc = config.getBuildEnvironment();
		
		if (StringUtils.isBlank(desc)) {
			throw new ConfigException("dotnet.config.no.environment", null);
		}
		
		final DotNetBuildEnvironmentDto[] envs = globalConfig.getBuildEnvironments();
		
		for (DotNetBuildEnvironmentDto env : envs) {
			if (desc.equals(env.getDescription())) {
				return env;
			}
		}
		
		throw new ConfigException("dotnet.config.environment.not.available", new String[] {desc});
	}
}
