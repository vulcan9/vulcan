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
package net.sourceforge.vulcan.subversion.dto;

import junit.framework.TestCase;

public class SubversionConfigDtoTest extends TestCase {
	SubversionConfigDto dto = new SubversionConfigDto();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		dto.setProfiles(new SubversionRepositoryProfileDto[] { new SubversionRepositoryProfileDto() });
		dto.setTagFolderNames(new String[] {"a"});
	}
	
	public void testCopy() throws Exception {
		SubversionConfigDto copy = dto.copy();
		
		assertNotSame(dto, copy);
		assertNotSame(dto.getProfiles(), copy.getProfiles());

		// Not modifiable during configuration, so it should not be cloned.
		assertSame(dto.getWorkingCopyByteCounts(), copy.getWorkingCopyByteCounts());
	}
}
