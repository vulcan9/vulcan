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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.struts.forms.ReportForm;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.jdom.Document;

@SvnRevision(id="$Id$", url="$HeadURL$")
public final class ViewProjectBuildHistoryAction extends ProjectReportBaseAction {
	private String filename;
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		final ReportForm reportForm = (ReportForm) form;
		final List<UUID> ids;
		final Object fromLabel;
		final Object toLabel;
		
		final String[] projectNames = reportForm.getProjectNames();
		
		if (reportForm.isDateMode()) {
			final Date from = reportForm.getStartDateAsDate();
			final Date to = reportForm.getEndDateAsDate();
			
			ids = buildManager.getAvailableStatusIdsInRange(
					new HashSet<String>(Arrays.asList(projectNames)),
					from,
					to);
			
			fromLabel = from;
			toLabel = to;
		} else if (reportForm.isIncludeAll()) {
			ids = new ArrayList<UUID>();
			
			for (String name : projectNames) {
				ids.addAll(buildManager.getAvailableStatusIds(name));
			}
			
			fromLabel = "0";

			if (projectNames.length > 1) {
				toLabel="*";
			} else {
				toLabel = Integer.toString(ids.size());
			}
		} else {
			final int startIndex = reportForm.getStartIndexAsInt();
			final int endIndex = reportForm.getEndIndexAsInt();
			
			final List<UUID> allIds = buildManager.getAvailableStatusIds(projectNames[0]);
			
			if (allIds == null || allIds.isEmpty()) {
				ids = null;
			} else if (endIndex >= allIds.size()) {
				BaseDispatchAction.saveError(request, "endIndex",
						new ActionMessage("errors.out.of.range", new Object[] { allIds.size() -1 }));
				return mapping.getInputForward();
			} else {
				ids = allIds.subList(startIndex, endIndex+1);
			}
			
			fromLabel = Integer.toString(startIndex);
			toLabel = Integer.toString(endIndex);
		}
		
		if (ids == null || ids.isEmpty()) {
			BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.no.history"));
			return mapping.getInputForward();
		}
		
		final List<ProjectStatusDto> outcomes = new ArrayList<ProjectStatusDto>();
		
		for (UUID id : ids) {
			outcomes.add(buildManager.getStatus(id));
		}
		
		final Document doc = projectDomBuilder.createProjectSummaries(outcomes, fromLabel, toLabel, request.getLocale());
		
		if (reportForm.isDownload()) {
			response.setHeader("Content-Disposition", "attachment; filename=" + filename);
		}
		
		return sendDocument(doc, reportForm.getTransform(), null, 0, mapping, request, response);
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
}
