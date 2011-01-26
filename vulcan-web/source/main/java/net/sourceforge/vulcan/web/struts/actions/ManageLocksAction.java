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
package net.sourceforge.vulcan.web.struts.actions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.web.struts.forms.MultipleProjectConfigForm;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

public final class ManageLocksAction extends BaseDispatchAction {
	private BuildManager buildManager;
	
	public ActionForward lock(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final MultipleProjectConfigForm configForm = (MultipleProjectConfigForm) form;

		if (configForm.getProjectNames() == null || configForm.getProjectNames().length == 0) {
			saveError(request, "projectNames",
					new ActionMessage("errors.required"));
			return mapping.getInputForward();
		}

		try {
			if (buildManager.isBuildingOrInQueue(configForm.getProjectNames())) {
				request.setAttribute("restResponseCode", HttpServletResponse.SC_CONFLICT);
				saveError(request, ActionMessages.GLOBAL_MESSAGE,
						new ActionMessage("errors.cannot.lock.project"));
				return mapping.findForward("failure");
			}
	
			String message = configForm.getMessage();
			if (StringUtils.isBlank(message)) {
				message = formatMessage("messages.project.locked.by.user", getUsername(request), new Date());
			}
			
			final long lockId = stateManager.lockProjects(message, configForm.getProjectNames());
			request.setAttribute("lockId", lockId);
			
			saveSuccessMessage(request);
			return mapping.findForward("success");
		} catch (NoSuchProjectException e) {
			saveError(request, "projectNames",
					new ActionMessage("errors.no.such.project", e.getMessage()));
			return mapping.getInputForward();
		}
	}
	
	public ActionForward unlock(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		final String[] idStrings = request.getParameterValues("lockId");
		if (idStrings==null || idStrings.length == 0) {
			saveError(request, "lockId",
					new ActionMessage("errors.required"));
			return mapping.getInputForward();
		}
		
		final List<Long> ids = new ArrayList<Long>(idStrings.length);
		
		try {
			for (String str : idStrings) {
				ids.add(Long.valueOf(str));
			}
		} catch (NumberFormatException e) {
			saveError(request, "lockId",
					new ActionMessage("errors.integer"));
			return mapping.getInputForward();
		}
		
		stateManager.removeProjectLock(ids.toArray(new Long[ids.size()]));
		
		saveSuccessMessage(request);
		return mapping.findForward("success");
	}

	public ActionForward clear(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final MultipleProjectConfigForm configForm = (MultipleProjectConfigForm) form;
		
		try {
			stateManager.clearProjectLocks(configForm.getProjectNames());
			
			saveSuccessMessage(request);
			return mapping.findForward("success");
		} catch (NoSuchProjectException e) {
			saveError(request, "projectNames",
					new ActionMessage("errors.no.such.project", e.getMessage()));
			return mapping.getInputForward();
		}
	}
	
	@Override
	protected ActionForward unspecified(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		saveError(request, "action", new ActionMessage("errors.required"));
		return mapping.getInputForward();
	}
	
	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
}
