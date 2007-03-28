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

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dto.PluginProfileDto;

public class SubversionRepositoryProfileDto extends PluginProfileDto {
	private String description = "";
	private String rootUrl = "";
	private String username = "";
	private String password = "";
	
	@Override
	public String getProjectConfigProfilePropertyName() {
		return "repositoryProfile";
	}
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "description", "SubversionRepositoryProfileDto.description.name", "SubversionRepositoryProfileDto.description.description", locale);
		addProperty(pds, "rootUrl", "SubversionRepositoryProfileDto.rootUrl.name", "SubversionRepositoryProfileDto.rootUrl.description", locale);
		addProperty(pds, "username", "SubversionRepositoryProfileDto.username.name", "SubversionRepositoryProfileDto.username.description", locale);
		addProperty(pds, "password", "SubversionRepositoryProfileDto.password.name", "SubversionRepositoryProfileDto.password.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.PASSWORD));
		
		return pds;
	}
	@Override
	public SubversionRepositoryProfileDto copy() {
		return (SubversionRepositoryProfileDto) super.copy();
	}
	@Override
	public String toString() {
		return description;
	}
	@Override
	public String getPluginId() {
		return SubversionConfigDto.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return SubversionConfigDto.PLUGIN_NAME;
	}
	@Override
	public String getHelpTopic() {
		return "SubversionRepositoryProfileConfiguration";
	}
	public String getRootUrl() {
		return rootUrl;
	}
	public void setRootUrl(String rootUrl) {
		this.rootUrl = rootUrl;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	@Override
	public String getName() {
		return description;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
