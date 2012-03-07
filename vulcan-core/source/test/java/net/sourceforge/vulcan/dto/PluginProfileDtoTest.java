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
package net.sourceforge.vulcan.dto;

import junit.framework.TestCase;

public class PluginProfileDtoTest extends TestCase {
	public void testNotRenamedByDefault() throws Exception {
		assertFalse(new PluginProfileDtoStub().isRenamed());
	}
	
	public void testNotRenamedAfterCheckpointOldNameNull() throws Exception {
		final PluginProfileDtoStub dto = new PluginProfileDtoStub();
		
		dto.checkPoint();
		
		assertFalse(dto.isRenamed());
	}
	
	public void testNotRenamedAfterCheckpointOnSetNameIfOldNameWasNull() throws Exception {
		final PluginProfileDtoStub dto = new PluginProfileDtoStub();
		
		dto.checkPoint();
		
		dto.setName("a name");
		
		assertFalse(dto.isRenamed());
	}
	
	public void testRenamedAfterCheckpoint() throws Exception {
		final PluginProfileDtoStub dto = new PluginProfileDtoStub();

		dto.setName("old");
		
		dto.checkPoint();
		
		dto.setName("a name");
		
		assertTrue(dto.isRenamed());
	}
	
	public void testRenamedCopyAfterCheckpoint() throws Exception {
		final PluginProfileDtoStub dto = new PluginProfileDtoStub();

		dto.setName("old");
		
		dto.checkPoint();
		
		final PluginProfileDtoStub copy = (PluginProfileDtoStub) dto.copy();
		
		copy.setName("a name");
		
		assertTrue(copy.isRenamed());
	}
	
	public void testRenamedAfterCheckpointNullSafe() throws Exception {
		final PluginProfileDtoStub dto = new PluginProfileDtoStub();

		dto.setName("old");
		
		dto.checkPoint();
		
		dto.setName(null);
		
		assertTrue(dto.isRenamed());
	}
	
	public void testNotRenamedAfterCheckpoint() throws Exception {
		final PluginProfileDtoStub dto = new PluginProfileDtoStub();

		dto.setName("old");
		
		dto.checkPoint();
		
		assertFalse(dto.isRenamed());
	}
	
	public void testEqualsAfterCheckpoint() throws Exception {
		final PluginProfileDtoStub dto = new PluginProfileDtoStub();

		dto.setName("old");
		
		final PluginProfileDtoStub copy = (PluginProfileDtoStub) dto.copy();
		
		assertEquals(dto, copy);
		
		dto.checkPoint();
		
		assertEquals(dto, copy);
	}
}
