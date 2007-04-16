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

import net.sourceforge.vulcan.metadata.SvnRevision;
import junit.framework.TestCase;
import servletunit.HttpServletRequestSimulator;
import servletunit.ServletContextSimulator;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class UserAgentRestRequestDetectorTest extends TestCase {
	UserAgentRestRequestDetector detector = new UserAgentRestRequestDetector();
	HttpServletRequestSimulator request = new HttpServletRequestSimulator(new ServletContextSimulator());
	
	public void testUserAgentIsMozilla() throws Exception {
		request.setHeader("User-Agent", "Something having to do with Mozilla, etc.");
		assertFalse(detector.isRestRequest(request));
	}

	public void testUserAgent() throws Exception {
		assertTrue(detector.isRestRequest(request));
	}
}