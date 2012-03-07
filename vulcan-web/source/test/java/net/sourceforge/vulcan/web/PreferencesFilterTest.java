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
package net.sourceforge.vulcan.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import net.sourceforge.vulcan.dto.PreferencesDto;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

public class PreferencesFilterTest extends ServletFilterTestCase {
	EasyMock mock = new EasyMock();
	PreferencesFilter filter = new PreferencesFilter();
	StaticWebApplicationContext wac = new StaticWebApplicationContext();
	PreferencesStore prefStore;
	
	IMocksControl control = EasyMock.createControl();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		prefStore = control.createMock(PreferencesStore.class);
		wac.getBeanFactory().registerSingleton("preferencesStore", prefStore);
		context.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		filter.init(filterConfig);
	}
	
	public void testDoesNotCreateSession() throws Exception {
		EasyMock.expect(prefStore.getDefaultPreferences()).andReturn(new PreferencesDto());
		
		filter();
		
		assertNotNull(request.getAttribute(Keys.PREFERENCES));
	}
	
	public void testLoadsPreferencesFromCookieIntoRequest() throws Exception {
		request.addCookie(new Cookie(Keys.PREFERENCES, "descending"));
		
		EasyMock.expect(prefStore.convertFromString("descending")).andReturn(new PreferencesDto());
		
		filter();

		assertNotNull(request.getAttribute(Keys.PREFERENCES));
	}

	public void testLoadsPreferencesFromCookieIntoSessionIfPresent() throws Exception {
		request.addCookie(new Cookie(Keys.PREFERENCES, "descending"));
		request.getSession();
		
		EasyMock.expect(prefStore.convertFromString("descending")).andReturn(new PreferencesDto());
		
		filter();

		assertNotNull(request.getSession().getAttribute(Keys.PREFERENCES));
	}
	
	public void testDoesNotReloadsPreferencesFromCookieIntoSessionIfPresent() throws Exception {
		final PreferencesDto oldPrefs = new PreferencesDto();
		request.getSession().setAttribute(Keys.PREFERENCES, oldPrefs);
		
		filter();

		assertSame(oldPrefs, request.getSession().getAttribute(Keys.PREFERENCES));
	}
	
	private void filter() throws ServletException, IOException {
		final boolean sessionExisted = request.getSession(false) != null;
		
		assertFalse(chain.doFilterCalled());
		
		control.replay();
		
		filter.doFilter(request, response, chain);
		
		control.verify();
		
		assertTrue("Should call chain", chain.doFilterCalled());
		
		if (!sessionExisted) {
			assertNull("Should not create session", request.getSession(false));
		}
	}
}
