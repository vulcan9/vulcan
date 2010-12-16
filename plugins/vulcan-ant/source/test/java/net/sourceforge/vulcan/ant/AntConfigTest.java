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
package net.sourceforge.vulcan.ant;

import java.util.Arrays;

import junit.framework.TestCase;

public class AntConfigTest extends TestCase {
	AntConfig config = new AntConfig();
	
	public void testCopy() throws Exception {
		JavaHome[] homes = new JavaHome[] { new JavaHome() };
	
		final String[] strings = new String[] {"a"};
		
		homes[0].setSystemProperties(strings);
		
		config.setJavaHomes(homes);
		
		AntConfig two = (AntConfig) config.copy();
		
		assertEquals(config, two);
		
		assertNotSame(homes, two.getJavaHomes());
		assertNotSame(homes[0], two.getJavaHomes()[0]);
		
		assertEquals("a", two.getJavaHomes()[0].getSystemProperties()[0]);
		
		strings[0] = "b";
		
		assertEquals("a", two.getJavaHomes()[0].getSystemProperties()[0]);
	}
	
	public void testSetAntProps() throws Exception {
		config.setAntProperties(new String[] {" a=b\t", "", "c=d"});
		
		assertTrue(Arrays.toString(config.getAntProperties()), Arrays.equals(new String[] {"a=b", "c=d"}, config.getAntProperties()));
	}
}
