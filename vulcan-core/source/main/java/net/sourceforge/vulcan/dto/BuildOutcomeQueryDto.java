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
package net.sourceforge.vulcan.dto;

import java.util.Set;

import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class BuildOutcomeQueryDto extends BaseDto {
	private Set<String> projectNames;
	
	private java.util.Date minDate;
	private java.util.Date maxDate;
	
	private Integer minBuildNumber;
	private Integer maxBuildNumber;
	
	private Set<ProjectStatusDto.Status> statuses;

	public Set<String> getProjectNames() {
		return projectNames;
	}

	public void setProjectNames(Set<String> projectNames) {
		this.projectNames = projectNames;
	}

	public java.util.Date getMinDate() {
		return minDate;
	}

	public void setMinDate(java.util.Date minDate) {
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
}
