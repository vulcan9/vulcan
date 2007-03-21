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
package net.sourceforge.vulcan.dotnet.dto;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dto.BaseDto;

import org.apache.commons.lang.ArrayUtils;

public class DotNetGlobalConfigDto extends DotNetBaseDto {
	public static enum GlobalBuildConfiguration {
		Unspecified,
		Debug,
		Release
	}

	private DotNetBuildEnvironmentDto[] buildEnvironments = {};
	private GlobalBuildConfiguration buildConfiguration = GlobalBuildConfiguration.Unspecified;
	private String[] properties = {};
	
	private String buildNumberProperty = "BuildNumber";
	private String revisionProperty = "RepositoryRevisionLabel";
	private String numericRevisionProperty = "RepositoryRevision";
	private String tagProperty = "RepositoryTag";
	
	@Override
	public String getHelpTopic() {
		return "DotNetConfiguration";
	}
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "buildEnvironments", "DotNetGlobalConfigDto.buildEnvironments.name",
				"DotNetGlobalConfigDto.buildEnvironments.text", locale);
		
		addProperty(pds, "buildConfiguration", "DotNetGlobalConfigDto.buildConfiguration.name",
				"DotNetGlobalConfigDto.buildConfiguration.text", locale);
		
		addProperty(pds, "properties", "DotNetGlobalConfigDto.properties.name",
				"DotNetGlobalConfigDto.properties.text", locale);
		
		addProperty(pds, "buildNumberProperty", "DotNetGlobalConfigDto.buildNumberProperty.name",
				"DotNetGlobalConfigDto.buildNumberProperty.text", locale);
		
		addProperty(pds, "revisionProperty", "DotNetGlobalConfigDto.revisionProperty.name",
				"DotNetGlobalConfigDto.revisionProperty.text", locale);

		addProperty(pds, "numericRevisionProperty", "DotNetGlobalConfigDto.numericRevisionProperty.name",
				"DotNetGlobalConfigDto.numericRevisionProperty.text", locale);
		
		addProperty(pds, "tagProperty", "DotNetGlobalConfigDto.tagProperty.name",
				"DotNetGlobalConfigDto.tagProperty.text", locale);
		return pds;
	}

	@Override
	public BaseDto copy() {
		final DotNetGlobalConfigDto copy = (DotNetGlobalConfigDto) super.copy();
		
		copy.setBuildEnvironments((DotNetBuildEnvironmentDto[]) copyArray(buildEnvironments));
		copy.setProperties((String[]) ArrayUtils.clone(properties));
		
		return copy;
	}
	
	public GlobalBuildConfiguration getBuildConfiguration() {
		return buildConfiguration;
	}

	public void setBuildConfiguration(GlobalBuildConfiguration buildConfiguration) {
		this.buildConfiguration = buildConfiguration;
	}

	public DotNetBuildEnvironmentDto[] getBuildEnvironments() {
		return buildEnvironments;
	}

	public void setBuildEnvironments(DotNetBuildEnvironmentDto[] buildEnvironments) {
		this.buildEnvironments = buildEnvironments;
	}
	
	public String[] getProperties() {
		return properties;
	}
	
	public void setProperties(String[] properties) {
		this.properties = properties;
	}
	
	public String getBuildNumberProperty() {
		return buildNumberProperty;
	}

	public void setBuildNumberProperty(String buildNumberProperty) {
		this.buildNumberProperty = buildNumberProperty;
	}

	public String getNumericRevisionProperty() {
		return numericRevisionProperty;
	}

	public void setNumericRevisionProperty(String numericRevisionProperty) {
		this.numericRevisionProperty = numericRevisionProperty;
	}

	public String getRevisionProperty() {
		return revisionProperty;
	}

	public void setRevisionProperty(String revisionProperty) {
		this.revisionProperty = revisionProperty;
	}

	public String getTagProperty() {
		return tagProperty;
	}

	public void setTagProperty(String tagProperty) {
		this.tagProperty = tagProperty;
	}
}
