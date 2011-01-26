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
package net.sourceforge.vulcan.web.struts;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.vulcan.dto.BuildArtifactLocationDto;

public class SaveArtifactLocationsActionTest extends MockApplicationContextStrutsTestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/admin/setup/saveArtifactLocations.do");
	}
	
	public void testSaveNone() throws Exception {
		List<BuildArtifactLocationDto> list = new ArrayList<BuildArtifactLocationDto>();
		
		manager.updateArtifactLocations(list);
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		verifyForward("setup");
	}
	
	public void testSaveTwo() throws Exception {
		addRequestParameter("name", new String[] {"a", "b"});
		addRequestParameter("desc", new String[] {"a desc", "b desc"});
		addRequestParameter("path", new String[] {"a/path", "b/path"});
		
		List<BuildArtifactLocationDto> list = new ArrayList<BuildArtifactLocationDto>();
		list.add(new BuildArtifactLocationDto("a", "a desc", "a/path", true));
		list.add(new BuildArtifactLocationDto("b", "b desc", "b/path", true));
		
		manager.updateArtifactLocations(list);
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		verifyForward("setup");
	}
	
	public void testMismatchingArrayLengths() throws Exception {
		addRequestParameter("name", new String[] {"a", "b", "c"});
		addRequestParameter("desc", new String[] {"a desc", "b desc"});
		addRequestParameter("path", new String[] {"a/path"});
		
		replay();
		
		try {
			actionPerform();
			fail("expected exception");
		} catch (IllegalStateException e) {
		}
		
		verify();
	}
	
	public void testMissingParameters() throws Exception {
		addRequestParameter("name", new String[] {"a", "b"});
		
		replay();
		
		try {
			actionPerform();
			fail("expected exception");
		} catch (IllegalStateException e) {
		}
		
		verify();
	}
}
