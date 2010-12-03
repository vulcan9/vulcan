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
package net.sourceforge.vulcan.shell;

import java.io.File;

import org.jdom.Document;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.integration.BuildToolPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.support.PluginSupport;

public class ShellBuildPlugin extends PluginSupport
		implements BuildToolPlugin, ConfigurablePlugin {

	public static final String PLUGIN_ID = "net.sourceforge.vulcan.shell";
	public static final String PLUGIN_NAME = "Shell";

	private ShellBuildToolConfig globalConfig = new ShellBuildToolConfig();
	
	public BuildTool createInstance(BuildToolConfigDto config)
			throws ConfigException {
		
		final ShellBuildTool tool = new ShellBuildTool();
		
		tool.setGlobalConfig(globalConfig);
		tool.setProjectPluginConfig((ShellProjectConfig) config);
		
		return tool;
	}

	public ProjectBuildConfigurator createProjectConfigurator(String url,
			File buildSpecFile, Document xmlDocument) throws ConfigException {
		// Not supported.
		return null;
	}
	
	public BuildToolConfigDto getDefaultConfig() {
		return new ShellProjectConfig();
	}

	public String getId() {
		return PLUGIN_ID;
	}

	public String getName() {
		return PLUGIN_NAME;
	}

	public PluginConfigDto getConfiguration() {
		return globalConfig;
	}

	public void setConfiguration(PluginConfigDto bean) {
		globalConfig = (ShellBuildToolConfig) bean;
	}
}
