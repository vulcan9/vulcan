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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.BuildMessageDto;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;

class BuildMessagesQuery extends MappingSqlQuery {
	public BuildMessagesQuery(DataSource dataSource) {
		super(dataSource, "select message_type, message, code, file, line_number " +
				"from build_messages where build_id = ? order by message_type, file, line_number, message");
		declareParameter(new SqlParameter("build_id", Types.NUMERIC));
		compile();
	}
	
	public void queryBuildMessages(JdbcBuildOutcomeDto outcome) {
		final List<JdbcBuildMessageDto> messages = execute(outcome.getPrimaryKey());
		
		if (messages.isEmpty()) {
			return;
		}
		
		final BuildMessageType firstType = messages.get(0).getMessageType();
		
		if (firstType == BuildMessageType.Warning) {
			outcome.setWarnings(Collections.<BuildMessageDto>unmodifiableList(messages));
			return;
		}
		
		int i;
			
		for (i=0; i<messages.size(); i++) {
			if (messages.get(i).getMessageType() != firstType) {
				break;
			}
		}
		
		outcome.setErrors(Collections.<BuildMessageDto>unmodifiableList(messages.subList(0, i)));
		
		if (i<messages.size()) {
			outcome.setWarnings(Collections.<BuildMessageDto>unmodifiableList(messages.subList(i, messages.size())));
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<JdbcBuildMessageDto> execute(int param) throws DataAccessException {
		return super.execute(param);
	}
	
	@Override
	protected JdbcBuildMessageDto mapRow(ResultSet rs, int rowNumber) throws SQLException {
		final JdbcBuildMessageDto dto = new JdbcBuildMessageDto();
		
		dto.setMessageType(BuildMessageType.fromRdbmsValue(rs.getString("message_type")));
		dto.setMessage(rs.getString("message"));
		dto.setFile(rs.getString("file"));
		dto.setCode(rs.getString("code"));
		
		final int lineNumber = rs.getInt("line_number");
		
		if (!rs.wasNull()) {
			dto.setLineNumber(lineNumber);
		}
		
		return dto;
	}
}