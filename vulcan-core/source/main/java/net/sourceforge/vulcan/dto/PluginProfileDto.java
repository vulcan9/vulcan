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


public abstract class PluginProfileDto extends PluginConfigDto implements NamedObject {
	private String name;
	
	// mark transient to exclude from EqualsBuilder.reflectionEquals.
	private transient String oldName;
	
	/**
	 * @return The name of the property, on the project-scope plugin configuration,
	 * which selects this type of profile.
	 */
	public abstract String getProjectConfigProfilePropertyName();
	
	/**
	 * Save state to determine if this object is renamed at a point after
	 * this method is called.
	 */
	public void checkPoint() {
		oldName = getName();
	}
	
	public boolean isRenamed() {
		if (oldName == null) {
			return false;
		}
		
		return !oldName.equals(getName());
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getOldName() {
		return oldName;
	}
}
