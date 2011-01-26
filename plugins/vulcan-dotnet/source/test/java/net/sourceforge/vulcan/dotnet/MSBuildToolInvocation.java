/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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

import java.io.File;

import net.sourceforge.vulcan.dotnet.dto.DotNetGlobalConfigDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetProjectConfigDto;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.io.FileUtils;

public class MSBuildToolInvocation extends MSBuildToolTestBase {
	public void testDefaultTarget() throws Exception {
		tool.buildProject(projectConfig, status, buildLog, detailCallback);
	}
	
	public void testWritesToBuildLog() throws Exception {
		assertFalse(buildLog.exists());
		tool.buildProject(projectConfig, status, buildLog, detailCallback);
		assertTrue(buildLog.exists());
		assertTrue(buildLog.length() > 0);
	}
	
	public void testFailure() throws Exception {
		dotNetProjectConfig.setTargets("Fail");
		
		try {
			tool.buildProject(projectConfig, status, buildLog, detailCallback);
			fail("expected exception");
		} catch (BuildFailedException e) {
			assertEquals("Fail", e.getTarget());
			assertEquals("Build FAILED.", e.getMessage());
			
			assertEquals(2, targets.size());
			
			assertEquals("Fail", targets.get(0));
			assertEquals(null, targets.get(1));
			
			assertEquals(1, errors.size());
			
			final BuildMessageDto error = errors.get(0);
			assertEquals("You can't do that on television.", error.getMessage());
			
			assertTrue(error.getFile() != null && error.getFile().length() > 0);
			assertEquals(Integer.valueOf(8), error.getLineNumber());
			assertEquals("", error.getCode());
		}
	}
	
	public void testCompileErrorsHaveAbsolutePathsToFiles() throws Exception {
		dotNetProjectConfig.setTargets("Build");
		
		try {
			tool.buildProject(projectConfig, status, buildLog, detailCallback);
			fail("expected exception");
		} catch (BuildFailedException e) {
			assertEquals("Build", e.getTarget());
			assertEquals("Build FAILED.", e.getMessage());
			
			assertEquals(2, errors.size());
			
			final BuildMessageDto error = errors.get(0);
			assertEquals("CS0246", error.getCode());
			
			assertTrue(error.getFile() != null && error.getFile().length() > 0);
			assertTrue(new File(error.getFile()).isAbsolute());
		}
	}
	
	public void testFailsOnManyProjectFilesNoneSpecified() throws Exception {
		final File otherFile = new File(projectConfig.getWorkDir(), "other.proj");
		FileUtils.touch(otherFile);

		try {
			try {
				tool.buildProject(projectConfig, status, buildLog, detailCallback);
				fail("expected exception");
			} catch (BuildFailedException e) {
				assertEquals(1, e.getExitCode());
			}
		} finally {
			otherFile.delete();
		}
	}
	
	public void testSpecifyProjectFile() throws Exception {
		final File otherFile = new File(projectConfig.getWorkDir(), "other.proj");
		FileUtils.touch(otherFile);

		dotNetProjectConfig.setBuildScript("msbuild.proj");

		try {
			tool.buildProject(projectConfig, status, buildLog, detailCallback);
		} finally {
			otherFile.delete();
		}
	}
	
	public void testSetProperty() throws Exception {
		dotNetProjectConfig.setAntProperties(
				new String[] {"Foo=123", "Bar=baz"});

		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		doEchoTest("", "", "", "", "", "", "123", "baz");
	}

	public void testSetsRevisionAndTagName() throws Exception {
		globalConfig.setBuildNumberProperty("BuildNumber");
		globalConfig.setRevisionProperty("ProjectRevision");
		globalConfig.setNumericRevisionProperty("RepositoryRevision");
		globalConfig.setTagProperty("ProjectTag");

		status.setBuildNumber(747463);
		status.setTagName("tags/5.4");
		status.setRevision(new RevisionTokenDto(14322l, "1.4.32.2"));
		
		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		String expectedBuildNumber = "747463";
		String expectedNumericRevision = "14322";
		
		doEchoTest(expectedBuildNumber, "1.4.32.2", expectedNumericRevision, "tags/5.4", "", "", "", "");
	}

	public void testSetPropertyNoValueOrNoEquals() throws Exception {
		dotNetProjectConfig.setAntProperties(
				new String[] {"Foo", "Bar="});

		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		doEchoTest("", "", "", "", "", "", "", "");
	}
	
	public void testSetGlobalProperty() throws Exception {
		globalConfig.setProperties(new String[] {"Foo=321"});
		
		dotNetProjectConfig.setAntProperties(
				new String[] {"Bar=baz"});

		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		doEchoTest("", "", "", "", "", "", "321", "baz");
	}

	public void testOverrideGlobalProperty() throws Exception {
		globalConfig.setProperties(new String[] {"Foo=321"});
		
		dotNetProjectConfig.setAntProperties(
				new String[] {"Foo=abc"});

		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		doEchoTest("", "", "", "", "", "", "abc", "");
	}
	
	public void testSetTargetFramework() throws Exception {
		dotNetProjectConfig.setTargetFrameworkVersion("v2.0");
	
		doEchoTest("", "", "", "", "", "v2.0", "", "");
	}
	
	public void testSetConfiguration() throws Exception {
		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Debug);
	
		doEchoTest("", "", "", "", "Debug", "", "", "");
	}
	
	public void testSetConfigurationInherits() throws Exception {
		globalConfig.setBuildConfiguration(DotNetGlobalConfigDto.GlobalBuildConfiguration.Release);
		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Inherit);
	
		doEchoTest("", "", "", "", "Release", "", "", "");
	}
	
	public void testSetConfigurationOverrideUnspecified() throws Exception {
		globalConfig.setBuildConfiguration(DotNetGlobalConfigDto.GlobalBuildConfiguration.Release);
		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		doEchoTest("", "", "", "", "", "", "", "");
	}
	
	public void testSetConfigurationOverride() throws Exception {
		globalConfig.setBuildConfiguration(DotNetGlobalConfigDto.GlobalBuildConfiguration.Release);
		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Debug);
	
		doEchoTest("", "", "", "", "Debug", "", "", "");
	}
	
	public void testBadExecPath() throws Exception {
		buildEnv.setLocation("msbuild.none.such.program");
		
		try {
			tool.buildProject(projectConfig, status, buildLog, detailCallback);
			fail("expected exception");
		} catch (ConfigException e) {
			assertEquals("ant.exec.failure", e.getKey());
		}
	}
}
