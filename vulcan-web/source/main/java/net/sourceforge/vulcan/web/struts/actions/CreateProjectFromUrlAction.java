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

import static net.sourceforge.vulcan.web.struts.actions.BaseDispatchAction.saveError;
import static net.sourceforge.vulcan.web.struts.actions.BaseDispatchAction.saveSuccessMessage;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.core.ProjectImporter;
import net.sourceforge.vulcan.dto.ProjectImportStatusDto;
import net.sourceforge.vulcan.exception.AuthenticationRequiredRepositoryException;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.web.struts.forms.ProjectImportForm;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;


public final class CreateProjectFromUrlAction extends Action {
	private ProjectImporter projectImporter;
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final ProjectImportForm importForm = (ProjectImportForm) form;
		
		final Set<String> labels = new HashSet<String>(Arrays.asList(importForm.getLabels()));
		
		if (isNotBlank(importForm.getNewLabel())) {
			labels.add(importForm.getNewLabel());
		}
		
		final ProjectImportStatusDto projectImportStatusDto = new ProjectImportStatusDto();
		request.getSession().setAttribute("projectImportStatus", projectImportStatusDto);
		
		try {
			projectImporter.createProjectsForUrl(
					importForm.getUrl(),
					importForm.getUsername(),
					importForm.getPassword(),
					importForm.isCreateSubprojects(),
					importForm.parseNameCollisionResolutionMode(),
					importForm.getSchedulerNames(),
					labels,
					projectImportStatusDto);
			
			saveSuccessMessage(request);
		} catch (DuplicateNameException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.duplicate.project.name", e.getName()));

			return mapping.getInputForward();
		} catch (AuthenticationRequiredRepositoryException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage(e.getKey()));

			importForm.setAuthenticationRequired(true);
			final String suggestedUsername = e.getSuggestedUsername();
			if (isNotBlank(suggestedUsername)) {
				importForm.setUsername(suggestedUsername);
			}
			
			return mapping.getInputForward();
		} catch (ConfigException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage(e.getKey(), e.getArgs()));

			return mapping.getInputForward();
		} catch (StoreException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage(
							"messages.save.failure",
							new Object[] {e.getMessage()}));
			
			return mapping.findForward("failure");
		}
		
		return mapping.findForward("success");
	}
	
	public void setProjectImporter(ProjectImporter projectImporter) {
		this.projectImporter = projectImporter;
	}
}
