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
package net.sourceforge.vulcan.cvs.dto;

import junit.framework.TestCase;

public class CvsAggregateRevisionTokenDtoTest extends TestCase {
	@SuppressWarnings("deprecation")
	public void testEquals() throws Exception {
		CvsAggregateRevisionTokenDto r1 = new CvsAggregateRevisionTokenDto("1234", "1900/10/22 05:24:52");
		CvsAggregateRevisionTokenDto r2 = new CvsAggregateRevisionTokenDto("1234", "2006/10/22 05:24:52");
		
		assertEquals(r1, r2);
	}
	@SuppressWarnings("deprecation")
	public void testNotEquals() throws Exception {
		CvsAggregateRevisionTokenDto r1 = new CvsAggregateRevisionTokenDto("1234", "2006/10/22 05:24:52");
		CvsAggregateRevisionTokenDto r2 = new CvsAggregateRevisionTokenDto("5678", "2006/10/22 05:24:52");
		
		assertFalse(r1.equals(r2));
	}
	@SuppressWarnings("deprecation")
	public void testCreate() throws Exception {
		CvsAggregateRevisionTokenDto r = new CvsAggregateRevisionTokenDto("1234", "2006/10/22 05:24:52");
		
		assertEquals("2006/10/22 05:24:52", r.getLabel());
		assertEquals(20061022052452l, r.getRevision().longValue());
	}
}
