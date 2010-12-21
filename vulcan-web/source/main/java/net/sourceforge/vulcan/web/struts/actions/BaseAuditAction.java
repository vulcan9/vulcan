/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2008 Chris Eldredge
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
package net.sourceforge.vulcan.web.struts.actions;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.event.EventHandler;

import org.apache.commons.logging.Log;
import org.apache.struts.action.Action;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;


public abstract class BaseAuditAction extends Action implements MessageSourceAware {
	protected StateManager stateManager;
	protected Log auditLog;
	protected EventHandler eventHandler;
	protected MessageSource messageSource;
	
	public void setStateManager(StateManager manager) {
		this.stateManager = manager;
	}
	public void setAuditLog(Log auditLog) {
		this.auditLog = auditLog;
	}
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
