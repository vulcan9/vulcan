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
package net.sourceforge.vulcan.core.support;

import java.io.File;
import java.io.IOException;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.exception.RepositoryException;

public class RepositoryUtilsTest extends EasyMockTestCase {

	private FileSystem fileSystem;
	private BuildDetailCallback buildDetail;
	private RepositoryUtils utils;
	private File workDir = new File("fake");
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		fileSystem = createMock(FileSystem.class);
		buildDetail = createMock(BuildDetailCallback.class);
		
		utils = new RepositoryUtils(fileSystem);
	}
	
	public void testCreatesDirWhenMissing() throws Exception {
		expect(fileSystem.directoryExists(workDir)).andReturn(false);
		fileSystem.createDirectory(workDir);
		
		replay();
		
		utils.createOrCleanWorkingCopy(workDir, buildDetail);
		
		verify();
	}
	
	public void testThrowsRepositoryExceptionOnFailure() throws Exception {
		expect(fileSystem.directoryExists(workDir)).andReturn(false);
		fileSystem.createDirectory(workDir);
		final IOException ioe = new IOException();
		expectLastCall().andThrow(ioe);
		
		replay();
		
		try {
			utils.createOrCleanWorkingCopy(workDir, buildDetail);
			fail("expected exception");
		} catch (RepositoryException e) {
			assertSame("e.getCause()", ioe, e.getCause());
		}
		
		verify();
	}
	
	public void testCleans() throws Exception {
		expect(fileSystem.directoryExists(workDir)).andReturn(true);
		buildDetail.setDetail("build.messages.clean");
		fileSystem.cleanDirectory(workDir, null);
		
		replay();
		
		utils.createOrCleanWorkingCopy(workDir, buildDetail);
		
		verify();
	}
	
	public void testCleanWrapsException() throws Exception {
		expect(fileSystem.directoryExists(workDir)).andReturn(true);
		buildDetail.setDetail("build.messages.clean");
		fileSystem.cleanDirectory(workDir, null);
		final IOException ioe = new IOException();
		expectLastCall().andThrow(ioe);
		
		replay();
		
		try {
			utils.createOrCleanWorkingCopy(workDir, buildDetail);
			fail("expected exception");
		} catch (RepositoryException e) {
			assertSame("e.getCause()", ioe, e.getCause());
		}
		
		verify();
	}
}
