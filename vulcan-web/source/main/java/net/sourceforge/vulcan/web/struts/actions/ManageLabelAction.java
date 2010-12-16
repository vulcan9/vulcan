/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2007 Chris Eldredge
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

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.web.struts.forms.LabelForm;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public final class ManageLabelAction extends BaseDispatchAction {
	
	public ActionForward edit(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		final LabelForm labelForm = (LabelForm) form;
		
		final String name = labelForm.getName();
		
		if (StringUtils.isNotBlank(name)) {
			final List<String> projectNames = stateManager.getProjectConfigNamesByLabel(name);
			labelForm.setProjectNames(projectNames.toArray(new String[projectNames.size()]));
		}
		
		return mapping.findForward("labelForm");
	}
	
	public ActionForward save(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		final LabelForm labelForm = (LabelForm) form;
		
		stateManager.applyProjectLabel(labelForm.getName(), Arrays.asList(labelForm.getProjectNames()));
		
		saveSuccessMessage(request);
		
		return mapping.findForward("setup");
	}
}
