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
package net.sourceforge.vulcan.core.support;

import java.util.Arrays;

import junit.framework.TestCase;

public class MetricsSelectorTest extends TestCase {
	MetricSelector sel = new MetricSelector();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		sel.setPreferredMetrics(Arrays.asList("one", "two", "three"));
	}
	
	public void testNoneMatch() throws Exception {
		assertEquals(Arrays.asList("1"), sel.selectDefaultMetrics(Arrays.asList("1")));
	}

	public void testNoneMatchTwoAvailable() throws Exception {
		assertEquals(Arrays.asList("1", "2"), sel.selectDefaultMetrics(Arrays.asList("1", "2")));
	}
	
	public void testNoneMatchStopsAtTwo() throws Exception {
		assertEquals(Arrays.asList("1", "2"), sel.selectDefaultMetrics(Arrays.asList("1", "2", "3")));
	}
	
	public void testMatchOne() throws Exception {
		assertEquals(Arrays.asList("one"), sel.selectDefaultMetrics(Arrays.asList("one")));
	}

	public void testMatchInOrder() throws Exception {
		assertEquals(Arrays.asList("one", "two"), sel.selectDefaultMetrics(Arrays.asList("two", "one")));
	}

	public void testMatchStopsAtTwo() throws Exception {
		assertEquals(Arrays.asList("one", "two"), sel.selectDefaultMetrics(Arrays.asList("two", "one", "three")));
	}

}
