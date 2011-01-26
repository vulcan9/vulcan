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
import java.util.Arrays;

import junit.framework.TestCase;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.dotnet.dto.DotNetBuildEnvironmentDto;
import net.sourceforge.vulcan.dotnet.dto.DotNetProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;

import org.easymock.EasyMock;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

public class DotNetBuildPluginTest extends TestCase {
	PluginManager pluginManager = EasyMock.createMock(PluginManager.class);
	
	DotNetBuildPlugin plugin = new DotNetBuildPlugin();
	DotNetProjectConfigDto projectConfig = new DotNetProjectConfigDto();
	ProjectConfigDto cfg = new ProjectConfigDto();
	
	Document xmlDocument = new Document();
	Element msbuildProjectElement = new Element("Project", Namespace.getNamespace("http://schemas.microsoft.com/developer/msbuild/2003"));
	Element nantProjectElement = new Element("project", Namespace.getNamespace("http://nant.sf.net/schemas/nant.xsd"));
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		nantProjectElement.setAttribute("name", "nant-project");
		
		plugin.setPluginManager(pluginManager);
		
		EasyMock.expect(pluginManager.getPluginDirectory(DotNetBuildPlugin.PLUGIN_ID))
			.andReturn(new File("."));
		
		DotNetBuildEnvironmentDto msbuildEnv = new DotNetBuildEnvironmentDto();
		msbuildEnv.setDescription("myMsBuild");
		msbuildEnv.setType(DotNetBuildEnvironmentDto.DotNetEnvironmentType.MSBuild);
		
		DotNetBuildEnvironmentDto nantEnv = new DotNetBuildEnvironmentDto();
		nantEnv.setDescription("myNantBuild");
		nantEnv.setType(DotNetBuildEnvironmentDto.DotNetEnvironmentType.NAnt);
		
		DotNetBuildEnvironmentDto[] envs = { msbuildEnv, nantEnv };
		
		plugin.getConfiguration().setBuildEnvironments(envs);
	}
	
	public void testDefaultConfig() throws Exception {
		assertNotNull(plugin.getConfiguration());
	}
	public void testNoEnvironmentSelected() throws Exception {
		try {
			plugin.createInstance(projectConfig);
			fail("expected exception");
		} catch (ConfigException e) {
			assertEquals("dotnet.config.no.environment", e.getKey());
		}
	}
	public void testEnvironmentNotFound() throws Exception {
		projectConfig.setBuildEnvironment("nonesuch");
		try {
			plugin.createInstance(projectConfig);
			fail("expected exception");
		} catch (ConfigException e) {
			assertEquals("dotnet.config.environment.not.available", e.getKey());
			assertEquals("nonesuch", e.getArgs()[0]);
		}
	}
	public void testNAntUnsupported() throws Exception {
		projectConfig.setBuildEnvironment("myNantBuild");
		
		assertNotNull(plugin.createInstance(projectConfig));
	}
	public void testCreateConfiguratorForProjFile() throws Exception {
		xmlDocument.addContent(msbuildProjectElement);
		
		final DotNetProjectConfigurator cfgr = (DotNetProjectConfigurator) 
			plugin.createProjectConfigurator(":pserver:anon@localhost:/cvsroot:module/subdir/Foo.Bar.Baz.csproj", null, xmlDocument);
		
		assertNotNull(cfgr);
		assertTrue(cfgr.shouldCreate());
		assertEquals("myMsBuild", cfgr.getBuildEnvironment());
	}
	public void testCreateConfiguratorForSln() throws Exception {
		File buildSpecFile = TestUtils.resolveRelativeFile("source/test/test-solutions/simple.sln");
		
		final DotNetProjectConfigurator cfgr = (DotNetProjectConfigurator)
			plugin.createProjectConfigurator(
				"http://localhost/svn/simple.sln", buildSpecFile, null);
		
		assertNotNull(cfgr);
		
		assertEquals(".", cfgr.getRelativePathToProjectBasedir());
		assertEquals(
				Arrays.asList(
						"Com.Example.Project/Com.Example.Project.csproj",
						"Com.Example.Project.Tests/Com.Example.Project.Tests.csproj"),
				cfgr.getSubprojectUrls());
		assertFalse(cfgr.shouldCreate());
	}
	public void testCreateConfiguratorForSlnNested() throws Exception {
		File buildSpecFile = TestUtils.resolveRelativeFile("source/test/test-solutions/nested.sln");
		
		final DotNetProjectConfigurator cfgr = (DotNetProjectConfigurator)
			plugin.createProjectConfigurator(
				"http://localhost/svn/project/data/solutions/nested.sln", buildSpecFile, null);
		
		assertNotNull(cfgr);
		
		assertEquals("../../", cfgr.getRelativePathToProjectBasedir());
		assertEquals(
				Arrays.asList(
						"../Com.Example.Project/Com.Example.Project.csproj",
						"../../Tests/Com.Example.Project.Tests/Com.Example.Project.Tests.csproj"),
				cfgr.getSubprojectUrls());
		assertFalse(cfgr.shouldCreate());
	}
	public void testCreateConfiguratorForNant() throws Exception {
		xmlDocument.addContent(nantProjectElement);
		
		final DotNetProjectConfigurator cfgr = (DotNetProjectConfigurator) 
			plugin.createProjectConfigurator(":pserver:anon@localhost:/cvsroot:module/subdir/nant.build", null, xmlDocument);
		
		assertNotNull(cfgr);
		assertTrue(cfgr.shouldCreate());
		assertEquals("nant-project", cfgr.getDeclaredProjectName());
		assertEquals("myNantBuild", cfgr.getBuildEnvironment());
	}

}
