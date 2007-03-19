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
package net.sourceforge.vulcan.web.struts.forms;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.integration.ConfigChoice;
import net.sourceforge.vulcan.integration.PluginConfigStub;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.struts.MockApplicationContextStrutsTestCase;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class PluginConfigFormTest extends MockApplicationContextStrutsTestCase {
	PluginConfigForm form = new PluginConfigForm();
	
	public void testGetsBeanInfo() throws Exception {
		form.setPluginConfig(request, new PluginConfigStub());
		
		final List<PropertyDescriptor> pds = form.getAllProperties();
		assertNotNull(pds);
		
		assertEquals(4, pds.size());

		assertEquals("pluginConfig.value", pds.get(0).getName());
		assertEquals("the foo value", pds.get(0).getShortDescription());
		
		assertEquals("pluginConfig.obj", pds.get(1).getName());
		
		assertEquals("pluginConfig.bool", pds.get(2).getName());
		
		assertEquals("pluginConfig.password", pds.get(3).getName());
	}
	public void testTwice() throws Exception {
		testGetsBeanInfo();
		testGetsBeanInfo();
	}
	public void testFocusOnInnerBean() throws Exception {
		PluginConfigStub config = new PluginConfigStub();
		
		form.setPluginConfig(request, config);
		
		form.setFocus("pluginConfig.obj");

		form.introspect(request);
		
		final List<PropertyDescriptor> pds = form.getAllProperties();
		assertEquals(2, pds.size());
		
		assertEquals("pluginConfig.obj.class", pds.get(0).getName());
		assertEquals("pluginConfig.obj.nestedValue", pds.get(1).getName());
		assertEquals("Setup > Plugins > Mock Plugin > Nested Object", (String)request.getAttribute("location"));
	}
	public void testSetsLocationOnProject() throws Exception {
		PluginConfigStub config = new PluginConfigStub();
		
		form.setProjectPlugin(true);
		form.setProjectName("My Dumb Project");
		form.setPluginConfig(request, config);
		form.setFocus("pluginConfig.obj");

		form.introspect(request);
		
		final List<PropertyDescriptor> pds = form.getAllProperties();
		assertEquals(2, pds.size());
		
		assertEquals("pluginConfig.obj.class", pds.get(0).getName());
		assertEquals("pluginConfig.obj.nestedValue", pds.get(1).getName());
		assertEquals("Setup > Projects > My Dumb Project > Mock Plugin > Nested Object", (String)request.getAttribute("location"));
	}
	public void testGetChoices() throws Exception {
		final List<String> projects = Arrays.asList(new String[] {"a", "b"});
		
		expect(manager.getProjectConfigNames()).andReturn(projects);
		replay();
		
		form.setServlet(actionServlet);
		form.reset(null, request);
		form.setPluginConfig(request, new PluginConfigStubWithProjectsChoice());
		form.introspect(request);
		
		verify();
		
		final List<PropertyDescriptor> all = form.getAllProperties();
		assertEquals("pluginConfig.project", all.get(0).getName());
		assertEquals("enum", form.getTypes().get(all.get(0).getName()));
		assertEquals(projects, form.getChoices().get(all.get(0).getName()));

		assertEquals("pluginConfig.projects", all.get(1).getName());
		assertEquals("choice-array", form.getTypes().get(all.get(1).getName()));
		assertEquals(projects, form.getChoices().get(all.get(1).getName()));

		assertEquals("pluginConfig.custom", all.get(2).getName());
		assertEquals("enum", form.getTypes().get(all.get(2).getName()));
		assertEquals(Arrays.asList(new String[] {"a","b"}), form.getChoices().get(all.get(2).getName()));

		assertEquals("pluginConfig.customs", all.get(3).getName());
		assertEquals("choice-array", form.getTypes().get(all.get(3).getName()));
		assertEquals(Arrays.asList(new String[] {"c","d"}), form.getChoices().get(all.get(3).getName()));
		
		assertEquals("pluginConfig.bool", all.get(4).getName());
		assertEquals("boolean", form.getTypes().get(all.get(4).getName()));
		
		assertEquals("pluginConfig.enums", all.get(5).getName());
		assertEquals("choice-array", form.getTypes().get(all.get(5).getName()));
		assertEquals(Arrays.asList(new String[] {"ONE","TWO"}), form.getChoices().get(all.get(5).getName()));

		assertEquals("pluginConfig.password", all.get(6).getName());
		assertEquals("password", form.getTypes().get(all.get(6).getName()));
	}
	public static class PluginConfigStubWithProjectsChoice extends PluginConfigStub {
		private String project;
		private String[] projects;
		private String custom;
		private String[] customs;
		private boolean bool;
		private NestedEnum[] enums;
		
		public String getCustom() {
			return custom;
		}
		public void setCustom(String custom) {
			this.custom = custom;
		}
		public String[] getCustoms() {
			return customs;
		}
		public void setCustoms(String[] customs) {
			this.customs = customs;
		}
		public String getProject() {
			return project;
		}
		public void setProject(String project) {
			this.project = project;
		}
		public String[] getProjects() {
			return projects;
		}
		public void setProjects(String[] projects) {
			this.projects = projects;
		}
		public boolean isBool() {
			return bool;
		}
		public void setBool(boolean bool) {
			this.bool = bool;
		}
		public NestedEnum[] getEnums() {
			return enums;
		}
		public void setEnums(NestedEnum[] enums) {
			this.enums = enums;
		}
		@Override
		public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
			try {
				PropertyDescriptor[] pds = new PropertyDescriptor[] {
					new PropertyDescriptor("project", PluginConfigStubWithProjectsChoice.class),
					new PropertyDescriptor("projects", PluginConfigStubWithProjectsChoice.class),
					new PropertyDescriptor("custom", PluginConfigStubWithProjectsChoice.class),
					new PropertyDescriptor("customs", PluginConfigStubWithProjectsChoice.class),
					new PropertyDescriptor("bool", PluginConfigStubWithProjectsChoice.class),
					new PropertyDescriptor("enums", PluginConfigStubWithProjectsChoice.class),
					new PropertyDescriptor("password", PluginConfigStubWithProjectsChoice.class)
				};

				pds[0].setValue(ATTR_CHOICE_TYPE, ConfigChoice.PROJECTS);
				pds[1].setValue(ATTR_CHOICE_TYPE, ConfigChoice.PROJECTS);
				pds[2].setValue(ATTR_CHOICE_TYPE, ConfigChoice.INLINE);
				pds[2].setValue(ATTR_AVAILABLE_CHOICES, Arrays.asList(new String[] {"a", "b"}));
				pds[3].setValue(ATTR_CHOICE_TYPE, ConfigChoice.INLINE);
				pds[3].setValue(ATTR_AVAILABLE_CHOICES, Arrays.asList(new String[] {"c", "d"}));
				pds[6].setValue(ATTR_WIDGET_TYPE, Widget.PASSWORD);
				
				return Arrays.asList(pds);
			} catch (Exception t) {
				throw new RuntimeException("i sure hate checked exceptions");
			}
		}
	}
	public static enum NestedEnum {
		ONE, TWO
	}
}
