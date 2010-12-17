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
package net.sourceforge.vulcan.spring;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.vulcan.event.BrokenBuildClaimedEvent;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.BuildStartingEvent;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.integration.BuildManagerObserverPlugin;
import net.sourceforge.vulcan.integration.MetricsPlugin;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;


public class BuildEventPluginPublisher implements ApplicationListener {
	List<BuildManagerObserverPlugin> observers = new ArrayList<BuildManagerObserverPlugin>();
	
	public void add(BuildManagerObserverPlugin plugin) {
		final boolean isMetrics = plugin.getClass().getAnnotation(MetricsPlugin.class) != null;
		
		synchronized(observers) {
			if (isMetrics) {
				observers.add(0, plugin);
			} else {
				observers.add(plugin);
			}
		}
	}
	public boolean remove(BuildManagerObserverPlugin plugin) {
		synchronized(observers) {
			return observers.remove(plugin);
		}
	}
	public void onApplicationEvent(ApplicationEvent springEvent) {
		if (!(springEvent instanceof EventBridge)) {
			return;
		}
		final Event event = ((EventBridge)springEvent).getEvent();
		
		final BuildStartingEvent buildStartingEvent;
		final BuildCompletedEvent buildCompletedEvent;
		final BrokenBuildClaimedEvent brokenBuildClaimedEvent;
		
		if (event instanceof BuildStartingEvent) {
			buildStartingEvent = (BuildStartingEvent) event;
			buildCompletedEvent = null;
			brokenBuildClaimedEvent = null;
		} else if (event instanceof BuildCompletedEvent) {
			buildCompletedEvent = (BuildCompletedEvent) event;
			buildStartingEvent = null;
			brokenBuildClaimedEvent = null;
		} else if (event instanceof BrokenBuildClaimedEvent) {
			brokenBuildClaimedEvent = (BrokenBuildClaimedEvent) event;
			buildStartingEvent = null;
			buildCompletedEvent = null;
		} else {
			return;
		}
		
		final List<BuildManagerObserverPlugin> copy;
		synchronized(observers) {
			copy = new ArrayList<BuildManagerObserverPlugin>(observers);
		}
		
		for (BuildManagerObserverPlugin plugin : copy) {
			if (buildStartingEvent != null) {
				plugin.onBuildStarting(buildStartingEvent);
			} else if (buildCompletedEvent != null) {
				plugin.onBuildCompleted(buildCompletedEvent);
			} else {
				plugin.onBrokenBuildClaimed(brokenBuildClaimedEvent);
			}
		}
	}
}
