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
package net.sourceforge.vulcan.mercurial;

import java.io.File;

import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.ProjectRepositoryConfigurator;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.PluginNotFoundException;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.RepositoryAdaptorPlugin;

public class MercurialPlugin implements RepositoryAdaptorPlugin, ConfigurablePlugin {
	public static String PLUGIN_ID = "net.sourceforge.vulcan.mercurial";
	public static String PLUGIN_NAME = "Mercurial";
	
	private MercurialConfig config = new MercurialConfig();
	private PluginManager pluginManager;
	
	public MercurialRepository createInstance(ProjectConfigDto projectConfig)	throws ConfigException {
		MercurialRepository instance = new MercurialRepository(projectConfig, config);
		instance.setChangeLogTemplatePath(getChangeLogTemplatePath().getAbsolutePath());
		return instance;
	}

	private File getChangeLogTemplatePath() {
		try {
			return new File(pluginManager.getPluginDirectory(getId()), "vulcan-xml.template");
		} catch (PluginNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public ProjectRepositoryConfigurator createProjectConfigurator(String url, String username, String password) throws ConfigException {
		return null;
	}
	
	public MercurialProjectConfig getDefaultConfig() {
		return new MercurialProjectConfig();
	}

	public MercurialConfig getConfiguration() {
		return config;
	}
	
	public void setConfiguration(PluginConfigDto bean) {
		config = (MercurialConfig) bean;
	}
	
	public void setPluginManager(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}
	
	public String getId() {
		return PLUGIN_ID;
	}

	public String getName() {
		return PLUGIN_NAME;
	}
}
