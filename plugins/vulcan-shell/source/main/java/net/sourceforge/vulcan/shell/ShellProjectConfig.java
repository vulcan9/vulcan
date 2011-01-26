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
package net.sourceforge.vulcan.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.beans.PropertyDescriptor;

import org.apache.commons.lang.ArrayUtils;

import net.sourceforge.vulcan.dto.BaseDto;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;

public class ShellProjectConfig extends BuildToolConfigDto {
	private String[] arguments = {};
	private String[] environmentVariables = {};
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "arguments", "ShellConfig.arguments.name", "ShellConfig.arguments.text", locale);
		addProperty(pds, "environmentVariables", "ShellConfig.environment.name", "ShellConfig.environment.text", locale);
		
		return pds;
	}
	
	@Override
	public BaseDto copy() {
		final ShellProjectConfig copy = (ShellProjectConfig) super.copy();
		
		copy.setArguments((String[]) ArrayUtils.clone(arguments));
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

	@Override
	public String getHelpTopic() {
		return "ShellProjectConfiguration";
	}
	
	public String[] getArguments() {
		return arguments;
	}

	public void setArguments(String[] arguments) {
		this.arguments = arguments;
	}

	public String[] getEnvironmentVariables() {
		return environmentVariables;
	}

	public void setEnvironmentVariables(String[] environmentVariables) {
		this.environmentVariables = environmentVariables;
	}
}
