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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import junit.framework.TestCase;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcScreenNameMapperTest extends TestCase {
	NamingException namingException;
	
	Map<String, String> fakeResults = new HashMap<String, String>();
	
	JdbcScreenNameMapperConfig config = new JdbcScreenNameMapperConfig();
	JdbcScreenNameMapper mapper = new JdbcScreenNameMapper(config) {
		@Override
		protected JdbcTemplate createJdbcTemplate() throws NamingException {
			if (namingException != null) {
				throw namingException;
			}
			return new JdbcTemplate() {
				@Override
				@SuppressWarnings("unchecked")
				public Object queryForObject(String sql, Object[] args,
						Class requiredType) throws DataAccessException {
					if (fakeResults.containsKey(args[0].toString())) {
						return fakeResults.get(args[0].toString());
					}
					throw new IncorrectResultSizeDataAccessException(1, 0);
				}
			};
		}
		
	};
	
	public void testMapsUsers() throws Exception {
		fakeResults.put("Sam", "imsam84");
		fakeResults.put("Tara", "tara22");
		
		Map<String, String> map = new HashMap<String, String>();
		map.put("Sam", "imsam84");
		map.put("Tara", "tara22");
		
		assertEquals(map, mapper.lookupByAuthor(Arrays.asList("Sam", "Tara")));
	}
	
	public void testHandlesUserNotFound() throws Exception {
		fakeResults.put("Tara", "tara22");
		
		assertEquals(Collections.singletonMap("Tara", "tara22"), mapper.lookupByAuthor(Arrays.asList("Sam", "Tara")));
	}
	
	public void testHandlesNamingException() throws Exception {
		namingException = new NamingException();
		
		assertEquals(Collections.emptyMap(), mapper.lookupByAuthor(Arrays.asList("Sam", "Tara")));
	}
}
