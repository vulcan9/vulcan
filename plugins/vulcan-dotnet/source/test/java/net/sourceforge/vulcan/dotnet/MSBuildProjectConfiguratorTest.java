/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2007 Chris Eldredge
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.ProjectConfigDto;

import org.jdom.Document;
import org.jdom.Element;

public class MSBuildProjectConfiguratorTest extends TestCase {
	DotNetProjectConfigurator cfgr = new DotNetProjectConfigurator();
	
	Document doc = new Document();

	private ProjectConfigDto projectConfig = new ProjectConfigDto();

	private List<String> existingProjectNames = new ArrayList<String>();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		final Element root = new Element("Project");
		doc.addContent(root);
		
		final Element itemGroup = new Element("ItemGroup");
		
		addProjectReference(itemGroup, "..\\Com.Example.Foo.Bar\\Com.Example.Foo.Bar.csproj");
		addProjectReference(itemGroup, "..\\Com.Example.Other.Thing\\Com.Example.Other.Thing.csproj");
		
		root.addContent(itemGroup);
		
		cfgr.setDocument(doc);
		cfgr.setUrl("http://example.com/Com.Example.Master/Com.Example.Master.csproj");
	}

	private void addProjectReference(Element root, String path) {
		final Element ref = new Element("ProjectReference");
		ref.setAttribute("Include", path);
		root.addContent(ref);
	}
	
	public void testApply() throws Exception {
		cfgr.applyConfiguration(projectConfig, "build/script.xml", existingProjectNames, true);
		
		assertEquals("Com.Example.Master", projectConfig.getName());
		
		assertEquals(2, projectConfig.getDependencies().length);
		assertEquals("Com.Example.Foo.Bar", projectConfig.getDependencies()[0]);
		assertEquals("Com.Example.Other.Thing", projectConfig.getDependencies()[1]);
	}

	public void testApplyNoSubprojects() throws Exception {
		cfgr.applyConfiguration(projectConfig, "build/script.xml", existingProjectNames, false);
		
		assertEquals("Com.Example.Master", projectConfig.getName());
		
		assertEquals(0, projectConfig.getDependencies().length);
	}

	public void testGetSubprojects() throws Exception {
		cfgr.applyConfiguration(projectConfig, "build/script.xml", existingProjectNames, true);

		final List<String> expected = Arrays.asList(
				"../Com.Example.Foo.Bar/Com.Example.Foo.Bar.csproj", 
				"../Com.Example.Other.Thing/Com.Example.Other.Thing.csproj");
		
		assertEquals(expected, cfgr.getSubprojectUrls());
	}
}
