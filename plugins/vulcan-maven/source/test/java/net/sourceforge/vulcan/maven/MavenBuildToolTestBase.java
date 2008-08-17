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
package net.sourceforge.vulcan.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;
import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

public abstract class MavenBuildToolTestBase extends TestCase {
	MavenBuildTool tool;
	MavenConfig mavenConfig;
	MavenProjectConfig config;
	ProjectConfigDto projectConfig;
	
	static final String mavenHome;
	static final String maven2Home;
	
	static {
		String tmp1 = System.getProperty("maven1.home");
		String tmp2 = System.getProperty("maven2.home");

		if (tmp1 == null || tmp2 == null) {
			try {
				final Properties props = new Properties();
				props.load(new FileInputStream(TestUtils.resolveRelativeFile("build.properties")));
				tmp1 = props.getProperty("maven1.home");
				tmp2 = props.getProperty("maven2.home");
			} catch (IOException e) {
				tmp1 = null;
				tmp2 = null;
			}
		}
		mavenHome = tmp1;
		maven2Home = tmp2;
	}

	protected static class TokenException extends RuntimeException {};
	protected File foreheadFile;

	private final boolean useMaven2;
	
	public MavenBuildToolTestBase() {
		this(false);
	}
	
	public MavenBuildToolTestBase(boolean useMaven2) {
		this.useMaven2 = useMaven2;
	}

	@Override
	protected void setUp() throws Exception {
		mavenConfig = new MavenConfig();

		final MavenHome[] homes = new MavenHome[] {
					new MavenHome(),
					new MavenHome()
				};
		
		homes[0].setDescription("maven one-x");
		homes[0].setDirectory(mavenHome);
		homes[1].setDescription("maven two");
		homes[1].setDirectory(maven2Home);
		
		mavenConfig.setMavenHomes(homes);
		
		config = new MavenProjectConfig();
		config.setTargets("clean");
		config.setBuildScript("");
		
		int index = 0;
		
		if (useMaven2) {
			index = 1;
		}
		
		tool = new MavenBuildTool(config, mavenConfig, null, mavenConfig.getMavenHomes()[index]) {
			@Override
			protected void buildProjectInternal(ProjectConfigDto projectConfig, ProjectStatusDto status, File logFile, BuildDetailCallback callback) throws BuildFailedException, ConfigException {
				MavenBuildToolTestBase.this.foreheadFile = this.launchConfigFile;
				assertTrue(launchConfigFile.exists());
				assertTrue(launchConfigFile.length() > 0);
				throw new TokenException();
			}
		};
		
		projectConfig = new ProjectConfigDto();
		projectConfig.setWorkDir(TestUtils.resolveRelativePath("source/test/workdir"));
		
		if (StringUtils.isBlank(mavenHome) || !(new File(mavenHome)).isDirectory()) {
			fail("Please define maven1.home and maven2.home in build.properties");
		}
		
		// clear any pending interrupts
		Thread.interrupted();
	}
	
	protected File createFakePomFile() throws Exception {
		File file = File.createTempFile("vulcan-maven-test-pom", ".xml");
		
		file.deleteOnExit();
	
		FileUtils.writeStringToFile(file, createFakePomContents(), "UTF-8");

		return file;
	}

	protected String createFakePomContents() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("<?xml version='1.0' encoding='UTF-8'?>" +
						"<project xmlns='http://maven.apache.org/POM/4.0.0' " +
						"xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " + 
						"xsi:schemaLocation='http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd'>" +
						"<modelVersion>4.0.0</modelVersion>");
		
		sb.append("<packaging>").append("pom").append("</packaging>");
		sb.append("<groupId>").append("com.example").append("</groupId>");
		sb.append("<artifactId>").append("super-tool").append("</artifactId>");
		sb.append("<version>").append("1.0").append("</version>");

		sb.append("<modules>")
			.append("<module>").append("foo").append("</module>")
		.append("</modules>");
		
		sb.append("</project>");
		return sb.toString();
	}
}
