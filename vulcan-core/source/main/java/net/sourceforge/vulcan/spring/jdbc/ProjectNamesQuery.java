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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.object.MappingSqlQuery;

class ProjectNamesQuery extends MappingSqlQuery {
	final Map<String, Integer> projectIdMap = new HashMap<String, Integer>();
	
	public ProjectNamesQuery(DataSource dataSource) {
		super(dataSource, "select name from project_names");
		compile();
	}
	@Override
	@SuppressWarnings("unchecked")
	public List<String> execute() throws DataAccessException {
		return super.execute();
	}
	@Override
	protected Object mapRow(ResultSet rs, int rowNumber) throws SQLException {
		return rs.getString("name");
	}
}