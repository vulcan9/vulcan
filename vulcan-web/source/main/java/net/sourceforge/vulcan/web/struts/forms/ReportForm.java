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
package net.sourceforge.vulcan.web.struts.forms;

import static org.apache.commons.lang.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;

public class ReportForm extends ValidatorForm {
	private final DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
	
	private final static String[] defaultStatusTypes = { "PASS", "FAIL", "ERROR", "SKIP" };
	
	private String rangeType = "date";
	private String startDate;
	private String endDate;
	private String minBuildNumber;
	private String maxBuildNumber;
	private String transform;
	private String[] projectNames;
	private String[] statusTypes = defaultStatusTypes;
	private String updateType;
	private String requestedBy;
	private String tagName;
	private String maxResults;
	
	/*
	 * This property is only supplied so it is remembered.
	 * startDate and endDate will be populated by javascript,
	 * and only those values will be used to select the range.
	 */
	private String dateRangeSelector = "today";
	
	private boolean download;
	
	@Override
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		super.reset(mapping, request);
		
		if (isNotBlank(request.getParameter("rangeType"))) {
			projectNames = EMPTY_STRING_ARRAY;
			statusTypes = EMPTY_STRING_ARRAY;
			maxResults = EMPTY;
			download = false;
		}
	}
	
	@Override
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		final ActionErrors errors = super.validate(mapping, request);

		if (!errors.isEmpty()) {
			return errors;
		}
		
		if (isRangeMode()) {
			if (getMinBuildNumberAsInt() > getMaxBuildNumberAsInt()) {
				errors.add("minBuildNumber", new ActionMessage("errors.out.of.order"));
			}
		}
		
		return errors;
	}
	public boolean isDateMode() {
		return "date".equals(rangeType);
	}
	public boolean isIncludeAll() {
		return "all".equals(rangeType) || "recent".equals(rangeType);
	}
	public boolean isRangeMode() {
		return "index".equals(rangeType);
	}
	public String getRangeType() {
		return rangeType;
	}
	public void setRangeType(String rangeType) {
		this.rangeType = rangeType;
	}
	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	public String getMinBuildNumber() {
		return minBuildNumber;
	}
	public void setMinBuildNumber(String startIndex) {
		this.minBuildNumber = startIndex;
	}
	public String getMaxBuildNumber() {
		return maxBuildNumber;
	}
	public void setMaxBuildNumber(String endIndex) {
		this.maxBuildNumber = endIndex;
	}
	public String[] getProjectNames() {
		return projectNames;
	}
	public void setProjectNames(String[] projectNames) {
		this.projectNames = projectNames;
	}
	public String[] getStatusTypes() {
		return statusTypes;
	}
	public void setStatusTypes(String[] statusTypes) {
		this.statusTypes = statusTypes;
	}
	public String getTransform() {
		return transform;
	}
	public void setTransform(String transform) {
		this.transform = transform;
	}
	public int getMinBuildNumberAsInt() {
		return Integer.parseInt(minBuildNumber);
	}
	public int getMaxBuildNumberAsInt() {
		return Integer.parseInt(maxBuildNumber);
	}
	public Date getStartDateAsDate() {
		return parseDate(startDate);
	}
	public Date getEndDateAsDate() {
		return parseDate(endDate);
	}
	public boolean isDownload() {
		return download;
	}
	public void setDownload(boolean download) {
		this.download = download;
	}
	public String getDateRangeSelector() {
		return dateRangeSelector;
	}
	public void setDateRangeSelector(String dateRangeSelector) {
		this.dateRangeSelector = dateRangeSelector;
	}
	public String getUpdateType() {
		return updateType;
	}
	public void setUpdateType(String updateType) {
		this.updateType = updateType;
	}
	public String getRequestedBy() {
		return requestedBy;
	}
	public void setRequestedBy(String requestedBy) {
		this.requestedBy = requestedBy;
	}
	public String getTagName() {
		return tagName;
	}
	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	public String getMaxResults() {
		return maxResults;
	}
	public int getMaxResultsAsInt() {
		return StringUtils.isBlank(maxResults) ? -1 : Integer.parseInt(maxResults);
	}
	public void setMaxResults(String maxResults) {
		this.maxResults = maxResults;
	}

	private Date parseDate(final String string) {
		synchronized(format) {
			try {
				return format.parse(string);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
