/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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
package net.sourceforge.vulcan.dto;

import java.util.Date;
import java.util.Set;

import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;

public class BuildOutcomeQueryDto extends BaseDto {
	private Set<String> projectNames;
	
	private Date minDate;
	private Date maxDate;
	
	private Integer minBuildNumber;
	private Integer maxBuildNumber;
	
	private Set<ProjectStatusDto.Status> statuses;
	private UpdateType updateType;

	private String requestedBy;
	
	private Integer maxResults;
	
	public Set<String> getProjectNames() {
		return projectNames;
	}

	public void setProjectNames(Set<String> projectNames) {
		this.projectNames = projectNames;
	}

	public Date getMinDate() {
		return minDate;
	}

	public void setMinDate(Date minDate) {
		this.minDate = minDate;
	}

	public java.util.Date getMaxDate() {
		return maxDate;
	}

	public void setMaxDate(java.util.Date maxDate) {
		this.maxDate = maxDate;
	}

	public Integer getMinBuildNumber() {
		return minBuildNumber;
	}

	public void setMinBuildNumber(Integer minBuildNumber) {
		this.minBuildNumber = minBuildNumber;
	}

	public Integer getMaxBuildNumber() {
		return maxBuildNumber;
	}

	public void setMaxBuildNumber(Integer maxBuildNumber) {
		this.maxBuildNumber = maxBuildNumber;
	}

	public Set<ProjectStatusDto.Status> getStatuses() {
		return statuses;
	}

	public void setStatuses(Set<ProjectStatusDto.Status> statuses) {
		this.statuses = statuses;
	}

	public UpdateType getUpdateType() {
		return updateType;
	}
	
	public void setUpdateType(UpdateType updateType) {
		this.updateType = updateType;
	}
	
	public String getRequestedBy() {
		return requestedBy;
	}
	
	public void setRequestedBy(String requestedBy) {
		this.requestedBy = requestedBy;
	}
	
	public Integer getMaxResults() {
		return maxResults;
	}
	
	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
	}

	/**
	 * @return true when maxResults is specified and
	 * neither minDate nor minBuildNumber are specified; false
	 * otherwise.
	 */
	public boolean isUnbounded() {
		return getMaxResults() != null
			&& getMaxResults() > 0
			&& getMinDate() == null
			&& getMinBuildNumber() == null;
	}
}
