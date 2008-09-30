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
package net.sourceforge.vulcan.maven;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.maven.integration.MavenIntegration;
import net.sourceforge.vulcan.maven.integration.MavenProjectConfiguratorImpl;

import org.jdom.input.SAXBuilder;

public class MavenBuildPluginTest extends MavenBuildToolTestBase {
	MavenBuildPlugin plugin = new MavenBuildPlugin();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		plugin.setConfiguration(mavenConfig);
	}
	
	public void testCreateURLs() throws Exception {
		final URL[] urls = MavenBuildPlugin.createURLs(maven2Home);
		
		assertNotNull(urls);
		
		assertTrue(urls.length > 1);
	}
	
	public void testCreateIntegrationSupport() throws Exception {
		plugin.createConfigurationFactory();
		
		assertNotNull(plugin.configuratorFactory);
	}

	public void testFilterPackage() throws Exception {
		final URL[] urls = MavenBuildPlugin.createURLs(maven2Home);
		
		final ClassLoader ldr = new MavenBuildPlugin.PackageFilteringClassLoader(urls, getClass().getClassLoader(), "net.sourceforge.vulcan.maven.integration");
		
		assertNotSame(ldr.loadClass(MavenIntegration.class.getName()), MavenIntegration.class);
		assertSame(ldr.loadClass(MavenBuildPlugin.class.getName()), MavenBuildPlugin.class);
	}
	
	public void testCreateProjectConfigurator() throws Exception {
		final File pomFile = createFakePomFile();
		assertNotNull(plugin.createProjectConfigurator(null, pomFile, new SAXBuilder().build(pomFile)));
	}
	
	public void testProjectConfiguratorModules() throws Exception {
		final File pomFile = TestUtils.resolveRelativeFile("../pom.xml");
		final ProjectBuildConfigurator cfgr = plugin.createProjectConfigurator(
				null, pomFile, new SAXBuilder().build(pomFile));
		
		final List<String> urls = cfgr.getSubprojectUrls();
		
		assertEquals(9, urls.size());
		
		assertEquals("http://vulcan.googlecode.com/svn/trunk/plugins/vulcan-ant/pom.xml", urls.get(0));
		assertEquals(null, cfgr.getRelativePathToProjectBasedir());
	}
	
	public void testProjectConfiguratorModulesNestedParentPom() throws Exception {
		final File pomFile = TestUtils.resolveRelativeFile("source/test/import-test/parent/pom.xml");
		final MavenProjectBuildConfigurator cfgr = plugin.createProjectConfigurator(
				null, pomFile, new SAXBuilder().build(pomFile));
		
		final List<String> urls = cfgr.getSubprojectUrls();
		
		assertEquals(1, urls.size());
		
		assertEquals("pserver:anonymous@example.com:/cvsroot/import-test/module-1/pom.xml", urls.get(0));
	}
	
	public void testProjectConfiguratorBasedirSetOnModuleInParentPath() throws Exception {
		MavenProjectConfiguratorImpl cfgr = new MavenProjectConfiguratorImpl(null, null, null, null);
		
		cfgr.determineBasedir(Arrays.asList("../../module-a", "../module-b"));
		
		assertEquals("../../", cfgr.getRelativePathToProjectBasedir());
	}
	
	public void testProjectConfiguratorModulesGetsScm() throws Exception {
		// first, load the parent pom or else maven will not be able to find it.
		final File parentPomFile = TestUtils.resolveRelativeFile("source/test/import-test/parent/pom.xml");
		final MavenProjectBuildConfigurator pCfgr = plugin.createProjectConfigurator(
				null, parentPomFile, new SAXBuilder().build(parentPomFile));

		assertEquals("pserver:anonymous@example.com:/cvsroot/import-test/parent/", pCfgr.determineScmRootUrl());
		
		// load module pom
		final File pomFile = TestUtils.resolveRelativeFile("source/test/import-test/module-1/pom.xml");
		final MavenProjectBuildConfigurator cfgr = plugin.createProjectConfigurator(
				null, pomFile, new SAXBuilder().build(pomFile));
		
		assertEquals("pserver:anonymous@example.com:/cvsroot/import-test/module-1/", cfgr.determineScmRootUrl());
	}
	
	/* 
	 * Not sure why this is done in the wild; for an example see
	 * http://svn.apache.org/repos/asf/jakarta/commons/proper/collections/trunk/pom.xml@r523782
	 */
	public void testConfiguratorDeletesMultipleScmPrefix() throws Exception {
		final String normal = MavenProjectConfiguratorImpl.normalizeScmUrl("scm:svn:scm:svn:http://localhost/svn/trunk");
		assertEquals("http://localhost/svn/trunk/", normal);
	}
	
	public void testConfigureDependencies() throws Exception {
		final File pomFile = TestUtils.resolveRelativeFile("pom.xml");
		final ProjectBuildConfigurator cfgr = plugin.createProjectConfigurator(
				null, pomFile, new SAXBuilder().build(pomFile));

		cfgr.applyConfiguration(projectConfig, null, Arrays.asList("vulcan-core", "vulcan-test-utils"), false);
		
		assertEquals(2, projectConfig.getDependencies().length);
	}
	
	public void testConfigureDependencyOnAncestors() throws Exception {
		final File pomFile = TestUtils.resolveRelativeFile("pom.xml");
		
		final ProjectBuildConfigurator cfgr = plugin.createProjectConfigurator(
				null, pomFile, new SAXBuilder().build(pomFile));

		cfgr.applyConfiguration(projectConfig, null, Arrays.asList("plugins", "vulcan"), false);
		
		assertEquals(2, projectConfig.getDependencies().length);
	}

	public void testConfigureDependencyOnPlugins() throws Exception {
		final File pomFile = TestUtils.resolveRelativeFile("pom.xml");
		
		final ProjectBuildConfigurator cfgr = plugin.createProjectConfigurator(
				null, pomFile, new SAXBuilder().build(pomFile));

		cfgr.applyConfiguration(projectConfig, null, Arrays.asList("vulcan-maven-plugin"), false);
		
		assertEquals(1, projectConfig.getDependencies().length);
	}
}
