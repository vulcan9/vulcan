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
package net.sourceforge.vulcan.web.struts.actions;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.event.AuditEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.logging.Log;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;


@SvnRevision(id="$Id$", url="$HeadURL$")
public final class FlushBuildQueueAction extends Action implements MessageSourceAware {
	private StateManager stateManager;
	private EventHandler eventHandler;
	private Log	auditLog;
	private MessageSource messageSource;
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		stateManager.flushBuildQueue();
		
		final AuditEvent event = BaseDispatchAction.createAuditEvent(this, request, "flush", "build queue", null, null);
		eventHandler.reportEvent(event);
		auditLog.info(messageSource.getMessage(event.getKey(), event.getArgs(), Locale.getDefault()));

		return mapping.findForward("dashboard");
	}

	public void setStateManager(StateManager stateManager) {
		this.stateManager = stateManager;
	}
	
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	
	public void setAuditLog(Log auditLog) {
		this.auditLog = auditLog;
	}
	
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
