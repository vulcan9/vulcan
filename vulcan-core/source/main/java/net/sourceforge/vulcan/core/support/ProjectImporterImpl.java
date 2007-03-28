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

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.core.ProjectBuildConfigurator;
import net.sourceforge.vulcan.core.ProjectImporter;
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
	
	public void createProjectsForUrl(String url) throws ConfigException, StoreException, DuplicateNameException {
		final RepositoryAdaptor ra = createRepositoryAdaptorForUrl(url);
		
		final File buildSpecFile = downloadBuildSpecFile(ra);

		final ProjectBuildConfigurator buildConfigurator = createBuildToolConfigurator(buildSpecFile);
		
		final ProjectConfigDto projectConfig = ra.getProjectConfig();
		
		buildConfigurator.applyConfiguration(projectConfig);
		
		if (buildConfigurator.isStandaloneProject()) {
			ra.setNonRecursive();
		}
		
		try {
			final PluginConfigDto pluginConfig = pluginManager.getPluginConfigInfo(
					projectConfig.getRepositoryAdaptorPluginId());

			ra.updateGlobalConfig(pluginConfig);
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

	protected File createTempFile() throws IOException {
		return File.createTempFile("vulcan-new-project", ".tmp");
	}

	private RepositoryAdaptor createRepositoryAdaptorForUrl(String url) throws ConfigException {
		final List<RepositoryAdaptorPlugin> repositoryPlugins = pluginManager.getPlugins(RepositoryAdaptorPlugin.class);
		
		for (RepositoryAdaptorPlugin plugin : repositoryPlugins) {
			final RepositoryAdaptor instance = plugin.createInstanceForUrl(url);
			if (instance != null) {
				return instance;
			}
		}
		
		throw new ConfigException("errors.url.unsupported", null);
	}
	
	private ProjectBuildConfigurator createBuildToolConfigurator(File buildSpecFile) throws ConfigException {
		final List<BuildToolPlugin> buildToolPlugins = pluginManager.getPlugins(BuildToolPlugin.class);
		
		for (BuildToolPlugin plugin : buildToolPlugins) {
			final ProjectBuildConfigurator configurator = plugin.createProjectConfigurator(buildSpecFile);
			if (configurator != null) {
				return configurator;
			}
		}
		
		throw new ConfigException("errors.build.file.unsupported", null);
	}
	
	private File downloadBuildSpecFile(final RepositoryAdaptor ra) throws RepositoryException, ConfigException {
		try {
			final File tmpFile = createTempFile();
			ra.download(tmpFile);
			return tmpFile;
		} catch (IOException e) {
			throw new ConfigException("errors.import.download", new Object[] {e.getMessage()}, e);
		}
	}
}
