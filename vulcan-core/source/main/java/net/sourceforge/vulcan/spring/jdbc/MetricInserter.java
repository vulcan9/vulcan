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
import java.util.List;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.MetricDto;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.SqlUpdate;

class MetricInserter extends SqlUpdate {
	public MetricInserter(DataSource dataSource) {
		setDataSource(dataSource);
		setSql("insert into metrics " +
				"(build_id, message_key, metric_type, data) " +
				"values (?, ?, ?, ?)");
		
		declareParameter(new SqlParameter(Types.NUMERIC));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.CHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		
		compile();
	}
	
	public int insert(int buildId, List<MetricDto> metrics) {
		int count = 0;
		
		final Object[] params = new Object[4];
		
		params[0] = buildId;

		for (MetricDto dto : metrics) {
			params[1] = dto.getMessageKey();
			params[2] = dto.getType().getId();
			params[3] = dto.getValue();
			
			count += update(params);
		}
		
		return count;
	}
}