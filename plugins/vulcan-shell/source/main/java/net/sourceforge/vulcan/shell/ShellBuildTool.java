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
package net.sourceforge.vulcan.shell;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.HashMap;
import java.util.Map;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ShellBuildTool implements BuildTool {
	private static final Log LOG = LogFactory.getLog(ShellBuildTool.class);
	
	private ShellBuildToolConfig globalConfig;
	private ShellProjectConfig projectPluginConfig;

	private Thread stdoutThread;
	private Thread stderrThread;
	private BufferedWriter logWriter;
	
	public void buildProject(ProjectConfigDto projectConfig,
			ProjectStatusDto buildStatus, File logFile,
			BuildDetailCallback buildDetailCallback)
			throws BuildFailedException, ConfigException {
		
		final String[] arguments = projectPluginConfig.getArguments();
		
		if (arguments.length == 0) {
			throw new ConfigException("shell.missing.arguments", null);
		}
		
		final String[] environment = CreateEnvironment(buildStatus);
		final File dir = new File(projectConfig.getWorkDir());
		
		final Process process;
		
		try {
			process = execute(arguments, environment, dir);
		} catch (IOException e) {
			throw new ConfigException("shell.exec.failure", new String[] {arguments[0], e.getMessage()});
		}
		
		startOutputProcessors(process, logFile, buildDetailCallback);
		
		try {
			final int exitCode = process.waitFor();

			if (exitCode != 0) {
				throw new BuildFailedException("Process ended with exit code " + exitCode, null, exitCode);
			}
		} catch (InterruptedException e) {
			process.destroy();
			return;
		} finally {
			stopOutputProcessors();
		}
	}

	public String[] CreateEnvironment(ProjectStatusDto status) {
		final Map<String, String> map = new HashMap<String, String>(getCurrentEnvironment());
		
		mergeEnvironment(map, globalConfig.getEnvironmentVariables());
		mergeEnvironment(map, projectPluginConfig.getEnvironmentVariables());
		
		addIfKeyDefined(map, globalConfig.getBuildNumberVariableName(), status.getBuildNumber().toString());
		addIfKeyDefined(map, globalConfig.getRevisionVariableName(), status.getRevision().getLabel());
		addIfKeyDefined(map, globalConfig.getNumericRevisionVariableName(), status.getRevision().getRevision().toString());
		addIfKeyDefined(map, globalConfig.getTagNameVariableName(), status.getTagName());
		
		final String[] env = map.keySet().toArray(new String[map.size()]);
		
		for (int i=0; i<env.length; i++) {
			env[i] = env[i] + "=" + map.get(env[i]);
		}
		
		return env;
	}

	public ShellBuildToolConfig getGlobalConfig() {
		return globalConfig;
	}

	public void setGlobalConfig(ShellBuildToolConfig globalConfig) {
		this.globalConfig = globalConfig;
	}

	public ShellProjectConfig getProjectPluginConfig() {
		return projectPluginConfig;
	}

	public void setProjectPluginConfig(ShellProjectConfig projectConfig) {
		this.projectPluginConfig = projectConfig;
	}

	protected Process execute(final String[] arguments, final String[] environment,
			final File dir) throws IOException {
		
		return Runtime.getRuntime().exec(
				arguments,
				environment,
				dir);
	}
	
	protected Map<String, String> getCurrentEnvironment() {
		return System.getenv();
	}

	private void mergeEnvironment(final Map<String, String> map, final String[] keyValuePairs) {
		for (String keyValue : keyValuePairs) {
			final String[] split = keyValue.split("=");
			
			if (split.length == 1) {
				map.remove(split[0].trim());
			} else {
				map.put(split[0].trim(), split[1].trim());	
			}
			
		}
	}

	private void addIfKeyDefined(final Map<String, String> map,
			final String key, final String value) {
		if (isNotBlank(key)) {
			map.put(key, value);
		}
	}

	private OutputStream startOutputProcessors(final Process process, final File logFile, final BuildDetailCallback buildDetailCallback) {
		final OutputStream logOutputStream;
		
		try {
			logOutputStream = new FileOutputStream(logFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		logWriter = new BufferedWriter(new OutputStreamWriter(logOutputStream));
		
		stdoutThread = new Thread() {
			@Override
			public void run() {
				try {
					new ShellOutputProcessor(logWriter, buildDetailCallback).processStream(process.getInputStream());
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		};
		
		stdoutThread.start();
		
		stderrThread = new Thread() {
			@Override
			public void run() {
				try {
					new ShellOutputProcessor(logWriter, buildDetailCallback).processStream(process.getErrorStream());
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		};
		
		stderrThread.start();
		
		return logOutputStream;
	}

	private void stopOutputProcessors() {
		try {
			if (stdoutThread != null) {
				stdoutThread.join();
			}
			
			if (stderrThread != null) {
				stderrThread.join();
			}
		} catch (InterruptedException ignore) {
		}
		
		try {
			logWriter.close();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}
}
