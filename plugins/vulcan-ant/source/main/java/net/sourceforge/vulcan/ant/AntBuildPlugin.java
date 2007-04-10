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
package net.sourceforge.vulcan.ant;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.integration.BuildToolPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.support.PluginSupport;

public class AntBuildPlugin extends PluginSupport
		implements BuildToolPlugin, ConfigurablePlugin, ApplicationContextAware {
	private AntConfig globalConfig = new AntConfig();
	private ApplicationContext applicationContext;
	
	public void init() {
		addSystemJavaHomeIfMissing(globalConfig, applicationContext);
	}
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	public String getId() {
		return AntConfig.PLUGIN_ID;
	}
	public String getName() {
		return AntConfig.PLUGIN_NAME;
	}
	public BuildTool createInstance(BuildToolConfigDto config) throws ConfigException {
		final AntProjectConfig antProjectConfig = (AntProjectConfig)config;
		
		String javaHomeName = antProjectConfig.getJavaHome();
		final JavaHome javaHome;
		
		if (isBlank(javaHomeName) || (isNotBlank(javaHomeName) && javaHomeName.startsWith("System"))) {
			javaHomeName = "System";
		}

		javaHome = getSelectedEnvironment(
			this.globalConfig.getJavaHomes(),
			javaHomeName,
			"ant.java.profile.missing",
			true);
		
		return new AntBuildTool(antProjectConfig, this.globalConfig, javaHome);
	}
	public ProjectBuildConfigurator createProjectConfigurator(String url, File buildSpecFile, Document xmlDocument) throws ConfigException {
		if (xmlDocument == null) {
			return null;
		}
		
		final Element root = xmlDocument.getRootElement();
		
		if (!"".equals(root.getNamespaceURI())) {
			return null;
		}
		
		if (url.endsWith(".build")) {
			// probably a NAnt project.
			return null;
		}
		
		if (!"project".equals(root.getName())) {
			return null;
		}
		
		final String projectName = root.getAttributeValue("name");
		final String basedir = root.getAttributeValue("basedir");
		
		if (projectName == null) {
			return null;
		}
		
		return new AntProjectBuildConfigurator(applicationContext, projectName, basedir);
	}
	public AntProjectConfig getDefaultConfig() {
		return new AntProjectConfig();
	}
	public AntConfig getConfiguration() {
		return globalConfig;
	}
	public void setConfiguration(PluginConfigDto config) {
		this.globalConfig = (AntConfig) config;
		addSystemJavaHomeIfMissing(globalConfig, applicationContext);
	}
	public static void addSystemJavaHomeIfMissing(AntConfig antConfig, ApplicationContext applicationContext) {
		JavaHome javaHome;
		
		try {
			javaHome = getSelectedEnvironment(antConfig.getJavaHomes(), "System", null, true);
		} catch (ConfigException e) {
			throw new RuntimeException(e);
		}
		
		if (javaHome == null) {
			javaHome = new JavaHome();
			javaHome.setApplicationContext(applicationContext);
			javaHome.setMaxMemory("128");
			
			List<JavaHome> homes = new ArrayList<JavaHome>(Arrays.asList(antConfig.getJavaHomes()));
			homes.add(0, javaHome);
			antConfig.setJavaHomes(homes.toArray(new JavaHome[homes.size()]));
		}
		
		javaHome.setDescription(JavaHome.SYSTEM_DESC);
		javaHome.setJavaHome(JavaHome.SYSTEM_HOME);
	}
}
