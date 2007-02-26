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
package net.sourceforge.vulcan.web.struts;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.struts.action.ActionServlet;

import servletunit.HttpServletRequestSimulator;
import servletunit.HttpServletResponseSimulator;
import servletunit.ServletConfigSimulator;
import servletunit.ServletContextSimulator;

import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class SavePreferencesActionTest extends MockApplicationContextStrutsTestCase {
	String expectedStyleSheetPath = "/css/";
	Set<String> fakeStyleSheets = new HashSet<String>(Arrays.asList("a", "b"));
	
	@Override
	public void setUp() throws Exception {
        actionServlet = new ActionServlet();
        context = new ServletContextSimulator() {
        	@Override
        	public Set getResourcePaths(String path) {
        		return SavePreferencesActionTest.this.getResourcePaths(path);
        	}
        };
        config = new ServletConfigSimulator() {
        	@Override
        	public ServletContext getServletContext() {
        		return context;
        	}
        };
        request = new HttpServletRequestSimulator(config.getServletContext());
        response = new HttpServletResponseSimulator();
        
        requestWrapper = null;
        responseWrapper = null;
        isInitialized = true;

		super.setUp(false);
		
        setRequestPathInfo("/savePreferences.do");
	}
	
	protected Set<String> getResourcePaths(String path) {
		assertEquals(expectedStyleSheetPath, path);
		
		return fakeStyleSheets;
	}

	public void test() throws Exception {
		actionPerform();
		
		verifyActionMessages(new String[] {"messages.save.success"});
	}

}
