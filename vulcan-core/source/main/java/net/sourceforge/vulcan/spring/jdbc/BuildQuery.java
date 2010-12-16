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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.MappingSqlQuery;

class BuildQuery extends MappingSqlQuery {
	public BuildQuery(DataSource dataSource) {
		this(dataSource, true);
		setSql(HistoryQueryBuilder.BUILD_INFO_SQL + "where uuid = ?");
		declareParameter(new SqlParameter("uuid", Types.VARCHAR));
		compile();
	}
	
	protected BuildQuery(DataSource dataSource, boolean marker) {
		setDataSource(dataSource);
	}
	
	@SuppressWarnings("unchecked")
	public JdbcBuildOutcomeDto queryForBuild(UUID uuid) {
		final List<JdbcBuildOutcomeDto> results = execute(new Object[] {uuid.toString()});
		
		if (results.size() == 1) {
			return results.get(0);
		}
		
		return null;
	}
	@Override
	protected JdbcBuildOutcomeDto mapRow(ResultSet rs, int rowNumber) throws SQLException {
		final JdbcBuildOutcomeDto dto = new JdbcBuildOutcomeDto();
		
		dto.setPrimaryKey(rs.getInt("id"));
		dto.setName(rs.getString("name"));
		dto.setId(UUID.fromString(rs.getString("uuid")));
		
		dto.setStatus(ProjectStatusDto.Status.valueOf(rs.getString("status")));
		dto.setMessageKey(rs.getString("message_key"));
		dto.setBuildReasonKey(rs.getString("build_reason_key"));
		dto.setStartDate(new Date(rs.getTimestamp("start_date").getTime()));
		dto.setCompletionDate(new Date(rs.getTimestamp("completion_date").getTime()));
		dto.setBuildNumber(rs.getInt("build_number"));
		dto.setWorkDir(rs.getString("work_dir"));
		dto.setWorkDirSupportsIncrementalUpdate(rs.getBoolean("work_dir_vcs_clean"));
		
		dto.setTagName(rs.getString("tag_name"));
		dto.setRepositoryUrl(rs.getString("repository_url"));
		dto.setScheduledBuild(rs.getBoolean("scheduled_build"));
		dto.setStatusChanged(rs.getBoolean("status_changed"));
		dto.setRequestedBy(rs.getString("requested_by"));
		dto.setBrokenBy(rs.getString("broken_by_user_name"));
		
		final Timestamp claimed_date = rs.getTimestamp("claimed_date");
		if (StringUtils.isNotBlank(dto.getBrokenBy()) && !rs.wasNull()) {
			dto.setClaimDate(new Date(claimed_date.getTime()));
		}
		
		final String updateTypeString = rs.getString("update_type");
		if (!rs.wasNull() && StringUtils.isNotEmpty(updateTypeString)) {
			dto.setUpdateType(ProjectStatusDto.UpdateType.valueOf(updateTypeString));
		} else {
			dto.setUpdateType(null);
		}

		final Integer lastGoodBuildNumber = rs.getInt("last_good_build_number");
		if (!rs.wasNull()) {
			dto.setLastGoodBuildNumber(lastGoodBuildNumber);
		}
		
		final Long revision = rs.getLong("revision");
		if (!rs.wasNull()) {
			final RevisionTokenDto revisionTokenDto = new RevisionTokenDto(revision, rs.getString("revision_label"));
			
			if (rs.getBoolean("revision_unavailable")) {
				dto.setLastKnownRevision(revisionTokenDto);
			} else {
				dto.setRevision(revisionTokenDto);
			}
		}
		
		dto.setMessageArgs(mapMessageArgs(rs, "message_arg_"));
		dto.setBuildReasonArgs(mapMessageArgs(rs, "build_reason_arg_"));
		
		return dto;
	}
	private Object[] mapMessageArgs(ResultSet rs, String columnPrefix) throws SQLException {
		final List<String> args = new ArrayList<String>();
		
		for (int i = 0; i<4; i++) {
			final String arg = rs.getString(columnPrefix + i);
			if (rs.wasNull()) {
				break;
			}
			args.add(arg);
		}

		if (args.isEmpty()) {
			return null;
		}
		
		return args.toArray(new String[args.size()]);
	}
}
