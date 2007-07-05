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
package net.sourceforge.vulcan.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.beans.PropertyDescriptor;

import net.sourceforge.vulcan.dto.BaseDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;

import org.apache.commons.lang.ArrayUtils;

public class ShellBuildToolConfig extends PluginConfigDto {
	private String buildNumberVariableName = "ProjectBuildNumber";
	private String revisionVariableName = "ProjectRevision";
	private String numericRevisionVariableName = "ProjectNumericRevision";
	private String tagNameVariableName = "ProjectTag";
	private String[] environmentVariables = {};
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "buildNumberVariableName", "ShellConfig.buildNumberVariableName.name", "ShellConfig.buildNumberVariableName.text", locale);
		addProperty(pds, "revisionVariableName", "ShellConfig.revisionVariableName.name", "ShellConfig.revisionVariableName.text", locale);
		addProperty(pds, "numericRevisionVariableName", "ShellConfig.numericRevisionVariableName.name", "ShellConfig.numericRevisionVariableName.text", locale);
		addProperty(pds, "tagNameVariableName", "ShellConfig.tagNameVariableName.name", "ShellConfig.tagNameVariableName.text", locale);
		addProperty(pds, "environmentVariables", "ShellConfig.environment.name", "ShellConfig.environment.text", locale);
		
		return pds;
	}

	@Override
	public BaseDto copy() {
		final ShellBuildToolConfig copy = (ShellBuildToolConfig) super.copy();
		
		copy.setEnvironmentVariables((String[]) ArrayUtils.clone(environmentVariables));
		
		return copy;
	}
	
	@Override
	public String getPluginId() {
		return ShellBuildPlugin.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return ShellBuildPlugin.PLUGIN_NAME;
	}

	public String getBuildNumberVariableName() {
		return buildNumberVariableName;
	}

	public void setBuildNumberVariableName(String buildNumberVariableName) {
		this.buildNumberVariableName = buildNumberVariableName;
	}

	public String getRevisionVariableName() {
		return revisionVariableName;
	}

	public void setRevisionVariableName(String revisionVariableName) {
		this.revisionVariableName = revisionVariableName;
	}

	public String getNumericRevisionVariableName() {
		return numericRevisionVariableName;
	}

	public void setNumericRevisionVariableName(String numericRevisionVariableName) {
		this.numericRevisionVariableName = numericRevisionVariableName;
	}

	public String getTagNameVariableName() {
		return tagNameVariableName;
	}

	public void setTagNameVariableName(String tagNameVariableName) {
		this.tagNameVariableName = tagNameVariableName;
	}

	public String[] getEnvironmentVariables() {
		return environmentVariables;
	}

	public void setEnvironmentVariables(String[] environmentVariables) {
		this.environmentVariables = environmentVariables;
	}
}
