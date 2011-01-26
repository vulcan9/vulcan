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
package net.sourceforge.vulcan.ant;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import junit.framework.TestCase;
import net.sourceforge.vulcan.ant.buildlistener.Constants;
import net.sourceforge.vulcan.ant.buildlistener.UdpBuildEventPublisher;
import net.sourceforge.vulcan.ant.io.ObjectSerializer;
import net.sourceforge.vulcan.ant.receiver.UdpEventSource;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;
import net.sourceforge.vulcan.exception.ConfigException;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

public class AntBuildToolTest extends AntBuildToolTestBase {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		antConfig.setRevisionPropertyName(null);
		antConfig.setTagNamePropertyName(null);
		antConfig.setBuildNumberPropertyName(null);
		antConfig.setBuildSchedulerPropertyName(null);
		antConfig.setBuildUserPropertyName(null);
	}
	
	public void testRecordsMetrics() throws Exception {
		antConfig.setRecordMetrics(true);
		
		final IMocksControl control = EasyMock.createStrictControl();
		
		BuildDetailCallback cb = control.createMock(BuildDetailCallback.class);
		
		cb.addMetric(new MetricDto("vulcan.metrics.build.script", config.getBuildScript(), MetricType.STRING));
		cb.addMetric(new MetricDto("vulcan.metrics.build.targets", config.getTargets(), MetricType.STRING));
		
		control.replay();
		
		tool.recordMetrics(cb);
		
		control.verify();
	}
	
	public void testDoesNotRecordMetricsWhenOptionNotSet() throws Exception {
		antConfig.setRecordMetrics(false);
		
		final IMocksControl control = EasyMock.createStrictControl();
		
		BuildDetailCallback cb = control.createMock(BuildDetailCallback.class);
		
		control.replay();
		
		tool.recordMetrics(cb);
		
		control.verify();
	}

	public void testThrowsOnScriptNotFound() throws Exception {
		config.setBuildScript("no-such-script.xml");
		
		try {
			tool.checkBuildScriptExists(projectConfig);
			fail("expected exception");
		} catch (ConfigException e) {
			assertEquals("ant.script.invalid", e.getKey());
			assertEquals(new File(projectConfig.getWorkDir(), config.getBuildScript()), e.getArgs()[0]);
		}
	}

	public void testDoesNotThrowOnScriptFound() throws Exception {
		config.setBuildScript("build.xml");
		
		tool.checkBuildScriptExists(projectConfig);
	}
	
	public void testGetVirtualMachineExecutable() throws Exception {
		final File program = AntBuildTool.getVirtualMachineExecutable(System.getProperty("java.home"));
		
		assertTrue(program.exists());
		
		assertTrue(program.getName().startsWith("java"));
	}

	public void testCheckVirtualMachineExecutable() throws Exception {
		try {
			AntBuildTool.getVirtualMachineExecutable("a");
			fail();
		} catch (ConfigException e) {
			assertEquals("java.home.invalid", e.getKey());
			assertEquals("a", e.getArgs()[1]);
			assertEquals("bin" + File.separator + "java", e.getArgs()[0]);
		}
	}
	
	public void testGetAntLauncher() throws Exception {
		final File launcher = AntBuildTool.getAntHomeResource(antHome, AntBuildTool.LAUNCHER_PATH);
		
		assertTrue(launcher.canRead());
		assertEquals("ant-launcher.jar", launcher.getName());
		assertEquals("lib", launcher.getParentFile().getName());
	}
	public void testGetAntLauncherNoLibDir() throws Exception {
		final String badHome = antHome + File.separator + "..";
		try {
			AntBuildTool.getAntHomeResource(badHome, AntBuildTool.LAUNCHER_PATH);
			fail();
		} catch (ConfigException e) {
			assertEquals("ant.home.missing.resource", e.getKey());
			assertEquals("lib" + File.separator + "ant-launcher.jar", e.getArgs()[0]);
			assertEquals(badHome, e.getArgs()[1]);
		}
	}
	public void testGetAntLauncherAntHomeBlankOrNull() throws Exception {
		try {
			AntBuildTool.getAntHomeResource("", AntBuildTool.LAUNCHER_PATH);
			fail();
		} catch (ConfigException e) {
			assertEquals("ant.home.required", e.getKey());
		}
		try {
			AntBuildTool.getAntHomeResource(null, AntBuildTool.LAUNCHER_PATH);
			fail();
		} catch (ConfigException e) {
			assertEquals("ant.home.required", e.getKey());
		}
	}
	public void testGetsListenerClassPathFromFile() throws Exception {
		final File path = AntBuildTool.getLocalClassPathEntry(UdpBuildEventPublisher.class, null);
		
		assertTrue("returned path is not a directory", path.isDirectory());
		assertTrue("wrong path: " + path.getCanonicalPath(), 
				path.getCanonicalPath().contains(File.separator + "target" + File.separator));
	}
	public void testGetsListenerClassPathFromJar() throws Exception {
		final File path = AntBuildTool.getLocalClassPathEntry(TestCase.class, null);
		
		assertTrue("wrong path: " + path.getCanonicalPath(), path.getPath().endsWith(".jar"));
		assertTrue(path.exists());
	}
	public void testCreateJavaAntCommand() throws Exception {
		tool.setEventSource(null);
		
		final JavaCommandBuilder expected = createTestCommand();
		
		checkBuilder(expected);
	}
	public void testCreateJavaAntCommandDebug() throws Exception {
		config.setDebug(true);
		tool.setEventSource(null);
		
		final JavaCommandBuilder expected = new JavaCommandBuilder();
		
		expected.addArgument("-debug");

		setDefaults(expected);
		
		checkBuilder(expected);
	}
	public void testCreateJavaAntCommandVerboseWithGlobalAntProp() throws Exception {
		antConfig.setAntProperties(new String[] {"example=bar", "-Dsimple"});
		config.setVerbose(true);
		config.setAntProperties(new String[] {"example=car"});
		tool.setEventSource(null);
		
		final JavaCommandBuilder expected = new JavaCommandBuilder();
		
		expected.addArgument("-Dexample=car");
		expected.addArgument("-Dsimple=");

		expected.addArgument("-verbose");
		
		setDefaults(expected);
		
		checkBuilder(expected);
	}
	public void testCreateJavaAntCommandIgnoresBlankProps() throws Exception {
		antConfig.setAntProperties(new String[] {"example=bar", "-Dsimple"});
		config.setVerbose(true);
		config.setAntProperties(new String[] {""});
		tool.setEventSource(null);
		
		final JavaCommandBuilder expected = new JavaCommandBuilder();
		
		expected.addArgument("-Dexample=bar");
		expected.addArgument("-Dsimple=");

		expected.addArgument("-verbose");
		
		setDefaults(expected);
		
		checkBuilder(expected);
	}
	public void testCreateJavaAntCommandSpecifyJavaHome() throws Exception {
		JavaHome[] homes = new JavaHome[] { new JavaHome(), new JavaHome() };
		homes[0].setDescription("other jdk");
		homes[0].setJavaHome("/fake/other");
		homes[1].setDescription("my jdk");
		homes[1].setJavaHome(System.getProperty("java.home"));
		homes[1].setMaxMemory("123");
		homes[1].setSystemProperties(
				new String[] {"com.example.fun=realFun", "-Dsimple"});

		antConfig.setJavaHomes(homes);
		
		config.setJavaHome("my jdk");
		tool.javaEnvironment = homes[1];
		tool.setEventSource(null);
		
		final JavaCommandBuilder expected = new JavaCommandBuilder();
		expected.setMaxMemoryInMegabytes(123);
		expected.addSystemProperty("com.example.fun", "realFun");
		expected.addSystemProperty("simple", "");
		
		setDefaults(expected);
		
		checkBuilder(expected);
	}
	public void testCreateJavaAntCommandWithListener() throws Exception {
		config.setJavaHome("System (Fake 1.1)");
		
		final InetAddress address = InetAddress.getByAddress(new byte[] {127,0,0,1});
		tool.setEventSource(new UdpEventSource(address, 1234, 0, new ObjectSerializer()));
		
		final JavaCommandBuilder expected = new JavaCommandBuilder();
		
		expected.addArgument("-lib");
		expected.addArgument(AntBuildTool.getLocalClassPathEntry(UdpBuildEventPublisher.class, null).getCanonicalPath());
		expected.addArgument("-listener");
		expected.addArgument("net.sourceforge.vulcan.ant.buildlistener.UdpBuildEventPublisher");

		setDefaults(expected);
		
		expected.addSystemProperty(Constants.HOST_PROPERTY, address.getHostName());
		expected.addSystemProperty(Constants.PORT_PROPERTY, "1234");
		
		checkBuilder(expected);
	}

	private void checkBuilder(final JavaCommandBuilder expected) throws ConfigException {
		final ProjectStatusDto status = new ProjectStatusDto();
		status.setBuildNumber(543);
		
		final JavaCommandBuilder actual = tool.createJavaCommand(projectConfig, status, new File("target/test.ant.log"));

		assertEquals(expected.toString(), actual.toString());
	}

	private JavaCommandBuilder createTestCommand() throws ConfigException, IOException {
		final JavaCommandBuilder expected = new JavaCommandBuilder();
		setDefaults(expected);
		return expected;
	}

	private void setDefaults(final JavaCommandBuilder expected) throws ConfigException, IOException {
		expected.setJavaExecutablePath(AntBuildTool.getVirtualMachineExecutable(System.getProperty("java.home")).getCanonicalPath());
		
		expected.addClassPathEntry(AntBuildTool.getAntHomeResource(antHome, AntBuildTool.LAUNCHER_PATH).getCanonicalPath());
		
		expected.addSystemProperty("ant.home", antHome);
		expected.addSystemProperty("ant.library.dir", antHome + File.separator + "lib");
		
		expected.setMainClassName("org.apache.tools.ant.launch.Launcher");

		expected.addArgument("-noinput");
		expected.addArgument("-logfile");
		expected.addArgument(new File("target/test.ant.log").getCanonicalPath());
		expected.addArgument("-buildfile");
		expected.addArgument(tool.checkBuildScriptExists(projectConfig).getCanonicalPath());
		expected.addArgument("clean");
		expected.addArgument("compile");
	}
}
