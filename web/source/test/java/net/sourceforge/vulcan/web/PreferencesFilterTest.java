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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import net.sourceforge.vulcan.dto.PreferencesDto;
import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class PreferencesFilterTest extends ServletFilterTestCase {
	PreferencesFilter filter = new PreferencesFilter();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		filter.init(filterConfig);
	}
	
	public void testDoesNotCreateSession() throws Exception {
		filter();
		
		assertEquals(0, cookies.size());
	}
	
	public void testSetsCookies() throws Exception {
		request.addParameter("sortColumn", "name");
		request.addParameter("sortOrder", "ascending");
		
		filter();
		
		assertEquals(2, cookies.size());
		
		assertCookieValues("name", "ascending");
		assertPreferences("name", "ascending");
	}

	public void testGetsCookies() throws Exception {
		request.addCookie(new Cookie("VULCAN_sortOrder", "descending"));
		filter();
		
		assertEquals(0, cookies.size());
		
		assertPreferences(null, "descending");
	}

	private void filter() throws ServletException, IOException {
		assertFalse(chain.doFilterCalled());
		
		filter.doFilter(request, response, chain);
		
		assertTrue("Should call chain", chain.doFilterCalled());
		
		assertNull("Should not create session", request.getSession(false));
	}
	
	private void assertCookieValues(String sortColumn, String sortOrder) {
		assertCookieValue("VULCAN_sortColumn", sortColumn);
		assertCookieValue("VULCAN_sortOrder", sortOrder);
	}

	private void assertPreferences(String sortColumn, String sortOrder) {
		final PreferencesDto prefs = (PreferencesDto) request.getAttribute("preferences");
		
		assertNotNull("Expected preferences in request but found null", prefs);
		
		assertEquals(sortColumn, prefs.getSortColumn());
		assertEquals(sortOrder, prefs.getSortOrder());
	}

	private void assertCookieValue(String cookieName, String expected) {
		final Cookie cookie = cookies.get(cookieName);
		final String value;
		if (cookie != null) {
			assertEquals(-1, cookie.getMaxAge());
			value = cookie.getValue();
		} else {
			value = null;
		}
		
		assertEquals(expected, value);
	}
}
