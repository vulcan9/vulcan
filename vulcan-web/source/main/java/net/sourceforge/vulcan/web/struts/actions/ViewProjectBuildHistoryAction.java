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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.core.BuildOutcomeStore;
import net.sourceforge.vulcan.core.support.MetricSelector;
import net.sourceforge.vulcan.core.support.ReportHelper;
import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.Keys;
import net.sourceforge.vulcan.web.struts.forms.ReportForm;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.jdom.Document;

@SvnRevision(id="$Id$", url="$HeadURL$")
public final class ViewProjectBuildHistoryAction extends ProjectReportBaseAction {
	private BuildOutcomeStore buildOutcomeStore;
	private MetricSelector metricSelector;
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
		
		final String[] omitTypes = reportForm.getOmitTypes();
		if (omitTypes.length > 0) {
			final Set<Status> statuses = new HashSet<Status>();
			statuses.addAll(Arrays.asList(Status.values()));
			statuses.removeAll(parseOmittedTypes(omitTypes));
			
			query.setStatuses(statuses);
		}
		
		final List<ProjectStatusDto> outcomes = buildOutcomeStore.loadBuildSummaries(query);
		
		if (outcomes.isEmpty()) {
			BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.no.history"));
			return mapping.getInputForward();
		}
		
		final Document doc = projectDomBuilder.createProjectSummaries(outcomes, fromLabel, toLabel, request.getLocale());
		
		if (reportForm.isDownload()) {
			response.setHeader("Content-Disposition", "attachment; filename=" + filename);
		}
		
		final String transformType = reportForm.getTransform();
		if (StringUtils.isNotBlank(transformType)) {
			final ActionForward forward = mapping.findForward(transformType);
			if (forward != null) {
				prepareMetrics(request, outcomes, doc, fromLabel, toLabel);
				prepareStatistics(request, query);
				return forward;
			}
		}
		
		return sendDocument(doc, transformType, null, 0, null, mapping, request, response);
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public void setBuildOutcomeStore(BuildOutcomeStore buildOutcomeStore) {
		this.buildOutcomeStore = buildOutcomeStore;
	}
	
	private void prepareStatistics(HttpServletRequest request, BuildOutcomeQueryDto query) {
		request.setAttribute("topErrors", buildOutcomeStore.loadTopBuildErrors(query, 5));
		request.setAttribute("topTestFailures", buildOutcomeStore.loadTopTestFailures(query, 5));
	}
	
	private void prepareMetrics(HttpServletRequest request,
			final List<ProjectStatusDto> outcomes, final Document doc,
			final Object fromLabel, final Object toLabel) {
		
		Date maxDate = null;
		if (toLabel instanceof Date) {
			maxDate = (Date) toLabel;
		}
		
		final ReportHelper reportHelper = new ReportHelper(outcomes, maxDate);
		
		request.setAttribute("fromLabel", fromLabel);
		request.setAttribute("toLabel", toLabel);
		request.setAttribute("sampleCount", outcomes.size());
		request.setAttribute("successCount", reportHelper.getSuccessCount());
		request.setAttribute("averageTimeToFixBuild", reportHelper.getAverageTimeToFixBuild());
		request.setAttribute("longestTimeToFixBuild", reportHelper.getLongestTimeToFixBuild());
		request.setAttribute("failingBuildNumber", reportHelper.getFailingBuildNumber());
		request.setAttribute("fixedInBuildNumber", reportHelper.getFixedInBuildNumber());
		request.setAttribute("longestElapsedFailureName", reportHelper.getLongestElapsedFailureName());
		
		request.getSession().setAttribute(Keys.BUILD_HISTORY, doc);
		final List<String> availableMetrics = getAvailableMetrics(outcomes);
		request.setAttribute("availableMetrics", availableMetrics);
		
		final List<String> defaultMetrics = metricSelector.selectDefaultMetrics(availableMetrics);
		if (defaultMetrics.size() > 0) {
			request.setAttribute("selectedMetric1", defaultMetrics.get(0));
			if (defaultMetrics.size() > 1) {
				request.setAttribute("selectedMetric2", defaultMetrics.get(1));
			}
		}
	}
	
	public void setMetricSelector(MetricSelector metricSelector) {
		this.metricSelector = metricSelector;
	}
	
	private List<String> getAvailableMetrics(List<ProjectStatusDto> outcomes) {
		final Set<String> keys = new HashSet<String>();
		
		for (ProjectStatusDto outcome : outcomes) {
			final List<MetricDto> metrics = outcome.getMetrics();
			if (metrics == null) {
				continue;
			}
			
			for (MetricDto metric : metrics) {
				if (metric.getType() == MetricType.NUMBER ||
					metric.getType() == MetricType.PERCENT)
						keys.add(metric.getMessageKey());
			}
		}
		
		final List<String> list = new ArrayList<String>(keys);
		
		Collections.sort(list);
		
		return list;
	}

	private Set<Status> parseOmittedTypes(String[] typeStrings) {
		final Set<Status> types = new HashSet<Status>();
		
		for (String omitType : typeStrings) {
			types.add(Status.valueOf(omitType));
		}
		
		return types;
	}
}
