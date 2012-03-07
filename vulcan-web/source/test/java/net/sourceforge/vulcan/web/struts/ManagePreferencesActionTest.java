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
package net.sourceforge.vulcan.web.struts;

import net.sourceforge.vulcan.dto.PreferencesDto;
import net.sourceforge.vulcan.web.Keys;


public class ManagePreferencesActionTest extends MockApplicationContextStrutsTestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/managePreferences.do");
		
		expect(preferencesStore.convertToString((PreferencesDto)notNull())).andReturn("pref_data").anyTimes();
	}
	
	public void testSaveUpdatesSession() throws Exception {
		addRequestParameter("action", "save");
		addRequestParameter("config.labels", new String[] {"x"});
		
		replay();
		
		actionPerform();
		
		verify();

		verifyForward("dashboard");
		
		final PreferencesDto prefs = (PreferencesDto) request.getSession().getAttribute(Keys.PREFERENCES);
		assertNotNull(prefs);
		
		final PreferencesDto expected = new PreferencesDto();
		expected.setLabels(new String[] {"x"});
		
		assertEquals(expected, prefs);
	}
	
	public void testSaveWritesCookie() throws Exception {
		addRequestParameter("action", "save");
		addRequestParameter("config.labels", new String[] {"x"});
		
		replay();
		
		actionPerform();
		
		verify();

		assertNotNull(response.findCookie(Keys.PREFERENCES));
	}
	
	public void testClearsOnFlag() throws Exception {
		final PreferencesDto old = new PreferencesDto();
		old.setLabels(new String[] {"x"});
		old.setGroupByLabel(true);
		
		request.getSession().setAttribute(Keys.PREFERENCES, old);
		
		addRequestParameter("fullReset", "true");
		addRequestParameter("action", "save");
		
		replay();
		
		actionPerform();
		
		verify();

		verifyForward("dashboard");
		
		final PreferencesDto prefs = (PreferencesDto) request.getSession().getAttribute(Keys.PREFERENCES);
		assertNotNull(prefs);
		
		final PreferencesDto expected = new PreferencesDto();
		
		assertEquals(expected, prefs);
	}
	
	public void testToggleLabelOff() throws Exception {
		final PreferencesDto old = new PreferencesDto();
		old.setLabels(new String[] {"x", "y", "z"});
		old.setGroupByLabel(true);
		
		request.getSession().setAttribute(Keys.PREFERENCES, old);
		
		addRequestParameter("action", "toggleLabel");
		addRequestParameter("item", "x");
		
		replay();
		
		actionPerform();
		
		verify();

		verifyForward("dashboard");
		
		final PreferencesDto prefs = (PreferencesDto) request.getSession().getAttribute(Keys.PREFERENCES);
		assertNotNull(prefs);
		
		final PreferencesDto expected = (PreferencesDto) old.copy();
		
		expected.setLabels(new String[] {"y", "z"});
		
		assertEquals(expected, prefs);
	}
	
	public void testToggleLabelOn() throws Exception {
		final PreferencesDto old = new PreferencesDto();
		old.setLabels(new String[] {"y", "z"});
		old.setGroupByLabel(true);
		
		request.getSession().setAttribute(Keys.PREFERENCES, old);
		
		addRequestParameter("action", "toggleLabel");
		addRequestParameter("item", "x");
		
		replay();
		
		actionPerform();
		
		verify();

		verifyForward("dashboard");
		
		final PreferencesDto prefs = (PreferencesDto) request.getSession().getAttribute(Keys.PREFERENCES);
		assertNotNull(prefs);
		
		final PreferencesDto expected = (PreferencesDto) old.copy();
		
		expected.setLabels(new String[] {"x", "y", "z"});
		
		assertEquals(expected, prefs);
	}
	
	public void testToggleLabelOnNullLabels() throws Exception {
		final PreferencesDto old = new PreferencesDto();
		old.setLabels(null);
		
		request.getSession().setAttribute(Keys.PREFERENCES, old);
		
		addRequestParameter("action", "toggleLabel");
		addRequestParameter("item", "x");
		
		replay();
		
		actionPerform();
		
		verify();

		verifyForward("dashboard");
		
		final PreferencesDto prefs = (PreferencesDto) request.getSession().getAttribute(Keys.PREFERENCES);
		assertNotNull(prefs);
		
		final PreferencesDto expected = (PreferencesDto) old.copy();
		
		expected.setLabels(new String[] {"x"});
		
		assertEquals(expected, prefs);
	}
	
	public void testPopulatesFromRequest() throws Exception {
		final PreferencesDto old = new PreferencesDto();
		old.setLabels(new String[] {"x"});
		old.setGroupByLabel(true);
		
		request.setAttribute(Keys.PREFERENCES, old);
		
		addRequestParameter("action", "save");
		
		replay();
		
		actionPerform();
		
		verify();

		verifyForward("dashboard");
		
		final PreferencesDto prefs = (PreferencesDto) request.getSession().getAttribute(Keys.PREFERENCES);
		assertNotNull(prefs);
		
		assertEquals(old, prefs);
		
		assertNull(request.getAttribute(Keys.PREFERENCES));
	}
}
