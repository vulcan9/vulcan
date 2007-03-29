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
package net.sourceforge.vulcan.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@SvnRevision(id="$Id$", url="$HeadURL$")
public final class VulcanContextListener implements ServletContextListener {
	StateManager stateManager;
	
	public void contextInitialized(ServletContextEvent event) {
		final ServletContext context = event.getServletContext();
		
		final WebApplicationContext wac =
			WebApplicationContextUtils
				.getRequiredWebApplicationContext(context);
		
		stateManager = (StateManager) wac.getBean(Keys.STATE_MANAGER, StateManager.class);	
		
		context.setAttribute(Keys.STATE_MANAGER, stateManager);
		context.setAttribute(Keys.EVENT_POOL, wac.getBean(Keys.EVENT_POOL));
		
		try {
			stateManager.start();
		} catch (Exception e) {
			final EventHandler eventHandler = (EventHandler) wac.getBean(Keys.EVENT_HANDLER, EventHandler.class);
			
			eventHandler.reportEvent(new ErrorEvent(this, "errors.load.failure", new String[] {e.getMessage()}, e));
		}
	}

	public void contextDestroyed(ServletContextEvent event) {
		final ServletContext context = event.getServletContext();
		context.removeAttribute(Keys.STATE_MANAGER);
		context.removeAttribute(Keys.EVENT_POOL);
		
		try {
			stateManager.shutdown();
		} catch (Exception e) {
			context.log("Error during shutdown of stateManager", e);
		}
	}
}
