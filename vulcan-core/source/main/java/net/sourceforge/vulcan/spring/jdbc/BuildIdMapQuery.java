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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.object.MappingSqlQuery;

class BuildIdMapQuery extends MappingSqlQuery {
	private final Map<String, List<UUID>> map = new HashMap<String, List<UUID>>();
	private String prevName;
	private List<UUID> ids;
	
	BuildIdMapQuery(DataSource dataSource) {
		setDataSource(dataSource);
		setSql("select name, uuid from builds inner join project_names on project_id = project_names.id order by name, build_number");
		compile();
	}
	public Map<String, List<UUID>> executeForMap() {
		map.clear();
		
		execute();
		
		putList();
		
		return map;
		
	}
	@Override
	protected Object mapRow(ResultSet rs, int rowIndex)
			throws SQLException {
		final String name = rs.getString("name");
		
		if (!name.equals(prevName)) {
			putList();
			ids = new ArrayList<UUID>();
			prevName = name;
		}
		
		ids.add(UUID.fromString(rs.getString("uuid")));
		
		return null;
	}
	private void putList() {
		if (ids != null && !ids.isEmpty()) {
			map.put(prevName, ids);
		}
	}
}