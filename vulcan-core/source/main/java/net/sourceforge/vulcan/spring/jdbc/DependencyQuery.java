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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;

class DependencyQuery extends MappingSqlQuery {
	public DependencyQuery(DataSource dataSource) {
		super(dataSource, "select name, uuid " +
				"from builds " +
				"inner join project_names on builds.project_id = project_names.id " +
				"inner join build_dependencies on builds.id = build_dependencies.dependency_build_id " +
				"where build_dependencies.build_id = ?");
		declareParameter(new SqlParameter("build_dependencies.build_id", Types.NUMERIC));
		compile();
	}
	@SuppressWarnings("unchecked")
	public Map<String, UUID> queryForDependencyMap(int primaryKey) {
		final List<Entry<String, UUID>> deps = execute(new Object[] {primaryKey});
		
		final Map<String, UUID> map = new HashMap<String, UUID>();
		
		for (Entry<String, UUID> dep : deps) {
			map.put(dep.getKey(), dep.getValue());
		}
		return map;
	}
	@Override
	@SuppressWarnings("unchecked")
	protected Entry<String, UUID> mapRow(ResultSet rs, int rowNumber) throws SQLException {
		return new DefaultMapEntry(rs.getString("name"), UUID.fromString(rs.getString("uuid")));
	}
}