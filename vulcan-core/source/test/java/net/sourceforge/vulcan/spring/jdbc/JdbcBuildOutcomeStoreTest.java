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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import junit.framework.TestCase;
import net.sourceforge.vulcan.core.support.StoreStub;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.exception.StoreException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class JdbcBuildOutcomeStoreTest extends TestCase {
	JdbcBuildOutcomeStore store = new JdbcBuildOutcomeStore();
	SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
	JdbcBuildOutcomeDto outcome = new JdbcBuildOutcomeDto();
	
	boolean initCalled = false;
	
	boolean diffExistsFlag = false;
	boolean buildLogExistsFlag = false;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		final Properties props = new Properties();
		props.setProperty("autocommit", "false");

		dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
		dataSource.setUrl("jdbc:hsqldb:mem:JdbcBuildOutcomeStoreTest");
		dataSource.setUsername("sa");
		dataSource.setPassword("");
		dataSource.setConnectionProperties(props);
		dataSource.setAutoCommit(false);
		
		store.setConfigurationStore(new StoreStub(null) {
			@Override
			public boolean diffExists(String projectName, UUID diffId) {
				return diffExistsFlag;
			}
			@Override
			public boolean buildLogExists(String projectName, UUID diffId) {
				return buildLogExistsFlag;
			}
		});
		
		store.setDataSource(dataSource);
		store.setCreateScript(new ClassPathResource("net/sourceforge/vulcan/resources/create_tables.sql"));

		outcome.setId(UUID.randomUUID());
		outcome.setStatus(Status.PASS);
		outcome.setName("project-a");
		outcome.setBuildNumber(5);
		outcome.setBuildReasonKey("x.y.z");
		outcome.setMessageKey("r.x.t");
		outcome.setStartDate(new Date(10004000l));
		outcome.setCompletionDate(new Date(20004000l));
	}
	
	@Override
	protected void tearDown() throws Exception {
		new JdbcTemplate(dataSource).execute("shutdown;");
		super.tearDown();
	}
	
	public void testInitCreatesTablesWhenMissing() throws Exception {
		store.init();

		int count = new JdbcTemplate(dataSource).queryForInt("select count(1) from builds");
		assertEquals(0, count);
	}

	public void testInitDoesNotThrowWhenTableAlreadyPresent() throws Exception {
		store.init();
		store.init();

		int count = new JdbcTemplate(dataSource).queryForInt("select count(1) from builds");
		assertEquals(0, count);
	}
	
	public void testSaveSimple() throws Exception {
		assertPersistence();
	}

	public void testThrowsStoreException() throws Exception {
		store.init();

		outcome.setBuildNumber(null);

		try {
			store.storeBuildOutcome(outcome);
			fail("Expected exception");
		} catch (StoreException e) {
			assertTrue(e.getCause() instanceof DataAccessException);
		}
	}

	public void testSaveTwoReusesProjectName() throws Exception {
		assertPersistence();
		
		outcome.setBuildNumber(6);
		outcome.setId(UUID.randomUUID());
		
		assertPersistence();
	}
	
	public void testSaveGeneratesIdOnNull() throws Exception {
		store.init();
		
		outcome.setId(null);
		outcome.setStatus(Status.SKIP);
		outcome.setUpdateType(null);
		
		final UUID id = store.storeBuildOutcome(outcome);
		
		assertSame(id, outcome.getId());
		
		assertPersistence(store.loadBuildOutcome(id));
	}
	
	public void testSaveOptionalColumns() throws Exception {
		outcome.setUpdateType(UpdateType.Incremental);
		outcome.setWorkDir("a work dir");
		outcome.setRevision(new RevisionTokenDto(342l, "r342"));
		outcome.setLastGoodBuildNumber(43432);
		outcome.setTagName("a/tag");
		outcome.setRepositoryUrl("http://example.com");
		outcome.setStatusChanged(true);
		outcome.setScheduledBuild(true);
		outcome.setRequestedBy("Jessica");
		
		assertPersistence();
	}
	
	public void testFindMostRecentBuildNumberByWorkingCopy() throws Exception {
		outcome.setWorkDir("a work dir");
		
		assertPersistence();
		
		outcome.setId(UUID.randomUUID());
		outcome.setBuildNumber(42);
		
		assertPersistence();
		
		assertEquals(outcome.getBuildNumber(), store.findMostRecentBuildNumberByWorkDir(outcome.getWorkDir()));
	}

	public void testFindMostRecentBuildNumberByWorkingCopyNull() throws Exception {
		store.init();
		assertEquals(null, store.findMostRecentBuildNumberByWorkDir("never used"));
	}
	
	public void testSaveLastKnownRevision() throws Exception {
		outcome.setRevision(null);
		outcome.setLastKnownRevision(new RevisionTokenDto(342l, "r342"));
		
		assertPersistence();
	}
	
	public void testSaveMessageArg() throws Exception {
		outcome.setMessageArgs(new Object[] {"a"});
		
		assertPersistence();
	}
	
	public void testSaveFourMessageArgs() throws Exception {
		outcome.setMessageArgs(new Object[] {"a", "b", "c", "d"});
		
		assertPersistence();
	}
	
	public void testSaveFourBuildReasonArgs() throws Exception {
		outcome.setBuildReasonArgs(new Object[] {"a", "b", "c", "d"});
		
		assertPersistence();
	}
	
	public void testGetBuildOutcomeIds() throws Exception {
		store.init();
		
		outcome.setName("a");
		store.storeBuildOutcome(outcome);
		
		outcome.setName("b");
		outcome.setId(UUID.randomUUID());
		store.storeBuildOutcome(outcome);
		
		outcome.setName("a");
		outcome.setBuildNumber(3242);
		outcome.setId(UUID.randomUUID());
		store.storeBuildOutcome(outcome);
		
		final Map<String, List<UUID>> map = store.getBuildOutcomeIDs();
		assertNotNull(map);
		assertEquals(2, map.size());
		assertTrue(map.containsKey("a"));
		assertTrue(map.containsKey("b"));
		assertEquals(2, map.get("a").size());
		assertEquals(1, map.get("b").size());
	}
	
	public void testSaveDependencies() throws Exception {
		final String depName1 = outcome.getName();
		final UUID depId1 = outcome.getId();
		
		assertPersistence();
		
		outcome.setName("service");
		outcome.setId(UUID.randomUUID());

		final String depName2 = outcome.getName();
		final UUID depId2 = outcome.getId();

		assertPersistence();
		
		outcome.setName("ui");
		outcome.setId(UUID.randomUUID());
		
		final HashMap<String, UUID> depMap = new HashMap<String, UUID>();
		outcome.setDependencyIds(depMap);
		depMap.put(depName1, depId1);
		depMap.put(depName2, depId2);
		
		assertPersistence();
	}
	
	public void testSaveWarnings() throws Exception {
		outcome.setWarnings(Arrays.<BuildMessageDto>asList(
			new JdbcBuildMessageDto("sample message", "a file", 43, "X12", BuildMessageType.Warning),
			new JdbcBuildMessageDto("another sample message", "a file", 44, "X13", BuildMessageType.Warning)));

		assertPersistence();
	}
	
	public void testSaveErrors() throws Exception {
		outcome.setErrors(Arrays.<BuildMessageDto>asList(
				new JdbcBuildMessageDto("an error", null, null, null, BuildMessageType.Error),
				new JdbcBuildMessageDto("another error", "a bad file", 44, null, BuildMessageType.Error)));
		
		assertPersistence();
	}
	
	public void testSaveErrorsAndWarnings() throws Exception {
		outcome.setWarnings(Arrays.<BuildMessageDto>asList(
			new JdbcBuildMessageDto("sample message", "a file", 43, "X12", BuildMessageType.Warning),
			new JdbcBuildMessageDto("another sample message", "a file", 44, "X13", BuildMessageType.Warning)));

		outcome.setErrors(Arrays.<BuildMessageDto>asList(
				new JdbcBuildMessageDto("an error", null, null, null, BuildMessageType.Error),
				new JdbcBuildMessageDto("another error", "a bad file", 44, null, BuildMessageType.Error)));
		
		assertPersistence();
	}
	
	public void testLoadChecksForExistingBuildLog() throws Exception {
		outcome.setBuildLogId(outcome.getId());
		buildLogExistsFlag = true;
		
		assertPersistence();
	}
	
	public void testLoadChecksForExistingDiff() throws Exception {
		outcome.setDiffId(outcome.getId());
		diffExistsFlag = true;
		
		assertPersistence();
	}
	
	public void testSaveMetrics() throws Exception {
		final List<MetricDto> metrics = new ArrayList<MetricDto>();
		
		final MetricDto m1 = new MetricDto();
		m1.setMessageKey("a.key.of.some.sort");
		m1.setValue("0.12");
		m1.setType(MetricType.PERCENT);
		metrics.add(m1);

		final MetricDto m2 = new MetricDto();
		m2.setMessageKey("another.key");
		m2.setValue("53");
		m2.setType(MetricType.NUMBER);
		metrics.add(m2);
		
		final MetricDto m3 = new MetricDto();
		m3.setMessageKey("yet.another.key");
		m3.setValue("fifty three");
		m3.setType(MetricType.STRING);
		metrics.add(m3);
		
		outcome.setMetrics(metrics);
		
		assertPersistence();
	}
	
	public void testSaveTestFailures() throws Exception {
		final List<TestFailureDto> list = new ArrayList<TestFailureDto>();
		
		final TestFailureDto tf = new TestFailureDto();
		tf.setName("a name of a test");
		tf.setBuildNumber(outcome.getBuildNumber());
		
		list.add(tf);
		
		outcome.setTestFailures(list);
		
		assertPersistence();
	}
	
	public void testSaveCommitLog() throws Exception {
		final ChangeLogDto log = new ChangeLogDto();
		final ChangeSetDto a = new ChangeSetDto();
		a.setMessage("did some stuff");
		a.setRevisionLabel("1.42");

		final ChangeSetDto b = new ChangeSetDto();
		b.setMessage("made some changes");
		b.setRevisionLabel("<multiple>");
		b.setAuthor("Barbara");
		b.setTimestamp(new Date());
		
		log.setChangeSets(Arrays.asList(a, b));
		
		outcome.setChangeLog(log);
		
		assertPersistence();
	}
	
	public void testSaveCommitLogWithFiles() throws Exception {
		final ChangeLogDto log = new ChangeLogDto();
		final ChangeSetDto a = new ChangeSetDto();
		a.setMessage("did some stuff");
		a.setRevisionLabel("1.42");
		a.setModifiedPaths(new String[] {"a/b/c", "x/y/z"});
		
		log.setChangeSets(Arrays.asList(a));
		
		outcome.setChangeLog(log);
		
		assertPersistence();
	}
	
	public void testRenameProjectUpdatesTable() throws Exception {
		assertPersistence();

		store.projectNameChanged(outcome.getName(), "new name");
		outcome.setName("new name");
		
		assertPersistence(store.loadBuildOutcome(outcome.getId()));
	}
	
	public void testDoesNotCacheProjectNameOnFailure() throws Exception {
		store.init();
		initCalled = true;
		
		outcome.setBuildNumber(null);
		
		try {
			store.storeBuildOutcome(outcome);
			fail("Expected exception");
		} catch (StoreException e) {
		}

		dataSource.getConnection().rollback();
		
		outcome.setBuildNumber(33);
		assertPersistence();
	}
	
	public void testCountBuildErrors() throws Exception {
		store.init();
		initCalled = true;

		final BuildOutcomeQueryDto query = new BuildOutcomeQueryDto();
		query.setProjectNames(Collections.singleton(outcome.getName()));
		
		store.loadTopBuildErrors(query, 5);
	}
	private void assertPersistence() throws StoreException {
		if (!initCalled) {
			store.init();
			initCalled = true;
		}
		
		outcome.setPrimaryKey(0);
		
		store.storeBuildOutcome(outcome);
		
		final JdbcBuildOutcomeDto loadedOutcome = store.loadBuildOutcome(outcome.getId());
		
		assertPersistence(loadedOutcome);
	}

	private void assertPersistence(final JdbcBuildOutcomeDto loadedOutcome) {
		loadedOutcome.setPrimaryKey(0);
		
		final String expectedString = outcome.toString().replaceAll("@[^\\[]+", "");
		final String actualString = loadedOutcome.toString().replaceAll("@[^\\[]+", "");

		if (!expectedString.equals(actualString)) {
			System.out.println(expectedString);
			System.out.println(actualString);
		}
		
		assertEquals(expectedString, actualString);
	}
}
