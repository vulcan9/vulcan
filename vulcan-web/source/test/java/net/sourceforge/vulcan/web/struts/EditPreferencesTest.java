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

import net.sourceforge.vulcan.dto.PreferencesDto;
import net.sourceforge.vulcan.web.struts.forms.PreferencesForm;


public class EditPreferencesTest extends MockApplicationContextStrutsTestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/editPreferences.do");
	}
	
	public void testEditLoadsForm() throws Exception {
		replay();
		
		actionPerform();
		
		verify();
		
		final PreferencesForm form = (PreferencesForm) request.getAttribute("preferencesForm");
		assertNotNull(form);
		assertEquals(new PreferencesDto(), form.getConfig());
	}
	
	public void testEditLoadsExistingPreferences() throws Exception {
		final PreferencesDto prefs = new PreferencesDto();
		prefs.setSortColumn("custom");
		
		request.getSession().setAttribute("preferences", prefs);
		replay();
		
		actionPerform();
		
		verify();
		
		final PreferencesForm form = (PreferencesForm) request.getAttribute("preferencesForm");
		assertNotNull(form);
		assertEquals(prefs, form.getConfig());
		assertNotSame(prefs, form.getConfig());
	}
}
