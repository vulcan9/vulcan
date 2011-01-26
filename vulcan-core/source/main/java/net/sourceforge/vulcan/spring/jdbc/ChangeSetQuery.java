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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.ModifiedPathDto;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;

class ChangeSetQuery extends MappingSqlQuery {
	private final ModifiedPathQuery modifiedPathQuery;

	public ChangeSetQuery(DataSource dataSource) {
		super(dataSource, "select message, author, author_email, commit_timestamp, revision_label, build_id, change_set_id " +
				"from change_sets where build_id = ? order by commit_timestamp");
		declareParameter(new SqlParameter("build_id", Types.NUMERIC));
		compile();
		
		modifiedPathQuery = new ModifiedPathQuery(dataSource);
	}
	
	public void queryChangeSets(JdbcBuildOutcomeDto outcome) {
		final List<ChangeSetDto> changeSets = execute(outcome.getPrimaryKey());
		
		if (changeSets.isEmpty()) {
			return;
		}
		
		final ChangeLogDto changeLog = new ChangeLogDto();
		changeLog.setChangeSets(changeSets);
		outcome.setChangeLog(changeLog);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<ChangeSetDto> execute(int param) throws DataAccessException {
		return super.execute(param);
	}
	
	@Override
	protected ChangeSetDto mapRow(ResultSet rs, int rowNumber) throws SQLException {
		final ChangeSetDto dto = new ChangeSetDto();
		
		dto.setMessage(rs.getString("message"));
		dto.setAuthorName(rs.getString("author"));
		dto.setAuthorEmail(rs.getString("author_email"));
		dto.setRevisionLabel(rs.getString("revision_label"));
		
		final Timestamp timestamp = rs.getTimestamp("commit_timestamp");
		
		if (!rs.wasNull()) {
			dto.setTimestamp(new Date(timestamp.getTime()));
		}
		
		int buildId = rs.getInt("build_id");
		int changeSetId = rs.getInt("change_set_id");
		
		final List<ModifiedPathDto> paths = modifiedPathQuery.queryModifiedPaths(buildId, changeSetId);
		dto.setModifiedPaths(paths);
		
		return dto;
	}
}