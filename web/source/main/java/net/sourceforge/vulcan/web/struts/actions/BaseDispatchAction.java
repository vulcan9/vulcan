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

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.struts.forms.ConfigForm;
import net.sourceforge.vulcan.web.struts.forms.DispatchForm;

import org.apache.commons.logging.Log;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class BaseDispatchAction extends DispatchAction {
	private Log auditLog;
	private Set actionsToAudit;
	protected StateManager stateManager;
	
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

	public void setAuditLog(Log auditLog) {
		this.auditLog = auditLog;
	}
	
	public void setActionsToAudit(Set actionsToAudit) {
		this.actionsToAudit = actionsToAudit;
	}
	
	@Override
	protected final String getMethodName(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response,
			String parameter) throws Exception {
		
		if (form instanceof DispatchForm) {
			final DispatchForm dispatchForm = (DispatchForm) form;
			return dispatchForm.getAction();
		}
		return ConfigForm.translateAction(request.getParameter(parameter));
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
			final DispatchForm configForm = (DispatchForm) form;
			
			final String action = configForm.getAction();
			if (!actionsToAudit.contains(action)) {
				return;
			}
			
			final String oldName = configForm.getOriginalName();
			final String newName = configForm.getName();
			final String type = configForm.getTargetType();

			final String msg = createAuditMessage(request, action, type, oldName, newName);
			
			auditLog.info(msg);
		}

	}

	static String createAuditMessage(HttpServletRequest request, String action, String type,
			String oldName, String newName) {
		
		final StringBuilder msgBuf = new StringBuilder();
		msgBuf.append("User: ");
		
		if (request.getUserPrincipal() != null) {
			msgBuf.append(request.getUserPrincipal().getName());
		} else {
			msgBuf.append("(anonymous)");
		}
		
		msgBuf.append("; Host: ");
		msgBuf.append(request.getRemoteHost());
		msgBuf.append("; Action: ");
		msgBuf.append(action);
		msgBuf.append("; Type: ");
		msgBuf.append(type);
		
		if (oldName != null || newName != null) {
			msgBuf.append("; Name: ");
			
			if (isBlank(oldName) || oldName.equals(newName)) {
				msgBuf.append(newName);
			} else {
				msgBuf.append(oldName);
				msgBuf.append(" (renamed to ");
				msgBuf.append(newName);
				msgBuf.append(")");
			}
		}
		
		return msgBuf.toString();
	}
}
