/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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
package net.sourceforge.vulcan;

import java.io.File;
import java.io.IOException;

import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.RepositoryException;

/**
 * Used by ProjectImporter to configure ProjectConfigDto and related plugin configuration.
 */
public interface ProjectRepositoryConfigurator {
	/**
	 * This method should obtain the contents of the resource
	 * pointed to by the url used to construct this instance, and put them into the temp file
	 * provided.
	 */
	void download(File target) throws RepositoryException, IOException;

	/**
	 * Take settings and apply them to projectConfig.  Any settings can be applied, but
	 * at a minimum, the following should be defined:
	 * <ul>
	 * 	<li>projectConfig.repositoryAdaptorPluginId</li>
	 *  <li>projectConfig.repositoryAdaptorConfig</li>
	 * </ul>
	 * @param projectConfig The object onto which settings should be applied.
	 * @param projectBasedirUrl The URL to the directory which should be checked out.
	 */
	void applyConfiguration(ProjectConfigDto projectConfig, String projectBasedirUrl);
	
	/**
	 * If the implementation supports non-recursive mode (where folders nested under the
	 * folder pointed to by this project are not checked out), this setting should be applied.
	 */
	void setNonRecursive();

	/**
	 * If the configuration for the created project requires changes to the global
	 * configuration for this plugin, those changes should be applied here.
	 * @return true is configuration was modified, false if not changes were made.
	 */
	boolean updateGlobalConfig(PluginConfigDto globalRaConfig);
}
