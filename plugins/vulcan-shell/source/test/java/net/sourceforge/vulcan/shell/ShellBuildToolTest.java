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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.io.FileUtils;

public class ShellBuildToolTest extends EasyMockTestCase {
	int exitCode;
	boolean interrupt;
	boolean destroyed;
	
	File logFile;
	
	String stdout = "";
	String stderr = "";
	
	Process fakeProcess = new Process() {
		@Override
		public int waitFor() throws InterruptedException {
			if (interrupt) {
				throw new InterruptedException();
			}
			
			return exitCode;
		}

		@Override
		public void destroy() {
			destroyed = true;
		}

		@Override
		public int exitValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		public InputStream getErrorStream() {
			return new ByteArrayInputStream(stderr.getBytes());
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(stdout.getBytes());
		}

		@Override
		public OutputStream getOutputStream() {
			throw new UnsupportedOperationException();
		}
	};
	Map<String, String> fakeEnv = new HashMap<String, String>();
	
	ShellBuildTool tool = new ShellBuildTool() {
		@Override
		protected Process execute(String[] arguments, String[] environment,
				File dir) throws IOException {
			return fakeProcess;
		}
		@Override
		protected Map<String, String> getCurrentEnvironment() {
			return fakeEnv;
		}
	};
	
	ShellProjectConfig projectPluginConfig = new ShellProjectConfig();
	ShellBuildToolConfig globalConfig = new ShellBuildToolConfig();
	
	ProjectConfigDto projectConfig = new ProjectConfigDto();
	ProjectStatusDto status = new ProjectStatusDto();
	
	BuildDetailCallback buildDetailCallback;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		tool.setGlobalConfig(globalConfig);
		tool.setProjectPluginConfig(projectPluginConfig);
		
		globalConfig.setBuildNumberVariableName("");
		globalConfig.setRevisionVariableName("");
		globalConfig.setNumericRevisionVariableName("");
		globalConfig.setTagNameVariableName("");
		
		projectConfig.setWorkDir(".");
		status.setBuildNumber(0);
		status.setRevision(new RevisionTokenDto(0l));
		
		logFile = File.createTempFile("vulcan-shell-log", ".txt");
		logFile.deleteOnExit();
		
		buildDetailCallback = createMock(BuildDetailCallback.class);
	}
	
	public void testCreateEnvironmentEmpty() throws Exception {
		String[] env = tool.CreateEnvironment(status);
		
		assertEquals(0, env.length);
	}
	
	public void testInheritsEnvironment() throws Exception {
		fakeEnv.put("USER", "somebody");

		String[] env = tool.CreateEnvironment(status);
		
		assertEquals(1, env.length);
		assertEquals("USER=somebody", env[0]);
	}
	
	public void testOverrideInheritedVar() throws Exception {
		fakeEnv.put("USER", "somebody");
		globalConfig.setEnvironmentVariables(new String[] {"USER=otherbody"});
		
		String[] env = tool.CreateEnvironment(status);
		
		assertEquals(1, env.length);
		assertEquals("USER=otherbody", env[0]);
	}

	public void testProjectOverideGlobalVar() throws Exception {
		globalConfig.setEnvironmentVariables(new String[] {"USER=otherbody"});
		projectPluginConfig.setEnvironmentVariables(new String[] {"USER=projectbody"});
		
		String[] env = tool.CreateEnvironment(status);
		
		assertEquals(1, env.length);
		assertEquals("USER=projectbody", env[0]);
	}
	
	public void testTrimsEnvVar() throws Exception {
		projectPluginConfig.setEnvironmentVariables(new String[] {"	USER =	projectbody 	"});
		
		String[] env = tool.CreateEnvironment(status);
		
		assertEquals(1, env.length);
		assertEquals("USER=projectbody", env[0]);
	}
	
	public void testRemovesOnMissingEquals() throws Exception {
		fakeEnv.put("MISSING_EQUALS", "a value that should be supressed");
		projectPluginConfig.setEnvironmentVariables(new String[] {"MISSING_EQUALS"});

		String[] env = tool.CreateEnvironment(status);
		
		assertEquals(0, env.length);
	}

	public void testRemovesOnEmptyValue() throws Exception {
		fakeEnv.put("EMPTY_VALUE", "a value that should be overridden");
		projectPluginConfig.setEnvironmentVariables(new String[] {"EMPTY_VALUE="});

		String[] env = tool.CreateEnvironment(status);
		
		assertEquals(0, env.length);
	}
	
	public void testAddsBuildInfo() throws Exception {
		globalConfig.setBuildNumberVariableName("B");
		globalConfig.setNumericRevisionVariableName("N");
		globalConfig.setRevisionVariableName("R");
		globalConfig.setTagNameVariableName("T");
		globalConfig.setBuildUserVariableName("BU");
		globalConfig.setBuildSchedulerVariableName("BS");
		
		status.setBuildNumber(4231);
		status.setRevision(new RevisionTokenDto(7655l, "xyz.7655"));
		status.setTagName("b2.2");
		status.setRequestedBy("Kate");
		
		String[] env = tool.CreateEnvironment(status);
		
		Arrays.sort(env);
		
		assertEquals(5, env.length);
		assertEquals("B=4231", env[0]);
		assertEquals("BU=Kate", env[1]);
		assertEquals("N=7655", env[2]);
		assertEquals("R=xyz.7655", env[3]);
		assertEquals("T=b2.2", env[4]);
	}

	public void testAddsBuildInfoScheduledBuild() throws Exception {
		globalConfig.setBuildUserVariableName("BU");
		globalConfig.setBuildSchedulerVariableName("BS");
		
		status.setBuildNumber(4231);
		status.setRevision(new RevisionTokenDto(7655l, "xyz.7655"));
		status.setTagName("b2.2");
		status.setRequestedBy("Katebot");
		status.setScheduledBuild(true);
		
		String[] env = tool.CreateEnvironment(status);
		
		Arrays.sort(env);
		
		assertEquals(1, env.length);
		assertEquals("BS=Katebot", env[0]);
	}
	
	public void testExecuteNoArgsThrows() throws Exception {
		projectPluginConfig.setArguments(new String[0]);
		
		try {
			tool.buildProject(projectConfig, status, null, null);	
		} catch (ConfigException e) {
			assertEquals("shell.missing.arguments", e.getKey());
		}
	}

	public void testChecksExitCode() throws Exception {
		exitCode = 32;
		projectPluginConfig.setArguments(new String[] {"make"});
		
		try {
			tool.buildProject(projectConfig, status, logFile, null);
			fail("expected exception");
		} catch (BuildFailedException e) {
			assertEquals(32, e.getExitCode());
		}
	}

	public void testCancel() throws Exception {
		interrupt = true;
		projectPluginConfig.setArguments(new String[] {"make"});
		
		tool.buildProject(projectConfig, status, logFile, null);
		
		assertTrue(destroyed);
	}

	public void testExecuteSuccessful() throws Exception {
		projectPluginConfig.setArguments(new String[] {"make"});
	
		stdout = "success\nfinished in 0.00 seconds.\n";
		
		tool.buildProject(projectConfig, status, logFile, null);	
		
		assertEquals(stdout, FileUtils.readFileToString(logFile, "utf-8"));
	}
	
	public void testSetsDetail() throws Exception {
		buildDetailCallback.setDetail("foo");
		buildDetailCallback.setDetail("bar");
		
		replay();
		
		projectPluginConfig.setArguments(new String[] {"make"});
	
		stdout = "make  foo\nmake  bar\n";
		
		tool.buildProject(projectConfig, status, logFile, buildDetailCallback);	
		
		verify();
		
		assertEquals(stdout, FileUtils.readFileToString(logFile, "utf-8"));
	}

	public void testRecordsErrorsAndWarnings() throws Exception {
		buildDetailCallback.reportWarning("variable `foo' is never read", "file.c", 107, null);
		buildDetailCallback.reportError("unknown reference `bar'", "dir.c", 7, null);
		
		replay();
		
		projectPluginConfig.setArguments(new String[] {"make"});
	
		stderr =	"file.c:107: warning: variable `foo' is never read\n" +
					"dir.c:7: error: unknown reference `bar'\n";
		
		tool.buildProject(projectConfig, status, logFile, buildDetailCallback);	
		
		verify();
		
		assertEquals(stderr, FileUtils.readFileToString(logFile, "utf-8"));
	}
}
