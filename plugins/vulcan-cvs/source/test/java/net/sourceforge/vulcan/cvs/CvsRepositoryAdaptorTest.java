/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2007 Chris Eldredge
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
package net.sourceforge.vulcan.cvs;

import net.sourceforge.vulcan.cvs.dto.CvsConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsProjectConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsRepositoryProfileDto;
import junit.framework.TestCase;

public class CvsRepositoryAdaptorTest extends TestCase {
	CvsConfigDto globalConfig = new CvsConfigDto();
	CvsRepositoryProfileDto profile = new CvsRepositoryProfileDto();
	CvsProjectConfigDto config = new CvsProjectConfigDto();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		profile.setProtocol("pserver");
		profile.setHost("localhost");
		profile.setUsername("");
		profile.setPassword("");
		profile.setRepositoryPath("/x/y/z");
	}
	public void testUseHeadIfNoBranchSpecified() throws Exception {
		final CvsRepositoryAdaptor ra = new CvsRepositoryAdaptor(globalConfig, profile, config, "foo", false);
		
		assertEquals("HEAD", ra.getTagName());
	}
	
	public void testUseBranchIfSpecified() throws Exception {
		config.setBranch("branchy");
		
		final CvsRepositoryAdaptor ra = new CvsRepositoryAdaptor(globalConfig, profile, config, "foo", false);
		
		assertEquals("branchy", ra.getTagName());
	}
}
