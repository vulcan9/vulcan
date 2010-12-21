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

import java.io.InputStream;

import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.event.Event;

public class ImportConfigActionTest extends MockApplicationContextStrutsTestCase {
	StateManagerConfigDto stateManagerConfig = new StateManagerConfigDto();
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/admin/importConfiguration.do");
	}
	
	public void testUploadNoFile() throws Exception {
		addRequestParameter("action", "Upload");
		
		replay();
		
		actionPerform();
		
		verify();

		assertPropertyHasError("configFile", "errors.required");
		verifyInputForward();
	}

	public void testUpload() throws Exception {
		setMultipartRequestHandlerStub();
		
		final InputStream is = uploadFile("configFile", "foo.zip", "ababa").getInputStream();

		manager.shutdown();
		manager.start();
		
		configurationStore.importConfiguration(is);

		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		verifyForward("dashboard");
	}
	public void testStartFails() throws Exception {
		setMultipartRequestHandlerStub();
		
		final InputStream is = uploadFile("configFile", "foo.zip", "ababa").getInputStream();
		
		manager.shutdown();
		manager.start();
		
		expectLastCall().andThrow(new RuntimeException("your config sucks."));
		
		configurationStore.importConfiguration(is);

		eventHandler.reportEvent((Event) anyObject());
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		verifyForward("dashboard");
	}
}
