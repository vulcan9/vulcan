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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;

import org.springframework.jdbc.core.SqlParameter;

class HistoryQueryBuilder {
	protected final static String BUILD_INFO_SQL =
		"select builds.id, project_names.name, uuid, status, message_key, build_reason_key," +
		"message_arg_0, message_arg_1, message_arg_2, message_arg_3, " +
		"build_reason_arg_0, build_reason_arg_1, build_reason_arg_2, build_reason_arg_3, " +
		"start_date, completion_date, build_number, update_type," +
		"work_dir, revision, revision_label, last_good_build_number," +
		"tag_name, repository_url, status_changed, scheduled_build," +
		"requested_by, revision_unavailable " +
		"from builds inner join project_names on builds.project_id = project_names.id ";
	
	protected final static String BUILD_HISTORY_METRICS_SQL =
		"select metrics.build_id as build_id, metrics.message_key as message_key, metrics.metric_type as metric_type, metrics.data as data " +
		"from builds inner join project_names on builds.project_id = project_names.id inner join metrics on builds.id=metrics.build_id ";

	private HistoryQueryBuilder() {
	}

	static void buildQuery(BuildOutcomeQueryDto dto, BuilderQuery query) {
		buildQuery(BUILD_INFO_SQL, dto, query);
	}
	
	static void buildQuery(String selectClause, BuildOutcomeQueryDto dto, BuilderQuery query) {
		final Set<String> projectNames = dto.getProjectNames();
		if (projectNames == null || projectNames.isEmpty()) {
			throw new IllegalArgumentException("Must query for at least one project name");
		}
		
		final List<? super Object> params = new ArrayList<Object>();
		
		final StringBuilder sb = new StringBuilder();
		
		sb.append("where project_names.name");
		
		query.declareParameter(new SqlParameter(Types.VARCHAR));
		
		if (projectNames.size() == 1) {
			sb.append("=?");
			params.addAll(projectNames);
		} else {
			sb.append(" in (?");
			for (int i=1; i<projectNames.size(); i++) {
				sb.append(",?");
				query.declareParameter(new SqlParameter(Types.VARCHAR));
			}
			sb.append(")");
			
			final List<String> sorted = new ArrayList<String>(projectNames);
			Collections.sort(sorted);
			params.addAll(sorted);
		}
		
		if (dto.getMinDate() != null) {
			sb.append(" and completion_date>=?");
			params.add(dto.getMinDate());
			query.declareParameter(new SqlParameter(Types.TIMESTAMP));
		}
		
		if (dto.getMaxDate() != null) {
			sb.append(" and completion_date<?");
			params.add(dto.getMaxDate());
			query.declareParameter(new SqlParameter(Types.TIMESTAMP));
		}
		
		if (dto.getMinBuildNumber() != null) {
			sb.append(" and build_number>=?");
			params.add(dto.getMinBuildNumber());
			query.declareParameter(new SqlParameter(Types.INTEGER));
		}
		
		if (dto.getMaxBuildNumber() != null) {
			sb.append(" and build_number<=?");
			params.add(dto.getMaxBuildNumber());
			query.declareParameter(new SqlParameter(Types.INTEGER));
		}
		
		final Set<Status> statuses = dto.getStatuses();
		if (statuses != null && !statuses.isEmpty()) {
			query.declareParameter(new SqlParameter(Types.VARCHAR));

			sb.append(" and status in (?");
			for (int i=1; i<statuses.size(); i++) {
				query.declareParameter(new SqlParameter(Types.VARCHAR));
				sb.append(",?");
			}
			sb.append(")");

			params.addAll(statuses);
		}
		
		if (dto.getUpdateType()!=null) {
			query.declareParameter(new SqlParameter(Types.VARCHAR));
			sb.append(" and update_type=?");
			params.add(dto.getUpdateType().name());
		}
		
		if (isNotBlank(dto.getRequestedBy())) {
			query.declareParameter(new SqlParameter(Types.VARCHAR));
			sb.append(" and requested_by=?");
			params.add(dto.getRequestedBy());
		}
		
		query.setParameterValues(params.toArray());
		
		query.setSql(selectClause + sb.toString());
	}
}
