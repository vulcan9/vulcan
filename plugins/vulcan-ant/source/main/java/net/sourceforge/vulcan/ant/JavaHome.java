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
package net.sourceforge.vulcan.ant;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dto.NamedObject;
import net.sourceforge.vulcan.dto.PluginConfigDto;

import org.apache.commons.lang.ArrayUtils;

public class JavaHome extends PluginConfigDto implements NamedObject {
	public static final String SYSTEM_DESC = "System (" + 
		System.getProperty("java.vendor") + " " +
		System.getProperty("java.version") + ")";
	public static final String SYSTEM_HOME = System.getProperty("java.home");
	
	String description;
	String javaHome;
	String maxMemory;
	String[] systemProperties = {};
	
	public String getName() {
		return description;
	}
	@Override
	public String toString() {
		return description;
	}
	@Override
	public String getPluginId() {
		return AntConfig.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return AntConfig.PLUGIN_NAME;
	}

	@Override
	public JavaHome copy() {
		final JavaHome copy = (JavaHome) super.copy();
		copy.setSystemProperties((String[]) ArrayUtils.clone(systemProperties));
		return copy;
	}
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "description", "JavaHome.description.name", "JavaHome.description.text", locale);
		addProperty(pds, "javaHome", "JavaHome.javaHome.name", "JavaHome.javaHome.text", locale);
		addProperty(pds, "maxMemory", "JavaHome.maxMemory.name", "JavaHome.maxMemory.text", locale);
		addProperty(pds, "systemProperties", "JavaHome.systemProperties.name", "JavaHome.systemProperties.text", locale);
		
		return pds;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getJavaHome() {
		return javaHome;
	}
	public void setJavaHome(String javaHome) {
		this.javaHome = javaHome;
	}
	public String getMaxMemory() {
		return maxMemory;
	}
	public void setMaxMemory(String maxMemory) {
		this.maxMemory = maxMemory;
	}
	public String[] getSystemProperties() {
		return systemProperties;
	}
	public void setSystemProperties(String[] systemProperties) {
		this.systemProperties = systemProperties;
	}
	public static boolean isSystemVm(String vm) {
		return vm == null || vm.startsWith("System (");
	}
}