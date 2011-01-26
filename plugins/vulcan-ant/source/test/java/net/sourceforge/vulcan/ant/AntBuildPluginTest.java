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

import junit.framework.TestCase;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

public class AntBuildPluginTest extends TestCase {
	AntBuildPlugin plugin = new AntBuildPlugin();
	Document doc = new Document();
	
	public void testStartsWithSystemJavaHome() throws Exception {
		plugin.init();
		
		final AntConfig cfg = plugin.getConfiguration();
		
		assertEquals(1, cfg.getJavaHomes().length);
		assertEquals(JavaHome.SYSTEM_DESC, cfg.getJavaHomes()[0].getDescription());
		assertEquals(JavaHome.SYSTEM_HOME, cfg.getJavaHomes()[0].getJavaHome());
	}

	public void testInsertsSystemJavaHome() throws Exception {
		JavaHome other = new JavaHome();
		other.setDescription("foo");
		
		AntConfig cfg = new AntConfig();
		cfg.setJavaHomes(new JavaHome[] { other });
		
		plugin.setConfiguration(cfg);
		
		cfg = plugin.getConfiguration();
		
		assertEquals(2, cfg.getJavaHomes().length);
		assertEquals(JavaHome.SYSTEM_DESC, cfg.getJavaHomes()[0].getDescription());
		assertEquals(JavaHome.SYSTEM_HOME, cfg.getJavaHomes()[0].getJavaHome());
	}
	
	public void testUpdatesSystemJavaHome() throws Exception {
		JavaHome other = new JavaHome();
		other.setDescription("System (old vendor old version)");
		other.setJavaHome("/old/invalid/location");
		
		AntConfig cfg = new AntConfig();
		cfg.setJavaHomes(new JavaHome[] { other });
		
		plugin.setConfiguration(cfg);
		
		cfg = plugin.getConfiguration();
		
		assertEquals(1, cfg.getJavaHomes().length);
		assertEquals(JavaHome.SYSTEM_DESC, cfg.getJavaHomes()[0].getDescription());
		assertEquals(JavaHome.SYSTEM_HOME, cfg.getJavaHomes()[0].getJavaHome());
	}
	
	public void testCreateConfiguratorNullDoc() throws Exception {
		assertNull(plugin.createProjectConfigurator(null, null, null));
	}
	
	public void testCreateConfiguratorEmptyDoc() throws Exception {
		doc.addContent(new Element("unrelated-thing"));
		
		assertNull(plugin.createProjectConfigurator("", null, doc));
	}
	
	public void testCreateConfiguratorUnrelatedNamespace() throws Exception {
		final Element project = new Element("project");
		project.setNamespace(Namespace.getNamespace("http://example.com/unrelated_namespace"));
		
		doc.addContent(project);
		assertNull(plugin.createProjectConfigurator(null, null, doc));
	}

	public void testCreateConfiguratorEndsWithDotBuild() throws Exception {
		// NAnt projects use similar document structure, but filenames typically end with .build
		// These should be ignored by this plugin
		final Element project = new Element("project");
		project.setAttribute("name", "foo");
		
		doc.addContent(project);
		assertNull(plugin.createProjectConfigurator("http://example.com/svn/foo.build", null, doc));
	}
	
	public void testCreateConfigurator() throws Exception {
		final Element project = new Element("project");
		project.setAttribute("name", "foo");
		
		doc.addContent(project);
		assertNotNull(plugin.createProjectConfigurator("", null, doc));
	}
}
