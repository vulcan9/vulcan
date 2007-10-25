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

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.core.BuildOutcomeStore;
import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.JsonSerializer;
import net.sourceforge.vulcan.web.struts.forms.ReportForm;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.jdom.Document;

@SvnRevision(id="$Id$", url="$HeadURL$")
public final class ViewProjectBuildHistoryAction extends ProjectReportBaseAction {
	private JsonSerializer jsonSerializer;
	private BuildOutcomeStore buildOutcomeStore;
	private String filename;
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		final ReportForm reportForm = (ReportForm) form;
		final Object fromLabel;
		final Object toLabel;
		
		final String[] projectNames = reportForm.getProjectNames();
		final BuildOutcomeQueryDto query = new BuildOutcomeQueryDto();
		query.setProjectNames(new HashSet<String>(Arrays.asList(projectNames)));
		
		if (reportForm.isDateMode()) {
			final Date from = reportForm.getStartDateAsDate();
			final Date to = reportForm.getEndDateAsDate();
			
			query.setMinDate(from);
			query.setMaxDate(to);
			
			fromLabel = from;
			toLabel = to;
		} else if (reportForm.isIncludeAll()) {
			fromLabel = "0";
			toLabel="*";
		} else {
			final int minBuildNumber = reportForm.getMinBuildNumberAsInt();
			final int maxBuildNumber = reportForm.getMaxBuildNumberAsInt();
			
			query.setMinBuildNumber(minBuildNumber);
			query.setMaxBuildNumber(maxBuildNumber);
			
			fromLabel = Integer.toString(minBuildNumber);
			toLabel = Integer.toString(maxBuildNumber);
		}
		
		final Set<Status> statuses = new HashSet<Status>();
		statuses.addAll(Arrays.asList(Status.values()));
		statuses.removeAll(parseOmittedTypes(reportForm.getOmitTypes()));
		
		query.setStatuses(statuses);
		
		final List<ProjectStatusDto> outcomes = buildOutcomeStore.loadBuildSummaries(query);
		
		if (outcomes.isEmpty()) {
			BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.no.history"));
			return mapping.getInputForward();
		}
		
		if ("json".equals(reportForm.getTransform())) {
			prepareBuildHistoryForCharts(request, outcomes);
			return mapping.findForward("charts");
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
	
	public void setBuildOutcomeStore(BuildOutcomeStore buildOutcomeStore) {
		this.buildOutcomeStore = buildOutcomeStore;
	}
	
	public void setJsonSerializer(JsonSerializer jsonSerializer) {
		this.jsonSerializer = jsonSerializer;
	}

	private void prepareBuildHistoryForCharts(HttpServletRequest request, final List<ProjectStatusDto> outcomes) {
		final String data = jsonSerializer.toJSON(outcomes, request.getLocale());
		request.setAttribute("jsonBuildHistory", data.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;"));
	}
	
	private Set<Status> parseOmittedTypes(String[] typeStrings) {
		final Set<Status> types = new HashSet<Status>();
		
		for (String omitType : typeStrings) {
			types.add(Status.valueOf(omitType));
		}
		
		return types;
	}
}
