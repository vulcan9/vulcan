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

import org.apache.struts.action.ActionMessages;

import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

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
	}
	
	public void testImportUrl() throws Exception {
		addRequestParameter("url", "http://www.example.com");
		
		projectImporter.createProjectsForUrl("http://www.example.com", false);
		
		replay();
		
		actionPerform();
		
		verify();

		verifyForward("success");
	}
	
	public void testHandlesConfigException() throws Exception {
		addRequestParameter("url", "http://www.example.com");
		
		projectImporter.createProjectsForUrl("http://www.example.com", false);
		expectLastCall().andThrow(new ConfigException("foo.bar", new Object[] {"a", "b"}));
		
		replay();
		
		actionPerform();
		
		verify();

		verifyInputForward();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "foo.bar");
	}
	
	public void testHandlesStoreException() throws Exception {
		addRequestParameter("url", "http://www.example.com");
		
		projectImporter.createProjectsForUrl("http://www.example.com", false);
		expectLastCall().andThrow(new StoreException("a message", null));
		
		replay();
		
		actionPerform();
		
		verify();

		verifyForward("failure");
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "messages.save.failure");
	}
}
