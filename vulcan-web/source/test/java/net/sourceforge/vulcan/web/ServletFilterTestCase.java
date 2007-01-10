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
package net.sourceforge.vulcan.web;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

import junit.framework.TestCase;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.collections.iterators.IteratorEnumeration;

import servletunit.FilterChainSimulator;
import servletunit.FilterConfigSimulator;
import servletunit.HttpServletRequestSimulator;
import servletunit.HttpServletResponseSimulator;
import servletunit.ServletContextSimulator;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class ServletFilterTestCase extends TestCase {
	protected ServletContextSimulator context = new ServletContextSimulator();
	protected FilterConfigSimulator filterConfig = new FilterConfigSimulator(context) {
		@Override
		public String getFilterName() {
			return ServletFilterTestCase.this.getClass().getName();
		}
		@Override
		public Enumeration<?> getInitParameterNames() {
			return new IteratorEnumeration(initParams.keySet().iterator());
		}
		@Override
		public String getInitParameter(String parm1) {
			return initParams.get(parm1);
		}
	};
	protected HttpServletRequestSimulator request = new HttpServletRequestSimulator(context);
	protected HttpServletResponseSimulator response = new HttpServletResponseSimulator() {
		@Override
		public void addCookie(Cookie c) {
			cookies.put(c.getName(), c);
			super.addCookie(c);
		}
	};
	
	protected FilterChainSimulator chain = new FilterChainSimulator();
	
	protected final Map<String, Cookie> cookies = new HashMap<String, Cookie>();
	protected final Map<String, String> initParams = new HashMap<String, String>();
}
