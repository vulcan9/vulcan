/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2010 Chris Eldredge
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
package net.sourceforge.vulcan.mercurial;

import net.sourceforge.vulcan.ProjectRepositoryConfigurator;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.integration.RepositoryAdaptorPlugin;

public class MercurialPlugin implements RepositoryAdaptorPlugin {
	public static String PLUGIN_ID = "net.sourceforge.vulcan.mercurial";
	public static String PLUGIN_NAME = "Mercurial";
	
	public RepositoryAdaptor createInstance(ProjectConfigDto projectConfig)	throws ConfigException {
		return new MercurialRepository(projectConfig);
	}

	public ProjectRepositoryConfigurator createProjectConfigurator(String url, String username, String password) throws ConfigException {
		return null;
	}
	
	public RepositoryAdaptorConfigDto getDefaultConfig() {
		return new MercurialProjectConfig();
	}

	public String getId() {
		return PLUGIN_ID;
	}

	public String getName() {
		return PLUGIN_NAME;
	}
}
