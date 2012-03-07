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

import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.event.Event;

public class ToggleSchedulerActionTest extends MockApplicationContextStrutsTestCase {
	final SchedulerConfigDto config = new SchedulerConfigDto();
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/buildmanagement/toggleScheduler.do");
	}
	
	public void testToggle() throws Exception {
		addRequestParameter("schedulerName", "scheddy");
		
		expect(manager.getSchedulerConfig("scheddy")).andReturn(config);
		
		eventHandler.reportEvent((Event) notNull());
		
		SchedulerConfigDto copy = (SchedulerConfigDto) config.copy();
		copy.setPaused(true);
		
		manager.updateSchedulerConfig("scheddy", copy, false);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		verifyForward("dashboard");
	}
}
