/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2007 Chris Eldredge
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.TestFailureDto;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;

class TestFailureQuery extends MappingSqlQuery {
	public TestFailureQuery(DataSource dataSource) {
		super(dataSource, "select name, message, details, first_consecutive_build_number " +
				"from test_failures where build_id = ?");
		declareParameter(new SqlParameter("build_id", Types.NUMERIC));
		compile();
	}
	
	public void queryTestFailures(JdbcBuildOutcomeDto outcome) {
		final List<TestFailureDto> failures = execute(outcome.getPrimaryKey());
		
		if (failures.isEmpty()) {
			return;
		}
		
		outcome.setTestFailures(failures);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<TestFailureDto> execute(int param) throws DataAccessException {
		return super.execute(param);
	}
	
	@Override
	protected TestFailureDto mapRow(ResultSet rs, int rowNumber) throws SQLException {
		final TestFailureDto dto = new TestFailureDto();
		
		dto.setName(rs.getString("name"));
		dto.setMessage(rs.getString("message"));
		dto.setDetails(rs.getString("details"));
		dto.setBuildNumber(rs.getInt("first_consecutive_build_number"));
		
		return dto;
	}
}