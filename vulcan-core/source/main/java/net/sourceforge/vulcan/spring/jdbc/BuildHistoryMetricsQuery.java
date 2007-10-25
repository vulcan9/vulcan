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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.MetricDto;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.object.MappingSqlQuery;

class BuildHistoryMetricsQuery extends MappingSqlQuery {
	private static Log LOG = LogFactory.getLog(BuildHistoryMetricsQuery.class);
	private static String SQL = "select metrics.build_id, metrics.message_key, metrics.data " +
		"from builds left join project_names on builds.project_id = project_names.id left join metrics on builds.id=metrics.build_id ";
	
	private final Object[] parameterValues;
	
	@SuppressWarnings("unchecked")
	public BuildHistoryMetricsQuery(DataSource dataSource, String whereClause, List<?> declaredParameters, Object[] parameterValues) {
		this.parameterValues = parameterValues;
		setDataSource(dataSource);
		setSql(SQL + whereClause + " order by build_id, message_key");
		getDeclaredParameters().addAll(declaredParameters);
		compile();
	}
	
	public void queryMetrics(List<JdbcBuildOutcomeDto> builds) {
		final List<JdbcMetricDto> metrics = execute(parameterValues);
		
		if (metrics.isEmpty()) {
			return;
		}
		
		splitMetricsByBuildId(builds, metrics);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<JdbcMetricDto> execute(Object[] params) throws DataAccessException {
		return super.execute(params);
	}

	@Override
	protected JdbcMetricDto mapRow(ResultSet rs, int rowNumber) throws SQLException {
		final JdbcMetricDto dto = new JdbcMetricDto();
		
		dto.setBuildId(rs.getInt("build_id"));
		dto.setMessageKey(rs.getString("message_key"));
		dto.setValue(rs.getString("data"));
		
		return dto;
	}

	static void splitMetricsByBuildId(List<JdbcBuildOutcomeDto> builds, List<JdbcMetricDto> metrics) {
		final Map<Integer, JdbcBuildOutcomeDto> buildsById = new HashMap<Integer, JdbcBuildOutcomeDto>();
		
		for (JdbcBuildOutcomeDto build : builds) {
			buildsById.put(build.getPrimaryKey(), build);
		}

		final int size = metrics.size();

		if (size == 1) {
			buildsById.get(metrics.get(0).getBuildId()).setMetrics(Collections.<MetricDto>unmodifiableList(metrics));
			return;
		}
		
		Integer currentBuildId = metrics.get(0).getBuildId();
		int i = 0;
		int j = 1;
		
		while (j < size) {
			while (j < size && metrics.get(j).getBuildId().equals(currentBuildId)) {
				j++;
			}
			
			final List<MetricDto> metricsForBuild = Collections.<MetricDto>unmodifiableList(metrics.subList(i, j));
			
			if (buildsById.containsKey(currentBuildId)) {
				buildsById.get(currentBuildId).setMetrics(metricsForBuild);
			} else {
				LOG.error("Got metrics for missing build " + currentBuildId);
			}
			
			i = j;
			
			if (j < size) {
				currentBuildId = metrics.get(j).getBuildId();
			}
		}
	}
}