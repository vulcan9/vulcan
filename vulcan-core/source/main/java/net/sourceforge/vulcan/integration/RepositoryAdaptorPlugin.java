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
package net.sourceforge.vulcan.integration;

import net.sourceforge.vulcan.ProjectRepositoryConfigurator;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;

public interface RepositoryAdaptorPlugin extends Plugin {
	/**
	 * @param projectConfig The top level configuration for the project
	 * @return An instance of RepositoryAdaptor configured with the passed in data.
	 * @throws ConfigException
	 */
	RepositoryAdaptor createInstance(ProjectConfigDto projectConfig) throws ConfigException;
	
	/**
	 * @return an instance of ProjectRepositoryConfigurator if the url is
	 * supported by this plugin; null otherwise.
	 * @throws ConfigException If the url is supported but an error occurs
	 * while creating the instance.
	 */
	ProjectRepositoryConfigurator createProjectConfigurator(String url, String username, String password) throws ConfigException;

	RepositoryAdaptorConfigDto getDefaultConfig();

	
}
