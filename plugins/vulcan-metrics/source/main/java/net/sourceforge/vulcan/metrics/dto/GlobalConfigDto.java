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
package net.sourceforge.vulcan.metrics.dto;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.metrics.XmlMetricsPlugin;

public class GlobalConfigDto extends PluginConfigDto {
	private String[] includes = {"target/test-reports/*.xml"};
	private String[] excludes = {};
	
	@Override
	public String getPluginId() {
		return XmlMetricsPlugin.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return XmlMetricsPlugin.PLUGIN_NAME;
	}
	@Override
	public String getHelpTopic() {
		return "XmlMetricsConfiguration";
	}

	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> props = new ArrayList<PropertyDescriptor>();
		
		addProperty(props, "includes", "vulcan.metrics.includes.name",
				"vulcan.metrics.includes.description", locale);
		addProperty(props, "excludes", "vulcan.metrics.excludes.name",
				"vulcan.metrics.excludes.description", locale);
		
		return props;
	}
	public String[] getExcludes() {
		return excludes;
	}
	public void setExcludes(String[] excludes) {
		this.excludes = excludes;
	}
	public String[] getIncludes() {
		return includes;
	}
	public void setIncludes(String[] includes) {
		this.includes = includes;
	}
}
