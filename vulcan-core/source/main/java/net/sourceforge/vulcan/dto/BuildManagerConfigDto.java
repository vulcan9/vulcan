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

import java.util.Map;

import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class BuildManagerConfigDto extends BaseDto {
	private boolean enabled = true;
	private Map<String, ProjectStatusDto> projectStatus;
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		final boolean old = this.enabled;
		this.enabled = enabled;
		propertyChangeSupport.firePropertyChange("enabled", old, enabled);
	}
	@Deprecated
	public Map<String, ProjectStatusDto> getProjectStatus() {
		return projectStatus;
	}
	@Deprecated
	public void setProjectStatus(Map<String, ProjectStatusDto> projectStatus) {
		this.projectStatus = projectStatus;
	}
}
