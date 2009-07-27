/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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
package net.sourceforge.vulcan.jabber;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.core.ProjectNameChangeListener;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.BuildStartingEvent;
import net.sourceforge.vulcan.integration.BuildManagerObserverPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.jabber.JabberPluginConfig.ProjectsToMonitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JabberPlugin implements BuildManagerObserverPlugin, ConfigurablePlugin, ProjectNameChangeListener {
	final static Log LOG = LogFactory.getLog(JabberPlugin.class);
	
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.jabber";
	public static final String PLUGIN_NAME = "Jabber Instant Messaging";
	
	private final Map<String, JabberBuildStatusListener> instances = new HashMap<String, JabberBuildStatusListener>();
	
	JabberPluginConfig config = new JabberPluginConfig();
	
	private JabberClient client;
	
	public void setClient(JabberClient client) {
		this.client = client;
	}

	public JabberPluginConfig getConfiguration() {
		return config;
	}
	
	public void setConfiguration(PluginConfigDto bean) {
		config = (JabberPluginConfig) bean;
		client.refreshConnection(config.getServer(), config.getPort(), config.getServiceName(), config.getUsername(), config.getPassword());
	}
	
	public void onBuildStarting(BuildStartingEvent event) {
		final ProjectStatusDto status = event.getStatus();

		if (!isProjectMonitored(status.getName())) {
			return;
		}
		
		client.refreshConnection(config.getServer(), config.getPort(), config.getServiceName(), config.getUsername(), config.getPassword());
		
		final BuildManager mgr = (BuildManager)event.getSource();
		
		final ScreenNameMapper screenNameResolver;
		
		switch(config.getScreenNameMapper()) {
			case Dictionay:
				screenNameResolver = new DictionaryScreenNameMapper((DictionaryScreenNameMapperConfig) config.getScreenNameMapperConfig());
				break;
			case Jdbc:
				screenNameResolver = new JdbcScreenNameMapper((JdbcScreenNameMapperConfig) config.getScreenNameMapperConfig());
				break;
			default:
				screenNameResolver = new RegexScreenNameMapper((RegexScreenNameMapperConfig) config.getScreenNameMapperConfig());
				break;
		}
		
		final ProjectBuilder projectBuilder = mgr.getProjectBuilder(status.getName());
		final JabberBuildStatusListener listener = new JabberBuildStatusListener(client, projectBuilder, screenNameResolver, config, status);
		
		listener.attach();
		
		addBuildListener(status.getName(), listener);
	}

	public void onBuildCompleted(BuildCompletedEvent event) {
		final String projectName = event.getStatus().getName();
		
		final JabberBuildStatusListener listener;

		synchronized (instances) {
			listener = instances.remove(projectName);
		}

		if (listener == null) {
			return;
		}

		listener.detach();
	}

	public void projectNameChanged(String oldName, String newName) {
		final String[] projects = config.getSelectedProjects();
		
		for (int i = 0; i < projects.length; i++) {
			if (oldName.equals(projects[i])) {
				projects[i] = newName;
			}
		}
	}
	
	public String getId() {
		return PLUGIN_ID;
	}

	public String getName() {
		return PLUGIN_NAME;
	}

	private boolean isProjectMonitored(String projectName) {
		if (config.getProjectsToMonitor() == ProjectsToMonitor.All) {
			return true;
		}
		
		for (String s : config.getSelectedProjects()) {
			if (projectName.equals(s)) {
				return true;
			}
		}
		
		return false;
	}

	void addBuildListener(String name, JabberBuildStatusListener listener) {
		synchronized (instances) {
			instances.put(name, listener);
		}
	}
}
