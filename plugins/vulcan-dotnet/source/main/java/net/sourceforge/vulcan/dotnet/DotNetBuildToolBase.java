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
package net.sourceforge.vulcan.dotnet;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.util.List;
import java.util.Map;

import net.sourceforge.vulcan.ant.AntBuildTool;
import net.sourceforge.vulcan.ant.io.ByteSerializer;
import net.sourceforge.vulcan.ant.receiver.UdpEventSource;
import net.sourceforge.vulcan.dotnet.dto.DotNetBuildEnvironmentDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetGlobalConfigDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;

public abstract class DotNetBuildToolBase extends AntBuildTool {
	protected final DotNetGlobalConfigDto globalConfig;
	protected final DotNetProjectConfigDto dotNetProjectConfig;
	protected final DotNetBuildEnvironmentDto buildEnv;
	protected final File pluginDir;

	private String propertySwitch;
	
	public DotNetBuildToolBase(DotNetGlobalConfigDto globalConfig, DotNetProjectConfigDto dotNetProjectConfig, DotNetBuildEnvironmentDto buildEnv, File pluginDir) {
		super(dotNetProjectConfig, null, new UdpEventSource(new ByteSerializer()));
		
		this.globalConfig = globalConfig;
		this.dotNetProjectConfig = dotNetProjectConfig;
		this.buildEnv = buildEnv;
		this.pluginDir = pluginDir;

	}

	protected void addConfigurationProperty(List<String> args) {
		final DotNetProjectConfigDto.BuildConfiguration buildConfiguration = dotNetProjectConfig.getBuildConfiguration();

		if (buildConfiguration == null || buildConfiguration.equals(DotNetProjectConfigDto.BuildConfiguration.Unspecified)) {
			return;
		}

		final DotNetGlobalConfigDto.GlobalBuildConfiguration globalConfiguration = globalConfig.getBuildConfiguration();
		
		final String prefix = propertySwitch + ":Configuration=";
		
		if (!buildConfiguration.equals(DotNetProjectConfigDto.BuildConfiguration.Inherit)) {
			args.add(prefix + buildConfiguration.name());
		} else if (globalConfiguration != null && !globalConfiguration.equals(DotNetGlobalConfigDto.GlobalBuildConfiguration.Unspecified)) {
			args.add(prefix + globalConfiguration.name());
		}
	}

	protected void addDotNetProperties(List<String> args, ProjectConfigDto projectConfig, ProjectStatusDto status) {
		addConfigurationProperty(args);
	
		addProps(null, globalConfig.getProperties(), true);
		addProps(null, dotNetProjectConfig.getAntProperties(), true);
		
		final Map<String, String> antProps = getAntProps();
		
		addPropertyIfNecessary(antProps, globalConfig.getBuildNumberProperty(), status.getBuildNumber());
		
		final RevisionTokenDto revision = status.getRevision();
		
		if (revision != null) {
			addPropertyIfNecessary(antProps, globalConfig.getRevisionProperty(), revision.getLabel());
			addPropertyIfNecessary(antProps, globalConfig.getNumericRevisionProperty(), revision.getRevision());
		}
		
		addPropertyIfNecessary(antProps, globalConfig.getTagProperty(), status.getTagName());
		
		if (status.isScheduledBuild()) {
			addPropertyIfNecessary(antProps, globalConfig.getSchedulerProperty(), status.getRequestedBy());
		} else {
			addPropertyIfNecessary(antProps, globalConfig.getBuildUsernameProperty(), status.getRequestedBy());
		}
		
		
		for (Map.Entry<String, String> e : antProps.entrySet()) {
			final StringBuilder sb = new StringBuilder();
			
			sb.append(propertySwitch);
			sb.append(":");
			sb.append(e.getKey());
			sb.append("=");
			sb.append(e.getValue());
			
			args.add(sb.toString());
		}
	}

	protected void addPropertyIfNecessary(Map<String, String> antProps, String propertyName, Object value) {
		if (isNotBlank(propertyName) && value != null) {
			antProps.put(propertyName, value.toString());
		}
	}
	
	protected void setPropertySwitch(String propertySwitch) {
		this.propertySwitch = propertySwitch;
	}
}
