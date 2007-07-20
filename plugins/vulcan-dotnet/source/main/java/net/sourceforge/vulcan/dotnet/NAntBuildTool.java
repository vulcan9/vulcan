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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sourceforge.vulcan.ant.buildlistener.Constants;
import net.sourceforge.vulcan.ant.receiver.UdpEventSource;
import net.sourceforge.vulcan.dotnet.dto.DotNetBuildEnvironmentDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetGlobalConfigDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NAntBuildTool extends DotNetBuildToolBase  {
	static final Log LOG = LogFactory.getLog(NAntBuildTool.class);

	private static final String NANT_LOGGER_CLASS_NAME = "SourceForge.Vulcan.DotNet.NAntListener";
	
	NAntBuildTool(DotNetGlobalConfigDto globalConfig, DotNetProjectConfigDto dotNetProjectConfig, DotNetBuildEnvironmentDto buildEnv, File pluginDir) {
		super(globalConfig, dotNetProjectConfig, buildEnv, pluginDir);
		
		setPropertySwitch("-D");
	}
	
	@Override
	protected File checkBuildScriptExists(ProjectConfigDto config) throws ConfigException {
		return null;
	}
	
	@Override
	protected String[] constructExecutableArgs(ProjectConfigDto projectConfig, ProjectStatusDto status, File logFile) throws ConfigException {
		final List<String> args = new ArrayList<String>();
		
		args.add(buildEnv.getLocation());
		
		constructLoggerArgs(args);
		
		args.add("-logfile:" + logFile.getAbsolutePath());
		
		addDotNetProperties(args, projectConfig, status);
		
		final String targets = antProjectConfig.getTargets();
		
		final String buildScript = dotNetProjectConfig.getBuildScript();
		
		if (isNotBlank(buildScript)) {
			args.add("-buildfile:" + buildScript);
		}

		if (isNotBlank(targets)) {
			String[] targetsArr = targets.split(",");
			
			for (String target : targetsArr) {
				args.add(target.trim());	
			}
		}
		
		return args.toArray(new String[args.size()]);
	}

	private void constructLoggerArgs(List<String> args) {
		UdpEventSource eventSource = (UdpEventSource) this.eventSource;
		
		final StringBuilder sb = new StringBuilder("-listener:");
		sb.append(NANT_LOGGER_CLASS_NAME);
		
		args.add(sb.toString());
		
		sb.delete(0, sb.length());
		
		sb.append("-ext:");
		sb.append(pluginDir.getAbsolutePath());
		sb.append(File.separatorChar);
		sb.append("NantBuildListener.dll");
		
		args.add(sb.toString());

		final Map<String, String> antProps = getAntProps();
		
		antProps.put(Constants.HOST_PROPERTY, eventSource.getHostname());
		antProps.put(Constants.PORT_PROPERTY, Integer.toString(eventSource.getPort()));
	}
}
