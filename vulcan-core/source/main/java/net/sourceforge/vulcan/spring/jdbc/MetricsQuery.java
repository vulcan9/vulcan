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
package net.sourceforge.vulcan.spring.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;

class MetricsQuery extends MappingSqlQuery {
	public MetricsQuery(DataSource dataSource) {
		super(dataSource, "select message_key, metric_type, data " +
				"from metrics where build_id = ? order by message_key");
		declareParameter(new SqlParameter("build_id", Types.NUMERIC));
		compile();
	}
	
	public void queryMetrics(JdbcBuildOutcomeDto outcome) {
		final List<MetricDto> metrics = execute(outcome.getPrimaryKey());
		
		if (metrics.isEmpty()) {
			return;
		}
		
		outcome.setMetrics(metrics);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<MetricDto> execute(int param) throws DataAccessException {
		return super.execute(param);
	}
	
	@Override
	protected MetricDto mapRow(ResultSet rs, int rowNumber) throws SQLException {
		final MetricDto dto = new MetricDto();
		
		dto.setMessageKey(rs.getString("message_key"));
		dto.setType(MetricType.fromId(rs.getString("metric_type").charAt(0)));
		dto.setValue(rs.getString("data"));
		
		return dto;
	}
}