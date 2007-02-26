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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import junit.framework.TestCase;

import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.struts.forms.PreferencesForm.NameValue;

import org.apache.struts.action.ActionServlet;

import servletunit.ServletContextSimulator;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class PreferencesFormTest extends TestCase {
	PreferencesForm form = new PreferencesForm() {
		@Override
		protected ActionServlet getServlet() {
			return new ActionServlet() {
				@Override
				public ServletContext getServletContext() {
					return context;
				}
			};
		}
	};

	ServletContextSimulator context = new ServletContextSimulator() {
		@Override
		public Set getResourcePaths(String path) {
			assertEquals("/css/", path);
			return cssPaths;
		}
	};
	
	Set<String> cssPaths = new HashSet<String>(Arrays.asList(
			"/css/b.css",
			"/css/a.css",
			"/css/included/",
			"/css/not a css file.txt",
			"/css/standard.css"));

	public void testResetGetsAvailableStyleSheets() throws Exception {
		form.reset(null, null);
		
		final NameValue[] available = form.getAvailableStyleSheets();
		
		assertNotNull(available);
		
		assertEquals("/css/a.css", available[0].getValue());
		assertEquals("a", available[0].getName());
		assertEquals("/css/b.css", available[1].getValue());
		assertEquals("b", available[1].getName());
		assertEquals("/css/standard.css", available[2].getValue());
		assertEquals("standard", available[2].getName());
	}
}
