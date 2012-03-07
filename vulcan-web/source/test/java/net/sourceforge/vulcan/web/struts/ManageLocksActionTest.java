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
package net.sourceforge.vulcan.web.struts;

import net.sourceforge.vulcan.exception.NoSuchProjectException;

public class ManageLocksActionTest extends MockApplicationContextStrutsTestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/buildmanagement/manageLocks");
	}
	
	public void testUnspecifiedAction() throws Exception {
		replay();
		
		actionPerform();
		
		verify();
		
		verifyActionErrors(new String[] {"errors.required"});
		verifyNoActionMessages();
		
		verifyInputForward();
	}
	
	public void testLock() throws Exception {
		addRequestParameter("action", "lock");
		addRequestParameter("projectNames", new String[] {"a", "b"});
		addRequestParameter("message", "locked for QA");
		
		buildManager.isBuildingOrInQueue("a", "b");
		expectLastCall().andReturn(false);
		manager.lockProjects("locked for QA", "a", "b");
		expectLastCall().andReturn(1234l);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		
		verifyForward("success");
	}
	
	public void testLockProjectNotFound() throws Exception {
		addRequestParameter("action", "lock");
		addRequestParameter("projectNames", new String[] {"a"});
		addRequestParameter("message", "locked for QA");
		
		buildManager.isBuildingOrInQueue("a");
		expectLastCall().andReturn(false);
		
		manager.lockProjects("locked for QA", "a");
		expectLastCall().andThrow(new NoSuchProjectException("a"));
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyActionErrors(new String[] {"errors.no.such.project"});
		verifyNoActionMessages();
		
		verifyInputForward();
	}
	
	public void testLockSetsMessageWhenBlank() throws Exception {
		addRequestParameter("action", "lock");
		addRequestParameter("projectNames", new String[] {"a", "b"});
		addRequestParameter("message", "");
		
		buildManager.isBuildingOrInQueue("a", "b");
		expectLastCall().andReturn(false);
		manager.lockProjects("messages.project.locked.by.user", "a", "b");
		expectLastCall().andReturn(9876l);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		
		verifyForward("success");
		
		assertEquals(request.getAttribute("lockId"), 9876l);
	}
	
	public void testLockFailsWhenBuilding() throws Exception {
		addRequestParameter("action", "lock");
		addRequestParameter("projectNames", new String[] {"a", "b"});
		addRequestParameter("message", "locked for QA");
		
		buildManager.isBuildingOrInQueue("a", "b");
		expectLastCall().andReturn(true);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyActionErrors(new String[] {"errors.cannot.lock.project"});
		verifyNoActionMessages();
		
		verifyForward("failure");
	}
	
	public void testUnlockRequiresLockId() throws Exception {
		addRequestParameter("action", "unlock");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError("lockId", "errors.required");
		
		verifyInputForward();
	}
	
	public void testUnlockBadLockId() throws Exception {
		addRequestParameter("action", "unlock");
		addRequestParameter("lockId", "not a number");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError("lockId", "errors.integer");
		
		verifyInputForward();
	}
	
	public void testUnlock() throws Exception {
		addRequestParameter("action", "unlock");
		addRequestParameter("lockId", new String[] {"1234", "9876"});
		
		manager.removeProjectLock(1234l, 9876l);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		
		verifyForward("success");
	}
	
	public void testClear() throws Exception {
		addRequestParameter("action", "clear");
		addRequestParameter("projectNames", new String[] {"a", "b"});
		
		manager.clearProjectLocks("a", "b");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		
		verifyForward("success");
	}
}
