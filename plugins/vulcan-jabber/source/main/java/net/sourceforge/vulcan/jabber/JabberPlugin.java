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
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.BuildStartingEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.integration.BuildManagerObserverPlugin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JabberPlugin implements BuildManagerObserverPlugin {
	final static Log LOG = LogFactory.getLog(JabberPlugin.class);
	
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.jabber";
	public static final String PLUGIN_NAME = "Jabber Instant Messaging";
	
	private EventHandler eventHandler;
	
	private final Map<String, JabberBuildStatusListener> instances = new HashMap<String, JabberBuildStatusListener>();
	
	public void onBuildStarting(BuildStartingEvent event) {
		final ProjectStatusDto status = event.getStatus();
		LOG.info("Project " + status.getName() + " starting build");
		
		final BuildManager mgr = (BuildManager)event.getSource();
		final JabberBuildStatusListener listener = new JabberBuildStatusListener(status);
		
		mgr.getProjectBuilder(status.getName()).addBuildStatusListener(listener);
		synchronized (instances) {
			instances.put(status.getName(), listener);
		}
	}

	public void onBuildCompleted(BuildCompletedEvent event) {
		final BuildManager mgr = (BuildManager)event.getSource();
		final ProjectStatusDto status = event.getStatus();
		final String projectName = status.getName();
		
		final JabberBuildStatusListener listener;
		
		synchronized (instances) {
			listener = instances.remove(projectName);
		}
		
		mgr.getProjectBuilder(projectName).removeBuildStatusListener(listener);
		LOG.info("Project " + projectName + " finished build with result " + status.getStatus().name());
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
