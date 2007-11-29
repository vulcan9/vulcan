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

import java.util.Arrays;
import java.util.List;

import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.struts.forms.LabelForm;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class ManageLabelActionTest extends MockApplicationContextStrutsTestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/admin/setup/manageLabels.do");
	}
	
	public void testEditLoadsForm() throws Exception {
		addRequestParameter("action", "edit");
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("labelForm");
		
		final LabelForm form = (LabelForm) request.getAttribute("labelForm");
		assertNotNull(form);
		assertEquals(null, form.getName());
	}
	
	public void testEditLoadsFormSpecifyName() throws Exception {
		addRequestParameter("action", "edit");
		addRequestParameter("name", "x");
		
		final List<String> projectsByLabel = Arrays.asList("a", "b", "c");
		expect(manager.getProjectConfigNamesByLabel("x")).andReturn(projectsByLabel);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("labelForm");
		
		final LabelForm form = (LabelForm) request.getAttribute("labelForm");
		assertNotNull(form);
		assertEquals("x", form.getName());
		assertEquals(projectsByLabel, Arrays.asList(form.getProjectNames()));
	}
	
	public void testSave() throws Exception {
		addRequestParameter("action", "save");
		addRequestParameter("name", "x");
		addRequestParameter("projectNames", new String[] {"b"});
		
		manager.applyProjectLabel("x", Arrays.asList("b"));
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("setup");
		verifyActionMessages(new String[] {"messages.save.success"});
	}
}
