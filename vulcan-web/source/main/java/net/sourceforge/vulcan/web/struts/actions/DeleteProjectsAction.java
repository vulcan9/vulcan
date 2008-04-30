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

import static net.sourceforge.vulcan.web.struts.actions.BaseDispatchAction.saveSuccessMessage;

import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.exception.ProjectNeedsDependencyException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.struts.forms.MultipleProjectConfigForm;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

@SvnRevision(id="$Id$", url="$HeadURL$")
public final class DeleteProjectsAction extends Action {
	private StateManager stateManager;
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final MultipleProjectConfigForm configForm = (MultipleProjectConfigForm) form;

		try {
			// remove duplicates
			final String[] set = new HashSet<String>(Arrays.asList(configForm.getProjectNames())).toArray(new String[0]);
			// sort for unit testing purposes
			Arrays.sort(set);
			
			stateManager.deleteProjectConfig(set);
			saveSuccessMessage(request);
			return mapping.findForward("projectList");
		} catch (ProjectNeedsDependencyException e) {
			request.setAttribute("projectsWithDependents", e.getProjectsToDelete());
			request.setAttribute("dependentProjects", e.getDependantProjects());
			return mapping.getInputForward();
		}
	}

	public void setStateManager(StateManager stateManager) {
		this.stateManager = stateManager;
	}
}
