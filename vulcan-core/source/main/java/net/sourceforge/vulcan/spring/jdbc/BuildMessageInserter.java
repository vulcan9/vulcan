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

import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.BuildMessageDto;

import org.springframework.jdbc.core.SqlParameter;

class BuildMessageInserter extends RecordInserter {
	public BuildMessageInserter(DataSource dataSource) {
		setDataSource(dataSource);
		setSql("insert into build_messages " +
				"(build_id, message_type, message, line_number, file, code) " +
				"values (?, ?, ?, ?, ?, ?)");
		
		declareParameter(new SqlParameter(Types.NUMERIC));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.NUMERIC));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		
		compile();
	}
	
	public int insert(int primaryKey, List<BuildMessageDto> messages, BuildMessageType type) {
		int count = 0;
		
		final Object[] params = new Object[6];
		
		params[0] = primaryKey;
		params[1] = type.getRdbmsValue(); 

		for (BuildMessageDto dto : messages) {
			params[2] = truncate(dto.getMessage(), JdbcBuildOutcomeStore.MAX_BUILD_MESSAGE_LENGTH);
			params[3] = dto.getLineNumber();
			params[4] = dto.getFile();
			params[5] = dto.getCode();
			
			count += update(params);
		}
		
		return count;
	}
}