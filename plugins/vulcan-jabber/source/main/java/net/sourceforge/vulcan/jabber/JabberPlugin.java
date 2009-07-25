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
import net.sourceforge.vulcan.core.BuildStatusListener;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.BuildStartingEvent;
import net.sourceforge.vulcan.integration.BuildManagerObserverPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.jabber.JabberPluginConfig.ProjectsToMonitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JabberPlugin implements BuildManagerObserverPlugin, ConfigurablePlugin {
	final static Log LOG = LogFactory.getLog(JabberPlugin.class);
	
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.jabber";
	public static final String PLUGIN_NAME = "Jabber Instant Messaging";
	
	private final Map<String, BuildStatusListener> instances = new HashMap<String, BuildStatusListener>();
	
	JabberPluginConfig config = new JabberPluginConfig();
	
	private JabberClient client;
	private ScreenNameResolver screenNameResolver;
	
	public void setClient(JabberClient client) {
		this.client = client;
	}

	public void setScreenNameResolver(ScreenNameResolver screenNameResolver) {
		this.screenNameResolver = screenNameResolver;
	}

	public JabberPluginConfig getConfiguration() {
		return config;
	}
	
	public void setConfiguration(PluginConfigDto bean) {
		config = (JabberPluginConfig) bean;
		client.refreshConnection(config.getServer(), config.getPort(), config.getUsername(), config.getPassword());
	}
	
	public void onBuildStarting(BuildStartingEvent event) {
		final ProjectStatusDto status = event.getStatus();

		if (!isProjectMonitored(status.getName())) {
			return;
		}
		
		client.refreshConnection(config.getServer(), config.getPort(), config.getUsername(), config.getPassword());
		
		final BuildManager mgr = (BuildManager)event.getSource();
		final JabberBuildStatusListener listener = new JabberBuildStatusListener(client, screenNameResolver, status);
		
		listener.addRecipients(config.getRecipients());
		
		mgr.getProjectBuilder(status.getName()).addBuildStatusListener(listener);
		
		addBuildListener(status.getName(), listener);
	}

	public void onBuildCompleted(BuildCompletedEvent event) {
		final String projectName = event.getStatus().getName();
		
		final BuildStatusListener listener;

		synchronized (instances) {
			listener = instances.remove(projectName);
		}

		if (listener == null) {
			return;
		}
		
		final BuildManager mgr = (BuildManager)event.getSource();

		mgr.getProjectBuilder(projectName).removeBuildStatusListener(listener);
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

	void addBuildListener(String name, BuildStatusListener listener) {
		synchronized (instances) {
			instances.put(name, listener);
		}
	}
}
