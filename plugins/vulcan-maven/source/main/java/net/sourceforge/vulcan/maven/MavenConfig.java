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
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.ant.AntConfig;

public class MavenConfig extends AntConfig {
	private MavenHome[] mavenHomes = {};
	
	@Override
	public String getPluginId() {
		return MavenBuildPlugin.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return MavenBuildPlugin.PLUGIN_NAME;
	}
	@Override
	public String getHelpTopic() {
		return "MavenConfiguration";
	}

	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "mavenHomes", "MavenConfig.mavenHomes.name", "MavenConfig.mavenHomes.text", locale);
		addProperty(pds, "javaHomes", "AntConfig.javaHomes.name", "AntConfig.javaHomes.text", locale);
		addProperty(pds, "antProperties", "MavenConfig.mavenProperties.name", "MavenConfig.mavenProperties.text", locale);
		addProperty(pds, "buildNumberPropertyName", "AntConfig.buildNumberPropertyName.name", "AntConfig.buildNumberPropertyName.text", locale);
		addProperty(pds, "revisionPropertyName", "AntConfig.revisionPropertyName.name", "AntConfig.revisionPropertyName.text", locale);
		addProperty(pds, "numericRevisionPropertyName", "AntConfig.numericRevisionPropertyName.name", "AntConfig.numericRevisionPropertyName.text", locale);
		addProperty(pds, "tagNamePropertyName", "AntConfig.tagNamePropertyName.name", "AntConfig.tagNamePropertyName.text", locale);

		return pds;
	}
	
	@Override
	public MavenConfig copy() {
		final MavenConfig copy = (MavenConfig) super.copy();
		
		copy.setMavenHomes((MavenHome[]) copyArray(mavenHomes));
		
		return copy;
	}
	
	public MavenHome[] getMavenHomes() {
		return mavenHomes;
	}
	
	public void setMavenHomes(MavenHome[] mavenHomes) {
		this.mavenHomes = mavenHomes;
	}
	
	@Override @Deprecated
	public void setAntHome(String antHome) {
		final MavenHome mavenHome = new MavenHome();
		mavenHome.setDescription("Default");
		mavenHomes = new MavenHome[] {mavenHome};
		mavenHome.setDirectory(antHome);
	}
	
	@Override @Deprecated
	public String getAntHome() {
		return null;
	}
}
