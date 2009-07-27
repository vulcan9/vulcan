/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.naming.NamingException;

import net.sourceforge.vulcan.EasyMockTestCase;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

public class JdbcScreenNameMapperTest extends EasyMockTestCase {
	ResultSet rs = createStrictMock(ResultSet.class);
	
	NamingException namingException;
	
	JdbcScreenNameMapperConfig config = new JdbcScreenNameMapperConfig();
	JdbcScreenNameMapper mapper = new JdbcScreenNameMapper(config) {
		@Override
		protected JdbcTemplate createJdbcTemplate() throws NamingException {
			if (namingException != null) {
				throw namingException;
			}
			return new JdbcTemplate() {
				@SuppressWarnings("unchecked")
				@Override
				public List query(String sql, Object[] args, RowCallbackHandler rch) throws DataAccessException {
					try {
						rch.processRow(rs);
					} catch (SQLException e) {
						throw new UncategorizedSQLException("", "", e);
					}
					return null;
				}
			};
		}
		
	};
	
	public void testMapsUsers() throws Exception {
		expect(rs.getString(1)).andReturn("imsam84");
		expect(rs.getString(1)).andReturn("tara22");
		
		replay();
		
		assertEquals(Arrays.asList("imsam84", "tara22"), mapper.lookupByAuthor(Arrays.asList("Sam", "Tara")));
		
		verify();
	}
	
	public void testHandlesNamingException() throws Exception {
		namingException = new NamingException();
		replay();
		
		assertEquals(Arrays.asList(), mapper.lookupByAuthor(Arrays.asList("Sam", "Tara")));
		
		verify();
	}
}
