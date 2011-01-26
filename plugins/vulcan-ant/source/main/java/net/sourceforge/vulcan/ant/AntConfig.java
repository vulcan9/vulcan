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
package net.sourceforge.vulcan.ant;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dto.BaseDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

public class AntConfig extends PluginConfigDto {
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.ant";
	public static final String PLUGIN_NAME = "Apache Ant";

	private String antHome;
	private JavaHome[] javaHomes = {};
	private String[] antProperties = {};
	
	private String buildNumberPropertyName = "project.build.number";
	private String revisionPropertyName = "project.revision";
	private String numericRevisionPropertyName = "project.revision.numeric";
	private String tagNamePropertyName = "project.tag";
	private String buildUserPropertyName = "project.build.user";
	private String buildSchedulerPropertyName = "project.build.scheduler";
	private boolean recordMetrics;
	
	@Override
	public String getPluginId() {
		return PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}
	@Override
	public String getHelpTopic() {
		return "AntConfiguration";
	}
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "antHome", "AntConfig.antHome.name", "AntConfig.antHome.text", locale);
		addProperty(pds, "javaHomes", "AntConfig.javaHomes.name", "AntConfig.javaHomes.text", locale);
		addProperty(pds, "antProperties", "AntConfig.antProperties.name", "AntConfig.antProperties.text", locale);
		addProperty(pds, "buildNumberPropertyName", "AntConfig.buildNumberPropertyName.name", "AntConfig.buildNumberPropertyName.text", locale);
		addProperty(pds, "revisionPropertyName", "AntConfig.revisionPropertyName.name", "AntConfig.revisionPropertyName.text", locale);
		addProperty(pds, "numericRevisionPropertyName", "AntConfig.numericRevisionPropertyName.name", "AntConfig.numericRevisionPropertyName.text", locale);
		addProperty(pds, "tagNamePropertyName", "AntConfig.tagNamePropertyName.name", "AntConfig.tagNamePropertyName.text", locale);
		addProperty(pds, "buildUserPropertyName", "AntConfig.buildUsername.name", "AntConfig.buildUsername.text", locale);
		addProperty(pds, "buildSchedulerPropertyName", "AntConfig.scheduler.name", "AntConfig.scheduler.text", locale);
		addProperty(pds, "recordMetrics", "AntConfig.recordMetrics.name", "AntConfig.recordMetrics.text", locale);
		
		return pds;
	}
	@Override
	public BaseDto copy() {
		final AntConfig copy = (AntConfig) super.copy();
		
		copy.setAntProperties((String[]) ArrayUtils.clone(antProperties));
		copy.setJavaHomes(copyArray(javaHomes));
		
		return copy;
	}
	public String getAntHome() {
		return antHome;
	}
	public void setAntHome(String antHome) {
		this.antHome = antHome;
	}
	public JavaHome[] getJavaHomes() {
		return javaHomes;
	}
	public void setJavaHomes(JavaHome[] javaHomes) {
		this.javaHomes = javaHomes;
	}
	public String[] getAntProperties() {
		return antProperties;
	}
	public void setAntProperties(String[] antProperties) {
		this.antProperties = trim(antProperties);
	}
	public String getBuildNumberPropertyName() {
		return buildNumberPropertyName;
	}
	public void setBuildNumberPropertyName(String buildNumberPropertyName) {
		this.buildNumberPropertyName = buildNumberPropertyName;
	}
	public String getRevisionPropertyName() {
		return revisionPropertyName;
	}
	public void setRevisionPropertyName(String revisionPropertyName) {
		this.revisionPropertyName = revisionPropertyName;
	}
	public String getNumericRevisionPropertyName() {
		return numericRevisionPropertyName;
	}
	public void setNumericRevisionPropertyName(
			String numericRevisionPropertyName) {
		this.numericRevisionPropertyName = numericRevisionPropertyName;
	}
	public String getTagNamePropertyName() {
		return tagNamePropertyName;
	}
	public void setTagNamePropertyName(String tagNamePropertyName) {
		this.tagNamePropertyName = tagNamePropertyName;
	}
	public String getBuildUserPropertyName() {
		return buildUserPropertyName;
	}
	public void setBuildUserPropertyName(String buildUserPropertyName) {
		this.buildUserPropertyName = buildUserPropertyName;
	}
	public String getBuildSchedulerPropertyName() {
		return buildSchedulerPropertyName;
	}
	public void setBuildSchedulerPropertyName(String buildSchedulerPropertyName) {
		this.buildSchedulerPropertyName = buildSchedulerPropertyName;
	}
	public boolean isRecordMetrics() {
		return recordMetrics;
	}
	public void setRecordMetrics(boolean recordMetrics) {
		this.recordMetrics = recordMetrics;
	}
	static String[] trim(String[] antProperties) {
		for (int i=0; i<antProperties.length; i++) {
			antProperties[i] = antProperties[i].trim();
		}

		final List<String> tmp = new ArrayList<String>(Arrays.asList(antProperties));
		
		for (Iterator<String> itr = tmp.iterator(); itr.hasNext();) {
			String prop = itr.next();
			
			if (StringUtils.isBlank(prop)) {
				itr.remove();
			}
		}
		
		return tmp.toArray(new String[tmp.size()]);
	}
}
