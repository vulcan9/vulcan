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
package net.sourceforge.vulcan.spring;

import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


public class SpringEventHandler
		implements EventHandler, ApplicationContextAware {

	private ApplicationContext appCtx;

	public void reportEvent(Event event) {
		if (event instanceof ErrorEvent) {
			logErrorEvent((ErrorEvent)event);
		}
		appCtx.publishEvent(new EventBridge(event));
	}
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.appCtx = applicationContext;
	}
	private void logErrorEvent(ErrorEvent event) {
		Log log = LogFactory.getLog(event.getSource().getClass());
		
		log.error(appCtx.getMessage(event.getKey(), event.getArgs(), null), event.getError());
	}
}
