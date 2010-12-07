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
package net.sourceforge.vulcan.filesystem;

import net.sourceforge.vulcan.ProjectRepositoryConfigurator;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.filesystem.dto.FileSystemProjectConfigDto;
import net.sourceforge.vulcan.integration.RepositoryAdaptorPlugin;

public class FileSystemPlugin 
		implements RepositoryAdaptorPlugin {
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.filesystem";
	public static final String PLUGIN_NAME = "File System";

	EventHandler eventHandler;
	
	public RepositoryAdaptor createInstance(ProjectConfigDto projectConfig) throws RepositoryException {
		return new FileSystemRepositoryAdaptor(projectConfig, (FileSystemProjectConfigDto)projectConfig.getRepositoryAdaptorConfig());
	}

	public ProjectRepositoryConfigurator createProjectConfigurator(String url, String username, String password) throws ConfigException {
		return null;
	}
	
	public RepositoryAdaptorConfigDto getDefaultConfig() {
		return new FileSystemProjectConfigDto();
	}
	
	public String getId() {
		return PLUGIN_ID;
	}
	public String getName() {
		return PLUGIN_NAME;
	}
	public EventHandler getEventHandler() {
		return eventHandler;
	}
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
}
