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
package net.sourceforge.vulcan.web.struts;

import junit.framework.AssertionFailedError;
import net.sourceforge.vulcan.SimplePrincipal;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.scheduler.BuildDaemon;
import net.sourceforge.vulcan.scheduler.thread.BuildDaemonImpl;

public class KillBuildActionTest extends MockApplicationContextStrutsTestCase {
	boolean abortCalled = false;
	String abortUsername;
	ProjectConfigDto currentTarget;
	
	BuildDaemon bd = new BuildDaemonImpl() {
		@Override
		public void abortCurrentBuild(String requestUsername) {
			abortCalled = true;
			abortUsername = requestUsername;
		}
		@Override
		protected ProjectBuilder createBuilder() {
			throw new UnsupportedOperationException();
		}
		@Override
		public synchronized ProjectConfigDto getCurrentTarget() {
			return KillBuildActionTest.this.currentTarget;
		}
	};
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/buildmanagement/kill.do");
		
		currentTarget = new ProjectConfigDto();
		currentTarget.setName("myProject");
		
	}
	
	public void testKill() throws Exception {
		request.setUserPrincipal(new SimplePrincipal("Suzanne"));
		doTest();
		assertEquals("Suzanne", abortUsername);
	}

	public void testKillNoUsername() throws Exception {
		request.setRemoteHost("killers.com");
		doTest();
		assertEquals("killers.com", abortUsername);
	}
	
	public void testNullDaemon() throws Exception {
		
		addRequestParameter("daemonName", "a");
		
		expect(manager.getBuildDaemon("a"))
			.andReturn(null);
		
		replay();
		
		assertFalse(abortCalled);
		
		actionPerform();
		
		assertFalse(abortCalled);
		
		verify();
		
		verifyActionErrors(new String[] {"errors.build.daemon.not.found"});
		verifyNoActionMessages();
		
		verifyForward("failure");
	}
	
	private void doTest() throws AssertionFailedError {
		addRequestParameter("daemonName", "a");
		
		expect(manager.getBuildDaemon("a"))
			.andReturn(bd);
		
		eventHandler.reportEvent((Event) notNull());
		
		replay();
		
		assertFalse(abortCalled);
		
		actionPerform();
		
		assertTrue(abortCalled);
		
		verify();
		
		verifyNoActionErrors();
		verifyNoActionMessages();
		
		verifyForward("dashboard");
	}
}
