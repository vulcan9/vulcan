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

import net.sourceforge.vulcan.dto.SchedulerConfigDto;


public class ManageBuildDaemonConfigActionTest extends MockApplicationContextStrutsTestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/admin/setup/manageBuildDaemonConfig.do");
	}
	
	public void testCreateScheduler() throws Exception {
		SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("a name");
		config.setInterval(50);
		
		addRequestParameter("action", "create");
		addRequestParameter("config.name", "a name");
		addRequestParameter("intervalScalar", "10");
		addRequestParameter("intervalMultiplier", "5");
		
		manager.addBuildDaemonConfig(config);
		replay();
		
		actionPerform();
		
		verify();
		
		verifyActionMessages(new String[] {"messages.save.success"});
	}

	public void testErrorOnNoScalar() throws Exception {
		SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("a name");
		config.setInterval(50);
		
		addRequestParameter("action", "create");
		addRequestParameter("config.name", "a name");
		addRequestParameter("intervalMultiplier", "5");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		assertPropertyHasError("intervalScalar", "errors.required");
	}
	public void testErrorOnBadNumber() throws Exception {
		SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("a name");
		config.setInterval(50);
		
		addRequestParameter("action", "create");
		addRequestParameter("config.name", "a name");
		addRequestParameter("intervalScalar", "dfsa");
		addRequestParameter("intervalMultiplier", "dfsa");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		assertPropertyHasError("intervalScalar", "errors.integer");
		assertPropertyHasError("intervalMultiplier", "errors.integer");
	}
	public void testDelete() throws Exception {
		SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("a name");
		config.setInterval(50);
		
		addRequestParameter("action", "Delete");
		addRequestParameter("config.name", "a name");

		manager.deleteBuildDaemonConfig("a name");
		replay();
		
		actionPerform();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		
		verify();
	}
}
