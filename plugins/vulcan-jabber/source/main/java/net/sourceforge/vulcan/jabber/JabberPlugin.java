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
package net.sourceforge.vulcan.jabber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.core.ProjectNameChangeListener;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.event.BrokenBuildClaimedEvent;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.BuildStartingEvent;
import net.sourceforge.vulcan.integration.BuildManagerObserverPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.jabber.JabberPluginConfig.EventsToMonitor;
import net.sourceforge.vulcan.jabber.JabberPluginConfig.ProjectsToMonitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.MessageSource;

public class JabberPlugin implements BuildManagerObserverPlugin, ConfigurablePlugin, ProjectNameChangeListener {
	final static Log LOG = LogFactory.getLog(JabberPlugin.class);
	
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.jabber";
	public static final String PLUGIN_NAME = "Jabber Instant Messaging";
	
	private final Map<String, JabberBuildStatusListener> instances = new HashMap<String, JabberBuildStatusListener>();
	
	JabberPluginConfig config = new JabberPluginConfig();
	
	private MessageSource messageSource;
	private JabberClient client;
	private JabberResponder responder;
	private ScreenNameMapper screenNameResolver;
	
	public void setClient(JabberClient client) {
		this.client = client;
	}
	
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
	
	public void setResponder(JabberResponder responder) {
		this.responder = responder;
	}
	
	public JabberPluginConfig getConfiguration() {
		return config;
	}
	
	public void setConfiguration(PluginConfigDto bean) {
		config = (JabberPluginConfig) bean;
		
		client.refreshConnection(config.getServer(), config.getPort(), config.getServiceName(), config.getUsername(), config.getPassword());
		responder.setConfiguration(config);
		screenNameResolver = config.createScreenNameMapper();
	}
	
	public void onBuildStarting(BuildStartingEvent event) {
		final ProjectStatusDto status = event.getStatus();

		if (!isProjectMonitored(status.getName())) {
			return;
		}
		
		client.refreshConnection(config.getServer(), config.getPort(), config.getServiceName(), config.getUsername(), config.getPassword());
		
		if (!client.isConnected()) {
			return;
		}
		
		final BuildManager mgr = (BuildManager)event.getSource();
		
		final ProjectBuilder projectBuilder = mgr.getProjectBuilder(status.getName());
		
		final JabberBuildStatusListener listener = new JabberBuildStatusListener(projectBuilder, client, responder, screenNameResolver, config, status);
		
		final List<ProjectStatusDto> previousFailures = getPreviousBuildFailures(mgr, status);

		listener.setPreviousFailures(previousFailures);
		
		listener.attach();
		
		addBuildListener(status.getName(), listener);
	}

	public void onBuildCompleted(BuildCompletedEvent event) {
		final ProjectStatusDto outcome = event.getStatus();
		final String projectName = outcome.getName();
		
		final JabberBuildStatusListener listener;

		synchronized (instances) {
			listener = instances.remove(projectName);
		}

		if (listener == null) {
			return;
		}

		if (listener.isAttached()) {
			final Status result = outcome.getStatus();
			
			if (result == Status.ERROR || result == Status.FAIL) {
				final BuildMessageDto message = new BuildMessageDto();
				message.setMessage(messageSource.getMessage(outcome.getMessageKey(), outcome.getMessageArgs(), null));
				
				listener.onBuildMessageLogged(EventsToMonitor.Errors, null, message, "");
			}
			
			listener.detach();	
		}
		
	}

	public void onBrokenBuildClaimed(BrokenBuildClaimedEvent event) {
		final ProjectStatusDto status = event.getStatus();
		responder.notifyBuildClaimed(status.getName(), status.getBuildNumber(), status.getBrokenBy());
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

	public ScreenNameMapper getScreenNameResolver() {
		return screenNameResolver;
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
	
	JabberBuildStatusListener getBuildListener(String name) {
		synchronized (instances) {
			return instances.get(name);
			
		}
	}

	private List<ProjectStatusDto> getPreviousBuildFailures(final BuildManager mgr, final ProjectStatusDto current) {
		final ProjectStatusDto previousBuild = mgr.getLatestStatus(current.getName());
		
		if (previousBuild == null || previousBuild.getStatus() != Status.FAIL) {
			return Collections.emptyList();
		}
		
		final List<ProjectStatusDto> previousFailures = new ArrayList<ProjectStatusDto>();
		
		Integer lastGoodBuildNumber = previousBuild.getLastGoodBuildNumber();
		if (lastGoodBuildNumber == null) {
			lastGoodBuildNumber = previousBuild.getBuildNumber();
		}
		
		for (int i = lastGoodBuildNumber+1; i<previousBuild.getBuildNumber(); i++) {
			final ProjectStatusDto failure = mgr.getStatusByBuildNumber(current.getName(), i);
			if (failure != null) {
				previousFailures.add(failure);
			}
		}
		
		previousFailures.add(previousBuild);
		return previousFailures;
	}
}
