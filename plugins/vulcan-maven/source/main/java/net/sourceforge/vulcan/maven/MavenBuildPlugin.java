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
package net.sourceforge.vulcan.maven;

import static net.sourceforge.vulcan.ant.AntBuildPlugin.addSystemJavaHomeIfMissing;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.ant.AntProjectConfig;
import net.sourceforge.vulcan.ant.JavaHome;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.integration.BuildToolPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.support.PluginSupport;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class MavenBuildPlugin extends PluginSupport
		implements BuildToolPlugin, ConfigurablePlugin, ApplicationContextAware {
	
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.maven";
	public static final String PLUGIN_NAME = "Apache Maven";
	
	MavenConfig config = new MavenConfig();
	MavenBuildToolFactory mavenBuildToolFactory = new MavenBuildToolFactory();
	
	private ApplicationContext applicationContext;
	
	public void init() {
		addSystemJavaHomeIfMissing(config, applicationContext);
	}
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public String getId() {
		return PLUGIN_ID;
	}
	public String getName() {
		return PLUGIN_NAME;
	}
	
	public BuildTool createInstance(BuildToolConfigDto projectConfig) throws ConfigException {
		final MavenProjectConfig antProjectConfig = (MavenProjectConfig)projectConfig;
		
		final String javaHomeName = antProjectConfig.getJavaHome();
		final JavaHome javaHome;
		
		if (isNotBlank(javaHomeName) && javaHomeName.startsWith("System")) {
			javaHome = getSelectedEnvironment(
				this.config.getJavaHomes(),
				"System",
				null,
				true);
		} else {
			javaHome = getSelectedEnvironment(
				this.config.getJavaHomes(),
				javaHomeName,
				"ant.java.profile.missing");
		}

		final MavenHome mavenHome = getSelectedEnvironment(
				this.config.getMavenHomes(),
				antProjectConfig.getMavenHome(),
				"maven.home.profile.missing");
		
		return mavenBuildToolFactory.createMavenBuildTool((AntProjectConfig) projectConfig, config, javaHome, mavenHome);
	}
	public BuildToolConfigDto getDefaultConfig() {
		return new MavenProjectConfig();
	}
	
	public MavenConfig getConfiguration() {
		return config;
	}
	public void setConfiguration(PluginConfigDto config) {
		this.config = (MavenConfig) config;
		addSystemJavaHomeIfMissing(this.config, applicationContext);
	}
}
