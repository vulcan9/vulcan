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
import java.util.Date;
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcBuildOutcomeStore implements BuildOutcomeStore, ProjectNameChangeListener {
	public static int MAX_TEST_FAILURE_MESSAGE_LENGTH = 1024;
	public static int MAX_TEST_FAILURE_DETAILS_LENGTH = 4096;
	public static int MAX_COMMIT_MESSAGE_LENGTH = 2048;
	
	private final Set<String> projectNames = new HashSet<String>();
	private final Set<String> brokenBuildUsers = new HashSet<String>();
	
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
		
		final LookupTableQuery projectNamesQuery = new LookupTableQuery(dataSource, "project_names", "name");
		projectNames.addAll(projectNamesQuery.execute());

		final LookupTableQuery usersQuery = new LookupTableQuery(dataSource, "users", "username");
		brokenBuildUsers.addAll(usersQuery.execute());
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

	public ProjectStatusDto loadMostRecentBuildOutcomeByWorkDir(String projectName, String workDir) throws StoreException {
		final ProjectStatusDto dto = findAndLoadBuildOutcome(projectName, "work_dir", workDir, false);

		if (dto != null && !dto.getName().equals(projectName)) {
			LogFactory.getLog(getClass()).warn("Project " + projectName + " and " + dto.getName() + " seem to share the same work directory.");
			return null;
		}
		
		return dto;
	}
	
	public ProjectStatusDto loadMostRecentBuildOutcomeByTagName(String projectName, String tagName) throws StoreException {
		return findAndLoadBuildOutcome(projectName, "tag_name", tagName, true);
	}
	
	/**
	 * Finds the most recent build outcome with the specified column value.
	 * This is a helper method for loadMostRecentBuildOutcomeByWorkDir
	 * and loadMostRecentBuildOutcomeByTagName
	 * @param projectName Name of project
	 * @param queryColumn SQL column to compare
	 * @param value Value to locate
	 * @param scopeToProject If true, limit result to project name.  If false, return most recent
	 * build from any project that matches criteria.
	 */
	private ProjectStatusDto findAndLoadBuildOutcome(String projectName, String queryColumn, String value, boolean scopeToProject) throws StoreException {
		String sql = "select uuid from builds o inner join " +
				"(select max(id) as max_id from builds where " + queryColumn + "=?";
		Object[] params;
		
		if (scopeToProject) {
			sql += " and project_id=(select id from project_names where name=?)";
			params = new Object[] {value, projectName};
		} else {
			params = new Object[] {value};
		}
		
		sql += ") i on o.id = i.max_id";
		
		final String result;
		
		try {
			result = (String) jdbcTemplate.queryForObject(sql, params, String.class);	
		} catch (IncorrectResultSizeDataAccessException e) {
			if (e.getActualSize() != 0) {
				// This is a bug.
				throw e;
			}
			// This just means "no results found."
			return null;
		}
		
		return loadBuildOutcome(UUID.fromString(result));
	}
	
	public List<ProjectStatusDto> loadBuildSummaries(BuildOutcomeQueryDto query) {
		final BuildHistoryQuery historyQuery = new BuildHistoryQuery(dataSource, query);
		
		final List<JdbcBuildOutcomeDto> results = historyQuery.queryForHistory();
		
		if (results.size() == 0) {
			return Collections.<ProjectStatusDto>unmodifiableList(results);
		}
		
		/*
		 * If the query specified max results and no other filtering criteria,
		 * we need to avoid loading all metrics when we only need the ones related
		 * to the results we have.
		 */
		if (query.isUnbounded()) {
			// Make a copy to avoid modifying passed in object.
			query = (BuildOutcomeQueryDto) query.copy();
			
			query.setMinDate(results.get(0).getCompletionDate());
		}
		
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
	
	public synchronized UUID storeBuildOutcome(ProjectStatusDto outcome)	throws StoreException {
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

	public synchronized void claimBrokenBuild(UUID id, String userName, Date claimDate) {
		boolean brokenByNameAdded = false;
		
		if (!brokenBuildUsers.contains(userName.toLowerCase())) {
			jdbcTemplate.update("insert into users (username) values (?)", new Object[] {userName});
			brokenByNameAdded = true;
		}

		jdbcTemplate.update("update builds set broken_by_user_id=(select id from users " +
				"where username=?), claimed_date=? where uuid=?", new Object[] {
				userName, claimDate, id.toString()});
		
		if (brokenByNameAdded) {
			brokenBuildUsers.add(userName.toLowerCase());
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
	
	public synchronized void projectNameChanged(String oldName, String newName) {
		String origNewName = newName;
		oldName = oldName.toLowerCase();
		newName = newName.toLowerCase();
		
		if (projectNames.contains(newName)) {
			int i=2;
			while (projectNames.contains(newName + "_" + i)) {
				i++;
			}
			
			jdbcTemplate.update("update project_names set name=? where name=?",
				new Object[] {origNewName + "_" + i, newName});
			
			projectNames.add(newName + "_" + i);
		}
		
		jdbcTemplate.update("update project_names set name=? where name=?",
			new Object[] {origNewName, oldName});
		
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
		boolean projectNameAdded = false;
		boolean brokenByNameAdded = false;
		final String projectName = outcome.getName();
		
		if (!projectNames.contains(projectName.toLowerCase())) {
			jdbcTemplate.update("insert into project_names (name) values (?)", new Object[] {projectName});
			projectNameAdded = true;
		}
		
		final String brokenBy = outcome.getBrokenBy();
		if (StringUtils.isNotBlank(brokenBy) && !brokenBuildUsers.contains(brokenBy.toLowerCase())) {
			jdbcTemplate.update("insert into users (username) values (?)", new Object[] {brokenBy});
			brokenByNameAdded = true;
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
		
		if (projectNameAdded) {
			projectNames.add(projectName.toLowerCase());
		}
		
		if (brokenByNameAdded) {
			brokenBuildUsers.add(brokenBy.toLowerCase());
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
