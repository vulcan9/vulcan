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
package net.sourceforge.vulcan.core.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.exception.StoreException;

public class AbstractFileStoreTest extends TestCase {
	AbstractFileStore store = new AbstractFileStore() {
		public void exportConfiguration(OutputStream os) throws StoreException, IOException {
		}
		public File getBuildLog(String projectName, UUID diffId) throws StoreException {
			return null;
		}
		public File getChangeLog(String projectName, UUID diffId) throws StoreException {
			return null;
		}
		public String getExportMimeType() {
			return null;
		}
		public void importConfiguration(InputStream is) throws StoreException, IOException {
		}
		public StateManagerConfigDto loadConfiguration() throws StoreException {
			return null;
		}
		public void storeConfiguration(StateManagerConfigDto config) throws StoreException {
		}
		public boolean buildLogExists(String projectName, UUID diffId) {
			return false;
		}
		public boolean diffExists(String projectName, UUID diffId) {
			return false;
		}
	};
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		store.setConfigRoot("/foo");
	}
	public void testValidWorkingCopyLocation() throws Exception {
		assertFalse(store.isWorkingCopyLocationInvalid("/bar"));
	}
	public void testInvalidWorkingCopyLocationVulcanRoot() throws Exception {
		assertTrue(store.isWorkingCopyLocationInvalid("/foo"));
	}
	public void testValidNestedWorkingCopyLocation() throws Exception {
		assertFalse(store.isWorkingCopyLocationInvalid("/foo/bar"));
	}
	public void testInvalidWorkingCopyLocationProjects() throws Exception {
		assertTrue(store.isWorkingCopyLocationInvalid("/foo/projects"));
	}
	public void testInvalidWorkingCopyLocationProjectTopLevel() throws Exception {
		assertTrue(store.isWorkingCopyLocationInvalid("/foo/projects/bar"));
	}
	public void testValidWorkingCopyLocationNestedInProjects() throws Exception {
		assertFalse(store.isWorkingCopyLocationInvalid("/foo/projects/bar/sandbox"));
	}
}
