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
package net.sourceforge.vulcan.dotnet;

import java.util.Arrays;
import java.util.List;

import net.sourceforge.vulcan.dotnet.dto.MSBuildConsoleLoggerParametersDto.ConsoleLoggerParameter;
import net.sourceforge.vulcan.dotnet.dto.MSBuildConsoleLoggerParametersDto.Verbosity;
import net.sourceforge.vulcan.exception.ConfigException;

public class MSBuildToolTest extends MSBuildToolTestBase {
	
	public void testSetsTargetFrameworkWhenSpecified() throws Exception {
		dotNetProjectConfig.setTargetFrameworkVersion("v4.0");
		assertArgsContains("/p:TargetFrameworkVersion=v4.0");
	}
	
	public void testOmitTargetFrameworkWhenBlank() throws Exception {
		dotNetProjectConfig.setTargetFrameworkVersion("");
		assertArgsNotContains("/p:TargetFrameworkVersion=");
	}

	public void testSetsToolsVersionWhenSpecified() throws Exception {
		buildEnv.setToolsVersion("3.5");
		assertArgsContains("/toolsversion:3.5");
	}

	public void testSetsToolsVersionWhenBlank() throws Exception {
		buildEnv.setToolsVersion("");
		assertArgsNotContains("/toolsversion:");
	}
	
	public void testSetsToolsVersionWhenUnspecified() throws Exception {
		buildEnv.setToolsVersion("Unspecified");
		assertArgsNotContains("/toolsversion:");
	}
	
	public void testOmitsNodeReuseWhenEnabled() throws Exception {
		buildEnv.setNodeReuseEnabled(true);
		assertArgsNotContains("/nodeReuse:");
	}
	
	public void testOmitsNodeReuseWhenMaxJobsUnspecified() throws Exception {
		buildEnv.setMaxJobs("");
		buildEnv.setNodeReuseEnabled(false);
		assertArgsNotContains("/nodeReuse:");
	}
	
	public void testSetsNodeReuseWhenDisabled() throws Exception {
		buildEnv.setMaxJobs("2");
		buildEnv.setNodeReuseEnabled(false);
		assertArgsContains("/nodeReuse:false");
	}

	public void testSetsMaxCpuCountWhenSpecified() throws Exception {
		buildEnv.setMaxJobs("4");
		assertArgsContains("/maxcpucount:4");		
	}

	public void testSetsVerbosityWhenSpecified() throws Exception {
		dotNetProjectConfig.getConsoleLoggerParameters().setVerbosity(Verbosity.Diagnostic);
		assertArgsContains("/verbosity:diagnostic");		
	}
	
	public void testSetsAssortedLoggerParams() throws Exception {
		ConsoleLoggerParameter[] parameters = { ConsoleLoggerParameter.PerformanceSummary, ConsoleLoggerParameter.NoItemAndPropertyList };
		dotNetProjectConfig.getConsoleLoggerParameters().setParameters(parameters );
		assertArgsContains("/consoleloggerparameters:PerformanceSummary;NoItemAndPropertyList");		
	}
	
	private void assertArgsContains(String expected) throws ConfigException {
		final List<String> args = Arrays.asList(tool.constructExecutableArgs(projectConfig, status, buildLog));
		
		for (String arg : args) {
			if (arg.startsWith(expected)) {
				return;
			}
		}
		
		fail("Expceted args to contain " + expected + " but did not.");
	}
	
	private void assertArgsNotContains(String expected) throws ConfigException {
		final List<String> args = Arrays.asList(tool.constructExecutableArgs(projectConfig, status, buildLog));
		
		for (String arg : args) {
			if (arg.startsWith(expected)) {
				fail("Expceted args not to contain " + expected + " but contained " + arg);
			}
		}
	}
}
