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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.event.AuditEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.struts.forms.ConfigForm;
import net.sourceforge.vulcan.web.struts.forms.DispatchForm;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class BaseDispatchAction extends DispatchAction implements MessageSourceAware {
	private Log auditLog;
	private Set<String> actionsToAudit;
	private EventHandler eventHandler;
	protected StateManager stateManager;
	private MessageSource messageSource;
	
	@Override
	public final ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		try {
			final ActionForward forward = super.execute(mapping, form, request, response);
			
			if (Boolean.TRUE.equals(request.getAttribute("success"))) {
				audit(form, request);				
			}
			
			return forward;
		} catch (DuplicateNameException e) {
			saveError(request, "config.name", new ActionMessage("errors.unique", new String[] {e.getName()}));
			return mapping.getInputForward();
		} catch (StoreException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE, new ActionMessage("messages.save.failure",
							new Object[] {e.getMessage()}));
			return mapping.findForward("failure");
		}

	}

	public void setStateManager(StateManager stateManager) {
		this.stateManager = stateManager;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
	
	public void setAuditLog(Log auditLog) {
		this.auditLog = auditLog;
	}
	
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	
	public void setActionsToAudit(Set<String> actionsToAudit) {
		this.actionsToAudit = actionsToAudit;
	}
	
	@Override
	protected final String getMethodName(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response,
			String parameter) throws Exception {
		
		String name;
		
		if (form instanceof DispatchForm) {
			final DispatchForm dispatchForm = (DispatchForm) form;
			name = dispatchForm.getAction();
		} else {
			name = ConfigForm.translateAction(request.getParameter(parameter));
		}
		
		if (StringUtils.isBlank(name)) {
			return null;
		}
		
		return name;
	}
	
	protected final String formatMessage(String key, Object... args) {
		return messageSource.getMessage(key, args, Locale.getDefault());
	}
	
	protected final static void saveError(HttpServletRequest request, String propertyName, ActionMessage message) {
		addMessage(request, Globals.ERROR_KEY, propertyName, message);
	}
	protected final static void saveSuccessMessage(HttpServletRequest request) {
		request.setAttribute("success", Boolean.TRUE);
		addMessage(request, Globals.MESSAGE_KEY, ActionMessages.GLOBAL_MESSAGE,
				new ActionMessage("messages.save.success"));
	}

	protected final static void addMessage(HttpServletRequest request, String messagesKey, String propertyName, ActionMessage message) {
		ActionMessages msgs = (ActionMessages) request.getAttribute(messagesKey);
		if (msgs == null) {
			msgs = new ActionMessages();
		}
		msgs.add(propertyName, message);
		request.setAttribute(messagesKey, msgs);
	}
	
	protected final void audit(ActionForm form, HttpServletRequest request) {
		if (auditLog == null || auditLog.isInfoEnabled() == false) {
			return;
		}
		
		if (form instanceof DispatchForm) {
			final DispatchForm dispatchForm = (DispatchForm) form;
			
			final String action = dispatchForm.getAction();
			if (!actionsToAudit.contains(action)) {
				return;
			}
			
			final String oldName = dispatchForm.getOriginalName();
			final String newName = dispatchForm.getName();
			final String type = dispatchForm.getTargetType();

			//TODO: move this method onto DispatchForm
			final AuditEvent event = createAuditEvent(this, request, action, type, oldName, newName);
			final String message = messageSource.getMessage(event.getKey(), event.getArgs(), Locale.getDefault());

			eventHandler.reportEvent(event);
			auditLog.info(message);
		}
	}

	static AuditEvent createAuditEvent(Object source, HttpServletRequest request, String action,
			String type, String oldName, String newName) {
		
		final String user = getUsername(request);
		final String host = request.getRemoteHost();
		String name = null;
		String oldName1 = null;
		String messageKey = "audit.without.name";
		
		if (oldName != null || newName != null) {
			name = newName;
			messageKey = "audit.with.name";
			
			if (!isBlank(oldName) && !oldName.equals(newName)) {
				oldName1 = oldName;
				messageKey = "audit.with.rename";
			}
		}
		
		return new AuditEvent(source, messageKey, user, host, action, type, name, oldName1);
	}
	
	protected static String getUsername(HttpServletRequest request) {
		String user = request.getRemoteUser();
		
		if (user == null) {
			user = "(anonymous)";
		}
		
		return user;
	}
}
