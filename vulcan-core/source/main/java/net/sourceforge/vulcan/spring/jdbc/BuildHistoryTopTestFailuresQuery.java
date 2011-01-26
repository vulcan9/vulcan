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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;
import net.sourceforge.vulcan.dto.TestFailureDto;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.object.MappingSqlQuery;

class BuildHistoryTopTestFailuresQuery extends MappingSqlQuery implements BuilderQuery {
	private static String SQL = "select test.name as name, count(test.name) as msg_count " +
		"from builds inner join project_names on builds.project_id = project_names.id inner join test_failures test on builds.id=test.build_id ";
	
	private final int maxResultCount;
	private Object[] parameterValues;
	
	public BuildHistoryTopTestFailuresQuery(DataSource dataSource, BuildOutcomeQueryDto query, int maxResultCount) {
		this.maxResultCount = maxResultCount;
		setDataSource(dataSource);
		HistoryQueryBuilder.buildQuery(SQL, query, this);
		compile();
	}
	
	@Override
	public void setSql(String sql) {
		super.setSql(sql + " group by name order by msg_count desc limit " + maxResultCount);
	}
	
	public List<TestFailureDto> queryTopMessages() {
		return execute(parameterValues);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<TestFailureDto> execute(Object[] params) throws DataAccessException {
		return super.execute(params);
	}

	public void setParameterValues(Object[] parameterValues) {
		this.parameterValues = parameterValues;
	}
	
	@Override
	protected TestFailureDto mapRow(ResultSet rs, int rowNumber) throws SQLException {
		final TestFailureDto dto = new TestFailureDto();
		
		dto.setCount(rs.getInt("msg_count"));
		dto.setName(rs.getString("name"));
		
		return dto;
	}
}