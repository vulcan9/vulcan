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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.ProjectRepositoryConfigurator;
import net.sourceforge.vulcan.StateManager;
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
	
	/*
	 * TODO: allow build configurator to configure emails (?)
	 * TODO: specify "use existing," "overwrite," "report error" or "rename" for existing project names.
	 * TODO: allow build tool to specify relative path to project url (e.g. . for ./ant/build.xml).
	 */
	public void createProjectsForUrl(String startUrl, boolean createSubprojects) throws ConfigException, StoreException, DuplicateNameException {
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
			final ProjectRepositoryConfigurator repoConfigurator = createRepositoryConfiguratorForUrl(
					repositoryPlugins, projectConfig, url);
			
			File buildSpecFile = null;
			final ProjectBuildConfigurator buildConfigurator;
			
			try {
				buildSpecFile = downloadBuildSpecFile(repoConfigurator);
				final Document xmlDocument = tryParse(buildSpecFile);
				buildConfigurator = createBuildToolConfigurator(
						buildToolPlugins, projectConfig, buildSpecFile, xmlDocument);
			} finally {
				deleteIfPresent(buildSpecFile);
			}
			
			buildConfigurator.applyConfiguration(projectConfig, existingProjectNames, createSubprojects);
			repoConfigurator.applyConfiguration(projectConfig);
			
			if (createSubprojects && buildConfigurator.isStandaloneProject()) {
				repoConfigurator.setNonRecursive();
			}
			
			if (isBlank(projectConfig.getWorkDir())) {
				final String workDir = store.getWorkingCopyLocationPattern().replace("${projectName}", projectConfig.getName());
				projectConfig.setWorkDir(workDir);
			}
			
			if (createSubprojects) {
				final List<String> subprojectUrls = buildConfigurator.getSubprojectUrls();
				if (subprojectUrls != null) {
					urls.addAll(subprojectUrls);
				}
			}
			
			existingProjectNames.add(projectConfig.getName());
			
			newProjects.add(projectConfig);
			repoConfigurators.add(repoConfigurator);
			
			log.info("Configured project " + projectConfig.getName());
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

		stateManager.addProjectConfig(newProjects.toArray(new ProjectConfigDto[newProjects.size()]));
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

	protected Document tryParse(File buildSpecFile) {
		try {
			return new SAXBuilder().build(buildSpecFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (JDOMException e) {
			return null;
		}
	}

	private ProjectRepositoryConfigurator createRepositoryConfiguratorForUrl(final List<RepositoryAdaptorPlugin> repositoryPlugins, ProjectConfigDto projectConfig, String url) throws ConfigException {
		for (RepositoryAdaptorPlugin plugin : repositoryPlugins) {
			final ProjectRepositoryConfigurator configurator = plugin.createProjectConfigurator(url);
			if (configurator != null) {
				log.info("Using " + plugin.getName() + " to download " + url);
				projectConfig.setRepositoryAdaptorPluginId(plugin.getId());
				return configurator;
			}
		}
		
		throw new ConfigException("errors.url.unsupported", null);
	}
	
	private ProjectBuildConfigurator createBuildToolConfigurator(List<BuildToolPlugin> buildToolPlugins, ProjectConfigDto projectConfig, File buildSpecFile, Document xmlDocument) throws ConfigException {
		for (BuildToolPlugin plugin : buildToolPlugins) {
			final ProjectBuildConfigurator configurator = plugin.createProjectConfigurator(
					buildSpecFile, xmlDocument);
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
