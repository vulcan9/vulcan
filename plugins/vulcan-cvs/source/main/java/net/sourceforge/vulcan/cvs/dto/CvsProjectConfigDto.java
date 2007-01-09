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
package net.sourceforge.vulcan.cvs.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.beans.PropertyDescriptor;

import net.sourceforge.vulcan.cvs.CvsPlugin;
import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.integration.ConfigChoice;

public class CvsProjectConfigDto extends RepositoryAdaptorConfigDto {
	private String repositoryProfile;
	private String module;
	private String branch;
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		final List<String> profileNames = new ArrayList<String>();
		final CvsRepositoryProfileDto[] profiles = getPlugin(CvsPlugin.class)
			.getConfiguration().getProfiles();
		
		for (CvsRepositoryProfileDto profile : profiles) {
			profileNames.add(profile.getDescription());
		}
		
		final Map<String, Object> props = new HashMap<String, Object>();
		props.put(ATTR_CHOICE_TYPE, ConfigChoice.INLINE);
		props.put(ATTR_AVAILABLE_CHOICES, profileNames);
		
		addProperty(pds, "repositoryProfile", "CvsProjectConfigDto.repositoryProfile.name", "CvsProjectConfigDto.repositoryProfile.description",
				locale, props);
		addProperty(pds, "module", "CvsProjectConfigDto.module.name", "CvsProjectConfigDto.module.description", locale);
		addProperty(pds, "branch", "CvsProjectConfigDto.branch.name", "CvsProjectConfigDto.branch.description", locale);
		
		return pds;
	}
	@Override
	public CvsProjectConfigDto copy() {
		return (CvsProjectConfigDto) super.copy();
	}
	@Override
	public String getPluginId() {
		return CvsConfigDto.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return CvsConfigDto.PLUGIN_NAME;
	}
	public String getBranch() {
		return branch;
	}
	public void setBranch(String branch) {
		this.branch = branch;
	}
	public String getRepositoryProfile() {
		return repositoryProfile;
	}
	public void setRepositoryProfile(String repositoryProfile) {
		this.repositoryProfile = repositoryProfile;
	}
	public String getModule() {
		return module;
	}
	public void setModule(String path) {
		this.module = path;
	}
}
