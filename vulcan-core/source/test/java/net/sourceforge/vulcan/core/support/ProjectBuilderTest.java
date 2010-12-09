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
package net.sourceforge.vulcan.core.support;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.BuildPhase;
import net.sourceforge.vulcan.core.BuildStatusListener;
import net.sourceforge.vulcan.core.BuildTarget;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id = "$Id$", url = "$HeadURL$")
public class ProjectBuilderTest extends EasyMockTestCase {
	boolean createWorkingDirectoriesSuccess = true;
	
	ProjectConfigDto project = new ProjectConfigDto();
	ProjectStatusDto buildStatus = new ProjectStatusDto();
	BuildTargetImpl buildTarget = new BuildTargetImpl(project, buildStatus);
	
	ProjectStatusDto previousStatusByTagName;
	ProjectStatusDto previousStatusByWorkDir;
	
	BuildFailedException be;
	
	boolean gotInterrupt;
	boolean invokedBuild;
	boolean suppressStartDate = true;
	
	File logFile = new File("fakeBuildLog.log");
	File diffFile = new File("fakeDiff.log");
	
	UpdateType updateType = null;
	
	Long estimatedBuildTimeMillis = null;
	
	final List<BuildMessageDto> listenedErrors = new ArrayList<BuildMessageDto>();
	final List<BuildMessageDto> listenedWarnings = new ArrayList<BuildMessageDto>();
	final List<BuildPhase> listenedPhases = new ArrayList<BuildPhase>();
	
	BuildDetailCallback buildDetailCallback = new BuildDetailCallback() {
		public void setPhaseMessageKey(String phase) {}
		public void setDetail(String detail) {}
		public void setDetailMessage(String messageKey, Object[] args) {}
		public void reportError(String message, String file, Integer lineNumber, String code) {}
		public void reportWarning(String message, String file, Integer lineNumber, String code) {}
		public void addMetric(MetricDto metric) {}
		@Override
		public boolean equals(Object obj) {
			return true;
		}
	};
	
	ProjectBuilderImpl builder = new ProjectBuilderImpl() {
		@Override
		protected void invokeBuilder(ProjectConfigDto currentTarget) throws TimeoutException, KilledException, BuildFailedException, ConfigException, IOException, StoreException {
			if (be != null) {
				throw be;
			}
			synchronized(this) {
				invokedBuild = true;
				notifyAll();
			}
			super.invokeBuilder(currentTarget);
		}
		@Override
		protected BuildContext initializeBuildStatus(BuildTarget buildTarget) throws StoreException, ConfigException {
			BuildContext ctx = super.initializeBuildStatus(buildTarget);
			
			if (suppressStartDate) {
				ctx.getCurrentStatus().setStartDate(null);
			}
			
			return ctx;
		}
	};
	
	BuildManager mgr = createStrictMock(BuildManager.class);
	ProjectManager projectMgr = createStrictMock(ProjectManager.class);
	RepositoryAdaptor ra = createStrictMock(RepositoryAdaptor.class);
	BuildTool tool = createStrictMock(BuildTool.class);

	UUID id = UUID.randomUUID();
	
	StoreStub store = new StoreStub(null) {
		@Override
		public File getBuildLog(String projectName, UUID diffId)
				throws StoreException {
			return logFile;
		}
		@Override
		public File getChangeLog(String projectName, UUID diffId)
				throws StoreException {
			return diffFile;
		}
		@Override
		public Long loadAverageBuildTimeMillis(String name, UpdateType updateType) {
			return estimatedBuildTimeMillis;
		}
		@Override
		public ProjectStatusDto loadMostRecentBuildOutcomeByTagName(
				String projectName, String tagName) {
			assertEquals(project.getName(), projectName);
			assertEquals(project.getRepositoryTagName(), tagName);
			return previousStatusByTagName;
		}
		@Override
		public ProjectStatusDto loadMostRecentBuildOutcomeByWorkDir(
				String projectName, String workDir) {
			assertEquals(project.getName(), projectName);
			assertEquals(project.getWorkDir(), workDir);
			return previousStatusByWorkDir;
		}
	};
	
	BuildDaemonInfoDto info = new BuildDaemonInfoDto();
	ProjectStatusDto previousStatus = new ProjectStatusDto();

	RevisionTokenDto rev0 = new RevisionTokenDto(0l);
	RevisionTokenDto rev1 = new RevisionTokenDto(1l);

	Throwable error;
	
	WorkingCopyUpdateExpert updateExpert = new WorkingCopyUpdateExpert() {
		UpdateType determineUpdateStrategy(ProjectConfigDto currentTarget, ProjectStatusDto previousStatus) {
			if (previousStatusByWorkDir != null) {
				assertSame(previousStatusByWorkDir, previousStatus);
			}

			return updateType != null ? updateType : UpdateType.Full;
		}
	};
	
	@Override
	public void setUp() throws Exception {
		checkOrder(false);
		
		UUIDUtils.setForcedUUID(id);
		
		project.setWorkDir("default_workdir");
		
		builder.setWorkingCopyUpdateExpert(updateExpert);
		builder.setConfigurationStore(store);
		builder.setBuildOutcomeStore(store);
		builder.setBuildManager(mgr);
		builder.setProjectManager(projectMgr);
		builder.setDiffsEnabled(true);
		
		builder.init();
		
		expect(ra.getRepositoryUrl()).andReturn("http://localhost").anyTimes();
		expect(projectMgr.getPluginModificationDate((String)anyObject())).andReturn(null).anyTimes();

		mgr.registerBuildStatus((BuildDaemonInfoDto)notNull(), (ProjectBuilder) notNull(),
				(ProjectConfigDto)notNull(), (ProjectStatusDto)notNull());
		expectLastCall().anyTimes();
		
		info.setHostname(InetAddress.getLocalHost());
		info.setName("mock");

		previousStatus.setStatus(Status.PASS);
		previousStatus.setRevision(rev0);
		previousStatus.setBuildNumber(42);
		previousStatus.setTagName("trunk");
		previousStatus.setWorkDir(project.getWorkDir());
		
		buildStatus.setName("a name");
		buildStatus.setRevision(rev0);
		buildStatus.setStatus(Status.BUILDING);
		buildStatus.setTagName("trunk");
		buildStatus.setId(id);
		buildStatus.setDiffId(id);
		buildStatus.setBuildLogId(id);
		buildStatus.setRepositoryUrl("http://localhost");
		buildStatus.setErrors(new ArrayList<BuildMessageDto>());
		buildStatus.setWarnings(new ArrayList<BuildMessageDto>());
		buildStatus.setMetrics(new ArrayList<MetricDto>());
		buildStatus.setBuildNumber(0);
		buildStatus.setWorkDir("dir");
		buildStatus.setWorkDirSupportsIncrementalUpdate(true);
		
		logFile.deleteOnExit();
		diffFile.deleteOnExit();
	}
	
	public void testRaisesPhaseChanged() throws Exception {
		project = new ProjectConfigDto();
		
		doBuildListenerTest();
		
		assertEquals(1, listenedPhases.size());
		assertEquals(BuildPhase.Build, listenedPhases.get(0));
	}
	
	public void testCapturesMetrics() throws Exception {
		project = new ProjectConfigDto();
		
		doBuildListenerTest();
		
		assertEquals(1, buildStatus.getMetrics().size());
	}

	public void testCapturesErrorsAndWarnings() throws Exception {
		project = new ProjectConfigDto();
		
		doBuildListenerTest();
		
		assertEquals(1, buildStatus.getErrors().size());
		assertEquals(1, listenedErrors.size());
		assertEquals(1, buildStatus.getWarnings().size());
		assertEquals(1, listenedWarnings.size());
	}

	public void testSuppressErrors() throws Exception {
		project = new ProjectConfigDto();
		project.setSuppressErrors(true);
		
		doBuildListenerTest();
		
		assertEquals(0, buildStatus.getErrors().size());
		assertEquals(0, listenedErrors.size());
		assertEquals(1, buildStatus.getWarnings().size());
		assertEquals(1, listenedWarnings.size());
	}

	public void testSuppressWarnings() throws Exception {
		project = new ProjectConfigDto();
		project.setSuppressWarnings(true);
		
		doBuildListenerTest();
		
		assertEquals(1, buildStatus.getErrors().size());
		assertEquals(1, listenedErrors.size());
		assertEquals(0, buildStatus.getWarnings().size());
		assertEquals(0, listenedWarnings.size());
	}

	public void testEventWithNoListeners() throws Exception {
		builder.initializeBuildDetailCallback(new BuildTargetImpl(project, buildStatus), buildDetailCallback);
		
		builder.buildDetailCallback.reportError("no one is listening", "file", 2, "code");
	}
	
	public void testRemoveListenerWhenNonePresent() throws Exception {
		builder.initializeBuildDetailCallback(new BuildTargetImpl(project, buildStatus), buildDetailCallback);
		
		assertEquals("removeBuildStatusListener()", false, builder.removeBuildStatusListener(null));
	}
	
	private void doBuildListenerTest() throws Exception {
		builder = new ProjectBuilderImpl() {
			@Override
			protected void invokeBuilder(ProjectConfigDto target) throws TimeoutException, KilledException,	BuildFailedException, ConfigException, IOException,	StoreException {
				buildDetailCallback.setPhase(BuildPhase.Build);
				buildDetailCallback.reportError("an error occurred.", "file", 0, "code");
				buildDetailCallback.reportWarning("you should not do that.", "file", 0, "code");
				buildDetailCallback.addMetric(new MetricDto("message.key", "1", MetricType.PERCENT));
			}
		};
		
		builder.initializeBuildDetailCallback(new BuildTargetImpl(project, buildStatus), buildDetailCallback);
		
		builder.addBuildStatusListener(new BuildStatusListener() {
			public void onBuildPhaseChanged(Object source, BuildPhase phase) {
				listenedPhases.add(phase);
			}
			public void onErrorLogged(Object source, BuildMessageDto error) {
				listenedErrors.add(error);
			}
			public void onWarningLogged(Object source, BuildMessageDto warning) {
				listenedWarnings.add(warning);
			}
		});
		
		builder.invokeBuilder(project);
	}

	public void testInitializeBuildStatus() throws Exception {
		suppressStartDate = false;
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);
		expect(ra.getTagOrBranch()).andReturn("trunk");
		
		replay();
		
		BuildContext ctx = builder.initializeBuildStatus(buildTarget);
		
		verify();
		
		assertSame(previousStatus, ctx.getLastBuild());
		assertSame(previousStatus, ctx.getLastBuildFromSameTag());
		assertSame(previousStatus, ctx.getLastBuildInSameWorkDir());
		
		assertNotNull(ctx.getCurrentStatus().getStartDate());
	}

	public void testInitializeBuildStatusGetsLastBuildInSameWorkDir() throws Exception {
		previousStatus.setWorkDir("other");
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);
		expect(ra.getTagOrBranch()).andReturn("trunk");
		
		replay();
		
		BuildContext ctx = builder.initializeBuildStatus(buildTarget);
		
		verify();
		
		assertSame(previousStatus, ctx.getLastBuild());
		assertSame(previousStatus, ctx.getLastBuildFromSameTag());
		assertSame("lastBuildInSameWorkDir", previousStatusByWorkDir, ctx.getLastBuildInSameWorkDir());
	}

	public void testInitializeBuildStatusGetsLastBuildFromSameTag() throws Exception {
		previousStatus.setTagName("other");
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);
		expect(ra.getTagOrBranch()).andReturn("trunk");
		replay();
		
		BuildContext ctx = builder.initializeBuildStatus(buildTarget);
		
		verify();
		
		assertSame(previousStatus, ctx.getLastBuild());
		assertSame(previousStatusByTagName, ctx.getLastBuildFromSameTag());
		assertSame(previousStatus, ctx.getLastBuildInSameWorkDir());
	}

	public void testInitializeBuildStatusSetsTag() throws Exception {
		project.setRepositoryTagName("other");
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);
		ra.setTagOrBranch(project.getRepositoryTagName());
		expect(ra.getTagOrBranch()).andReturn(project.getRepositoryTagName());
		
		replay();
		
		BuildContext ctx = builder.initializeBuildStatus(buildTarget);
		
		verify();
		
		assertSame(previousStatus, ctx.getLastBuild());
		assertSame(previousStatusByTagName, ctx.getLastBuildFromSameTag());
		assertSame(previousStatus, ctx.getLastBuildInSameWorkDir());
	}

	public void testMain() throws Exception {
		
		// make builder initialize last build, last build in same work dir, last build with same tag.
		replay();
		
		builder.build(info, buildTarget, buildDetailCallback);
		
		verify();
	}
	
	public void _testKillProjectDuringBuild() throws Throwable {
		//sleepTime = 10000;
		
		project = new ProjectConfigDto();
		project.setName("foo");
		project.setWorkDir("dir");
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(mgr.getLatestStatus("foo")).andReturn(null);
		expect(ra.getLatestRevision(null)).andReturn(rev1);
		
		ra.createPristineWorkingCopy(buildDetailCallback);
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev1, "trunk", Status.ERROR, "messages.build.killed", new String[] {"a user"}, null, "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), true, estimatedBuildTimeMillis, false));

		replay();

		assertFalse(builder.isBuilding());

		final Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					builder.build(info, new BuildTargetImpl(project, new ProjectStatusDto()), buildDetailCallback);
				} catch (Throwable e) {
					error = e;
				}
			}
		};

		thread.start();

		synchronized (builder) {
			if (!invokedBuild) {
				builder.wait(1000);
			}
		}
		
		assertTrue(builder.isBuilding());
		
		synchronized (builder) {
			if (!invokedBuild) {
				builder.wait(1000);
			}
		}

		builder.abortCurrentBuild(false, "a user");

		thread.join();
		
		if (error != null) {
			throw error;
		}
		
		verify();
		
		assertTrue("did not interrupt", gotInterrupt);
		
		assertFalse(builder.isBuilding());
	}

	public void _testBuildProjectPreviousNull() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");

		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(null)).andReturn(rev0);

		ra.createPristineWorkingCopy(buildDetailCallback);

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		//buildStatus.setBuildReasonKey("messages.build.reason.repository.changes");
		buildStatus.setWorkDir("a");
		buildStatus.setDiffId(null);
		
		tool.buildProject(
				eq(project),
				eq(buildStatus),
				eq(logFile),
				(BuildDetailCallback)notNull());
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev0, "trunk", Status.PASS, null, null, null, "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), true, estimatedBuildTimeMillis, false));
		
		checkBuild();
		
		assertTrue(invokedBuild);
	}
	public void _testBuildProjectRequestedByUser() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		project.setRequestedBy("Deborah");

		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(null)).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		ra.createPristineWorkingCopy(buildDetailCallback);

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildStatus.setBuildReasonKey("messages.build.reason.repository.changes");
		buildStatus.setRequestedBy("Deborah");
		buildStatus.setDiffId(null);
		
		tool.buildProject(
				eq(project),
				(ProjectStatusDto) notNull(),
				eq(logFile),
				(BuildDetailCallback)notNull());
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev0, "trunk", Status.PASS, null, null, null, "http://localhost", true, "Deborah", null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), true, estimatedBuildTimeMillis, false));
		
		checkBuild();
	}
	

	public void _testGetsChangeLog() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(rev0)).andReturn(rev1);
		
		final ProjectStatusDto prevStatus = new ProjectStatusDto();
		prevStatus.setRevision(rev0);
		prevStatus.setTagName("trunk");
		prevStatus.setStatus(Status.ERROR);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(prevStatus);

		ra.createPristineWorkingCopy(buildDetailCallback);
		
		ra.getChangeLog(eq(rev0), eq(rev1), (OutputStream) notNull());
		expectLastCall().andReturn(new ChangeLogDto());

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildStatus.setTagName("trunk");
		buildStatus.setRevision(rev1);
		buildStatus.setChangeLog(new ChangeLogDto());
		buildStatus.setWorkDir("a");
		
		tool.buildProject(
				eq(project),
				eq(buildStatus),
				eq(logFile),
				(BuildDetailCallback)notNull());
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev1, "trunk", Status.PASS, null, null, new ChangeLogDto(), "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), true, estimatedBuildTimeMillis, true));
		
		checkBuild();
	}
	public void _testGetsChangeLogSkipWhenFlagIsFalse() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		
		builder.setDiffsEnabled(false);
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(rev0)).andReturn(rev1);
		
		final ProjectStatusDto prevStatus = new ProjectStatusDto();
		prevStatus.setRevision(rev0);
		prevStatus.setTagName("trunk");
		prevStatus.setStatus(Status.ERROR);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(prevStatus);

		ra.createPristineWorkingCopy(buildDetailCallback);
		
		ra.getChangeLog(eq(rev0), eq(rev1), (OutputStream) eq(null));
		expectLastCall().andReturn(new ChangeLogDto());

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildStatus.setTagName("trunk");
		buildStatus.setRevision(rev1);
		buildStatus.setChangeLog(new ChangeLogDto());
		buildStatus.setWorkDir("a");
		buildStatus.setDiffId(null);
		
		tool.buildProject(
				eq(project),
				eq(buildStatus),
				eq(logFile),
				(BuildDetailCallback)notNull());
		
		final ProjectStatusDto fakeBuildOutcome = createFakeBuildOutcome(project.getName(), 0, rev1, "trunk", Status.PASS, null, null, new ChangeLogDto(), "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), true, estimatedBuildTimeMillis, false);
				
		mgr.targetCompleted(info, project, fakeBuildOutcome);
		
		checkBuild();
	}
	public void _testGetsChangeLogWithRevisionFromLastBuildWithSameTag() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(rev0)).andReturn(rev1);
		
		previousStatus.setRevision(rev0);
		previousStatus.setTagName("tags/not-trunk");
		previousStatus.setStatus(Status.ERROR);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);

		previousStatusByTagName = new ProjectStatusDto();
		previousStatusByTagName.setTagName("trunk");
		previousStatusByTagName.setRevision(rev0);
		
		ra.createPristineWorkingCopy(buildDetailCallback);
		
		ra.getChangeLog(eq(rev0), eq(rev1), (OutputStream) notNull());
		expectLastCall().andReturn(new ChangeLogDto());

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildStatus.setTagName("trunk");
		buildStatus.setRevision(rev1);
		buildStatus.setChangeLog(new ChangeLogDto());
		buildStatus.setWorkDir("a");
		buildStatus.setBuildNumber(previousStatus.getBuildNumber() + 1);
		
		tool.buildProject(
				eq(project),
				eq(buildStatus),
				eq(logFile),
				(BuildDetailCallback)notNull());
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 43, rev1, "trunk", Status.PASS, null, null, new ChangeLogDto(), "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), true, estimatedBuildTimeMillis, true));
		
		checkBuild();
	}
	public void _testIncremental() throws Exception {
		updateType = UpdateType.Incremental;
		estimatedBuildTimeMillis = 11242341234l;
		
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		project.setUpdateStrategy(ProjectConfigDto.UpdateStrategy.IncrementalAlways);
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(rev0)).andReturn(rev1);
		
		final ProjectStatusDto prevStatus = new ProjectStatusDto();
		prevStatus.setRevision(rev0);
		prevStatus.setTagName("trunk");
		prevStatus.setStatus(Status.ERROR);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(prevStatus);

		ra.updateWorkingCopy(buildDetailCallback);
		
		ra.getChangeLog(eq(rev0), eq(rev1), (OutputStream)notNull());
		expectLastCall().andReturn(new ChangeLogDto());

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildStatus.setTagName("trunk");
		buildStatus.setRevision(rev1);
		buildStatus.setUpdateType(ProjectStatusDto.UpdateType.Incremental);
		buildStatus.setChangeLog(new ChangeLogDto());
		buildStatus.setWorkDir("a");
		buildStatus.setEstimatedBuildTimeMillis(estimatedBuildTimeMillis);
		
		tool.buildProject(
				eq(project),
				eq(buildStatus),
				eq(logFile),
				(BuildDetailCallback)notNull());
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev1, "trunk", Status.PASS, null, null, new ChangeLogDto(), "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Incremental, project.getWorkDir(), true, estimatedBuildTimeMillis, true));
		
		checkBuild();
	}
	public void _testIncrementalGetsPreviousBuildByWorkDir() throws Exception {
		updateType = UpdateType.Incremental;
		estimatedBuildTimeMillis = 11242341234l;
		
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		project.setUpdateStrategy(ProjectConfigDto.UpdateStrategy.IncrementalAlways);
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(rev0)).andReturn(rev1);
		
		final ProjectStatusDto prevStatus = new ProjectStatusDto();
		prevStatus.setRevision(rev0);
		prevStatus.setTagName("trunk");
		prevStatus.setStatus(Status.ERROR);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(prevStatus);

		previousStatusByWorkDir = new ProjectStatusDto();
		previousStatusByWorkDir.setStatus(Status.PASS);
		previousStatusByWorkDir.setWorkDir("a");
		
		ra.updateWorkingCopy(buildDetailCallback);
		
		ra.getChangeLog(eq(rev0), eq(rev1), (OutputStream)notNull());
		expectLastCall().andReturn(new ChangeLogDto());

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildStatus.setTagName("trunk");
		buildStatus.setRevision(rev1);
		buildStatus.setUpdateType(ProjectStatusDto.UpdateType.Incremental);
		buildStatus.setChangeLog(new ChangeLogDto());
		buildStatus.setWorkDir("a");
		buildStatus.setEstimatedBuildTimeMillis(estimatedBuildTimeMillis);
		
		tool.buildProject(
				eq(project),
				eq(buildStatus),
				eq(logFile),
				(BuildDetailCallback)notNull());
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev1, "trunk", Status.PASS, null, null, new ChangeLogDto(), "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Incremental, project.getWorkDir(), true, estimatedBuildTimeMillis, true));
		
		checkBuild();
	}
	public void _testChangeLogUsesLastKnownRevisionWhenPrevNull() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		
		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(rev0)).andReturn(rev1);
		
		final ProjectStatusDto prevStatus = new ProjectStatusDto();
		prevStatus.setLastKnownRevision(rev0);
		prevStatus.setRevision(null);
		prevStatus.setTagName("trunk");
		prevStatus.setStatus(Status.ERROR);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(prevStatus);

		ra.createPristineWorkingCopy(buildDetailCallback);
		
		ra.getChangeLog(eq(rev0), eq(rev1), (OutputStream)notNull());
		expectLastCall().andReturn(new ChangeLogDto());

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildStatus.setTagName("trunk");
		buildStatus.setRevision(rev1);
		buildStatus.setChangeLog(new ChangeLogDto());
		buildStatus.setWorkDir("a");
		
		tool.buildProject(
				eq(project),
				eq(buildStatus),
				eq(logFile),
				(BuildDetailCallback)notNull());

		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev1, "trunk", Status.PASS, null, null, new ChangeLogDto(), "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), true, estimatedBuildTimeMillis, true));
		
		checkBuild();
	}
	public void _testBuildProjectWithTag() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		project.setRepositoryTagName("rc1");
		
		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		ra.setTagOrBranch("rc1");
		expect(ra.getLatestRevision(null)).andReturn(rev1);
		
		final ProjectStatusDto prevStatus = new ProjectStatusDto();
		prevStatus.setRevision(rev0);
		prevStatus.setStatus(Status.PASS);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(prevStatus);

		ra.createPristineWorkingCopy(buildDetailCallback);
		
		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildStatus.setTagName("rc1");
		buildStatus.setRevision(rev1);
		buildStatus.setWorkDir("a");
		buildStatus.setDiffId(null);
		
		tool.buildProject(
				eq(project),
				eq(buildStatus),
				eq(logFile),
				(BuildDetailCallback)notNull());
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev1, "rc1", Status.PASS, null, null, null, "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), true, estimatedBuildTimeMillis, false));
		
		checkBuild();
	}
	public void _testBuildFailsWithNoBuildTarget() throws Exception {
		project = new ProjectConfigDto();
		project.setWorkDir("a");

		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(null)).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		ra.createPristineWorkingCopy(buildDetailCallback);

		tool = new BuildTool() {
			public void buildProject(ProjectConfigDto projectConfig, ProjectStatusDto buildStatus, File logFile, BuildDetailCallback buildDetailCallback) throws BuildFailedException, ConfigException {
				throw new BuildFailedException("none", null, 0);
			}
		};
		
		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev0, "trunk", Status.FAIL, "messages.build.failure", new String[] {"none"}, null, "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), true, estimatedBuildTimeMillis, false));
		
		checkBuild();
	}
	public void _testBuildProjectNullOrBlankWorkDir() throws Exception {
		project = new ProjectConfigDto();
		project.setWorkDir("");

		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, null,
				null, Status.ERROR, "messages.build.null.work.dir", null, null, null, true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), false, estimatedBuildTimeMillis, true));
		

		checkBuild();
	}
	public void _testBuildProjectCannotCreateWorkDir() throws Exception {
		createWorkingDirectoriesSuccess = false;

		project = new ProjectConfigDto();
		project.setWorkDir("hello");

		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(null)).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev0,
				"trunk", Status.ERROR, "errors.cannot.create.dir", new Object[] {new File("hello").getCanonicalPath()}, null, "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), false, estimatedBuildTimeMillis, true));
		
		checkBuild();
	}
	public void _testBuildProjectValidatesWorkingCopyBeforeDelete() throws Exception {
		final File invalid = new File(".").getCanonicalFile();

		createWorkingDirectoriesSuccess = false;

		project = new ProjectConfigDto();
		project.setWorkDir(invalid.getPath());

		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(null)).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		expect(ra.isWorkingCopy()).andReturn(true);
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev0,
				"trunk", Status.ERROR, "errors.cannot.create.dir", new Object[] {invalid.getCanonicalPath()}, null, "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), false, estimatedBuildTimeMillis, true));
		
		checkBuild();
	}
	public void _testBuildProjectRefusesToDeleteNonWorkingCopy() throws Exception {
		final File invalid = new File(".").getCanonicalFile();

		createWorkingDirectoriesSuccess = false;

		project = new ProjectConfigDto();
		project.setWorkDir(invalid.getPath());

		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagOrBranch()).andReturn("trunk");
		expect(ra.getLatestRevision(null)).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		expect(ra.isWorkingCopy()).andReturn(false);
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev0,
				"trunk", Status.ERROR, "errors.wont.delete.non.working.copy", new Object[] {invalid.getCanonicalPath()}, null, "http://localhost", true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), false, estimatedBuildTimeMillis, true));
		
		checkBuild();
	}

	public void _testGetLoc() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a");
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);
		
		//re = new RepositoryException(new SQLException(
		//		"table or view does not exist"));

		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 43, null,
				null, Status.ERROR, "messages.repository.error",
				new Object[]{"table or view does not exist"}, null, null, true, null, null, null,
				ProjectStatusDto.UpdateType.Full, project.getWorkDir(), false, estimatedBuildTimeMillis, true));

		checkBuild();
	}
	public void _testTreatsGeneralExceptionAsError()
			throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);

		Exception re = new RuntimeException("this should not have happened");

		final ProjectStatusDto completedOutcome = createFakeBuildOutcome(project.getName(), 43, null,
				null, Status.ERROR, "messages.build.uncaught.exception",
				new String[]{project.getName(), re.getMessage(), BuildPhase.Build.name()},
				null, null, true, null, null, null, ProjectStatusDto.UpdateType.Full, project.getWorkDir(), false, estimatedBuildTimeMillis, true);
		
		mgr.targetCompleted((BuildDaemonInfoDto)anyObject(), eq(project), eq(completedOutcome));

		checkBuild();
	}

	private void checkBuild() throws Exception {
		replay();

		try {
			builder.build(info, new BuildTargetImpl(project, new ProjectStatusDto()), buildDetailCallback);
		} catch (Exception e) {
			verify();
			throw e;
		}
		verify();
	}

	private ProjectStatusDto createFakeBuildOutcome(String name, int buildNumber, RevisionTokenDto rev, String tagName, Status status, String key, Object[] objects, ChangeLogDto changeLog, String repoUrl, boolean statusChanged, String requestedBy, String reasonKey, String reasonArg, UpdateType updateType, String workDir, boolean workDirSupportsIncrementalUpdate, Long estimatedBuildTimeMillis, boolean diffAvailable) {
		final ProjectStatusDto dto = new ProjectStatusDto();
		dto.setBuildNumber(buildNumber);
		dto.setId(id);
		dto.setDiffId(diffAvailable ? id : null);
		dto.setBuildLogId(id);
		dto.setName(name);
		dto.setRevision(rev);
		dto.setStatus(status);
		dto.setMessageKey(key);
		dto.setMessageArgs(objects);
		dto.setChangeLog(changeLog);
		dto.setTagName(tagName);
		dto.setRepositoryUrl(repoUrl);
		dto.setStatusChanged(statusChanged);
		dto.setRequestedBy(requestedBy);
		dto.setErrors(new ArrayList<BuildMessageDto>());
		dto.setWarnings(new ArrayList<BuildMessageDto>());
		dto.setMetrics(new ArrayList<MetricDto>());
		dto.setBuildReasonKey(reasonKey);
		dto.setUpdateType(updateType);
		dto.setWorkDir(workDir);
		dto.setWorkDirSupportsIncrementalUpdate(workDirSupportsIncrementalUpdate);
		dto.setEstimatedBuildTimeMillis(estimatedBuildTimeMillis);
		
		if (reasonArg != null) {
			dto.setBuildReasonArgs(new String[] {reasonArg});
		}
		
		return dto;
	}
}
