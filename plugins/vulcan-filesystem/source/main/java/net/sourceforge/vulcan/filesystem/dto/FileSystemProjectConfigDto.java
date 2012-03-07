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
package net.sourceforge.vulcan.filesystem.dto;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.filesystem.FileSystemPlugin;

public class FileSystemProjectConfigDto extends RepositoryAdaptorConfigDto {
	private String sourceDirectory;
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "sourceDirectory", "sourceDirectory.name", "sourceDirectory.text", locale);
		
		return pds;
	}
	@Override
	public String getPluginId() {
		return FileSystemPlugin.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return FileSystemPlugin.PLUGIN_NAME;
	}
	@Override
	public String getHelpTopic() {
		return "FileSystemProjectConfiguration";
	}

	public String getSourceDirectory() {
		return sourceDirectory;
	}
	
	public void setSourceDirectory(String sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}
}
