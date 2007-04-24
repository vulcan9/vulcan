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

import static net.sourceforge.vulcan.dotnet.dto.DotNetBuildEnvironmentDto.DotNetEnvironmentType.MSBuild;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.dotnet.dto.DotNetBuildEnvironmentDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetGlobalConfigDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetProjectConfigDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetBuildEnvironmentDto.DotNetEnvironmentType;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.PluginNotFoundException;
import net.sourceforge.vulcan.integration.BuildToolPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.support.PluginSupport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class DotNetBuildPlugin extends PluginSupport implements
		BuildToolPlugin, ConfigurablePlugin, ApplicationContextAware {
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.dotnet";
	public static final String PLUGIN_NAME = ".NET";

	public static final String MSBUILD_NAMESPACE_URI = "http://schemas.microsoft.com/developer/msbuild/2003";
	
	private static final Pattern SOLUTION_PROJECT_PATTERN = Pattern.compile("^Project.* = \".+\", \"(.+)\",", Pattern.MULTILINE);
	private static final Pattern PARENT_DIR_PATTERN = Pattern.compile("^(../)+");
	
	private PluginManager pluginManager;
	
	private DotNetGlobalConfigDto globalConfig = new DotNetGlobalConfigDto();
	private ApplicationContext applicationContext;
	
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

		if (MSBuild.equals(buildEnv.getType())) {
			return new MSBuildTool(globalConfig, dotNetProjectConfig, buildEnv, dir);
		}
		
		throw new ConfigException("dotnet.config.nant.unsupported", null);
	}

	public ProjectBuildConfigurator createProjectConfigurator(String url, File buildSpecFile, Document xmlDocument) throws ConfigException {
		if (isMSBuildFile(url, buildSpecFile, xmlDocument)) {
			final MSBuildProjectConfigurator cfgr = new MSBuildProjectConfigurator();
			cfgr.setApplicationContext(applicationContext);
			cfgr.setUrl(url);
			cfgr.setBuildEnvironment(findBuildEnvironmentByType(MSBuild));
			cfgr.setDocument(xmlDocument);
			return cfgr;
		}
		
		final ProjectBuildConfigurator solutionConfigurator = tryParseSolution(url, buildSpecFile, xmlDocument);
		
		if (solutionConfigurator != null) {
			return solutionConfigurator;
		}
		
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

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
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

	private boolean isMSBuildFile(String url, File buildSpecFile, Document xmlDocument) {
		if (xmlDocument != null) {
			final Element root = xmlDocument.getRootElement();
			if ("Project".equals(root.getName())
					&& MSBUILD_NAMESPACE_URI.equals(root.getNamespaceURI())) {
				return true;
			}
		}
		
		return false;
	}
	
	private ProjectBuildConfigurator tryParseSolution(String url, File buildSpecFile, Document xmlDocument) {
		final String contents;
		
		try {
			contents = FileUtils.readFileToString(buildSpecFile, "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if (contents.contains("Microsoft Visual Studio Solution File")) {
			final List<String> projectPaths = parseProjectPaths(contents);
			
			final MSBuildProjectConfigurator cfgr = new MSBuildProjectConfigurator();
			cfgr.setApplicationContext(applicationContext);
			cfgr.setUrl(url);
			cfgr.setBuildEnvironment(findBuildEnvironmentByType(MSBuild));
			cfgr.setSubprojectUrls(projectPaths);
			cfgr.setRelativePathToProjectBasedir(findLongestRelativePath(projectPaths));
			cfgr.setShouldCreate(false);
			
			return cfgr;
		}
		
		return null;
	}
	
	private String findLongestRelativePath(List<String> projectPaths) {
		String longest = ".";
		
		for (String path : projectPaths) {
			final Matcher matcher = PARENT_DIR_PATTERN.matcher(path);
			if (matcher.find()) {
				final String parents = matcher.group();
				if (parents.length() > longest.length()) {
					longest = parents;
				}
			}
		}
		
		return longest;
	}

	private List<String> parseProjectPaths(String contents) {
		final List<String> paths = new ArrayList<String>();
		
		final Pattern pattern = SOLUTION_PROJECT_PATTERN;
		
		final Matcher matcher = pattern.matcher(contents);
		while (matcher.find()) {
			final String projectPath = matcher.group(1).replaceAll("\\\\", "/");
			
			if (!projectPath.toLowerCase().endsWith("proj")) {
				continue;
			}
			paths.add(projectPath);
		}

		return paths;
	}

	private String findBuildEnvironmentByType(DotNetEnvironmentType envType) {
		for (DotNetBuildEnvironmentDto env : globalConfig.getBuildEnvironments()) {
			if (envType == env.getType()) {
				return env.getDescription();
			}
		}
		return null;
	}
}
