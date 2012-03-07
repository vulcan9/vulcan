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

import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.SimplePrincipal;

public class ClaimBrokenBuildActionTest extends MockApplicationContextStrutsTestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/wall/claimBrokenBuild.do");
	}
	
	public void testClaim() throws Exception {
		addRequestParameter("action", "claim");
		addRequestParameter("projectName", "my project");
		addRequestParameter("buildNumber", "1423");
		request.setUserPrincipal(new SimplePrincipal("Suzanne"));
		
		expect(buildManager.claimBrokenBuild("my project", 1423, "Suzanne")).andReturn(true);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		
		verifyForward("dashboard");
	}
	
	public void testClaimInvalidBuild() throws Exception {
		addRequestParameter("action", "claim");
		addRequestParameter("projectName", "my project");
		addRequestParameter("buildNumber", "1423");
		request.setUserPrincipal(new SimplePrincipal("Suzanne"));
		
		expect(buildManager.claimBrokenBuild("my project", 1423, "Suzanne"))
			.andThrow(new IllegalArgumentException());
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyActionErrors(new String[] {"errors.status.not.available.by.build.number"});
		
		verifyForward("error");
	}
	
	public void testClaimInvalidProjectName() throws Exception {
		addRequestParameter("action", "claim");
		addRequestParameter("buildNumber", "1423");
		request.setUserPrincipal(new SimplePrincipal("Suzanne"));
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyActionErrors(new String[] {"errors.required.with.name"});
		verifyForward("error");
	}
	
	public void testRedirectsToReferrerWhenSpecified() throws Exception {
		addRequestParameter("action", "claim");
		addRequestParameter("projectName", "my project");
		addRequestParameter("buildNumber", "1423");
		request.setHeader("referer", "http://example.com");
		
		request.setUserPrincipal(new SimplePrincipal("Suzanne"));
		
		expect(buildManager.claimBrokenBuild("my project", 1423, "Suzanne")).andReturn(true);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		
		assertEquals("http://example.com", response.getHeader("Location"));
		assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatusCode());
	}
}
