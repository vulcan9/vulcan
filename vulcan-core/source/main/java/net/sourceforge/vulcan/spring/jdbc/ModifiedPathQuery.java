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

import java.util.List;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;

class ModifiedPathQuery extends MappingSqlQuery {
	public ModifiedPathQuery(DataSource dataSource) {
		super(dataSource, "select modified_path " +
				"from modified_paths where build_id = ? and change_set_id = ? order by modified_path");
		
		declareParameter(new SqlParameter("build_id", Types.NUMERIC));
		declareParameter(new SqlParameter("modified_path", Types.NUMERIC));
		
		compile();
	}
	
	public List<String> queryModifiedPaths(int buildId, int changeSetId) {
		final List<String> paths = execute(buildId, changeSetId);
		
		if (paths.isEmpty()) {
			return null;
		}

		return paths;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<String> execute(int buildId, int changeSetId) throws DataAccessException {
		return super.execute(buildId, changeSetId);
	}
	
	@Override
	protected String mapRow(ResultSet rs, int rowNumber) throws SQLException {
		return rs.getString("modified_path");
	}
}