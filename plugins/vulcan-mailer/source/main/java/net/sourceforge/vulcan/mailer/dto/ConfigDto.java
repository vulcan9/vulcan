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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.beans.PropertyDescriptor;

import net.sourceforge.vulcan.dto.BaseDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;

public class ConfigDto extends PluginConfigDto {
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.mailer";
	public static final String PLUGIN_NAME = "E-Mail";
	
	private String senderAddress = "vulcan@localhost";
	private String replyToAddress = "";
	private String smtpHost = "localhost";
	private String vulcanUrl = "http://localhost/vulcan";
	private ProfileDto[] profiles = {};
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
	
		addProperty(pds, "smtpHost", "ConfigDto.smtpHost.name", "ConfigDto.smtpHost.description", locale);
		addProperty(pds, "senderAddress", "ConfigDto.senderAddress.name", "ConfigDto.senderAddress.description", locale);
		addProperty(pds, "replyToAddress", "ConfigDto.replyToAddress.name", "ConfigDto.replyToAddress.description", locale);
		addProperty(pds, "vulcanUrl", "ConfigDto.vulcanUrl.name", "ConfigDto.vulcanUrl.description", locale);
		addProperty(pds, "profiles", "ConfigDto.profiles.name", "ConfigDto.profiles.description", locale);
		
		return pds;
	}
	
	@Override
	public BaseDto copy() {
		final ConfigDto copy = (ConfigDto) super.copy();
		copy.setProfiles((ProfileDto[]) copyArray(this.profiles));
		return copy;
	}
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
		return "MailConfiguration";
	}
	public String getSenderAddress() {
		return senderAddress;
	}
	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}
	public String getReplyToAddress() {
		return replyToAddress;
	}
	public void setReplyToAddress(String replyToAddress) {
		this.replyToAddress = replyToAddress;
	}
	public String getSmtpHost() {
		return smtpHost;
	}
	public void setSmtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
	}
	public ProfileDto[] getProfiles() {
		return profiles;
	}
	public void setProfiles(ProfileDto[] profiles) {
		this.profiles = profiles;
	}
	public String getVulcanUrl() {
		return vulcanUrl;
	}
	public void setVulcanUrl(String vulcanUrl) {
		this.vulcanUrl = vulcanUrl;
	}
}
