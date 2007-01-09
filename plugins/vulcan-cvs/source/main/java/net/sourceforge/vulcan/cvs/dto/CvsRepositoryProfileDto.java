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
package net.sourceforge.vulcan.cvs.dto;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.vulcan.dto.NamedObject;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.integration.ConfigChoice;

public class CvsRepositoryProfileDto extends PluginConfigDto implements NamedObject {
	private String description;
	private String protocol;
	private String host;
	private String repositoryPath;
	
	private String username;
	private String password;
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "description", "CvsRepositoryProfileDto.description.name", "CvsRepositoryProfileDto.description.description", locale);
		
		final Map<String, Object> props = new HashMap<String, Object>();
		props.put(ATTR_CHOICE_TYPE, ConfigChoice.INLINE);
		props.put(ATTR_AVAILABLE_CHOICES, Arrays.asList(new String[] {"pserver", "ext", "extssh"}));

		addProperty(pds, "protocol", "CvsRepositoryProfileDto.protocol.name", "CvsRepositoryProfileDto.protocol.description", locale,
				props);
		
		addProperty(pds, "host", "CvsRepositoryProfileDto.host.name", "CvsRepositoryProfileDto.host.description", locale);
		addProperty(pds, "repositoryPath", "CvsRepositoryProfileDto.repositoryPath.name", "CvsRepositoryProfileDto.repositoryPath.description", locale);
		addProperty(pds, "username", "CvsRepositoryProfileDto.username.name", "CvsRepositoryProfileDto.username.description", locale);
		addProperty(pds, "password", "CvsRepositoryProfileDto.password.name", "CvsRepositoryProfileDto.password.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.PASSWORD));
		
		return pds;
	}
	@Override
	public CvsRepositoryProfileDto copy() {
		return (CvsRepositoryProfileDto) super.copy();
	}
	@Override
	public String toString() {
		return description;
	}
	@Override
	public String getPluginId() {
		return CvsConfigDto.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return CvsConfigDto.PLUGIN_NAME;
	}
	public String getName() {
		return description;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getRepositoryPath() {
		return repositoryPath;
	}
	public void setRepositoryPath(String repositoryPath) {
		this.repositoryPath = repositoryPath;
	}
}
