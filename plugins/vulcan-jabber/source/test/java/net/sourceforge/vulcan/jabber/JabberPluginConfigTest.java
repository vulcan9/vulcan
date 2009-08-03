/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.exception.ValidationException;

public class JabberPluginConfigTest extends TestCase {

	public void testCopyDeep() throws Exception {
		final JabberPluginConfig a = new JabberPluginConfig();
		final PluginConfigDto aa = a.getScreenNameMapperConfig();
		
		final JabberPluginConfig b = a.copy();
		
		assertNotSame(a.getScreenNameMapperConfigs(), b.getScreenNameMapperConfigs());
		
		assertNotSame(aa, b.getScreenNameMapperConfig());
	}
	
	public void testValidateTemplates() throws Exception {
		final JabberPluginConfig a = new JabberPluginConfig();
		
		a.setMessageFormat("some format");
		
		a.validate();
	}
	
	public void testValidateTemplateInvalid() throws Exception {
		final JabberPluginConfig a = new JabberPluginConfig();
		
		a.setMessageFormat("{Message,notapattern,notaformat}");
		
		try {
			a.validate();
			fail("expected exception");
		} catch (ValidationException e) {
			assertEquals("messageFormat", e.getPropertyName());
		}
		
	}
	
	public void testValidateTemplateInvalidParamName() throws Exception {
		final JabberPluginConfig a = new JabberPluginConfig();
		
		a.setMessageFormat("{NotValid}");
		
		try {
			a.validate();
			fail("expected exception");
		} catch (ValidationException e) {
			assertEquals("messageFormat", e.getPropertyName());
			assertEquals("NotValid", e.getArgs()[0]);
		}
		
	}
}
