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
package net.sourceforge.vulcan.cvs;

import java.io.File;

import junit.framework.TestCase;
import net.sourceforge.vulcan.TestUtils;

public class CvsPluginTest extends TestCase {
	CvsPlugin plugin = new CvsPlugin();
	String url = ":pserver:anonymous:foo@localhost:/cvsroot:tanks/build.xml";
	
	public void testCreateConfiguratorUnsupportedUrl() throws Exception {
		assertNull(plugin.createProjectConfigurator("http://example.com", null, null));
	}
	
	public void testCreateConfigurator() throws Exception {
		final CvsProjectConfigurator cfgr = plugin.createProjectConfigurator(url, null, null);
		assertNotNull(cfgr);
		assertEquals("tanks/build.xml", cfgr.getFile());
		assertEquals(":pserver:anonymous@localhost:/cvsroot", cfgr.cvsRoot.toString());
	}
	
	public void DtestLiveTest() throws Exception {
		final CvsProjectConfigurator cfgr = plugin.createProjectConfigurator(
				":pserver:gtkpod.cvs.sourceforge.net:/cvsroot/gtkpod:gtkpod/scripts/Makefile.am",
				"anonymous", "");
		
		final File target = TestUtils.resolveRelativeFile("target/cvs-download-test/foo.tmp");
		
		target.getParentFile().mkdirs();
		
		cfgr.download(target);
		
		assertTrue(target.exists());
	}
}
