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
package net.sourceforge.vulcan.mailer.dto;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dto.BaseDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.integration.ConfigChoice;

import org.apache.commons.lang.ArrayUtils;

public class ProfileDto extends PluginConfigDto {
	public static enum Policy { ALWAYS, PASS, FAIL, SKIP, ERROR };
	
	private String description = "default";
	private String locale = "";
	private String[] emailAddresses = {};
	private String[] projects = {};
	private Policy[] policy = {};
	private boolean onlyOnChange;
	
	@Override
	public String getPluginId() {
		return ConfigDto.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return ConfigDto.PLUGIN_NAME;
	}
	@Override
	public String getHelpTopic() {
		return "MailProfileConfiguration";
	}

	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();

		addProperty(pds, "description", "ProfileDto.description.name", "ProfileDto.description.description", locale);
		addProperty(pds, "locale", "ProfileDto.locale.name", "ProfileDto.locale.description", locale);
		addProperty(pds, "emailAddresses", "ProfileDto.emailAddresses.name", "ProfileDto.emailAddresses.description", locale);
		addProperty(pds, "policy", "ProfileDto.policy.name", "ProfileDto.policy.description", locale);
		addProperty(pds, "onlyOnChange", "ProfileDto.onlyOnChange.name", "ProfileDto.onlyOnChange.description", locale);
		addProperty(pds, "projects", "ProfileDto.projects.name", "ProfileDto.projects.description", locale,
				Collections.singletonMap(PluginConfigDto.ATTR_CHOICE_TYPE, ConfigChoice.PROJECTS));
		
		return pds;
	}
	@Override
	public BaseDto copy() {
		final ProfileDto copy = (ProfileDto) super.copy();
		
		copy.setEmailAddresses((String[]) ArrayUtils.clone(emailAddresses));
		copy.setProjects((String[]) ArrayUtils.clone(projects));
		copy.setPolicy((Policy[]) ArrayUtils.clone(policy));
		
		return copy;
	}
	@Override
	public String toString() {
		return description;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLocale() {
		return locale;
	}
	public void setLocale(String locale) {
		this.locale = locale;
	}
	public String[] getEmailAddresses() {
		return emailAddresses;
	}
	public void setEmailAddresses(String[] emailAddresses) {
		this.emailAddresses = emailAddresses;
	}
	public String[] getProjects() {
		return projects;
	}
	public void setProjects(String[] projects) {
		this.projects = projects;
	}
	public Policy[] getPolicy() {
		return policy;
	}
	public void setPolicy(Policy[] policy) {
		this.policy = policy;
	}
	public boolean isOnlyOnChange() {
		return onlyOnChange;
	}
	public void setOnlyOnChange(boolean onlyOnChange) {
		this.onlyOnChange = onlyOnChange;
	}
}
