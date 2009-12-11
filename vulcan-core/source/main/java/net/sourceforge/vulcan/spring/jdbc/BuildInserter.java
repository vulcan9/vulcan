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

import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;

import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.SqlUpdate;

class BuildInserter extends SqlUpdate {
	public BuildInserter(DataSource dataSource) {
		setDataSource(dataSource);
		setSql("insert into builds " +
				"(project_id, uuid, status, message_key," +
				"message_arg_0, message_arg_1, message_arg_2, message_arg_3, " +
				"build_reason_key, " +
				"build_reason_arg_0, build_reason_arg_1, build_reason_arg_2, build_reason_arg_3, " +
				"start_date, completion_date, build_number, update_type," +
				"work_dir, revision, revision_label, last_good_build_number," +
				"tag_name, repository_url, status_changed, scheduled_build," +
				"requested_by, revision_unavailable, broken_by_user_id, claimed_date, work_dir_vcs_clean) " +
				"values ((select id from project_names where name=?)," +
				" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
				" (select id from users where username=?), ?, ?)");
		
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.TIMESTAMP));
		declareParameter(new SqlParameter(Types.TIMESTAMP));
		declareParameter(new SqlParameter(Types.NUMERIC));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.BIGINT));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.NUMERIC));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.BOOLEAN));
		declareParameter(new SqlParameter(Types.BOOLEAN));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.BOOLEAN));
		declareParameter(new SqlParameter(Types.VARCHAR));
		declareParameter(new SqlParameter(Types.TIMESTAMP));
		declareParameter(new SqlParameter(Types.BOOLEAN));
		
		compile();
	}
	
	public int insert(ProjectStatusDto dto) {
		Long revision = null;
		String revisionLabel = null;
		Boolean revisionUnavailable = Boolean.TRUE;
		
		if (dto.getRevision() != null) {
			revision = dto.getRevision().getRevision();
			revisionLabel = dto.getRevision().getLabel();
			revisionUnavailable = Boolean.FALSE;
		} else if (dto.getLastKnownRevision() != null) {
			revision = dto.getLastKnownRevision().getRevision();
			revisionLabel = dto.getLastKnownRevision().getLabel();
		}
		
		final Object[] messageArgs = dto.getMessageArgs();
		final int numMessageArgs = messageArgs != null ? messageArgs.length : 0;
		
		final Object[] buildReasonArgs = dto.getBuildReasonArgs();
		final int numBuildReasonArgs = buildReasonArgs != null ? buildReasonArgs.length : 0;
		
		final UpdateType updateType = dto.getUpdateType();
		
		Object[] params = new Object[] {
			dto.getName(),
			dto.getId().toString(),
			dto.getStatus().toString(),
			dto.getMessageKey(),
			numMessageArgs > 0 ? messageArgs[0] : null,
			numMessageArgs > 1 ? messageArgs[1] : null,
			numMessageArgs > 2 ? messageArgs[2] : null,
			numMessageArgs > 3 ? messageArgs[3] : null,
			dto.getBuildReasonKey(),
			numBuildReasonArgs > 0 ? buildReasonArgs[0] : null,
			numBuildReasonArgs > 1 ? buildReasonArgs[1] : null,
			numBuildReasonArgs > 2 ? buildReasonArgs[2] : null,
			numBuildReasonArgs > 3 ? buildReasonArgs[3] : null,
			dto.getStartDate(),
			dto.getCompletionDate(),
			dto.getBuildNumber(),
			updateType != null ? updateType.toString() : null,
			dto.getWorkDir(),
			revision,
			revisionLabel,
			dto.getLastGoodBuildNumber(),
			dto.getTagName(),
			dto.getRepositoryUrl(),
			dto.isStatusChanged(),
			dto.isScheduledBuild(),
			dto.getRequestedBy(),
			revisionUnavailable,
			dto.getBrokenBy(),
			dto.getClaimDate(),
			dto.isWorkDirSupportsIncrementalUpdate()
		};
		
		return update(params);
	}
}