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
package net.sourceforge.vulcan.dotnet;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.vulcan.ant.receiver.UdpEventSource;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dotnet.dto.DotNetBuildEnvironmentDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetGlobalConfigDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetProjectConfigDto;
import net.sourceforge.vulcan.dotnet.dto.MSBuildConsoleLoggerParametersDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MSBuildTool extends DotNetBuildToolBase  {
	static final Log LOG = LogFactory.getLog(MSBuildTool.class);

	private static final String MSBUILD_LOGGER_CLASS_NAME = "SourceForge.Vulcan.DotNet.MsBuildListener";
	private static final String MSBUILD_3_5_LOGGER_CLASS_NAME = "SourceForge.Vulcan.DotNet.MsBuildNodeLogger";
	
	private Thread logThread;
	private String projectName;

	MSBuildTool(DotNetGlobalConfigDto globalConfig, DotNetProjectConfigDto dotNetProjectConfig, DotNetBuildEnvironmentDto buildEnv, File pluginDir) {
		super(globalConfig, dotNetProjectConfig, buildEnv, pluginDir);
		
		setPropertySwitch("/p");
	}
	
	@Override
	public void buildProject(ProjectConfigDto projectConfig, ProjectStatusDto status, File logFile, BuildDetailCallback callback) throws BuildFailedException, ConfigException {
		projectName = projectConfig.getName();
		
		super.buildProject(projectConfig, status, logFile, callback);
	}
	
	@Override
	protected File checkBuildScriptExists(ProjectConfigDto config) throws ConfigException {
		return null;
	}
	
	@Override
	protected String[] constructExecutableArgs(ProjectConfigDto projectConfig, ProjectStatusDto status, File logFile) throws ConfigException {
		final List<String> args = new ArrayList<String>();
		
		args.add(buildEnv.getLocation());
		
		final String toolsVersion = buildEnv.getToolsVersion();
		if (StringUtils.isNotBlank(toolsVersion) && !toolsVersion.equalsIgnoreCase("unspecified")) {
			args.add("/toolsversion:" + toolsVersion);
		}
		
		if (StringUtils.isNotBlank(buildEnv.getMaxJobs())) {
			args.add("/maxcpucount:" + buildEnv.getMaxJobs());
			if (!buildEnv.isNodeReuseEnabled()) {
				args.add("/nodeReuse:false");
			}
		}
		
		final MSBuildConsoleLoggerParametersDto clp = dotNetProjectConfig.getConsoleLoggerParameters();
		
		if (clp != null && clp.getVerbosity() != null) {
			args.add("/verbosity:" + clp.getVerbosity().name().toLowerCase());
		}
		
		if (clp != null && clp.getParameters() != null && clp.getParameters().length > 0) {
			args.add("/consoleloggerparameters:" + StringUtils.join(clp.getParameters(), ";"));
		}
		
		addPropertyIfNecessary(getAntProps(), "TargetFrameworkVersion", dotNetProjectConfig.getTargetFrameworkVersion());
		
		addDotNetProperties(args, projectConfig, status);
		
		final String targets = antProjectConfig.getTargets();
		
		if (isNotBlank(targets)) {
			args.add("/target:" + targets);
		}
		
		args.add(constructLoggerArg());
		
		final String buildScript = dotNetProjectConfig.getBuildScript();
		
		if (isNotBlank(buildScript)) {
			args.add(buildScript);
		}
		
		return args.toArray(new String[args.size()]);
	}

	@Override
	protected void preparePipes(final Process process) throws IOException {
		process.getOutputStream().close();
		process.getErrorStream().close();
		
		if (logFile == null) {
			process.getInputStream().close();
			return;
		}
		
		final OutputStream logOutputStream = new FileOutputStream(logFile);
		
		logThread = new Thread(".NET Build Logger [" + projectName + "]") {
			@Override
			public void run() {
				final InputStream inputStream = process.getInputStream();
				try {
					try {
						IOUtils.copy(inputStream, logOutputStream);
					} finally {
						try {
							logOutputStream.close();
						} finally {
							inputStream.close();
						}
					}
				} catch (IOException e) {
					LOG.error("IOException capturing maven build log", e);
				}
			}
		};
		
		logThread.start();
	}
	
	@Override
	protected void flushPipes() throws IOException {
		if (logThread == null) {
			return;
		}
		
		try {
			logThread.join();
		} catch (InterruptedException e) {
			LOG.error("Unexpected interrupt while waiting for logger thread to complete", e);
		} finally {
			logThread = null;
		}
	}

	private String constructLoggerArg() {
		UdpEventSource eventSource = (UdpEventSource) this.eventSource;
		
		final StringBuilder sb = new StringBuilder("/logger:");
		final String assemblyFile;
		
		if (StringUtils.isNotBlank(this.buildEnv.getMaxJobs()) && Integer.valueOf(this.buildEnv.getMaxJobs()) > 1) {
			sb.append(MSBUILD_3_5_LOGGER_CLASS_NAME);
			assemblyFile = "MsBuild35Listener.dll";
		} else {
			sb.append(MSBUILD_LOGGER_CLASS_NAME);
			assemblyFile = "MsBuildListener.dll";
		}
		
		sb.append(",");
		sb.append(pluginDir.getAbsolutePath());
		sb.append(File.separatorChar);
		sb.append(assemblyFile);
		sb.append(";");
		sb.append("hostname=");
		sb.append(eventSource.getHostname());
		sb.append(",port=");
		sb.append(eventSource.getPort());
		
		return sb.toString();
	}
}
