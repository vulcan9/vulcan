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
package net.sourceforge.vulcan.ant.io;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import junit.framework.TestCase;

public class ByteSerializerTest extends TestCase {
	Serializer s = new ByteSerializer();
	
	String longString;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		final StringBuilder sb = new StringBuilder();
		for (int i=0; i<1100; i++) {
			sb.append("a");
		}
		
		longString = sb.toString();
	}
	public void testSimple() throws Exception {
		final byte[] data = s.serialize(new AntEventSummary("Foo", null, null, null, null));
		
		assertNotNull(data);
		assertEquals(3, data[0] + (data[1] << 8));
	}
	public void testLong() throws Exception {
		final byte[] data = s.serialize(new AntEventSummary(longString, null, null, null, null));
		
		assertEquals(longString.length(), data[0] + (data[1] << 8));
	}
	public void testRoundTrip() throws Exception {
		final AntEventSummary a = new AntEventSummary("a", "b", "c", "d", "e", 4, "Jay.java", 33, "X32");
		
		final byte[] data = s.serialize(a);
		
		final AntEventSummary b = s.deserialize(data);
		
		assertEquals(a, b);
	}
	public void testBlanksAndNull() throws Exception {
		final AntEventSummary a = new AntEventSummary("a", "b", "", null, "e", 4, "x.java", null, null);
		
		final byte[] data = s.serialize(a);
		
		final AntEventSummary b = s.deserialize(data);
		
		assertEquals(a, b);
	}
	
	public static void assertEquals(AntEventSummary expected, AntEventSummary actual) {
		final boolean equal = EqualsBuilder.reflectionEquals(expected, actual);
		
		final StringBuilder sb = new StringBuilder("expected ");
		sb.append(ToStringBuilder.reflectionToString(expected));
		sb.append(" but was ");
		sb.append(ToStringBuilder.reflectionToString(actual));
		
		assertTrue(sb.toString(), equal);
	}
}
