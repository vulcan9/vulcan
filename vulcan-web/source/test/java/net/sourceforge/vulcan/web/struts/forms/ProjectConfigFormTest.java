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
package net.sourceforge.vulcan.web.struts.forms;

import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto.UpdateStrategy;
import junit.framework.TestCase;

public class ProjectConfigFormTest extends TestCase {
	ProjectConfigForm form = new ProjectConfigForm();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		form.populate(new ProjectConfigDto(), true);
	}
	
	public void testSetUpdateStrategy() throws Exception {
		final UpdateStrategy us = UpdateStrategy.CleanDaily;
		
		form.setUpdateStrategy(us.name());
		
		assertEquals(us, form.getProjectConfig().getUpdateStrategy());
	}

	public void testSetUpdateStrategyInvalidDefault() throws Exception {
		final UpdateStrategy us = UpdateStrategy.CleanAlways;
		
		form.setUpdateStrategy("NoneSuch");
		
		assertEquals(us, form.getProjectConfig().getUpdateStrategy());
	}
	
	public void testGetUpdateStrategy() throws Exception {
		final UpdateStrategy us = UpdateStrategy.CleanDaily;
		
		form.getProjectConfig().setUpdateStrategy(us);
		
		assertEquals(us.name(), form.getUpdateStrategy());
	}
}
