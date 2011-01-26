/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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
package net.sourceforge.vulcan.jabber;

import net.sourceforge.vulcan.exception.ValidationException;
import junit.framework.TestCase;

public class JabberTemplatesConfigTest extends TestCase {
	public void testValidateTemplates() throws Exception {
		final JabberTemplatesConfig a = new JabberTemplatesConfig();
		
		a.setNotifyCommitterTemplate("some format");
		
		a.validate();
	}
	
	public void testValidateTemplateInvalid() throws Exception {
		final JabberTemplatesConfig a = new JabberTemplatesConfig();
		
		a.setNotifyBuildMasterTemplate("{Message,notapattern,notaformat}");
		
		try {
			a.validate();
			fail("expected exception");
		} catch (ValidationException e) {
			assertEquals("notifyBuildMasterTemplate", e.getPropertyName());
		}
		
	}
	
	public void testValidateTemplateInvalidParamName() throws Exception {
		final JabberTemplatesConfig a = new JabberTemplatesConfig();
		
		a.setPithyRetortTemplate("{NotValid}");
		
		try {
			a.validate();
			fail("expected exception");
		} catch (ValidationException e) {
			assertEquals("pithyRetortTemplate", e.getPropertyName());
			assertEquals("NotValid", e.getArgs()[0]);
		}
	}
}
