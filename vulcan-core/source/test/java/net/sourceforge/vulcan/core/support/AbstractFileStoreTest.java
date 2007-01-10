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
package net.sourceforge.vulcan.core.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import junit.framework.TestCase;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class AbstractFileStoreTest extends TestCase {
	AbstractFileStore store = new AbstractFileStore() {
		public ProjectStatusDto createBuildOutcome(String projectName) {
			return null;
		}
		public void exportConfiguration(OutputStream os) throws StoreException, IOException {
		}
		public InputStream getBuildLogInputStream(String projectName, UUID buildLogId) throws StoreException {
			return null;
		}
		public OutputStream getBuildLogOutputStream(String projectName, UUID buildLogId) throws StoreException {
			return null;
		}
		public Map<String, List<UUID>> getBuildOutcomeIDs() {
			return null;
		}
		public InputStream getChangeLogInputStream(String projectName, UUID diffId) throws StoreException {
			return null;
		}
		public OutputStream getChangeLogOutputStream(String projectName, UUID diffId) throws StoreException {
			return null;
		}
		public String getExportMimeType() {
			return null;
		}
		public void importConfiguration(InputStream is) throws StoreException, IOException {
		}
		public ProjectStatusDto loadBuildOutcome(String projectName, UUID id) throws StoreException {
			return null;
		}
		public StateManagerConfigDto loadConfiguration() throws StoreException {
			return null;
		}
		public UUID storeBuildOutcome(ProjectStatusDto outcome) throws StoreException {
			return null;
		}
		public void storeConfiguration(StateManagerConfigDto config) throws StoreException {
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
