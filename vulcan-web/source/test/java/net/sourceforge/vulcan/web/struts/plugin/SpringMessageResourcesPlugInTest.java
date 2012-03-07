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
package net.sourceforge.vulcan.web.struts.plugin;

import java.util.Locale;

import javax.servlet.ServletContext;

import net.sourceforge.vulcan.EasyMockTestCase;

import org.apache.struts.Globals;
import org.apache.struts.util.MessageResources;
import org.springframework.web.context.WebApplicationContext;

import servletunit.ServletContextSimulator;

public class SpringMessageResourcesPlugInTest extends EasyMockTestCase {
	WebApplicationContext wac = createMock(WebApplicationContext.class);
	
	ServletContextSimulator context = new ServletContextSimulator();
	
	SpringMessageResourcesPlugIn plugIn = new SpringMessageResourcesPlugIn() {
		@Override
		protected WebApplicationContext getWacInternal() {
			return wac;
		}
		@Override
		protected ServletContext getServletContextInternal() {
			return context;
		}
	};
	
	public void test() throws Exception {
		assertNull(context.getAttribute(Globals.MESSAGES_KEY));
		
		plugIn.onInit();
		
		final MessageResources msgs = (MessageResources) context.getAttribute(Globals.MESSAGES_KEY);
		
		assertNotNull(msgs);
		
		expect(wac.getMessage("foo", null, null)).andReturn("hello");
		replay();
		
		assertEquals("hello", msgs.getMessage((Locale)null, "foo"));
		
		verify();
	}
}
