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
import java.util.List;
import java.util.regex.Pattern;

import net.sourceforge.vulcan.ant.JavaCommandBuilder;
import net.sourceforge.vulcan.dto.ProjectStatusDto;

public class Maven2BuildToolTest extends MavenBuildToolTestBase {
	ProjectStatusDto status = new ProjectStatusDto();
	
	public Maven2BuildToolTest() {
		super(true);
		
		status.setBuildNumber(1);
	}
	
	public void testGetLauncherJar() throws Exception {
		final File launcher = MavenBuildTool.getMavenHomeLibrary(maven2Home, MavenBuildTool.MAVEN2_LAUNCHER_PATH_PREFIX);
		
		assertTrue(launcher.canRead());
		
		assertTrue(launcher.getName() + " does not match ^classworlds.*jar$",
				Pattern.compile("^classworlds.*jar$").matcher(launcher.getName()).matches());
		
		assertEquals("boot", launcher.getParentFile().getName());
	}
	
	public void testConfigure() throws Exception {
		tool.configure();
		
		assertEquals(MavenBuildTool.MAVEN2_LAUNCHER_MAIN_CLASS_NAME, tool.getMainClass());
	}
	
	public void testCreateJavaCommand() throws Exception {
		tool.configure();
		
		final JavaCommandBuilder cmd = tool.createJavaCommand(projectConfig, status, null);
		
		final List<String> cp = cmd.getClassPath();
		assertEquals(1, cp.size());
		assertTrue(cp.get(0).contains("classworlds"));
		
		assertEquals(MavenBuildTool.MAVEN2_LAUNCHER_MAIN_CLASS_NAME, cmd.getMainClassName());
	}
}
