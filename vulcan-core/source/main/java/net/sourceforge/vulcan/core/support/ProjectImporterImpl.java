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
package net.sourceforge.vulcan.core.support;

import static net.sourceforge.vulcan.core.NameCollisionResolutionMode.*;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.ProjectRepositoryConfigurator;
import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.core.NameCollisionResolutionMode;
import net.sourceforge.vulcan.core.ProjectImporter;
import net.sourceforge.vulcan.core.Store;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.PluginNotConfigurableException;
import net.sourceforge.vulcan.exception.PluginNotFoundException;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.integration.BuildToolPlugin;
import net.sourceforge.vulcan.integration.RepositoryAdaptorPlugin;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ProjectImporterImpl implements ProjectImporter {
	private final Log log = LogFactory.getLog(getClass());
	
	private PluginManager pluginManager;
	private StateManager stateManager;
	private Store store;
	
	public void createProjectsForUrl(String startUrl, String username, String password, boolean createSubprojects, NameCollisionResolutionMode nameCollisionResolutionMode, String[] schedulerNames) throws ConfigException, StoreException, DuplicateNameException {
		final List<RepositoryAdaptorPlugin> repositoryPlugins = pluginManager.getPlugins(RepositoryAdaptorPlugin.class);
		final List<BuildToolPlugin> buildToolPlugins = pluginManager.getPlugins(BuildToolPlugin.class);
		
		final List<String> urls = new ArrayList<String>();
		urls.add(startUrl);
		
		final List<ProjectConfigDto> newProjects = new ArrayList<ProjectConfigDto>();
		final List<ProjectRepositoryConfigurator> repoConfigurators = new ArrayList<ProjectRepositoryConfigurator>();
		
		final List<String> existingProjectNames = new ArrayList<String>(stateManager.getProjectConfigNames());
		
		while (!urls.isEmpty()) {
			final String url = urls.remove(0);
			
			final ProjectConfigDto projectConfig = new ProjectConfigDto();
			projectConfig.setSchedulerNames(schedulerNames);
			
			final ProjectRepositoryConfigurator repoConfigurator = createRepositoryConfiguratorForUrl(
					repositoryPlugins, projectConfig, url, username, password);
			
			File buildSpecFile = null;
			final ProjectBuildConfigurator buildConfigurator;
			
			try {
				buildSpecFile = downloadBuildSpecFile(repoConfigurator);
				final Document xmlDocument = tryParse(buildSpecFile);
				buildConfigurator = createBuildToolConfigurator(
						buildToolPlugins, projectConfig, url, buildSpecFile, xmlDocument);
			} finally {
				deleteIfPresent(buildSpecFile);
			}
			
			final boolean shouldCreate = configureProject(
					projectConfig, repoConfigurator, buildConfigurator, url,
					existingProjectNames, nameCollisionResolutionMode, createSubprojects);
			
			if (createSubprojects) {
				final List<String> subprojectUrls = buildConfigurator.getSubprojectUrls();
				if (subprojectUrls != null) {
					urls.addAll(subprojectUrls);
				}
			}
			
			if (shouldCreate) {
				existingProjectNames.add(projectConfig.getName());
				
				newProjects.add(projectConfig);
				repoConfigurators.add(repoConfigurator);
				
				log.info("Configured project " + projectConfig.getName());
			} else {
				log.info("Skipping project " + projectConfig.getName() + " because it already exists.");
			}
		}
		
		//TODO: stateManager should be aware that pluginConfig is being modified.
		//TODO: this entire block should succeed or fail atomically.
		for (int i=0; i<newProjects.size(); i++) {
			final ProjectConfigDto projectConfig = newProjects.get(i);
			try {
				final PluginConfigDto pluginConfig = pluginManager.getPluginConfigInfo(
						projectConfig.getRepositoryAdaptorPluginId());
	
				repoConfigurators.get(i).updateGlobalConfig(pluginConfig);
			} catch (PluginNotConfigurableException ignore) {
			} catch (PluginNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		stateManager.addOrReplaceProjectConfig(newProjects.toArray(new ProjectConfigDto[newProjects.size()]));
		log.info("Successfully imported project(s) for URL " + startUrl);
	}

	public void setPluginManager(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}
	
	public void setStateManager(StateManager stateManager) {
		this.stateManager = stateManager;
	}

	public void setStore(Store store) {
		this.store = store;
	}
	
	protected File createTempFile() throws IOException {
		return File.createTempFile("vulcan-new-project", ".tmp");
	}

	/**
	 * @return <code>true</code> if the project should be created, <code>false</code>
	 * if it should be skipped due to a name collision.
	 */
	protected boolean configureProject(final ProjectConfigDto projectConfig, final ProjectRepositoryConfigurator repoConfigurator, final ProjectBuildConfigurator buildConfigurator, String url, final List<String> existingProjectNames, NameCollisionResolutionMode nameCollisionResolutionMode, boolean createSubprojects) throws DuplicateNameException {
		final String relativePath = buildConfigurator.getRelativePathToProjectBasedir();
		final PathInfo pathInfo = computeProjectBasedirUrl(url, relativePath);
		
		buildConfigurator.applyConfiguration(projectConfig, pathInfo.getBuildSpecPath(), existingProjectNames, createSubprojects);

		repoConfigurator.applyConfiguration(projectConfig, pathInfo.getProjectBasedirUrl());
		
		final boolean namingConflict = existingProjectNames.contains(projectConfig.getName());
		
		if (namingConflict && Abort == nameCollisionResolutionMode) {
			throw new DuplicateNameException(projectConfig.getName());
		} else if (namingConflict && UseExisting == nameCollisionResolutionMode) {
			return false;
		}
		
		if (createSubprojects && buildConfigurator.isStandaloneProject()) {
			repoConfigurator.setNonRecursive();
		}
		
		if (isBlank(projectConfig.getWorkDir())) {
			final String workDir = store.getWorkingCopyLocationPattern().replace("${projectName}", projectConfig.getName());
			projectConfig.setWorkDir(workDir);
		}
		
		return true;
	}
	
	protected PathInfo computeProjectBasedirUrl(String url, String relativePath) {
		final PathInfo pathInfo = new PathInfo();
		
		final StringBuilder sb = new StringBuilder(url);
		sb.delete(sb.lastIndexOf("/") + 1, sb.length());
		if (relativePath != null) {
			sb.append(relativePath);
		}
		
		try {
			String normalized = new URI(sb.toString()).normalize().toString();
			
			if (url.startsWith("file:///") && !normalized.startsWith("///")) {
				/* Special case for file protocol.
				 * URI.normalize eats the extra slashes at the begining.
				 * This behavior does not seem to be documented in the JavaDoc for URI.
				 */
				normalized = "file:///" + normalized.substring(6);
			}
			
			pathInfo.setProjectBasedirUrl(normalized);
			pathInfo.setBuildSpecPath(url.substring(normalized.length()));
			
			return pathInfo;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	protected Document tryParse(File buildSpecFile) {
		try {
			return new SAXBuilder().build(buildSpecFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (JDOMException e) {
			return null;
		}
	}

	protected static class PathInfo {
		private String projectBasedirUrl;
		private String buildSpecPath;
		
		public String getBuildSpecPath() {
			return buildSpecPath;
		}
		public void setBuildSpecPath(String buildSpecPath) {
			this.buildSpecPath = buildSpecPath;
		}
		public String getProjectBasedirUrl() {
			return projectBasedirUrl;
		}
		public void setProjectBasedirUrl(String projectBasedirUrl) {
			this.projectBasedirUrl = projectBasedirUrl;
		}
	}
	private ProjectRepositoryConfigurator createRepositoryConfiguratorForUrl(final List<RepositoryAdaptorPlugin> repositoryPlugins, ProjectConfigDto projectConfig, String url, String username, String password) throws ConfigException {
		for (RepositoryAdaptorPlugin plugin : repositoryPlugins) {
			final ProjectRepositoryConfigurator configurator = plugin.createProjectConfigurator(url, username, password);
			if (configurator != null) {
				log.info("Using " + plugin.getName() + " to download " + url);
				projectConfig.setRepositoryAdaptorPluginId(plugin.getId());
				return configurator;
			}
		}
		
		throw new ConfigException("errors.url.unsupported", null);
	}
	
	private ProjectBuildConfigurator createBuildToolConfigurator(List<BuildToolPlugin> buildToolPlugins, ProjectConfigDto projectConfig, String url, File buildSpecFile, Document xmlDocument) throws ConfigException {
		for (BuildToolPlugin plugin : buildToolPlugins) {
			final ProjectBuildConfigurator configurator = plugin.createProjectConfigurator(
					url, buildSpecFile, xmlDocument);
			if (configurator != null) {
				log.info("Using " + plugin.getName() + " to configure project.");
				projectConfig.setBuildToolPluginId(plugin.getId());
				return configurator;
			}
		}
		
		throw new ConfigException("errors.build.file.unsupported", null);
	}
	
	private File downloadBuildSpecFile(ProjectRepositoryConfigurator ra) throws RepositoryException, ConfigException {
		File tmpFile = null;
		
		try {
			tmpFile = createTempFile();
			ra.download(tmpFile);
			return tmpFile;
		} catch (IOException e) {
			deleteIfPresent(tmpFile);
			throw new ConfigException("errors.import.download", new Object[] {e.getMessage()}, e);
		}
	}

	private void deleteIfPresent(File file) {
		if (file != null && !file.delete()) {
			file.deleteOnExit();
		}
	}
}
