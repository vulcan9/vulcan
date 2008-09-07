/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2008 Chris Eldredge
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
package net.sourceforge.vulcan.subversion.dto;

import net.sourceforge.vulcan.exception.ValidationException;
import junit.framework.TestCase;

public class SparseCheckoutDtoTest extends TestCase {
	SparseCheckoutDto dto = new SparseCheckoutDto();
	
	public void testDirectoryRequired() throws Exception {
		dto.setDirectoryName("");
		
		try {
			dto.validate();
			fail("expected exception");
		} catch (ValidationException e) {
			assertEquals("directoryName", e.getPropertyName());
			assertEquals("errors.required", e.getKey());
		}
	}
	
	public void testDirectoryCannotStartWithSlash() throws Exception {
		dto.setDirectoryName("/subdir");
		
		try {
			dto.validate();
			fail("expected exception");
		} catch (ValidationException e) {
			assertEquals("directoryName", e.getPropertyName());
			assertEquals("svn.errors.directoryName", e.getKey());
		}
	}
	
	public void testConvertsBackslash() throws Exception {
		dto.setDirectoryName("subdir\\nested");
		
		dto.validate();
		
		assertEquals("subdir/nested", dto.getDirectoryName());
	}
}
