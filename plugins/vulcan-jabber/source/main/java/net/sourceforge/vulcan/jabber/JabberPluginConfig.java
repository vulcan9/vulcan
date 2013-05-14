/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.exception.ValidationException;
import net.sourceforge.vulcan.integration.ConfigChoice;
import net.sourceforge.vulcan.metadata.Transient;

import org.apache.commons.lang.ArrayUtils;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;

public class JabberPluginConfig extends PluginConfigDto {
	public static enum ProjectsToMonitor {
		All,
		Specify
	}
	
	public static enum EventsToMonitor {
		Errors,
		Warnings
	}
	
	public static enum ScreenNameMapperType {
		Dictionary(new DictionaryScreenNameMapperConfig()) {
			@Override
			public ScreenNameMapper createScreenNameMapper(PluginConfigDto config) {
				return new DictionaryScreenNameMapper((DictionaryScreenNameMapperConfig) config);
			}
		},
		
		Regex(new RegexScreenNameMapperConfig()) {
			@Override
			public ScreenNameMapper createScreenNameMapper(PluginConfigDto config) {
				return new RegexScreenNameMapper((RegexScreenNameMapperConfig) config);
			}
		},
		
		Jdbc(new JdbcScreenNameMapperConfig()) {
			@Override
			public ScreenNameMapper createScreenNameMapper(PluginConfigDto config) {
				return new JdbcScreenNameMapper((JdbcScreenNameMapperConfig) config);
			}
		};
		
		private final PluginConfigDto defaultConfig;
		
		ScreenNameMapperType(PluginConfigDto defaultConfig) {
			this.defaultConfig = defaultConfig;
		}
		
		public PluginConfigDto getDefaultConfig() {
			return defaultConfig;
		}
		
		public abstract ScreenNameMapper createScreenNameMapper(PluginConfigDto config);
	}
	
	private String server = "";
	private int port = 5222;
	private String resource = "vulcan";
	private boolean SASLAuthenticationEnabled;
	private boolean selfSignedCertificateEnabled;
	private SecurityMode securityMode;
	private String serviceName = "";
	private String username = "";
	private String password = "";
	private String vulcanUrl = "http://localhost:8080/vulcan";
	
	private JabberTemplatesConfig templateConfig = new JabberTemplatesConfig();
	
	private ScreenNameMapperType screenNameMapper = ScreenNameMapperType.Dictionary;
	private String[] recipients = {};
	private ProjectsToMonitor projectsToMonitor = ProjectsToMonitor.All;
	private String[] selectedProjects = {};
	private Map<ScreenNameMapperType, ? super PluginConfigDto> screenNameMapperConfig = new HashMap<ScreenNameMapperType, PluginConfigDto>();
	
	private EventsToMonitor[] eventsToMonitor = { EventsToMonitor.Errors };
	private String errorRegex = "";
	private String warningRegex = "";
	
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

		addProperty(pds, "username", "JabberPluginConfig.username.name", "JabberPluginConfig.username.description", locale);
		addProperty(pds, "password", "JabberPluginConfig.password.name", "JabberPluginConfig.password.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.PASSWORD));
		addProperty(pds, "serviceName", "JabberPluginConfig.serviceName.name", "JabberPluginConfig.serviceName.description", locale);

		addProperty(pds, "server", "JabberPluginConfig.server.name", "JabberPluginConfig.server.description", locale);
		addProperty(pds, "port", "JabberPluginConfig.port.name", "JabberPluginConfig.port.description", locale);
		addProperty(pds, "resource", "JabberPluginConfig.resource.name", "JabberPluginConfig.resource.description", locale);

		addProperty(pds, "securityMode", "JabberPluginConfig.securityMode.name", "JabberPluginConfig.securityMode.description", locale);
		addProperty(pds, "SASLAuthenticationEnabled", "JabberPluginConfig.SASLAuthenticationEnabled.name", "JabberPluginConfig.SASLAuthenticationEnabled.description", locale);
		addProperty(pds, "selfSignedCertificateEnabled", "JabberPluginConfig.selfSignedCertificateEnabled.name", "JabberPluginConfig.selfSignedCertificateEnabled.description", locale);
		
		addProperty(pds, "vulcanUrl", "JabberPluginConfig.vulcanUrl.name", "JabberPluginConfig.vulcanUrl.description", locale);
		
		addProperty(pds, "templateConfig", "JabberPluginConfig.templateConfig.name", "JabberPluginConfig.templateConfig.description", locale);
		
		addProperty(pds, "screenNameMapper", "JabberPluginConfig.screenNameMapper.name", "JabberPluginConfig.screenNameMapper.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.DROPDOWN));
		addProperty(pds, "screenNameMapperConfig", "JabberPluginConfig.screenNameMapperConfig.name", "JabberPluginConfig.screenNameMapperConfig.description", locale);
		
		addProperty(pds, "projectsToMonitor", "JabberPluginConfig.projectsToMonitor.name", "JabberPluginConfig.projectsToMonitor.description", locale);
		addProperty(pds, "selectedProjects", "JabberPluginConfig.selectedProjects.name", "JabberPluginConfig.selectedProjects.description", locale,
				Collections.singletonMap(ATTR_CHOICE_TYPE, ConfigChoice.PROJECTS));

		addProperty(pds, "eventsToMonitor", "JabberPluginConfig.eventsToMonitor.name", "JabberPluginConfig.eventsToMonitor.description", locale);
		addProperty(pds, "errorRegex", "JabberPluginConfig.errorRegex.name", "JabberPluginConfig.errorRegex.description", locale);
		addProperty(pds, "warningRegex", "JabberPluginConfig.warningRegex.name", "JabberPluginConfig.warningRegex.description", locale);
		
		addProperty(pds, "recipients", "JabberPluginConfig.recipients.name", "JabberPluginConfig.recipients.description", locale);
		
		return pds;
	}
	
	@Override
	public JabberPluginConfig copy() {
		final JabberPluginConfig copy = (JabberPluginConfig) super.copy();
		copy.setTemplateConfig(getTemplateConfig().copy());
		copy.setSelectedProjects((String[]) ArrayUtils.clone(getSelectedProjects()));
		copy.setEventsToMonitor((EventsToMonitor[]) ArrayUtils.clone(getEventsToMonitor()));
		copy.screenNameMapperConfig = new HashMap<ScreenNameMapperType, PluginConfigDto>();
		for (ScreenNameMapperType key : screenNameMapperConfig.keySet()) {
			final PluginConfigDto pluginConfigDto = (PluginConfigDto) screenNameMapperConfig.get(key);
			copy.screenNameMapperConfig.put(key, (PluginConfigDto) pluginConfigDto.copy());
		}
		return copy;
	}
	
	@Override
	public void validate() throws ValidationException {
		super.validate();
		
		try {
			if (isNotBlank(errorRegex)) {
				Pattern.compile(errorRegex);
			}
		} catch (PatternSyntaxException e) {
			throw new ValidationException("errorRegex", "jabber.validation.regex");
		}
		
		try {
			if (isNotBlank(warningRegex)) {
				Pattern.compile(warningRegex);
			}
		} catch (PatternSyntaxException e) {
			throw new ValidationException("warningRegex", "jabber.validation.regex");
		}
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
	
	public SecurityMode getSecurityMode() {
		return securityMode;
	}
	
	public void setSecurityMode(SecurityMode securityMode) {
		this.securityMode = securityMode;
	}
	
	public boolean isSASLAuthenticationEnabled() {
		return SASLAuthenticationEnabled;
	}
	
	public void setSASLAuthenticationEnabled(boolean sASLAuthenticationEnabled) {
		SASLAuthenticationEnabled = sASLAuthenticationEnabled;
	}
	
	public boolean isSelfSignedCertificateEnabled() {
		return selfSignedCertificateEnabled;
	}
	
	public void setSelfSignedCertificateEnabled(
			boolean selfSignedCertificateEnabled) {
		this.selfSignedCertificateEnabled = selfSignedCertificateEnabled;
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
	
	public String getResource() {
		return resource;
	}
	
	public void setResource(String resource) {
		this.resource = resource;
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

	@Deprecated
	public void setMessageFormat(String messageFormat) {
		getTemplateConfig().setNotifyCommitterTemplate(messageFormat);
	}

	@Deprecated
	public void setBuildMasterMessageFormat(String buildMasterMessageFormat) {
		getTemplateConfig().setNotifyBuildMasterTemplate(buildMasterMessageFormat);
	}
	
	public JabberTemplatesConfig getTemplateConfig() {
		templateConfig.setApplicationContext(applicationContext);
		return templateConfig;
	}
	
	public void setTemplateConfig(JabberTemplatesConfig templateConfig) {
		this.templateConfig = templateConfig;
	}
	
	@Deprecated
	public void setOtherUsersMessageFormat(String otherUsersMessageFormat) {
		getTemplateConfig().setNotifyBuildMasterTemplate(otherUsersMessageFormat);
	}
	
	public ScreenNameMapperType getScreenNameMapper() {
		return screenNameMapper;
	}
	
	public void setScreenNameMapper(ScreenNameMapperType screenNameMapper) {
		this.screenNameMapper = screenNameMapper;
	}
	
	public Map<ScreenNameMapperType, ? super PluginConfigDto> getScreenNameMapperConfigs() {
		return screenNameMapperConfig;
	}
	
	public void setScreenNameMapperConfigs(
			Map<ScreenNameMapperType, ? super PluginConfigDto> screenNameMapperConfig) {
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
	
	public EventsToMonitor[] getEventsToMonitor() {
		return eventsToMonitor;
	}
	
	public void setEventsToMonitor(EventsToMonitor[] eventsToMonitor) {
		this.eventsToMonitor = eventsToMonitor;
	}
	
	public String getWarningRegex() {
		return warningRegex;
	}
	
	public void setWarningRegex(String warningRegex) {
		this.warningRegex = warningRegex;
	}
	
	public String getErrorRegex() {
		return errorRegex;
	}
	
	public void setErrorRegex(String errorRegex) {
		this.errorRegex = errorRegex;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public ScreenNameMapper createScreenNameMapper() {
		return getScreenNameMapper().createScreenNameMapper(getScreenNameMapperConfig());
	}
}
