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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.NameCollisionResolutionMode;
import net.sourceforge.vulcan.exception.AuthenticationRequiredRepositoryException;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.struts.forms.ProjectImportForm;

import org.apache.commons.lang.ArrayUtils;
import org.apache.struts.action.ActionMessages;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class CreateProjectFromUrlActionTest extends MockApplicationContextStrutsTestCase {
	RepositoryAdaptor ra = createMock(RepositoryAdaptor.class);
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/admin/setup/createProjectFromUrl.do");
	}

	public void testRequiredParams() throws Exception {
		replay();
		
		actionPerform();
		
		verify();

		assertPropertyHasError("url", "errors.required");
		assertPropertyHasError("nameCollisionResolutionMode", "errors.required");
	}
	
	public void testImportUrl() throws Exception {
		addRequestParameter("url", "http://www.example.com");
		addRequestParameter("nameCollisionResolutionMode", NameCollisionResolutionMode.Abort.name());
		addRequestParameter("labels", "x");
		addRequestParameter("newLabel", "y");
		
		final Set<String> labels = new HashSet<String>(Arrays.asList("x", "y"));
		
		projectImporter.createProjectsForUrl("http://www.example.com", "", "", false, NameCollisionResolutionMode.Abort, ArrayUtils.EMPTY_STRING_ARRAY, labels);
		
		replay();
		
		actionPerform();
		
		verify();

		verifyNoActionErrors();
		
		verifyForward("success");
	}
	
	public void testHandlesConfigException() throws Exception {
		addRequestParameter("url", "http://www.example.com");
		addRequestParameter("nameCollisionResolutionMode", NameCollisionResolutionMode.UseExisting.name());
		
		projectImporter.createProjectsForUrl("http://www.example.com", "", "", false, NameCollisionResolutionMode.UseExisting, ArrayUtils.EMPTY_STRING_ARRAY, Collections.<String>emptySet());
		
		expectLastCall().andThrow(new ConfigException("foo.bar", new Object[] {"a", "b"}));
		
		replay();
		
		actionPerform();
		
		verify();

		verifyInputForward();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "foo.bar");
	}
	
	public void testHandlesAuthException() throws Exception {
		addRequestParameter("url", "http://www.example.com");
		addRequestParameter("nameCollisionResolutionMode", NameCollisionResolutionMode.UseExisting.name());
		
		projectImporter.createProjectsForUrl("http://www.example.com", "", "", false, NameCollisionResolutionMode.UseExisting, ArrayUtils.EMPTY_STRING_ARRAY, Collections.<String>emptySet());
		
		expectLastCall().andThrow(new AuthenticationRequiredRepositoryException("teresa"));
		
		replay();
		
		actionPerform();
		
		verify();

		verifyInputForward();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "messages.repository.authentication.required");
		ProjectImportForm form = (ProjectImportForm) request.getSession().getAttribute("projectImportForm");
		
		assertTrue(form.isAuthenticationRequired());
		assertEquals("teresa", form.getUsername());
	}
	
	public void testHandlesStoreException() throws Exception {
		addRequestParameter("url", "http://www.example.com");
		addRequestParameter("nameCollisionResolutionMode", NameCollisionResolutionMode.Overwrite.name());
		
		projectImporter.createProjectsForUrl("http://www.example.com", "", "", false, NameCollisionResolutionMode.Overwrite, ArrayUtils.EMPTY_STRING_ARRAY, Collections.<String>emptySet());
		expectLastCall().andThrow(new StoreException("a message", null));
		
		replay();
		
		actionPerform();
		
		verify();

		verifyForward("failure");
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "messages.save.failure");
	}
	
	public void testHandlesDuplicateNameException() throws Exception {
		addRequestParameter("url", "http://www.example.com");
		addRequestParameter("nameCollisionResolutionMode", NameCollisionResolutionMode.Abort.name());
		
		projectImporter.createProjectsForUrl("http://www.example.com", "", "", false, NameCollisionResolutionMode.Abort, ArrayUtils.EMPTY_STRING_ARRAY, Collections.<String>emptySet());
		
		expectLastCall().andThrow(new DuplicateNameException("scuba"));
		
		replay();
		
		actionPerform();
		
		verify();

		verifyInputForward();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.duplicate.project.name");
	}

}
