/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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
package net.sourceforge.vulcan.jabber;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.vulcan.dto.BaseDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.integration.ConfigChoice;
import net.sourceforge.vulcan.metadata.Transient;

import org.apache.commons.lang.ArrayUtils;

public class JabberPluginConfig extends PluginConfigDto {
	public static enum ProjectsToMonitor {
		All,
		Specify
	}
	
	public static enum ScreenNameMapper {
		Dictionay(new DictionaryScreenNameMapperConfig()),
		Identity(new IdentityScreenNameMapperConfig()),
		Jdbc(new JdbcScreenNameMapperConfig());
		
		private final PluginConfigDto defaultConfig;
		
		ScreenNameMapper(PluginConfigDto defaultConfig) {
			this.defaultConfig = defaultConfig;
		}
		
		public PluginConfigDto getDefaultConfig() {
			return defaultConfig;
		}
	}
	
	private String server = "";
	private int port = 5222;
	private String username = "";
	private String password = "";
	private String vulcanUrl = "http://localhost:8080/vulcan";
	
	private ScreenNameMapper screenNameMapper = ScreenNameMapper.Dictionay;
	private String[] recipients = {};
	private ProjectsToMonitor projectsToMonitor = ProjectsToMonitor.All;
	private String[] selectedProjects = {};
	private Map<ScreenNameMapper, ? super PluginConfigDto> screenNameMapperConfig = new HashMap<ScreenNameMapper, PluginConfigDto>();
	private String messageFormat = "You may have broken the build!  See {url} for more info.";
	private String otherUsersMessageFormat = "These users were also notified: {users}.";
	
	@Override
	public String getPluginId() {
		return JabberPlugin.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return JabberPlugin.PLUGIN_NAME;
	}

	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();

		addProperty(pds, "server", "JabberPluginConfig.server.name", "JabberPluginConfig.server.description", locale);
		addProperty(pds, "port", "JabberPluginConfig.port.name", "JabberPluginConfig.port.description", locale);
		addProperty(pds, "username", "JabberPluginConfig.username.name", "JabberPluginConfig.username.description", locale);
		addProperty(pds, "password", "JabberPluginConfig.password.name", "JabberPluginConfig.password.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.PASSWORD));
		
		addProperty(pds, "vulcanUrl", "JabberPluginConfig.vulcanUrl.name", "JabberPluginConfig.vulcanUrl.description", locale);
		addProperty(pds, "messageFormat", "JabberPluginConfig.messageFormat.name", "JabberPluginConfig.messageFormat.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.TEXTAREA));
		addProperty(pds, "otherUsersMessageFormat", "JabberPluginConfig.otherUsersMessageFormat.name", "JabberPluginConfig.otherUsersMessageFormat.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.TEXTAREA));
		
		addProperty(pds, "screenNameMapper", "JabberPluginConfig.screenNameMapper.name", "JabberPluginConfig.screenNameMapper.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.DROPDOWN));
		addProperty(pds, "screenNameMapperConfig", "JabberPluginConfig.screenNameMapperConfig.name", "JabberPluginConfig.screenNameMapperConfig.description", locale);
		
		addProperty(pds, "projectsToMonitor", "JabberPluginConfig.projectsToMonitor.name", "JabberPluginConfig.projectsToMonitor.description", locale);
		addProperty(pds, "selectedProjects", "JabberPluginConfig.selectedProjects.name", "JabberPluginConfig.selectedProjects.description", locale,
				Collections.singletonMap(ATTR_CHOICE_TYPE, ConfigChoice.PROJECTS));

		addProperty(pds, "recipients", "JabberPluginConfig.recipients.name", "JabberPluginConfig.recipients.description", locale);
		
		return pds;
	}
	
	@Override
	public BaseDto copy() {
		final JabberPluginConfig copy = (JabberPluginConfig) super.copy();
		copy.setSelectedProjects((String[]) ArrayUtils.clone(getSelectedProjects()));
		return copy;
	}
	
	@Override
	public String getHelpTopic() {
		return "JabberPluginConfig";
	}

	public String getServer() {
		return server;
	}
	
	public void setServer(String server) {
		this.server = server;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	public ProjectsToMonitor getProjectsToMonitor() {
		return projectsToMonitor;
	}
	
	public void setProjectsToMonitor(ProjectsToMonitor projectsToMonitor) {
		this.projectsToMonitor = projectsToMonitor;
	}
	
	public String[] getSelectedProjects() {
		return selectedProjects;
	}
	
	public void setSelectedProjects(String[] selectedProjects) {
		this.selectedProjects = selectedProjects;
	}
	
	public String[] getRecipients() {
		return recipients;
	}

	public void setRecipients(String[] recipients) {
		this.recipients = recipients;
	}
	
	public String getVulcanUrl() {
		return vulcanUrl;
	}
	
	public void setVulcanUrl(String vulcanUrl) {
		this.vulcanUrl = vulcanUrl;
	}

	public String getMessageFormat() {
		return messageFormat;
	}
	
	public void setMessageFormat(String messageFormat) {
		this.messageFormat = messageFormat;
	}

	public String getOtherUsersMessageFormat() {
		return otherUsersMessageFormat;
	}
	
	public void setOtherUsersMessageFormat(String otherUsersMessageFormat) {
		this.otherUsersMessageFormat = otherUsersMessageFormat;
	}
	
	public ScreenNameMapper getScreenNameMapper() {
		return screenNameMapper;
	}
	
	public void setScreenNameMapper(ScreenNameMapper screenNameMapper) {
		this.screenNameMapper = screenNameMapper;
	}
	
	public Map<ScreenNameMapper, ? super PluginConfigDto> getScreenNameMapperConfigs() {
		return screenNameMapperConfig;
	}
	
	public void setScreenNameMapperConfigs(
			Map<ScreenNameMapper, ? super PluginConfigDto> screenNameMapperConfig) {
		this.screenNameMapperConfig = screenNameMapperConfig;
	}
	
	@Transient
	public PluginConfigDto getScreenNameMapperConfig() {
		PluginConfigDto dto = (PluginConfigDto) screenNameMapperConfig.get(screenNameMapper);
		
		if (dto == null) {
			dto = screenNameMapper.getDefaultConfig();
			
			screenNameMapperConfig.put(screenNameMapper, dto);
		}
		
		dto.setApplicationContext(applicationContext);
		
		return dto;
	}
	
	public void setScreenNameMapperConfig(PluginConfigDto config) {
		screenNameMapperConfig.put(screenNameMapper, config);
	}
}
