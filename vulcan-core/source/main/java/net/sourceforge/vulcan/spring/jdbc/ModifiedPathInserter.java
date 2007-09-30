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

import javax.sql.DataSource;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.SqlUpdate;

class ModifiedPathInserter extends SqlUpdate {
	public ModifiedPathInserter(DataSource dataSource) {
		setDataSource(dataSource);
		setSql("insert into modified_paths " +
				"(build_id, change_set_id, modified_path) " +
				"values (?, ?, ?)");
		
		declareParameter(new SqlParameter(Types.NUMERIC));
		declareParameter(new SqlParameter(Types.NUMERIC));
		declareParameter(new SqlParameter(Types.VARCHAR));
		
		compile();
	}
	
	public int insert(int buildId, int changeSetId, String[] modifiedPaths) {
		int count = 0;
		
		final Object[] params = new Object[3];
		
		params[0] = buildId;
		params[1] = changeSetId;

		for (String path : modifiedPaths) {
			params[2] = path;
			
			count += update(params);
		}
		
		return count;
	}
}