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

import java.util.Collection;
import java.util.UUID;

import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.SqlUpdate;

class DependencyInserter extends SqlUpdate {
	public DependencyInserter(DataSource dataSource) {
		setDataSource(dataSource);
		setSql("insert into build_dependencies " +
				"(build_id, dependency_build_id) " +
				"values (" +
				"(select id from builds where uuid=?), " +
				"(select id from builds where uuid=?))");
		
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		
		compile();
	}
	
	public int insert(UUID parentId, Collection<UUID> deps) {
		int count = 0;
		
		final UUID[] params = new UUID[2];
		params[0] = parentId;
		
		for (UUID depId : deps) {
			params[1] = depId;
			count += update(params);
		}
		
		return count;
	}
}