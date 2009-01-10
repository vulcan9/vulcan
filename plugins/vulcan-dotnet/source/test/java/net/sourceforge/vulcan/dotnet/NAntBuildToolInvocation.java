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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dotnet.dto.DotNetBuildEnvironmentDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetGlobalConfigDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetProjectConfigDto;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public class NAntBuildToolInvocation extends TestCase {
	DotNetBuildToolBase tool;
	
	DotNetProjectConfigDto dotNetProjectConfig = new DotNetProjectConfigDto();
	
	ProjectConfigDto projectConfig = new ProjectConfigDto();
	
	ProjectStatusDto status = new ProjectStatusDto();
	File buildLog;
	
	List<String> targets = new ArrayList<String>();
	
	List<BuildMessageDto> errors = new ArrayList<BuildMessageDto>();
	List<BuildMessageDto> warnings = new ArrayList<BuildMessageDto>();
	
	DotNetGlobalConfigDto globalConfig = new DotNetGlobalConfigDto();
	DotNetBuildEnvironmentDto buildEnv = new DotNetBuildEnvironmentDto();
	
	BuildDetailCallback detailCallback = new BuildDetailCallback() {
		public void setDetail(String detail) {
			targets.add(detail);
		}
		public void setDetailMessage(String messageKey, Object[] args) {
			fail("should not call this method");
		}
		public void setPhaseMessageKey(String phase) {
			fail("should not call this method");
		}
		public void reportError(String message, String file, Integer lineNumber, String code) {
			errors.add(new BuildMessageDto(message, file, lineNumber, code));
		}
		public void reportWarning(String message, String file, Integer lineNumber, String code) {
			warnings.add(new BuildMessageDto(message, file, lineNumber, code));
		}
		public void addMetric(MetricDto metric) {
		}
	};

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		buildEnv.setLocation("NAnt.exe");
		buildEnv.setType(DotNetBuildEnvironmentDto.DotNetEnvironmentType.NAnt);
		
		projectConfig.setWorkDir(TestUtils.resolveRelativePath("source/test/nant-workdir"));
		
		tool = new NAntBuildTool(globalConfig, dotNetProjectConfig, buildEnv, new File("target/assemblies"));
		
		buildLog = File.createTempFile("vulcan-dotnet-unit-test", ".txt");
		buildLog.delete();
		
		buildLog.deleteOnExit();
		
		globalConfig.setBuildNumberProperty("");
		globalConfig.setRevisionProperty("");
		globalConfig.setNumericRevisionProperty("");
		globalConfig.setTagProperty("");
	}
	
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
		dotNetProjectConfig.setTargets("fail");
		
		try {
			tool.buildProject(projectConfig, status, buildLog, detailCallback);
			fail("expected exception");
		} catch (BuildFailedException e) {
			assertEquals("fail", e.getTarget());
			assertTrue(e.getMessage().endsWith("Failed because you asked me to."));
			
			assertEquals(2, targets.size());
			
			assertEquals("fail", targets.get(0));
			assertEquals(null, targets.get(1));
		}
	}
	
	public void testSyntaxErrors() throws Exception {
		dotNetProjectConfig.setTargets("compile-syntax-errors");
		
		try {
			tool.buildProject(projectConfig, status, buildLog, detailCallback);
			fail("expected exception");
		} catch (BuildFailedException e) {
		}
		assertEquals(1, errors.size());
		assertEquals(1, warnings.size());

		final BuildMessageDto err = errors.get(0);
		assertTrue(err.getFile().endsWith("SyntaxErrors.cs"));
		assertTrue(new File(err.getFile()).isAbsolute());
		assertTrue(StringUtils.isNotBlank(err.getCode()));
		assertEquals((Integer)5, err.getLineNumber());
		
		final BuildMessageDto warn = warnings.get(0);
		assertTrue(warn.getFile().endsWith("SyntaxErrors.cs"));
		assertTrue(new File(warn.getFile()).isAbsolute());
		assertEquals("CS0219", warn.getCode());
		assertEquals((Integer)10, warn.getLineNumber());
	}
	public void testFailsOnManyProjectFilesNoneSpecified() throws Exception {
		final File otherFile = new File(projectConfig.getWorkDir(), "other.build");
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
		final File otherFile = new File(projectConfig.getWorkDir(), "other.build");
		FileUtils.touch(otherFile);

		dotNetProjectConfig.setBuildScript("nant.build");

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
	
		doEchoTest("", "", "", "", "", "net-2.0", "", "", "123", "baz");
	}

	public void testSetsDefinedProperties() throws Exception {
		globalConfig.setBuildNumberProperty("BuildNumber");
		globalConfig.setRevisionProperty("ProjectRevision");
		globalConfig.setNumericRevisionProperty("RepositoryRevision");
		globalConfig.setTagProperty("ProjectTag");
		globalConfig.setSchedulerProperty("BuildScheduler");
		globalConfig.setBuildUsernameProperty("BuildUser");
		
		status.setBuildNumber(747463);
		status.setTagName("tags/5.4");
		status.setRevision(new RevisionTokenDto(14322l, "1.4.32.2"));
		status.setRequestedBy("Night Time");
		status.setScheduledBuild(true);
		
		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		String expectedBuildNumber = "747463";
		String expectedNumericRevision = "14322";
		
		doEchoTest(expectedBuildNumber, "1.4.32.2", expectedNumericRevision, "tags/5.4", "", "net-2.0", "Night Time", "", "", "");
	}


	public void testSetsDefinedPropertiesCustomBuild() throws Exception {
		globalConfig.setBuildNumberProperty("BuildNumber");
		globalConfig.setRevisionProperty("ProjectRevision");
		globalConfig.setNumericRevisionProperty("RepositoryRevision");
		globalConfig.setTagProperty("ProjectTag");
		globalConfig.setSchedulerProperty("BuildScheduler");
		globalConfig.setBuildUsernameProperty("BuildUser");
		
		status.setBuildNumber(747463);
		status.setTagName("tags/5.4");
		status.setRevision(new RevisionTokenDto(14322l, "1.4.32.2"));
		status.setRequestedBy("Mary");
		status.setScheduledBuild(false);

		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		String expectedBuildNumber = "747463";
		String expectedNumericRevision = "14322";
		
		doEchoTest(expectedBuildNumber, "1.4.32.2", expectedNumericRevision, "tags/5.4", "", "net-2.0", "", "Mary", "", "");
	}
	
	public void testSetPropertyNoValueOrNoEquals() throws Exception {
		dotNetProjectConfig.setAntProperties(
				new String[] {"Foo", "Bar="});

		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		doEchoTest("", "", "", "", "", "net-2.0", "", "", "", "");
	}
	
	public void testSetGlobalProperty() throws Exception {
		globalConfig.setProperties(new String[] {"Foo=321"});
		
		dotNetProjectConfig.setAntProperties(
				new String[] {"Bar=baz"});

		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		doEchoTest("", "", "", "", "", "net-2.0", "", "", "321", "baz");
	}

	public void testOverrideGlobalProperty() throws Exception {
		globalConfig.setProperties(new String[] {"Foo=321"});
		
		dotNetProjectConfig.setAntProperties(
				new String[] {"Foo=abc"});

		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		doEchoTest("", "", "", "", "", "net-2.0", "", "", "abc", "");
	}
	
	public void testSetTargetFramework() throws Exception {
		dotNetProjectConfig.setTargetFrameworkVersion("netcf-2.0");
	
		doEchoTest("", "", "", "", "", "netcf-2.0", "", "", "", "");
	}
	
	public void testSetConfiguration() throws Exception {
		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Debug);
	
		doEchoTest("", "", "", "", "Debug", "net-2.0", "", "", "", "");
	}
	
	public void testSetConfigurationInherits() throws Exception {
		globalConfig.setBuildConfiguration(DotNetGlobalConfigDto.GlobalBuildConfiguration.Release);
		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Inherit);
	
		doEchoTest("", "", "", "", "Release", "net-2.0", "", "", "", "");
	}
	
	public void testSetConfigurationOverrideUnspecified() throws Exception {
		globalConfig.setBuildConfiguration(DotNetGlobalConfigDto.GlobalBuildConfiguration.Release);
		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Unspecified);
	
		doEchoTest("", "", "", "", "", "net-2.0", "", "", "", "");
	}
	
	public void testSetConfigurationOverride() throws Exception {
		globalConfig.setBuildConfiguration(DotNetGlobalConfigDto.GlobalBuildConfiguration.Release);
		dotNetProjectConfig.setBuildConfiguration(DotNetProjectConfigDto.BuildConfiguration.Debug);
	
		doEchoTest("", "", "", "", "Debug", "net-2.0", "", "", "", "");
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
	private void doEchoTest(
			String expectedBuildNumber, String expectedRevision,
			String expectedNumericRevision, String expectedTag,
			String expectedConfiguration, String expectedTargetFramework, String expectedScheduler, String expectedBuildUser, String expectedFoo, String expectedBar)
			throws BuildFailedException, ConfigException {
		dotNetProjectConfig.setTargets("echo");
		
		tool.buildProject(projectConfig, status, buildLog, detailCallback);
		
		assertTrue(warnings.size() > 0);
		
		final BuildMessageDto warning = warnings.get(warnings.size() - 1);
		
		assertEquals("Build Number: " + expectedBuildNumber +
				";Revision: " + expectedRevision +
				";NumericRevision: " + expectedNumericRevision +
				";ProjectTag: " + expectedTag +
				";Configuration: " + expectedConfiguration +
				";TargetFramework: " + expectedTargetFramework +
				";Scheduler: " + expectedScheduler +
				";BuildUser: " + expectedBuildUser +
				";Foo: " + expectedFoo +
				";Bar: " + expectedBar, warning.getMessage().replaceAll("\r", "").replaceAll("\n", ";"));
	}
}
