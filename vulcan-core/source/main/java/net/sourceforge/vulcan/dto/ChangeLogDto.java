/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2010 Chris Eldredge
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ChangeLogDto extends BaseDto {
	private List<ChangeSetDto> changeSets = Collections.emptyList();

	private String differences;
	
	public List<ChangeSetDto> getChangeSets() {
		return changeSets;
	}
	
	public void setChangeSets(List<ChangeSetDto> changeSets) {
		this.changeSets = changeSets;
	}
	
	public void addChangeSet(ChangeSetDto changeSet) {
		if (changeSets == Collections.<ChangeSetDto>emptyList()) {
			changeSets = new ArrayList<ChangeSetDto>();
		}
		
		changeSets.add(changeSet);
	}
	
	@Deprecated
	public String getDifferences() {
		return differences;
	}
	
	@Deprecated
	public void setDifferences(String differences) {
		this.differences = differences;
	}
}
