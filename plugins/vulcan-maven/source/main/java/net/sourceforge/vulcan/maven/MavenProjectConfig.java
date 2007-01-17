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
package net.sourceforge.vulcan.maven;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.vulcan.ant.AntProjectConfig;
import net.sourceforge.vulcan.dto.NamedObject;
import net.sourceforge.vulcan.integration.ConfigChoice;

public class MavenProjectConfig extends AntProjectConfig {
	private String mavenHome = "Default";
	
	public MavenProjectConfig() {
		setBuildScript("maven.xml");
	}
	
	@Override
	public String getPluginId() {
		return MavenBuildPlugin.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return MavenBuildPlugin.PLUGIN_NAME;
	}
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "targets", "MavenProjectConfig.goals.name",
				"MavenProjectConfig.goals.text", locale);
		
		final MavenConfig globalConfig = getPlugin(MavenBuildPlugin.class).getConfiguration();
		
		NamedObject[] homes = globalConfig.getMavenHomes();
		Map<String, Object> props = createEnumChoices(homes);
		
		addProperty(pds, "mavenHome", "MavenProjectConfig.mavenHome.name",
				"MavenProjectConfig.mavenHome.text", locale, props);
		
		homes = globalConfig.getJavaHomes();
		props = createEnumChoices(homes);
		
		addProperty(pds, "javaHome", "AntProjectConfig.javaHome.name",
				"AntProjectConfig.javaHome.text", locale, props);
		
		addProperty(pds, "debug", "AntProjectConfig.debug.name", "AntProjectConfig.debug.text", locale);
		addProperty(pds, "antProperties", "MavenProjectConfig.mavenProperties.name",
				"MavenProjectConfig.mavenProperties.text", locale);
		
		return pds;
	}

	private Map<String, Object> createEnumChoices(NamedObject[] homes) {
		final List<String> homeNames = new ArrayList<String>();
		
		for (NamedObject home : homes) {
			homeNames.add(home.getName());
		}
		
		final Map<String, Object> props = new HashMap<String, Object>();
		props.put(ATTR_CHOICE_TYPE, ConfigChoice.INLINE);
		props.put(ATTR_AVAILABLE_CHOICES, homeNames);
		return props;
	}

	public String getMavenHome() {
		return mavenHome;
	}
	
	public void setMavenHome(String mavenHome) {
		this.mavenHome = mavenHome;
	}
}
