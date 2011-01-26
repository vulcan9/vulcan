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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import net.sourceforge.vulcan.core.BuildManager;

public class ClaimBrokenBuildAction extends BaseDispatchAction {
	private BuildManager buildManager;
	
	public ActionForward claim(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		boolean valid = true;
		final String projectName = request.getParameter("projectName");
		final String buildNumberString = request.getParameter("buildNumber");
		Integer buildNumber = -1;
		
		if (StringUtils.isBlank(projectName)) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.required.with.name", "projectName"));
			valid = false;
		}
		if (StringUtils.isBlank(buildNumberString)) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.required.with.name", "buildNumber"));
			valid = false;
		} else {
			try {
				buildNumber = Integer.valueOf(buildNumberString);	
			} catch (NumberFormatException e) {
				saveError(request, ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.integer"));
				valid = false;
			}
		}
		
		if (!valid) {
			return mapping.findForward("error");
		}
		
		boolean claimed;
		try {
			claimed = buildManager.claimBrokenBuild(projectName, buildNumber, request.getUserPrincipal().getName());	
		} catch (IllegalArgumentException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.status.not.available.by.build.number", projectName, buildNumber));
			return mapping.findForward("error");
		}
		
		if (claimed) {
			saveSuccessMessage(request);
		} else {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.already.claimed"));
			return mapping.findForward("error");
		}
		
		final String referrer = request.getHeader("referer");
		if (StringUtils.isNotBlank(referrer)) {
			response.sendRedirect(referrer);
			return null;
		}
		
		return mapping.findForward("dashboard");
	}
	
	public BuildManager getBuildManager() {
		return buildManager;
	}
	
	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
}
