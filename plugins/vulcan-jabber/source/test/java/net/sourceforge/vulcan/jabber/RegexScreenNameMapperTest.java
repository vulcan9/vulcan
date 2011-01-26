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
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class RegexScreenNameMapperTest extends TestCase {
	public void testIdentity() throws Exception {
		final RegexScreenNameMapper mapper = new RegexScreenNameMapper(new RegexScreenNameMapperConfig());
		final Map<String, String> expected = new HashMap<String, String>();
		
		expected.put("Sam", "Sam");
		expected.put("Jill", "Jill");
		
		final Map<String, String> result = mapper.lookupByAuthor(expected.keySet());
		
		assertEquals(expected, result);
		assertNotSame(expected, result);
	}
	
	public void testReplacementWithCapture() throws Exception {
		final RegexScreenNameMapperConfig config = new RegexScreenNameMapperConfig();
		
		config.setRegex("domain\\\\(.*)");
		config.setReplacement("$1");
		
		final RegexScreenNameMapper mapper = new RegexScreenNameMapper(config);
		final Map<String, String> result = mapper.lookupByAuthor(Arrays.asList("domain\\username"));
		
		assertEquals(Collections.singletonMap("domain\\username", "username"), result);
	}
	
	public void testIdentityWithSuffix() throws Exception {
		final RegexScreenNameMapperConfig config = new RegexScreenNameMapperConfig();
		
		config.setRegex("(.*)");
		config.setReplacement("$1@gmail.com");
		
		final RegexScreenNameMapper mapper = new RegexScreenNameMapper(config);
		final Map<String, String> result = mapper.lookupByAuthor(Arrays.asList("chris.eldredge"));
		
		assertEquals(Collections.singletonMap("chris.eldredge", "chris.eldredge@gmail.com"), result);
	}
}
