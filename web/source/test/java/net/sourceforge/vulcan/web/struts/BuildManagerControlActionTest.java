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

import net.sourceforge.vulcan.dto.BuildManagerConfigDto;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class BuildManagerControlActionTest extends MockApplicationContextStrutsTestCase {
	StateManagerConfigDto stateMgrConfig = new StateManagerConfigDto();
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/admin/buildManagerControl.do");
		BuildManagerConfigDto mgrConfig = new BuildManagerConfigDto();
		mgrConfig.setEnabled(true);
		
		stateMgrConfig.setBuildManagerConfig(mgrConfig);
	}
	
	public void testDisable() throws Exception {
		addRequestParameter("action", "disable");
		
		expect(manager.getConfig())
			.andReturn(stateMgrConfig);
		
		replay();
		
		assertTrue(stateMgrConfig.getBuildManagerConfig().isEnabled());
		actionPerform();
		
		verify();
		
		assertFalse(stateMgrConfig.getBuildManagerConfig().isEnabled());
		
		verifyActionMessages(new String[] {"messages.save.success"});
	}

	public void testEnable() throws Exception {
		addRequestParameter("action", "Enable the manager");
		
		expect(manager.getConfig())
			.andReturn(stateMgrConfig);
		
		replay();
		
		stateMgrConfig.getBuildManagerConfig().setEnabled(false);
		assertFalse(stateMgrConfig.getBuildManagerConfig().isEnabled());
		actionPerform();
		
		verify();
		
		assertTrue(stateMgrConfig.getBuildManagerConfig().isEnabled());
		
		verifyActionMessages(new String[] {"messages.save.success"});
	}
}
