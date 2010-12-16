/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.object.MappingSqlQuery;


public class RequestUserQuery extends MappingSqlQuery {
	private final List<String> users = new ArrayList<String>();
	private final List<String> schedulers = new ArrayList<String>();
	
	public RequestUserQuery(DataSource dataSource) {
		setDataSource(dataSource);
		setSql("select distinct requested_by, scheduled_build from builds order by requested_by");
		compile();
	}
	
	public List<String> getUsers() {
		return users;
	}
	
	public List<String> getSchedulers() {
		return schedulers;
	}
	
	@Override
	protected Object mapRow(ResultSet rs, int rowNumber) throws SQLException {
		final String value = rs.getString("requested_by");
		
		if (rs.wasNull()) {
			return null;
		}
		
		final boolean isScheduler = rs.getBoolean("scheduled_build");
		
		if (isScheduler) {
			schedulers.add(value);
		} else {
			users.add(value);
		}
		
		return value;
	}
}
