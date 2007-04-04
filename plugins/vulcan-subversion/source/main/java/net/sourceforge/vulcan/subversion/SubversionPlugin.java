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
package net.sourceforge.vulcan.subversion;

import java.util.Map;

import net.sourceforge.vulcan.ProjectRepositoryConfigurator;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.core.ProjectNameChangeListener;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.RepositoryAdaptorPlugin;
import net.sourceforge.vulcan.subversion.dto.SubversionConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionProjectConfigDto;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SubversionPlugin 
		implements RepositoryAdaptorPlugin, ProjectNameChangeListener,
			ConfigurablePlugin,	ApplicationContextAware {
	
	StateManager stateManager;
	EventHandler eventHandler;
	ApplicationContext ctx;
	SubversionConfigDto config;
	
	public SubversionPlugin() {
		setConfiguration(new SubversionConfigDto());
	}
	public RepositoryAdaptor createInstance(ProjectConfigDto projectConfig) throws ConfigException {
		return new SubversionRepositoryAdaptor(this.config, projectConfig, (SubversionProjectConfigDto)projectConfig.getRepositoryAdaptorConfig(), stateManager);
	}
	public ProjectRepositoryConfigurator createProjectConfigurator(String url) throws ConfigException {
		return SubversionProjectConfigurator.createInstance(url, this.config, this.ctx);
	}
	public RepositoryAdaptorConfigDto getDefaultConfig() {
		return new SubversionProjectConfigDto();
	}
	public void projectNameChanged(String oldName, String newName) {
		final Map<String, Long> counters = config.getWorkingCopyByteCounts();
		
		synchronized(counters) {
			if (counters.containsKey(oldName)) {
				counters.put(newName, counters.remove(oldName));
			}
		}
	}
	public String getId() {
		return SubversionConfigDto.PLUGIN_ID;
	}
	public String getName() {
		return SubversionConfigDto.PLUGIN_NAME;
	}
	public void setStateManager(StateManager stateManager) {
		this.stateManager = stateManager;
	}
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	public SubversionConfigDto getConfiguration() {
		return config;
	}
	public void setConfiguration(PluginConfigDto config) {
		this.config = (SubversionConfigDto) config;
	}
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.ctx = applicationContext;
	}
}
