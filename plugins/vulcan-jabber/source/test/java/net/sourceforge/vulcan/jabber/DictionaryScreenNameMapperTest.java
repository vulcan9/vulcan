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
package net.sourceforge.vulcan.jabber;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;

public class DictionaryScreenNameMapperTest extends TestCase {
	DictionaryScreenNameMapperConfig config = new DictionaryScreenNameMapperConfig();
	DictionaryScreenNameMapper mapper = new DictionaryScreenNameMapper(config);
	
	public void testNoMappings() throws Exception {
		mapper.digest();
		assertEquals(Collections.emptyMap(), mapper.lookupByAuthor(Arrays.asList("noone")));
	}
	
	public void testMappings() throws Exception {
		config.setEntries(new String[] {"Sam=imsam84"});
		
		mapper.digest();
		assertEquals(Collections.singletonMap("Sam", "imsam84"), mapper.lookupByAuthor(Arrays.asList("Sam")));
	}
	
	public void testCaseInsensitive() throws Exception {
		config.setEntries(new String[] {"Sam=imsam84"});
		
		mapper.digest();
		assertEquals(Collections.singletonMap("sam", "imsam84"), mapper.lookupByAuthor(Arrays.asList("sam")));
	}
	
	public void testTrims() throws Exception {
		config.setEntries(new String[] {" Sam = im sam 84 "});
		
		mapper.digest();
		assertEquals(Collections.singletonMap("Sam", "im sam 84"), mapper.lookupByAuthor(Arrays.asList("Sam")));
	}
}
