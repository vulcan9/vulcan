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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;

public class ReportForm extends ValidatorForm {
	private final DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
	
	private String rangeType = "date";
	private String startDate;
	private String endDate;
	private String startIndex;
	private String endIndex;
	private String transform;
	private String[] projectNames;
	private String[] omitTypes;

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
		rangeType = "date";
		projectNames = EMPTY_STRING_ARRAY;
		omitTypes = EMPTY_STRING_ARRAY;
		download = false;
	}
	
	@Override
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		final ActionErrors errors = super.validate(mapping, request);

		if (!errors.isEmpty()) {
			return errors;
		}
		
		if (isRangeMode()) {
			if (getStartIndexAsInt() > getEndIndexAsInt()) {
				errors.add("startIndex", new ActionMessage("errors.out.of.order"));
			}
		}
		
		return errors;
	}
	public boolean isDateMode() {
		return "date".equals(rangeType);
	}
	public boolean isIncludeAll() {
		return "all".equals(rangeType);
	}
	public boolean isRangeMode() {
		return !(isDateMode() || isIncludeAll());
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
	public String getStartIndex() {
		return startIndex;
	}
	public void setStartIndex(String startIndex) {
		this.startIndex = startIndex;
	}
	public String getEndIndex() {
		return endIndex;
	}
	public void setEndIndex(String endIndex) {
		this.endIndex = endIndex;
	}
	public String[] getProjectNames() {
		return projectNames;
	}
	public void setProjectNames(String[] projectNames) {
		this.projectNames = projectNames;
	}
	public String[] getOmitTypes() {
		return omitTypes;
	}
	public void setOmitTypes(String[] omitTypes) {
		this.omitTypes = omitTypes;
	}
	public String getTransform() {
		return transform;
	}
	public void setTransform(String transform) {
		this.transform = transform;
	}
	public int getStartIndexAsInt() {
		return Integer.parseInt(startIndex);
	}
	public int getEndIndexAsInt() {
		return Integer.parseInt(endIndex);
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
