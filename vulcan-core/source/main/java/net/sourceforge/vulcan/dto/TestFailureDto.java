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

import net.sourceforge.vulcan.metadata.SvnRevision;

/**
 * Represents a test failure which will be displayed in
 * the build details report.  If a test has failed consecutively
 * the buildNumber will be set to the first build number in which
 * the test failed.
 */
@SvnRevision(id="$Id: ProjectStatusDto.java 296 2007-03-15 19:43:16Z chris.eldredge $", url="$HeadURL: https://vulcan.googlecode.com/svn/trunk/vulcan-core/source/main/java/net/sourceforge/vulcan/dto/ProjectStatusDto.java $")
public class TestFailureDto extends BaseDto {
	private String name;
	private Integer buildNumber;
	private Integer count;
	
	public Integer getBuildNumber() {
		return buildNumber;
	}
	public void setBuildNumber(Integer buildNumber) {
		this.buildNumber = buildNumber;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getCount() {
		return count;
	}
	public void setCount(Integer count) {
		this.count = count;
	}
}
