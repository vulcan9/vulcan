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
package net.sourceforge.vulcan.maven;

import java.io.File;
import java.io.IOException;

import net.sourceforge.vulcan.ant.AntBuildTool;
import net.sourceforge.vulcan.ant.JavaCommandBuilder;
import net.sourceforge.vulcan.ant.buildlistener.UdpBuildEventPublisher;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.maven.VulcanMavenExtensionsMarker;

public class MavenBuildToolTest extends MavenBuildToolTestBase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		config.setOffline(false);
	}
	
	public void testGetForeheadJar() throws Exception {
		final File launcher = MavenBuildTool.getMavenHomeLibrary(mavenHome, MavenBuildTool.MAVEN1_LAUNCHER_PATH_PREFIX);
		
		assertTrue(launcher.canRead());
		assertEquals("forehead-1.0-beta-5.jar", launcher.getName());
		assertEquals("lib", launcher.getParentFile().getName());
	}
	public void testGetLauncherNoLibDir() throws Exception {
		final String badHome = mavenHome + File.separator + "..";
		try {
			MavenBuildTool.getMavenHomeLibrary(badHome, MavenBuildTool.MAVEN1_LAUNCHER_PATH_PREFIX);
			fail();
		} catch (ConfigException e) {
			assertEquals("maven.home.missing.resource", e.getKey());
			assertEquals(MavenBuildTool.MAVEN1_LAUNCHER_PATH_PREFIX, e.getArgs()[0]);
			assertEquals(badHome, e.getArgs()[1]);
		}
	}
	public void testGetLauncherHomeBlankOrNull() throws Exception {
		try {
			MavenBuildTool.getMavenHomeLibrary("", MavenBuildTool.MAVEN1_LAUNCHER_PATH_PREFIX);
			fail();
		} catch (ConfigException e) {
			assertEquals("maven.home.required", e.getKey());
		}
		try {
			MavenBuildTool.getMavenHomeLibrary(null, MavenBuildTool.MAVEN1_LAUNCHER_PATH_PREFIX);
			fail();
		} catch (ConfigException e) {
			assertEquals("maven.home.required", e.getKey());
		}
	}
	public void testCreateJavaAntCommand() throws Exception {
		tool.setEventSource(null);
		tool.configure();
		
		final JavaCommandBuilder expected = createTestCommand();
		
		checkBuilder(expected);
	}
	public void testCreateJavaAntCommandDebugWithGlobalAntProp() throws Exception {
		mavenConfig.setAntProperties(new String[] {"example=bar", "-Dsimple"});
		config.setAntProperties(new String[] {"example=car"});
		config.setDebug(true);
		projectConfig.setScheduledBuild(true);
		tool.setEventSource(null);
		tool.configure();
		
		final JavaCommandBuilder expected = new JavaCommandBuilder();
		
		expected.addArgument("--debug");
		expected.addArgument("-Dexample=car");
		expected.addArgument("-Dproject.build.number=12");
		expected.addArgument("-Dproject.build.scheduler=");
		expected.addArgument("-Dsimple=");

		setDefaults(expected, false);
		
		checkBuilder(expected);
	}
	public void testBuildCreatesThenDeletesTmpFile() throws Exception {
		try {
			tool.buildProject(projectConfig, new ProjectStatusDto(), null, null);
			fail("expected exception");
		} catch (TokenException e) {
		}
		assertFalse(foreheadFile.exists());
	}
	public void testGetToolsJar() throws Exception {
		final File tools = MavenBuildTool.getJdkToolsJar(System.getProperty("java.home"));

		assertTrue(tools.exists());
	}
	private void checkBuilder(final JavaCommandBuilder expected) throws ConfigException {
		final ProjectStatusDto status = new ProjectStatusDto();
		status.setBuildNumber(12);
		status.setScheduledBuild(projectConfig.isScheduledBuild());
		final JavaCommandBuilder actual = tool.createJavaCommand(projectConfig, status, new File("target/test.ant.log"));

		assertEquals(expected.toString(), actual.toString());
	}
	private JavaCommandBuilder createTestCommand() throws ConfigException, IOException {
		final JavaCommandBuilder expected = new JavaCommandBuilder();
		setDefaults(expected, true);
		return expected;
	}
	private void setDefaults(final JavaCommandBuilder expected, boolean includeBuildNumberAndBuildUser) throws ConfigException, IOException {
		tool.launchConfigFile = File.createTempFile("foo", "bar");
		tool.launchConfigFile.deleteOnExit();
		
		final String javaHome = System.getProperty("java.home");
		expected.setJavaExecutablePath(AntBuildTool.getVirtualMachineExecutable(javaHome).getCanonicalPath());
		
		expected.addClassPathEntry(MavenBuildTool.getMavenHomeLibrary(mavenHome, MavenBuildTool.MAVEN1_LAUNCHER_PATH_PREFIX).getCanonicalPath());

		expected.addSystemProperty("vulcan-ant.jar", 
				AntBuildTool.getLocalClassPathEntry(
						UdpBuildEventPublisher.class, null)
							.getCanonicalPath());
		expected.addSystemProperty("vulcan-maven.jar", 
				AntBuildTool.getLocalClassPathEntry(
						VulcanMavenExtensionsMarker.class, null)
							.getCanonicalPath());
		
		expected.addSystemProperty("javax.xml.parsers.SAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl");
		expected.addSystemProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
		expected.addSystemProperty("java.endorsed.dirs",
				MavenBuildTool.getEndorsedClassPath(javaHome, mavenHome));
		expected.addSystemProperty("forehead.conf.file",
				tool.launchConfigFile.getCanonicalPath());
		
		expected.addSystemProperty("tools.jar", MavenBuildTool
				.getJdkToolsJar(javaHome).getCanonicalPath());
		expected.addSystemProperty("maven.home", mavenHome);
		
		expected.setMainClassName(MavenBuildTool.MAVEN1_LAUNCHER_MAIN_CLASS_NAME);
		
		if (includeBuildNumberAndBuildUser) {
			expected.addArgument("-Dproject.build.number=12");
			expected.addArgument("-Dproject.build.user=");
		}
		
		
		expected.addArgument("clean");
	}
}
