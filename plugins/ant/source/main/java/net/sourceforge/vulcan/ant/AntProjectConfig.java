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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.vulcan.dto.BaseDto;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.integration.ConfigChoice;

import org.apache.commons.lang.ArrayUtils;

public class AntProjectConfig extends BuildToolConfigDto {
	private String buildScript = "build.xml";
	private String targets = "";
	private String javaHome = JavaHome.SYSTEM_DESC;
	private String[] antProperties = {};
	private boolean verbose;
	private boolean debug;
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "buildScript", "AntProjectConfig.buildScript.name",
				"AntProjectConfig.buildScript.text", locale);
		addProperty(pds, "targets", "AntProjectConfig.targets.name",
				"AntProjectConfig.targets.text", locale);
		
		final List<String> javaHomeChoices = new ArrayList<String>();
		final JavaHome[] javaHomes = getPlugin(AntBuildPlugin.class).getConfiguration().getJavaHomes();
		
		for (JavaHome home : javaHomes) {
			javaHomeChoices.add(home.getDescription());
		}
		
		final Map<String, Object> props = new HashMap<String, Object>();
		props.put(ATTR_CHOICE_TYPE, ConfigChoice.INLINE);
		props.put(ATTR_AVAILABLE_CHOICES, javaHomeChoices);
		
		addProperty(pds, "javaHome", "AntProjectConfig.javaHome.name",
				"AntProjectConfig.javaHome.text", locale, props);
		
		addProperty(pds, "verbose", "AntProjectConfig.verbose.name", "AntProjectConfig.verbose.text", locale);
		addProperty(pds, "debug", "AntProjectConfig.debug.name", "AntProjectConfig.debug.text", locale);
		addProperty(pds, "antProperties", "AntProjectConfig.antProperties.name", "AntProjectConfig.antProperties.text", locale);
		
		return pds;
	}
	@Override
	public BaseDto copy() {
		final AntProjectConfig copy = (AntProjectConfig) super.copy();
		
		copy.setAntProperties((String[]) ArrayUtils.clone(antProperties));
		
		return copy;
	}
	@Override
	public String getPluginId() {
		return AntConfig.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return AntConfig.PLUGIN_NAME;
	}
	public String getBuildScript() {
		return buildScript;
	}
	public void setBuildScript(String buildScript) {
		this.buildScript = buildScript;
	}
	public String getTargets() {
		return targets;
	}
	public void setTargets(String targets) {
		this.targets = targets;
	}
	public boolean isDebug() {
		return debug;
	}
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	public String getJavaHome() {
		return javaHome;
	}
	public void setJavaHome(String javaHome) {
		this.javaHome = javaHome;
	}
	public boolean isVerbose() {
		return verbose;
	}
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	public String[] getAntProperties() {
		return antProperties;
	}
	public void setAntProperties(String[] antProperties) {
		this.antProperties = AntConfig.trim(antProperties);
	}
}
