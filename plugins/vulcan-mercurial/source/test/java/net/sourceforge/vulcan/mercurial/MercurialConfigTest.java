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
package net.sourceforge.vulcan.mercurial;

import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Locale;

import junit.framework.TestCase;
import net.sourceforge.vulcan.MockApplicationContext;
import net.sourceforge.vulcan.exception.ValidationException;

public class MercurialConfigTest extends TestCase {
	final MercurialConfig config = new MercurialConfig();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		final MockApplicationContext context = new MockApplicationContext();
		context.refresh();
		config.setApplicationContext(context);
	}
	
	public void testPropertyListNotEmpty() throws Exception {
		
		final List<PropertyDescriptor> list = config.getPropertyDescriptors(Locale.ROOT);
		
		assertNotNull("return value", list);
		assertEquals("empty", false, list.isEmpty());
	}
	
	public void testValidateSucceeds() throws Exception {
		config.setExecutable("hg");
		
		config.validate();
	}
	
	public void testValidateBlankExecutable() throws Exception {
		config.setExecutable("");
		
		try {
			config.validate();
			fail("expected exception");
		} catch (ValidationException e) {
			assertEquals("property name", "executable", e.getPropertyName());
			assertEquals("message key", "errors.required.with.name", e.getKey());
		}
	}
}
