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

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;

import org.springframework.jdbc.core.SqlParameter;

class BuildHistoryQuery extends BuildQuery {
	private Object[] parameterValues;
	private String whereClause;
	private BuildHistoryMetricsQuery metricsQuery;
	
	public BuildHistoryQuery(DataSource dataSource, BuildOutcomeQueryDto queryDto) {
		super(dataSource, false);
		
		buildQuery(queryDto);
		
		metricsQuery = new BuildHistoryMetricsQuery(dataSource, whereClause, getDeclaredParameters(), parameterValues);
	}

	public Object[] getParameterValues() {
		return parameterValues;
	}
	
	@SuppressWarnings("unchecked")
	public List<JdbcBuildOutcomeDto> queryForHistory() {
		final List<JdbcBuildOutcomeDto> builds = execute(parameterValues);
		
		metricsQuery.queryMetrics(builds);
		
		return builds;
	}
	
	private void buildQuery(BuildOutcomeQueryDto dto) {
		final Set<String> projectNames = dto.getProjectNames();
		if (projectNames == null || projectNames.isEmpty()) {
			throw new IllegalArgumentException("Must query for at least one project name");
		}
		
		final List<? super Object> params = new ArrayList<Object>();
		
		final StringBuilder sb = new StringBuilder();
		
		sb.append("where name");
		
		declareParameter(new SqlParameter(Types.VARCHAR));
		
		if (projectNames.size() == 1) {
			sb.append("=?");
			params.addAll(projectNames);
		} else {
			sb.append(" in (?");
			for (int i=1; i<projectNames.size(); i++) {
				sb.append(",?");
				declareParameter(new SqlParameter(Types.VARCHAR));
			}
			sb.append(")");
			
			final List<String> sorted = new ArrayList<String>(projectNames);
			Collections.sort(sorted);
			params.addAll(sorted);
		}
		
		if (dto.getMinDate() != null) {
			sb.append(" and completion_date>=?");
			params.add(dto.getMinDate());
			declareParameter(new SqlParameter(Types.TIMESTAMP));
		}
		
		if (dto.getMaxDate() != null) {
			sb.append(" and completion_date<?");
			params.add(dto.getMaxDate());
			declareParameter(new SqlParameter(Types.TIMESTAMP));
		}
		
		if (dto.getMinBuildNumber() != null) {
			sb.append(" and build_number>=?");
			params.add(dto.getMinBuildNumber());
			declareParameter(new SqlParameter(Types.INTEGER));
		}
		
		if (dto.getMaxBuildNumber() != null) {
			sb.append(" and build_number<=?");
			params.add(dto.getMaxBuildNumber());
			declareParameter(new SqlParameter(Types.INTEGER));
		}
		
		parameterValues = params.toArray();
		whereClause = sb.toString();
		
		setSql(SQL + whereClause + " order by completion_date");
		compile();
	}
}
