/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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
package net.sourceforge.vulcan.subversion;

import java.util.HashMap;

import junit.framework.TestCase;
import net.sourceforge.vulcan.subversion.dto.SubversionConfigDto;

public class SubversionPluginTest extends TestCase {
	SubversionPlugin plugin = new SubversionPlugin();
	SubversionConfigDto config = new SubversionConfigDto();
	final HashMap<String, Long> map = new HashMap<String,Long>();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		map.put("a", 1231l);
		config.setWorkingCopyByteCounts(map);
		
		plugin.setConfiguration(config);
	}
	
	public void testRenameProject() throws Exception {
		plugin.projectNameChanged("a", "b");
		
		assertFalse(map.containsKey("a"));
		assertEquals(1231l, map.get("b").longValue());
	}
}
