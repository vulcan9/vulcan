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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import junit.framework.TestCase;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.struts.action.ActionServlet;

import servletunit.ServletContextSimulator;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class PreferencesFormTest extends TestCase {
	PreferencesForm form = new PreferencesForm();
	
	Map<String, Set<String>> paths = new HashMap<String, Set<String>>();
	
	ServletContextSimulator context = new ServletContextSimulator() {
		@SuppressWarnings("unchecked")
		public Set getResourcePaths(String path) {
			if (!paths.containsKey(path)) {
				fail("no such path " + path);
			}
			return paths.get(path);
		}
	};
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		final ActionServlet actionServlet = new ActionServlet() {
			@Override
			public ServletContext getServletContext() {
				return context;
			}
		};
		
		form.setServlet(actionServlet);
	}
	
	public void testFindAvailableStylesheetsSimple() throws Exception {
		paths.put("/css/", new HashSet<String>(Arrays.asList("/css/x.css")));
		assertEquals(Arrays.asList("x"), form.findAvailableStylesheets());
	}

	public void testFindAvailableStylesheetsIgnoresNonCss() throws Exception {
		paths.put("/css/", new HashSet<String>(Arrays.asList("/css/includes/", "/css/foo.css", "/css/bar", "/css/baz.txt")));
		assertEquals(Arrays.asList("foo"), form.findAvailableStylesheets());
	}

	public void testFindAvailableStylesheetsSorts() throws Exception {
		paths.put("/css/", new HashSet<String>(Arrays.asList("/css/includes/a.css", "/css/b.css", "/css/c.css")));
		assertEquals(Arrays.asList("a", "b", "c"), form.findAvailableStylesheets());
	}
}
