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
package net.sourceforge.vulcan;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.ComponentVersionDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.PluginLoadFailureException;
import net.sourceforge.vulcan.exception.PluginNotConfigurableException;
import net.sourceforge.vulcan.exception.PluginNotFoundException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.integration.Plugin;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public interface PluginManager {
	public void init();
	public void importPluginZip(InputStream in) throws StoreException, PluginLoadFailureException;
	public void removePlugin(String id) throws StoreException, PluginNotFoundException;
	
	public PluginConfigDto getPluginConfigInfo(String id) throws PluginNotConfigurableException, PluginNotFoundException;
	public void configurePlugin(PluginConfigDto pluginConfig) throws PluginNotFoundException;
	public void projectNameChanged(String oldName, String newName);
	
	public List<ComponentVersionDto> getPluginVersions();
	
	public List<Plugin> getRepositoryPlugins();
	public RepositoryAdaptor createRepositoryAdaptor(String pluginId, ProjectConfigDto projectConfig) throws PluginNotFoundException, ConfigException;
	public RepositoryAdaptorConfigDto getRepositoryAdaptorDefaultConfig(String pluginId) throws PluginNotFoundException;
	
	public List<Plugin> getBuildToolPlugins();
	public BuildTool createBuildTool(String pluginId, BuildToolConfigDto buildToolConfig) throws PluginNotFoundException, ConfigException;
	public BuildToolConfigDto getBuildToolDefaultConfig(String pluginId) throws PluginNotFoundException;
	
	public Object createObject(String id, String className) throws PluginNotFoundException, ClassNotFoundException, InstantiationException, IllegalAccessException;
	public Enum<?> createEnum(String id, String className, String enumName) throws ClassNotFoundException, PluginNotFoundException;
	
	public File getPluginDirectory(String id) throws PluginNotFoundException;
}