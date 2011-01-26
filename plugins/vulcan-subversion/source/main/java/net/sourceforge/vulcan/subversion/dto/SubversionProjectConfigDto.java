/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.integration.ConfigChoice;
import net.sourceforge.vulcan.subversion.SubversionPlugin;

public class SubversionProjectConfigDto extends RepositoryAdaptorConfigDto {
	private String repositoryProfile;
	private String path;
	private CheckoutDepth checkoutDepth	= CheckoutDepth.Infinity;
	private boolean useCommitTimes = false;
	private boolean obtainBugtraqProperties = true;
	private SparseCheckoutDto[] folders = new SparseCheckoutDto[0];
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		final List<String> profileNames = new ArrayList<String>();
		
		final SubversionRepositoryProfileDto[] profiles = getPlugin(SubversionPlugin.class).getConfiguration().getProfiles();
		
		for (SubversionRepositoryProfileDto profile : profiles) {
			profileNames.add(profile.getDescription());
		}
		
		final Map<String, Object> props = new HashMap<String, Object>();
		props.put(ATTR_CHOICE_TYPE, ConfigChoice.INLINE);
		props.put(ATTR_AVAILABLE_CHOICES, profileNames);
		
		addProperty(pds, "repositoryProfile", "SubversionProjectConfigDto.repositoryProfile.name", "SubversionProjectConfigDto.repositoryProfile.description",
				locale, props);
		
		addProperty(pds, "path", "SubversionProjectConfigDto.path.name", "SubversionProjectConfigDto.path.description", locale);

		addProperty(pds, "useCommitTimes", "SubversionProjectConfigDto.useCommitTimes.name", "SubversionProjectConfigDto.useCommitTimes.description", locale);
		
		addProperty(pds, "obtainBugtraqProperties", "SubversionProjectConfigDto.obtainBugtraqProperties.name", "SubversionProjectConfigDto.obtainBugtraqProperties.description", locale);

		addProperty(pds, "checkoutDepth", "SparseCheckoutDto.checkoutDepth.name", "SparseCheckoutDto.checkoutDepth.description", locale);
		
		addProperty(pds, "folders", "SubversionProjectConfigDto.folders.name", "SubversionProjectConfigDto.folders.description", locale);
		
		return pds;
	}
	@Override
	public SubversionProjectConfigDto copy() {
		final SubversionProjectConfigDto copy = (SubversionProjectConfigDto) super.copy();
		copy.setFolders(copyArray(folders));
		return copy;
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
		return "SubversionProjectConfiguration";
	}

	public String getRepositoryProfile() {
		return repositoryProfile;
	}
	public void setRepositoryProfile(String repositoryProfile) {
		this.repositoryProfile = repositoryProfile;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public CheckoutDepth getCheckoutDepth() {
		return checkoutDepth;
	}
	public void setCheckoutDepth(CheckoutDepth checkoutDepth) {
		this.checkoutDepth = checkoutDepth;
	}
	@Deprecated
	public void setRecursive(boolean recursive) {
		setCheckoutDepth(recursive ? CheckoutDepth.Infinity : CheckoutDepth.Files);
	}
	public boolean isObtainBugtraqProperties() {
		return obtainBugtraqProperties;
	}
	public void setObtainBugtraqProperties(boolean obtainBugtraqProperties) {
		this.obtainBugtraqProperties = obtainBugtraqProperties;
	}
	public boolean isUseCommitTimes() {
		return useCommitTimes;
	}
	public void setUseCommitTimes(boolean useCommitTimes) {
		this.useCommitTimes = useCommitTimes;
	}
	public SparseCheckoutDto[] getFolders() {
		return folders;
	}
	public void setFolders(SparseCheckoutDto[] folders) {
		this.folders = folders;
	}
}
