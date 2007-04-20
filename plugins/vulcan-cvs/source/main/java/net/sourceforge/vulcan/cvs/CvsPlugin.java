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
package net.sourceforge.vulcan.cvs;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import net.sourceforge.vulcan.core.ProjectNameChangeListener;
import net.sourceforge.vulcan.cvs.dto.CvsConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsProjectConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsRepositoryProfileDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.RepositoryAdaptorPlugin;
import net.sourceforge.vulcan.integration.support.PluginSupport;

public class CvsPlugin extends PluginSupport
		implements RepositoryAdaptorPlugin,
			ProjectNameChangeListener,
			ConfigurablePlugin,
			ApplicationContextAware {
	
	private CvsConfigDto globalConfig;
	private ApplicationContext applicationContext;
	
	public CvsPlugin() {
		setConfiguration(new CvsConfigDto());
	}
	public CvsRepositoryAdaptor createInstance(ProjectConfigDto projectConfig) throws ConfigException {
		CvsProjectConfigDto cvsProjectConfig = (CvsProjectConfigDto) projectConfig.getRepositoryAdaptorConfig();
		final CvsRepositoryProfileDto env = getSelectedEnvironment(globalConfig.getProfiles(), cvsProjectConfig.getRepositoryProfile(),
				"cvs.errors.profile.not.found");
		
		final CvsRepositoryAdaptor cvsRepositoryAdaptor = new CvsRepositoryAdaptor(globalConfig, env, cvsProjectConfig, projectConfig.getName());
		
		return cvsRepositoryAdaptor;
	}
	public CvsProjectConfigurator createProjectConfigurator(String url, String username, String password) throws ConfigException {
		return CvsProjectConfigurator.createInstance(applicationContext, globalConfig, url, username, password);
	}
	public RepositoryAdaptorConfigDto getDefaultConfig() {
		return new CvsProjectConfigDto();
	}
	public void projectNameChanged(String oldName, String newName) {
		final Map<String, Long> counters = globalConfig.getWorkingCopyByteCounts();
		synchronized(counters) {
			if (counters.containsKey(oldName)) {
				counters.put(newName, counters.remove(oldName));
			}
		}
	}
	public String getId() {
		return CvsConfigDto.PLUGIN_ID;
	}
	public String getName() {
		return CvsConfigDto.PLUGIN_NAME;
	}
	public CvsConfigDto getConfiguration() {
		return globalConfig;
	}
	public void setConfiguration(PluginConfigDto config) {
		this.globalConfig = (CvsConfigDto) config;
	}
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
