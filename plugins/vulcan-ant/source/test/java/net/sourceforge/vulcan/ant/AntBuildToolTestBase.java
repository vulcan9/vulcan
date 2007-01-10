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
package net.sourceforge.vulcan.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;
import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.dto.ProjectConfigDto;

import org.apache.commons.lang.StringUtils;

abstract class AntBuildToolTestBase extends TestCase {
	AntBuildTool tool;
	AntConfig antConfig;
	AntProjectConfig config;
	ProjectConfigDto projectConfig;
	
	static final String antHome;
	
	static {
		String tmp = System.getProperty("ant.home");
		if (StringUtils.isBlank(tmp)) {
			try {
				final Properties props = new Properties();
				props.load(new FileInputStream(TestUtils.resolveRelativeFile("build.properties")));
				tmp = props.getProperty("ant.home");
			} catch (IOException e) {
				tmp = null;
			}
		}
		antHome = tmp;
		
	}
	@Override
	protected void setUp() throws Exception {
		antConfig = new AntConfig();
		antConfig.setAntHome(antHome);
		config = new AntProjectConfig();
		config.setTargets("clean compile");
		
		tool = new AntBuildTool(config, antConfig);
		
		projectConfig = new ProjectConfigDto();
		projectConfig.setWorkDir(TestUtils.resolveRelativePath("source/test/workdir"));
		
		if (StringUtils.isBlank(antHome) || !(new File(antHome)).isDirectory()) {
			fail("Please define ant.home in build.properties");
		}
		
		// clear any pending interrupts
		Thread.interrupted();
	}
}
