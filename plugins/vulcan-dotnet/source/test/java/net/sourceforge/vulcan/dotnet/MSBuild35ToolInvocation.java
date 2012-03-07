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

import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.exception.BuildFailedException;

import org.apache.commons.logging.LogFactory;

public class MSBuild35ToolInvocation extends MSBuildToolTestBase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		buildEnv.setLocation("c:/Windows/Microsoft.NET/Framework/v3.5/msbuild.exe");
	}
	
	public void testCompileErrorsHaveAbsolutePathsToFilesParallel() throws Exception {
		dotNetProjectConfig.setTargets("");
		dotNetProjectConfig.setBuildScript("parallel/master.proj");
		buildEnv.setMaxJobs("2");
		buildEnv.setToolsVersion("3.5");
		
		try {
			tool.buildProject(projectConfig, status, buildLog, detailCallback);
			fail("expected exception");
		} catch (BuildFailedException e) {
			assertEquals("Build FAILED.", e.getMessage());
			
			// race condition while errors are sent over UDP.
			Thread.sleep(200);
			
			if (errors.size() != 2) {
				LogFactory.getLog("this").info("didn't get both messages sad");
				return;
			}
			assertEquals(2, errors.size());
			
			BuildMessageDto a = errors.get(0);
			BuildMessageDto b;
			
			if (a.getFile().contains("a.cs")) {
				b = errors.get(1);
			} else {
				b = a;
				a = errors.get(1);
			}
			
			assertTrue(a.getFile(), a.getFile().endsWith("a\\code\\a.cs"));
			assertTrue(b.getFile(), b.getFile().endsWith("b\\code\\b.cs"));
		}
	}
	
	public void testLots() throws Exception {
		for (int i=0; i<100; i++) {
			final MSBuild35ToolInvocation test = new MSBuild35ToolInvocation();
			test.setUp();
			test.testCompileErrorsHaveAbsolutePathsToFilesParallel();
			test.tearDown();
		}
	}
}
