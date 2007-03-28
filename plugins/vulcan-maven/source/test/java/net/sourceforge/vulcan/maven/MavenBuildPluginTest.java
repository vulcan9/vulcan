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

import java.net.URL;
import java.net.URLClassLoader;

import net.sourceforge.vulcan.maven.integration.MavenIntegration;

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
	
	public void testClassLoaderCanSeeClass() throws Exception {
		final URL[] urls = MavenBuildPlugin.createURLs(maven2Home);
		
		final ClassLoader cl = URLClassLoader.newInstance(urls, null);
		
		final Class<?> cls = cl.loadClass(MavenIntegration.class.getName());
		
		assertFalse(cls.equals(MavenIntegration.class));
	}
	
	public void testCreateIntegrationSupport() throws Exception {
		plugin.createConfigurationFactory();
		
		assertNotNull(plugin.configuratorFactory);
	}

	public void testCreateProjectConfigurator() throws Exception {
		assertNotNull(plugin.createProjectConfigurator(createFakePomFile()));
	}
	
	public void testFilterPackage() throws Exception {
		final URL[] urls = MavenBuildPlugin.createURLs(maven2Home);
		
		final ClassLoader ldr = new MavenBuildPlugin.PackageFilteringClassLoader(urls, getClass().getClassLoader(), "net.sourceforge.vulcan.maven.integration");
		
		assertNotSame(ldr.loadClass(MavenIntegration.class.getName()), MavenIntegration.class);
		assertSame(ldr.loadClass(MavenBuildPlugin.class.getName()), MavenBuildPlugin.class);
	}
}
