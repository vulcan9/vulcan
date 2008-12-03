/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2008 Chris Eldredge
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

import net.sourceforge.vulcan.dotnet.dto.DotNetBuildEnvironmentDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetGlobalConfigDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import junit.framework.TestCase;

public abstract class MSBuildToolTestBase extends TestCase {
	protected File buildLog;
	protected MSBuildTool tool;
	protected DotNetProjectConfigDto dotNetProjectConfig = new DotNetProjectConfigDto();
	protected ProjectConfigDto projectConfig = new ProjectConfigDto();
	protected DotNetGlobalConfigDto globalConfig = new DotNetGlobalConfigDto();
	protected DotNetBuildEnvironmentDto buildEnv = new DotNetBuildEnvironmentDto();
	protected ProjectStatusDto status = new ProjectStatusDto();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		buildEnv.setLocation("msbuild.exe");
		
		projectConfig.setWorkDir("source/test/msbuild-workdir");
		
		tool = new MSBuildTool(globalConfig, dotNetProjectConfig, buildEnv, new File("target/vulcan-plugin-includes"));
		
		buildLog = File.createTempFile("vulcan-dotnet-unit-test", ".txt");
		buildLog.delete();
		
		buildLog.deleteOnExit();
		
		globalConfig.setBuildNumberProperty("");
		globalConfig.setRevisionProperty("");
		globalConfig.setNumericRevisionProperty("");
		globalConfig.setTagProperty("");
	
	}

}
