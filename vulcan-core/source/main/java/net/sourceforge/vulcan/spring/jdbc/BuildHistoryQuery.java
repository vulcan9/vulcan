/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2010 Chris Eldredge
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

import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;

class BuildHistoryQuery extends BuildQuery implements BuilderQuery {
	protected Object[] parameterValues;
	private final Integer maxResults;
	
	public BuildHistoryQuery(DataSource dataSource, BuildOutcomeQueryDto queryDto) {
		super(dataSource, true);
		
		maxResults = queryDto.getMaxResults();
		if (isMaxResultsSpecified()) {
			setMaxRows(maxResults);
		}
		
		HistoryQueryBuilder.buildQuery(queryDto, this);
	}

	public void setParameterValues(Object[] parameterValues) {
		this.parameterValues = parameterValues;
	}
	
	@Override
	public void setSql(String sql) {
		String orderBy = " order by completion_date";
		
		/*
		 * If maxResults is specified we want the most
		 * recent N records, so invert sort order.
		 */
		if (isMaxResultsSpecified()) {
			orderBy += " desc";
		}
		
		super.setSql(sql + orderBy);
	}
	
	@SuppressWarnings("unchecked")
	public List<JdbcBuildOutcomeDto> queryForHistory() {
		final List results = execute(parameterValues);

		/*
		 * Reverse to invert descending order specified
		 * in setSql.
		 */
		if (isMaxResultsSpecified()) {
			Collections.reverse(results);
		}
		
		return results;
	}

	private boolean isMaxResultsSpecified() {
		return maxResults != null && maxResults > 0;
	}
}
