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
package net.sourceforge.vulcan.subversion.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.beans.PropertyDescriptor;

import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;

public class SubversionConfigDto extends RepositoryAdaptorConfigDto {
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.subversion";
	public static final String PLUGIN_NAME = "Subversion";
	
	private SubversionRepositoryProfileDto[] profiles = {};
	private String[] tagFolderNames = {"branches", "tags"};
	
	@Override
	public SubversionConfigDto copy() {
		final SubversionConfigDto copy = (SubversionConfigDto) super.copy();

		copy.setProfiles((SubversionRepositoryProfileDto[]) copyArray(profiles));
		
		return copy;
	}
	@Override
	public String getPluginId() {
		return PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}
	@Override
	public String getHelpTopic() {
		return "SubversionRepositoryConfiguration";
	}
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();

		addProperty(pds, "profiles", "SubversionConfigDto.profiles.name", "SubversionConfigDto.profiles.description", locale);
		addProperty(pds, "tagFolderNames", "SubversionConfigDto.tagFolderNames.name", "SubversionConfigDto.tagFolderNames.description", locale);
		
		return pds;
	}
	public SubversionRepositoryProfileDto[] getProfiles() {
		return profiles;
	}
	public void setProfiles(SubversionRepositoryProfileDto[] profiles) {
		this.profiles = profiles;
	}
	public String[] getTagFolderNames() {
		return tagFolderNames;
	}
	public void setTagFolderNames(String[] tagFolderNames) {
		this.tagFolderNames = tagFolderNames;
	}
}
