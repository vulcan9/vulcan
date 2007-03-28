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

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ProjectImporterImpl implements ProjectImporter {
	private PluginManager pluginManager;
	private StateManager stateManager;
	private Store store;
	
	/*
	 * TODO: check if file is a maven2 pom before attempting to process in maven plugin
	 * TODO: handle maven exceptions like missing dependencies, missing parent, etc.
	 */
	public void createProjectsForUrl(String url) throws ConfigException, StoreException, DuplicateNameException {
		final ProjectConfigDto projectConfig = new ProjectConfigDto();
		final ProjectRepositoryConfigurator repoConfigurator = createRepositoryAdaptorForUrl(projectConfig, url);
		
		final File buildSpecFile = downloadBuildSpecFile(repoConfigurator);

		final ProjectBuildConfigurator buildConfigurator = createBuildToolConfigurator(projectConfig, buildSpecFile);
		
		repoConfigurator.applyConfiguration(projectConfig);
		buildConfigurator.applyConfiguration(projectConfig);
		
		if (buildConfigurator.isStandaloneProject()) {
			repoConfigurator.setNonRecursive();
		}
		
		if (isBlank(projectConfig.getWorkDir())) {
			final String workDir = store.getWorkingCopyLocationPattern().replace("${projectName}", projectConfig.getName());
			projectConfig.setWorkDir(workDir);
		}
		
		try {
			final PluginConfigDto pluginConfig = pluginManager.getPluginConfigInfo(
					projectConfig.getRepositoryAdaptorPluginId());

			repoConfigurator.updateGlobalConfig(pluginConfig);
		} catch (PluginNotConfigurableException ignore) {
		} catch (PluginNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		stateManager.addProjectConfig(projectConfig);
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

	private ProjectRepositoryConfigurator createRepositoryAdaptorForUrl(ProjectConfigDto projectConfig, String url) throws ConfigException {
		final List<RepositoryAdaptorPlugin> repositoryPlugins = pluginManager.getPlugins(RepositoryAdaptorPlugin.class);
		
		for (RepositoryAdaptorPlugin plugin : repositoryPlugins) {
			final ProjectRepositoryConfigurator configurator = plugin.createProjectConfigurator(url);
			if (configurator != null) {
				projectConfig.setRepositoryAdaptorPluginId(plugin.getId());
				return configurator;
			}
		}
		
		throw new ConfigException("errors.url.unsupported", null);
	}
	
	private ProjectBuildConfigurator createBuildToolConfigurator(ProjectConfigDto projectConfig, File buildSpecFile) throws ConfigException {
		final List<BuildToolPlugin> buildToolPlugins = pluginManager.getPlugins(BuildToolPlugin.class);
		
		for (BuildToolPlugin plugin : buildToolPlugins) {
			final ProjectBuildConfigurator configurator = plugin.createProjectConfigurator(buildSpecFile);
			if (configurator != null) {
				projectConfig.setBuildToolPluginId(plugin.getId());
				return configurator;
			}
		}
		
		throw new ConfigException("errors.build.file.unsupported", null);
	}
	
	private File downloadBuildSpecFile(ProjectRepositoryConfigurator ra) throws RepositoryException, ConfigException {
		try {
			final File tmpFile = createTempFile();
			ra.download(tmpFile);
			return tmpFile;
		} catch (IOException e) {
			throw new ConfigException("errors.import.download", new Object[] {e.getMessage()}, e);
		}
	}
}
