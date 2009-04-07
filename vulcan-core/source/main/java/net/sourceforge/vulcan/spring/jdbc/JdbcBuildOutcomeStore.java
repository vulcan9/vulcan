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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import net.sourceforge.vulcan.core.BuildOutcomeStore;
import net.sourceforge.vulcan.core.ConfigurationStore;
import net.sourceforge.vulcan.core.ProjectNameChangeListener;
import net.sourceforge.vulcan.core.support.UUIDUtils;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class JdbcBuildOutcomeStore implements BuildOutcomeStore, ProjectNameChangeListener {
	public static int MAX_TEST_FAILURE_MESSAGE_LENGTH = 1024;
	public static int MAX_TEST_FAILURE_DETAILS_LENGTH = 4096;
	public static int MAX_COMMIT_MESSAGE_LENGTH = 2048;
	
	private final Set<String> projectNames = new HashSet<String>();
	private List<String> buildUsers;
	private List<String> buildSchedulers;
	
	/* Dependencies */
	private ConfigurationStore configurationStore;
	private DataSource dataSource;
	private Map<String, String> sqlQueries;
	
	/* Helpers */
	private JdbcTemplate jdbcTemplate;
	private BuildQuery buildQuery;
	private DependencyQuery dependencyQuery;
	private BuildMessagesQuery buildMessagesQuery;
	private MetricsQuery metricsQuery;
	private TestFailureQuery testFailureQuery;
	private ChangeSetQuery changeSetQuery;
	private BuildInserter buildInserter;
	private DependencyInserter dependencyInserter;
	private BuildMessageInserter buildMessageInserter;
	private MetricInserter metricInserter;
	private TestFailureInserter testFailureInserter;
	private ChangeSetInserter changeSetInserter;
	
	public void init() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		buildQuery = new BuildQuery(dataSource);
		dependencyQuery = new DependencyQuery(dataSource);
		buildMessagesQuery = new BuildMessagesQuery(dataSource);
		metricsQuery = new MetricsQuery(dataSource);
		testFailureQuery = new TestFailureQuery(dataSource);
		changeSetQuery = new ChangeSetQuery(dataSource);
		buildInserter = new BuildInserter(dataSource);
		dependencyInserter = new DependencyInserter(dataSource);
		buildMessageInserter = new BuildMessageInserter(dataSource);
		metricInserter = new MetricInserter(dataSource);
		testFailureInserter = new TestFailureInserter(dataSource);
		changeSetInserter = new ChangeSetInserter(dataSource);
		
		final ProjectNamesQuery projectNamesQuery = new ProjectNamesQuery(dataSource);
		projectNames.addAll(projectNamesQuery.execute());
	}

	public Map<String, List<UUID>> getBuildOutcomeIDs() {
		final BuildIdMapQuery query = new BuildIdMapQuery(dataSource);
		
		return query.executeForMap();
	}

	public JdbcBuildOutcomeDto loadBuildOutcome(UUID id) throws StoreException {
		final JdbcBuildOutcomeDto dto = buildQuery.queryForBuild(id);
		
		dto.setDependencyIds(dependencyQuery.queryForDependencyMap(dto.getPrimaryKey()));
		
		buildMessagesQuery.queryBuildMessages(dto);
		metricsQuery.queryMetrics(dto);
		testFailureQuery.queryTestFailures(dto);
		changeSetQuery.queryChangeSets(dto);
		
		if (configurationStore.buildLogExists(dto.getName(), id)) {
			dto.setBuildLogId(id);
		}
		
		if (configurationStore.diffExists(dto.getName(), id)) {
			dto.setDiffId(id);
		}
		
		return dto;
	}

	public List<ProjectStatusDto> loadBuildSummaries(BuildOutcomeQueryDto query) {
		final BuildHistoryQuery historyQuery = new BuildHistoryQuery(dataSource, query);
		
		final List<JdbcBuildOutcomeDto> results = historyQuery.queryForHistory();
		
		final BuildHistoryMetricsQuery metricsQuery = new BuildHistoryMetricsQuery(dataSource, query);
		
		metricsQuery.queryMetrics(results);
		
		return Collections.<ProjectStatusDto>unmodifiableList(results);
	}
	
	public List<BuildMessageDto> loadTopBuildErrors(BuildOutcomeQueryDto query, int maxResultCount) {
		return new BuildHistoryTopErrorsQuery(dataSource, query, maxResultCount).queryTopMessages();
	}

	public List<TestFailureDto> loadTopTestFailures(BuildOutcomeQueryDto query,	int maxResultCount) {
		return new BuildHistoryTopTestFailuresQuery(dataSource, query, maxResultCount).queryTopMessages();
	}
	
	public Long loadAverageBuildTimeMillis(String name, UpdateType updateType) {
		final String sql = sqlQueries.get("select.average.build.time");
		final Object[] params = new Object[] { name, updateType.name() };
		
		final long result = jdbcTemplate.queryForLong(sql, params);
		
		if (result <= 0) {
			return null;
		}
		
		return result;
	}
	
	public UUID storeBuildOutcome(ProjectStatusDto outcome)	throws StoreException {
		try {
			final UUID uuid = storeBuildOutcomeInternal(outcome);
			
			final String requestedBy = outcome.getRequestedBy();
			
			if (StringUtils.isBlank(requestedBy) || buildUsers == null) {
				return uuid;
			}
			
			if (outcome.isScheduledBuild() && !buildSchedulers.contains(requestedBy)) {
				buildSchedulers.add(requestedBy);
				Collections.sort(buildSchedulers);
			} else if (!outcome.isScheduledBuild() && !buildUsers.contains(requestedBy)){
				buildUsers.add(requestedBy);
				Collections.sort(buildUsers);
			}
			
			return uuid;
		} catch (DataAccessException e) {
			throw new StoreException(e);
		}
	}

	public Integer findMostRecentBuildNumberByWorkDir(String workDir) {
		final int result = jdbcTemplate.queryForInt("select ifnull(max(build_number), -1) from builds where work_dir=?",
				new Object[] { workDir });
		
		if (result == -1) {
			return null;
		}
		
		return result;
	}
	
	public void projectNameChanged(String oldName, String newName) {
		if (projectNames.contains(newName)) {
			int i=2;
			while (projectNames.contains(newName + "_" + i)) {
				i++;
			}
			
			jdbcTemplate.update("update project_names set name=? where name=?",
				new Object[] {newName + "_" + i, newName});
			
			projectNames.add(newName + "_" + i);
		}
		
		jdbcTemplate.update("update project_names set name=? where name=?",
			new Object[] {newName, oldName});
		projectNames.add(newName);
		projectNames.remove(oldName);
	}
	
	public ConfigurationStore getConfigurationStore() {
		return configurationStore;
	}
	
	public void setConfigurationStore(ConfigurationStore configurationStore) {
		this.configurationStore = configurationStore;
	}
	
	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public List<String> getBuildSchedulers() {
		if (buildSchedulers == null) {
			loadUsersAndBuildSchedulers();
		}
		
		return buildSchedulers;
	}
	
	public List<String> getBuildUsers() {
		if (buildUsers == null) {
			loadUsersAndBuildSchedulers();
		}
		
		return buildUsers;
	}

	public Map<String, String> getSqlQueries() {
		return sqlQueries;
	}
	
	public void setSqlQueries(Map<String, String> sqlQueries) {
		this.sqlQueries = sqlQueries;
	}
	
	private UUID storeBuildOutcomeInternal(ProjectStatusDto outcome) {
		boolean nameAdded = false;
		final String projectName = outcome.getName();
		
		if (!projectNames.contains(projectName)) {
			jdbcTemplate.update("insert into project_names (name) values (?)", new Object[] {projectName});
			nameAdded = true;
		}
		
		if (outcome.getId() == null) {
			outcome.setId(UUIDUtils.generateTimeBasedUUID());
		}
		
		buildInserter.insert(outcome);
		
		final Map<String, UUID> depMap = outcome.getDependencyIds();
		
		if (depMap != null && !depMap.isEmpty()) {
			dependencyInserter.insert(outcome.getId(), depMap.values());
		}
		
		int primaryKey = new JdbcTemplate(dataSource).queryForInt("select id from builds where uuid=?", new Object[]{outcome.getId().toString()});
		
		if (outcome.getWarnings() != null) {
			buildMessageInserter.insert(primaryKey, outcome.getWarnings(), BuildMessageType.Warning);
		}
		
		if (outcome.getErrors() != null) {
			buildMessageInserter.insert(primaryKey, outcome.getErrors(), BuildMessageType.Error);
		}
		
		if (outcome.getMetrics() != null) {
			metricInserter.insert(primaryKey, outcome.getMetrics());
		}
		
		if (outcome.getTestFailures() != null) {
			testFailureInserter.insert(primaryKey, outcome.getTestFailures());
		}
		
		if (outcome.getChangeLog() != null && outcome.getChangeLog().getChangeSets() != null) {
			changeSetInserter.insert(primaryKey, outcome.getChangeLog().getChangeSets());
		}
		
		if (nameAdded) {
			projectNames.add(projectName);
		}
		
		return outcome.getId();
	}

	static String truncate(String str, int max) {
		if (str == null) {
			return null;
		}
		
		if (str.length() > max) {
			return str.substring(0, max);
		}
		return str;
	}

	private void loadUsersAndBuildSchedulers() {
		final RequestUserQuery query = new RequestUserQuery(dataSource);
		query.execute();
		
		buildUsers = query.getUsers();
		buildSchedulers = query.getSchedulers();
	}
}
