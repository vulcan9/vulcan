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
package net.sourceforge.vulcan.spring.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;

import org.springframework.jdbc.core.SqlParameter;

public class BuildHistoryQueryTest extends TestCase {
	BuildOutcomeQueryDto dto = new BuildOutcomeQueryDto();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		dto.setProjectNames(makeProjectNames("a"));
	}
	
	public void testThrowsOnNoProjects() throws Exception {
		dto.setProjectNames(makeProjectNames());
		
		try {
			assertWhereClause(null);
			fail("Expected exception");
		} catch (IllegalArgumentException e) {
		}
	}
	
	public void testBuildsQueryOneProject() throws Exception {
		assertWhereClause("where project_names.name=?", "a");
	}

	public void testBuildsQueryManyProjectSortsNames() throws Exception {
		dto.setProjectNames(makeProjectNames("b", "c", "a"));
		assertWhereClause("where project_names.name in (?,?,?)", "a", "b", "c");
	}
	
	public void testBuildsQueryByDate() throws Exception {
		dto.setMinDate(new Date());
		dto.setMaxDate(new Date());
		assertWhereClause("where project_names.name=? and completion_date>=? and completion_date<?", "a", dto.getMinDate(), dto.getMaxDate());
	}
	
	
	public void testBuildsQueryByBuildNumber() throws Exception {
		dto.setMinBuildNumber(22);
		dto.setMaxBuildNumber(45);
		assertWhereClause("where project_names.name=? and build_number>=? and build_number<=?", "a", 22, 45);
	}
	
	private void assertWhereClause(String whereClause, Object... params) {
		final List<SqlParameter> actualParams = new ArrayList<SqlParameter>();
		final Object[][] parameterValues = new Object[1][];
		final String[] sql = new String[1];
		
		HistoryQueryBuilder.buildQuery(dto, new BuilderQuery() {
			public void declareParameter(SqlParameter sqlParameter) {
				actualParams.add(sqlParameter);
			}
			public void setParameterValues(Object[] pv) {
				parameterValues[0] = pv;
			}
			public void setSql(String string) {
				sql[0] = string;
			}
		});
		
		assertEquals(HistoryQueryBuilder.BUILD_INFO_SQL + whereClause, sql[0]);
		
		assertTrue("Expected " + Arrays.toString(params)  +
				" but was "  + Arrays.toString(parameterValues[0]),
				Arrays.equals(params, parameterValues[0]));
	}
	
	private Set<String> makeProjectNames(String... names)
	{
		return new HashSet<String>(Arrays.asList(names));
	}
}
