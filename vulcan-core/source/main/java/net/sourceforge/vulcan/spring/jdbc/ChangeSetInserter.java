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

import java.sql.Types;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.ChangeSetDto;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.SqlUpdate;

class ChangeSetInserter extends SqlUpdate {
	private final ModifiedPathInserter modifiedPathInserter;
	
	public ChangeSetInserter(DataSource dataSource) {
		setDataSource(dataSource);
		setSql("insert into change_sets " +
				"(build_id, change_set_id, message, revision_label, commit_timestamp, author) " +
				"values (?, ?, ?, ?, ?, ?)");
		
		declareParameter(new SqlParameter(Types.NUMERIC));
		declareParameter(new SqlParameter(Types.NUMERIC));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.TIMESTAMP));
		declareParameter(new SqlParameter(Types.VARCHAR));
		
		compile();
		
		modifiedPathInserter = new ModifiedPathInserter(dataSource);
	}
	
	public int insert(int primaryKey, List<ChangeSetDto> changeSets) {
		int count = 0;
		
		final Object[] params = new Object[6];
		
		params[0] = primaryKey;

		int index = 0;
		
		for (ChangeSetDto dto : changeSets) {
			params[1] = index;
			params[2] = dto.getMessage();
			params[3] = dto.getRevisionLabel();
			params[4] = dto.getTimestamp();
			params[5] = dto.getAuthor();
			
			count += update(params);
			
			if (dto.getModifiedPaths() != null) {
				modifiedPathInserter.insert(primaryKey, index, dto.getModifiedPaths());
			}
			
			index++;
		}
		
		return count;
	}
}