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
package net.sourceforge.vulcan.maven;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dto.PluginProfileDto;

public class MavenHome extends PluginProfileDto {
	private String description;
	private String directory;
	
	@Override
	public String getProjectConfigProfilePropertyName() {
		return "mavenHome";
	}

	@Override
	public String getName() {
		return description;
	}
	
	@Override
	public String toString() {
		return description;
	}
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "description", "MavenHome.description.name", "MavenHome.description.text", locale);
		addProperty(pds, "directory", "MavenHome.directory.name", "MavenHome.directory.text", locale);
		
		return pds;
	}
	
	@Override
	public String getPluginId() {
		return MavenBuildPlugin.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return MavenBuildPlugin.PLUGIN_NAME;
	}
	@Override
	public String getHelpTopic() {
		return "MavenHomeConfiguration";
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}
}
