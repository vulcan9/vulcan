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

import net.sourceforge.vulcan.exception.ProjectNeedsDependencyException;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class DeleteProjectsActionTest extends MockApplicationContextStrutsTestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/admin/setup/deleteProjects");
	}
	
	public void testDelete() throws Exception {
		addRequestParameter("projectNames", new String[] {"a", "b"});
		
		manager.deleteProjectConfig("a", "b");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		
		verifyForward("projectList");
	}

	public void testHandlesDependencyException() throws Exception {
		addRequestParameter("projectNames", new String[] {"a", "b"});
		
		final ProjectNeedsDependencyException e = new ProjectNeedsDependencyException(
				new String[] {"a"}, new String[] {"c"});
		
		manager.deleteProjectConfig("a", "b");
		expectLastCall().andThrow(e);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyNoActionMessages();
		
		verifyInputForward();
		
		assertNotNull(request.getAttribute("projectsWithDependents"));
		assertNotNull(request.getAttribute("dependentProjects"));
	}
}
