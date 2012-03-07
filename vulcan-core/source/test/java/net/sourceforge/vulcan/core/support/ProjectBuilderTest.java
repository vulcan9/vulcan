/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.BuildPhase;
import net.sourceforge.vulcan.core.support.ProjectBuilderImpl.RunnablePhase;
import net.sourceforge.vulcan.core.support.ProjectBuilderImpl.RunnablePhaseImpl;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.exception.StoreException;

import org.apache.commons.logging.Log;
import org.easymock.IAnswer;

public class ProjectBuilderTest extends EasyMockTestCase {
	ProjectConfigDto project = new ProjectConfigDto();
	ProjectStatusDto buildStatus = new ProjectStatusDto();
	BuildTargetImpl buildTarget = new BuildTargetImpl(project, buildStatus);
	BuildContext buildContext = new BuildContext(buildTarget);
	
	ProjectStatusDto lastBuild = new ProjectStatusDto();
	ProjectStatusDto lastBuildFromSameTag = new ProjectStatusDto();
	ProjectStatusDto lastBuildInSameWorkDir = new ProjectStatusDto();

	BuildDaemonInfoDto info = new BuildDaemonInfoDto();

	RevisionTokenDto rev0 = new RevisionTokenDto(0L);
	RevisionTokenDto rev1 = new RevisionTokenDto(1L);
	
	boolean suppressStartDate = true;
	
	File logFile = new File("fakeBuildLog.log");
	File diffFile = new File("fakeDiff.log");
	
	Long estimatedBuildTimeMillis = null;
	
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
		protected void initializeBuildStatus(BuildContext ctx) throws StoreException, ConfigException {
			super.initializeBuildStatus(ctx);
			
			if (suppressStartDate) {
				ctx.getCurrentStatus().setStartDate(null);
			}
		}
	};
	
	BuildManager mgr = createStrictMock(BuildManager.class);
	ProjectManager projectMgr = createStrictMock(ProjectManager.class);
	FileSystem fileSystem = createStrictMock(FileSystem.class);
	RepositoryAdaptor repository = createStrictMock(RepositoryAdaptor.class);
	BuildTool buildTool = createStrictMock(BuildTool.class);

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
			return lastBuildFromSameTag;
		}
		@Override
		public ProjectStatusDto loadMostRecentBuildOutcomeByWorkDir(
				String projectName, String workDir) {
			assertEquals(project.getName(), projectName);
			assertEquals(project.getWorkDir(), workDir);
			return lastBuildInSameWorkDir;
		}
	};
	
	WorkingCopyUpdateExpert updateExpert = new WorkingCopyUpdateExpert() {
		UpdateType determineUpdateStrategy(ProjectConfigDto currentTarget, ProjectStatusDto previousStatus) {
			if (lastBuildInSameWorkDir != null) {
				assertSame(lastBuildInSameWorkDir, previousStatus);
			}

			return UpdateType.Full;
		}
	};
	
	@Override
	public void setUp() throws Exception {
		UUIDUtils.setForcedUUID(id);
		
		project.setName("example_project");
		project.setWorkDir("default_workdir");
		
		builder.setWorkingCopyUpdateExpert(updateExpert);
		builder.setConfigurationStore(store);
		builder.setBuildOutcomeStore(store);
		builder.setBuildManager(mgr);
		builder.setProjectManager(projectMgr);
		builder.setFileSystem(fileSystem);
		builder.setDiffsEnabled(true);
		
		builder.init();
		
		info.setHostname(InetAddress.getLocalHost());
		info.setName("mock");

		lastBuild.setStatus(Status.PASS);
		lastBuild.setRevision(rev0);
		lastBuild.setBuildNumber(42);
		lastBuild.setTagName("trunk");
		lastBuild.setWorkDir(project.getWorkDir());
		
		logFile.deleteOnExit();
		diffFile.deleteOnExit();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Thread.interrupted();
	}
	
	public void testDefaultPhases() throws Exception {
		assertEquals(Arrays.asList(builder.prepareRepository, builder.createWorkingCopy, builder.getChangeLog, builder.invokeBuildTool), builder.phases);
	}

	public void testExecutePhasesRunsPhases() throws Exception {
		RunnablePhase p1 = createStrictMock(RunnablePhase.class);
		RunnablePhase p2 = createStrictMock(RunnablePhase.class);
		
		p1.execute(buildContext);
		p2.execute(buildContext);
		
		replay();
		
		builder.phases = Arrays.asList(p1, p2);
		
		builder.executeBuildPhases(buildContext);
		
		verify();
	}

	public void testValidateNullOrBlankWorkDir() throws Exception {
		project.setWorkDir("");
		
		try {
			builder.validateWorkDir(buildContext);
			fail("expected exception");
		} catch (ConfigException e) {
			assertEquals("messages.build.null.work.dir", e.getKey());
		}
	}

	public void testValidateWorkDirDoesNotExist() throws Exception {
		fileSystem.directoryExists(new File(project.getWorkDir()));
		expectLastCall().andReturn(false);
		
		replay();
		
		builder.validateWorkDir(buildContext);
		
		verify();
	}

	public void testValidateWorkDirNotPreviouslyUsed() throws Exception {
		buildContext.setLastBuildInSameWorkDir(null);
		
		fileSystem.directoryExists(new File(project.getWorkDir()));
		expectLastCall().andReturn(true);
		
		replay();
		
		try {
			builder.validateWorkDir(buildContext);
			fail("expected exception");
		} catch (ConfigException e) {
			assertEquals("errors.wont.delete.non.working.copy", e.getKey());
			assertEquals(Arrays.asList(buildContext.getConfig().getWorkDir(), buildContext.getProjectName()), Arrays.asList(e.getArgs()));
		}
		
		verify();
	}

	public void testValidateWorkDirPreviouslyUsed() throws Exception {
		buildContext.setLastBuildInSameWorkDir(lastBuildInSameWorkDir);
		
		fileSystem.directoryExists(new File(project.getWorkDir()));
		expectLastCall().andReturn(true);
		
		replay();
		
		builder.validateWorkDir(buildContext);
		
		verify();
	}
	
	public void testInitializeBuildStatus() throws Exception {
		suppressStartDate = false;
		
		BuildContext ctx = doInitializeBuildStatusTest();
		
		assertNotNull(ctx.getCurrentStatus().getStartDate());
		assertEquals(lastBuild.getBuildNumber()+1, ctx.getCurrentStatus().getBuildNumber().intValue());
	}

	public void testInitializeBuildStatusSetsTagOnCurrentStatus() throws Exception {
		suppressStartDate = false;
		
		BuildContext ctx = doInitializeBuildStatusTest();
		
		assertEquals("trunk", ctx.getCurrentStatus().getTagName());
	}
	
	
	public void testInitializeBuildStatusSetsLastBuildFromSameTag() throws Exception {
		suppressStartDate = false;
		
		BuildContext ctx = doInitializeBuildStatusTest();
	
		assertSame("lastBuildFromSameTag", lastBuild, ctx.getLastBuildFromSameTag());
	}
	
	public void testInitializeBuildStatusPreviousNull() throws Exception {
		lastBuild = null;
		
		BuildContext ctx = doInitializeBuildStatusTest();
		
		assertEquals(0, ctx.getCurrentStatus().getBuildNumber().intValue());
	}

	public void testInitializeBuildStatusRequestedByUser() throws Exception {
		project.setRequestedBy("Deborah");
		
		BuildContext ctx = doInitializeBuildStatusTest();
		
		assertEquals(project.getRequestedBy(), ctx.getCurrentStatus().getRequestedBy());
	}

	public void testInitializeBuildStatusSetsScheduledFlag() throws Exception {
		project.setScheduledBuild(true);
		
		BuildContext ctx = doInitializeBuildStatusTest();
		
		assertEquals(project.isScheduledBuild(), ctx.getCurrentStatus().isScheduledBuild());
	}
	
	public void testInitializeBuildSpecifiedTag() throws Exception {
		project.setRepositoryTagName("v1.2");
		
		BuildContext ctx = doInitializeBuildStatusTest();
		
		assertSame("lastBuildFromSameTag", lastBuildFromSameTag, ctx.getLastBuildFromSameTag());
		assertEquals("v1.2", ctx.getCurrentStatus().getTagName());
	}
	
	public void testInitializeBuildSetsTagOnProject() throws Exception {
		buildContext.getConfig().setRepositoryTagName(null);
		
		BuildContext ctx = doInitializeBuildStatusTest();
	
		assertEquals("trunk", ctx.getConfig().getRepositoryTagName());
	}
	
	private BuildContext doInitializeBuildStatusTest() throws Exception {
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(repository);
		expect(mgr.getLatestStatus(project.getName())).andReturn(lastBuild);
		
		String tag = "trunk";
		
		if (project.getRepositoryTagName() != null) {
			tag = project.getRepositoryTagName();
			repository.setTagOrBranch(tag);
		}
		
		expect(repository.getTagOrBranch()).andReturn(tag);
		
		replay();
		
		builder.initializeBuildStatus(buildContext);
		
		verify();
		
		assertSame("lastBuild", lastBuild, buildContext.getLastBuild());
		assertSame("lastBuildInSameWorkDir", lastBuild, buildContext.getLastBuildInSameWorkDir());
		
		return buildContext;
	}
	
	public void testInitializeBuildStatusGetsLastBuildInSameWorkDir() throws Exception {
		lastBuild.setWorkDir("other");
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(repository);
		expect(mgr.getLatestStatus(project.getName())).andReturn(lastBuild);
		expect(repository.getTagOrBranch()).andReturn("trunk");
		
		replay();
		
		builder.initializeBuildStatus(buildContext);
		
		verify();
		
		assertSame(lastBuild, buildContext.getLastBuild());
		assertSame(lastBuild, buildContext.getLastBuildFromSameTag());
		assertSame("lastBuildInSameWorkDir", lastBuildInSameWorkDir, buildContext.getLastBuildInSameWorkDir());
	}
	
	public void testInitializeBuildStatusGetsLastBuildInSameWorkDirNullSafe() throws Exception {
		lastBuild.setStatus(Status.SKIP);
		lastBuild.setWorkDir(null);
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(repository);
		expect(mgr.getLatestStatus(project.getName())).andReturn(lastBuild);
		expect(repository.getTagOrBranch()).andReturn("trunk");
		
		replay();
		
		builder.initializeBuildStatus(buildContext);
		
		verify();
		
		assertSame(lastBuild, buildContext.getLastBuild());
		assertSame(lastBuild, buildContext.getLastBuildFromSameTag());
		assertSame("lastBuildInSameWorkDir", lastBuildInSameWorkDir, buildContext.getLastBuildInSameWorkDir());
	}

	public void testInitializeBuildStatusGetsLastBuildFromSameTag() throws Exception {
		lastBuild.setTagName("other");
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(repository);
		expect(mgr.getLatestStatus(project.getName())).andReturn(lastBuild);
		expect(repository.getTagOrBranch()).andReturn("trunk");
		
		replay();
		
		builder.initializeBuildStatus(buildContext);
		
		verify();
		
		assertSame(lastBuild, buildContext.getLastBuild());
		assertSame(lastBuildFromSameTag, buildContext.getLastBuildFromSameTag());
		assertSame(lastBuild, buildContext.getLastBuildInSameWorkDir());
	}

	public void testInitializeBuildStatusGetsLastBuildFromSameTagNullSafe() throws Exception {
		lastBuild.setStatus(Status.SKIP);
		lastBuild.setTagName(null);
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(repository);
		expect(mgr.getLatestStatus(project.getName())).andReturn(lastBuild);
		expect(repository.getTagOrBranch()).andReturn("trunk");
		
		replay();
		
		builder.initializeBuildStatus(buildContext);
		
		verify();
		
		assertSame(lastBuild, buildContext.getLastBuild());
		assertSame(lastBuildFromSameTag, buildContext.getLastBuildFromSameTag());
		assertSame(lastBuild, buildContext.getLastBuildInSameWorkDir());
	}

	public void testInitializeBuildStatusSetsTag() throws Exception {
		project.setRepositoryTagName("other");
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(repository);
		expect(mgr.getLatestStatus(project.getName())).andReturn(lastBuild);
		repository.setTagOrBranch(project.getRepositoryTagName());
		expect(repository.getTagOrBranch()).andReturn(project.getRepositoryTagName());
		
		replay();
		
		builder.initializeBuildStatus(buildContext);
		
		verify();
		
		assertSame(lastBuild, buildContext.getLastBuild());
		assertSame(lastBuildFromSameTag, buildContext.getLastBuildFromSameTag());
		assertSame(lastBuild, buildContext.getLastBuildInSameWorkDir());
	}



	public void testPrepareRepositorySetsProperties() throws Exception {
		doPrepareRepositoryTest();
		
		assertEquals(rev0, buildContext.getRevision());
		assertEquals("http://localhost", buildContext.getCurrentStatus().getRepositoryUrl());
	}

	public void testPrepareRepositorySetsUpdateType() throws Exception {
		buildContext.setUpdateType(null);
		
		doPrepareRepositoryTest();
		
		assertEquals(UpdateType.Full, buildContext.getUpdateType());
	}

	public void testPrepareRepositorySetsEstimatedBuildTime() throws Exception {
		estimatedBuildTimeMillis = 123L;
		
		doPrepareRepositoryTest();
		
		assertEquals(estimatedBuildTimeMillis, buildContext.getCurrentStatus().getEstimatedBuildTimeMillis());
	}

	private void doPrepareRepositoryTest() throws RepositoryException, InterruptedException, Exception {
		builder.initializeBuildDetailCallback(buildTarget, buildDetailCallback);
		
		buildContext.setLastBuildInSameWorkDir(lastBuildInSameWorkDir);
		buildContext.setRepositoryAdatpor(repository);
		
		repository.prepareRepository(builder.buildDetailCallback);
		
		expect(repository.getLatestRevision(null)).andReturn(rev0);
		expect(repository.getRepositoryUrl()).andReturn("http://localhost");
		
		replay();
		
		builder.prepareRepository.executePhase(buildContext);
		
		verify();
	}

	public void testCreateWorkingCopyFull() throws Exception {
		buildContext.setUpdateType(UpdateType.Full);
		buildContext.setRepositoryAdatpor(repository);
		buildContext.getCurrentStatus().setWorkDirSupportsIncrementalUpdate(false);
		
		repository.createPristineWorkingCopy(buildDetailCallback);
		
		replay();
		
		builder.createWorkingCopy.executePhase(buildContext);
		
		verify();
		
		assertEquals("WorkDirSupportsIncrementalUpdate", true, buildContext.getCurrentStatus().isWorkDirSupportsIncrementalUpdate());
	}

	public void testCreateWorkingCopyIncrementall() throws Exception {
		buildContext.setUpdateType(UpdateType.Incremental);
		buildContext.setRepositoryAdatpor(repository);
		buildContext.getCurrentStatus().setWorkDirSupportsIncrementalUpdate(false);
		
		repository.updateWorkingCopy(buildDetailCallback);
		
		replay();
		
		builder.createWorkingCopy.executePhase(buildContext);
		
		verify();
		
		assertEquals("WorkDirSupportsIncrementalUpdate", true, buildContext.getCurrentStatus().isWorkDirSupportsIncrementalUpdate());
	}
	
	public void testGetsChangeLog() throws Exception {
		final ChangeLogDto changeLog = new ChangeLogDto();
		
		lastBuildFromSameTag.setRevision(rev0);
		buildContext.setLastBuildFromSameTag(lastBuildFromSameTag);
		buildContext.getCurrentStatus().setRevision(rev1);
		buildContext.setRepositoryAdatpor(repository);
		
		repository.getChangeLog(eq(rev0), eq(rev1), (OutputStream) notNull());
		expectLastCall().andReturn(changeLog);

		replay();
		
		builder.getChangeLog.executePhase(buildContext);
		
		verify();
		
		assertSame("changeLog", changeLog, buildContext.getCurrentStatus().getChangeLog());
	}
	
	public void testGetsChangeLogNullDiffOutputStreamWhenFlagIsFalse() throws Exception {
		builder.setDiffsEnabled(false);
		
		lastBuildFromSameTag.setRevision(rev0);
		buildContext.setLastBuildFromSameTag(lastBuildFromSameTag);
		buildContext.getCurrentStatus().setRevision(rev1);
		buildContext.setRepositoryAdatpor(repository);
		
		repository.getChangeLog(eq(rev0), eq(rev1), (OutputStream) eq(null));
		expectLastCall().andReturn(new ChangeLogDto());

		replay();
		
		builder.getChangeLog.executePhase(buildContext);
		
		verify();
		
		assertEquals("diffId", null, buildContext.getCurrentStatus().getDiffId());
	}

	public void testGetsChangeLogSkipsOnNoPreviousBuildFromSameTag() throws Exception {
		buildContext.setLastBuildFromSameTag(null);
		buildContext.getCurrentStatus().setRevision(rev1);
		buildContext.setRepositoryAdatpor(repository);
		
		replay();
		
		builder.getChangeLog.executePhase(buildContext);
		
		verify();
		
		assertSame("changeLog", null, buildContext.getCurrentStatus().getChangeLog());
	}

	public void testGetsChangeLogSkipsWhenRevisionNotChanged() throws Exception {
		lastBuildFromSameTag.setRevision(rev0);
		buildContext.setLastBuildFromSameTag(lastBuildFromSameTag);
		buildContext.getCurrentStatus().setRevision(rev0);
		buildContext.setRepositoryAdatpor(repository);
		
		replay();
		
		builder.getChangeLog.executePhase(buildContext);
		
		verify();
		
		assertSame("changeLog", null, buildContext.getCurrentStatus().getChangeLog());
	}

	public void testInvokeBuildTool() throws Exception {
		expect(projectMgr.getBuildTool(project)).andReturn(buildTool);
		buildTool.buildProject(eq(project), eq(buildStatus), (File)notNull(), (BuildDetailCallback) notNull());
		
		replay();
		
		builder.initializeBuildDetailCallback(buildTarget, buildDetailCallback);
		builder.invokeBuildTool.executePhase(buildContext);
		
		verify();
	}
	
	public void testRunnablePhaseSetsAndClearsPhase() throws Exception {
		doRunnablePhaseTest(null);
	}

	public void testRunnablePhaseClearsInterrupt() throws Exception {
		doRunnablePhaseTest(new RunnableCallback() {
			public void run(BuildContext buildContext) throws Exception {
				Thread.currentThread().interrupt();
			}
		});
		
		assertEquals("Thread interrupted", false, Thread.currentThread().isInterrupted());
	}

	public void testRunnablePhaseHandlesInterruptedException() throws Exception {
		doRunnablePhaseTest(new RunnableCallback() {
			public void run(BuildContext buildContext) throws Exception {
				throw new InterruptedException();
			}
		});
	}

	public void testRunnablePhaseSetsAndClearsPhaseOnException() throws Exception {
		final RuntimeException e = new RuntimeException();
		
		try {
			doRunnablePhaseTest(new RunnableCallback() {
				public void run(BuildContext buildContext) {
					throw e;
				}
			});
			
			fail("expected exception");
		} catch (RuntimeException caught) {
			assertSame(e, caught);
		}
		
		assertEquals(null, builder.buildDetailCallback.getCurrentPhase());
	}

	public void testRunnablePhaseThrowsTimeoutException() throws Exception {
		try {
			doRunnablePhaseTest(new RunnableCallback() {
				public void run(BuildContext buildContext) {
					builder.timeout = true;
				}
			});
			fail("expected exception");
		} catch (TimeoutException e) {
		}
	}

	public void testRunnablePhaseThrowsTimeoutExceptionOnKillingFlag() throws Exception {
		try {
			doRunnablePhaseTest(new RunnableCallback() {
				public void run(BuildContext buildContext) {
					builder.killing = true;
				}
			});
			fail("expected exception");
		} catch (TimeoutException e) {
		}
	}

	private interface RunnableCallback {
		void run(BuildContext buildContext) throws Exception;
	}
	
	private void doRunnablePhaseTest(final RunnableCallback runInsidePhase) throws Exception {
		final RunnablePhaseImpl[] phase = new RunnablePhaseImpl[1];
		final boolean[] called = new boolean[1];
		
		builder = new ProjectBuilderImpl() {
			@Override
			public void init() {
				phase[0] = new RunnablePhaseImpl(BuildPhase.Publish) {
					@Override
					protected void executePhase(BuildContext buildContext) throws Exception {
						called[0] = true;
						assertEquals(BuildPhase.Publish, buildDetailCallback.getCurrentPhase());
						if (runInsidePhase != null) {
							runInsidePhase.run(buildContext);
						}
					}
				};
			}
		};
		
		builder.init();
		builder.initializeBuildDetailCallback(buildTarget, buildDetailCallback);
		
		phase[0].execute(buildContext);
		
		assertEquals("called", true, called[0]);
		assertEquals(null, builder.buildDetailCallback.getCurrentPhase());
	}
	
	public void testAbort() throws Exception {
		builder.initializeThreadState();
		
		builder.abortCurrentBuild(false, "impatient");
			
		assertEquals("interrupted", true, Thread.currentThread().isInterrupted());
		
		assertEquals("killing", true, builder.isKilling());
	}
	
	public void testTopLevelBuild() throws Exception {
		doTopLevelBuildTest(null);
		
		assertEquals(Status.PASS, buildStatus.getStatus());
	}
	
	public void testTopLevelBuildClearsPhase() throws Exception {
		doTopLevelBuildTest(null);
		
		assertEquals(null, builder.buildDetailCallback.getCurrentPhase());
	}
	
	public void testTopLevelBuildSetsStatusChanged() throws Exception {
		doTopLevelBuildTest(null);
		
		assertEquals("statusChanged", true, buildStatus.isStatusChanged());
	}
	
	public void testTopLevelBuildSetsStatusNotChanged() throws Exception {
		doTopLevelBuildTest(new RunnableCallback() {
			public void run(BuildContext buildContext) throws Exception {
				buildStatus.setStatus(Status.PASS);
				buildContext.setLastBuild((ProjectStatusDto) buildStatus.copy());
			}
		});
		
		assertEquals("statusChanged", false, buildStatus.isStatusChanged());
	}
	
	public void testTopLevelBuildReportsConfigException() throws Exception {
		doTopLevelBuildTest(new RunnableCallback() {
			public void run(BuildContext buildContext) throws Exception {
				throw new ConfigException("some.key", "arg1", "arg2");
			}
		});
		
		assertEquals(Status.ERROR, buildStatus.getStatus());
		assertEquals("some.key", buildStatus.getMessageKey());
		assertEquals(Arrays.asList("arg1", "arg2"), Arrays.asList(buildStatus.getMessageArgs()));
	}
	
	public void testTopLevelBuildReportsTimeout() throws Exception {
		doTopLevelBuildTest(new RunnableCallback() {
			public void run(BuildContext buildContext) throws Exception {
				builder.abortCurrentBuild(true, "watchdog");
				throw new TimeoutException();
			}
		});
		
		assertEquals(Status.ERROR, buildStatus.getStatus());
		assertEquals("messages.build.timeout", buildStatus.getMessageKey());
	}
	
	public void testTopLevelBuildReportsKilled() throws Exception {
		doTopLevelBuildTest(new RunnableCallback() {
			public void run(BuildContext buildContext) throws Exception {
				builder.abortCurrentBuild(false, "impatient user");
				throw new TimeoutException();
			}
		});
		
		assertEquals(Status.ERROR, buildStatus.getStatus());
		assertEquals("messages.build.killed", buildStatus.getMessageKey());
		assertEquals(Arrays.asList("impatient user"), Arrays.asList(buildStatus.getMessageArgs()));
	}
	
	public void testTopLevelBuildReportsUnexpectedException() throws Exception {
		doTopLevelBuildTest(new RunnableCallback() {
			public void run(BuildContext buildContext) throws Exception {
				throw new RuntimeException("this is probably a bug.");
			}
		});
		
		assertEquals(Status.ERROR, buildStatus.getStatus());
		assertEquals("messages.build.uncaught.exception", buildStatus.getMessageKey());
		assertEquals(Arrays.asList(project.getName(), "this is probably a bug.", "(none)"), Arrays.asList(buildStatus.getMessageArgs()));
	}
	
	public void testTopLevelBuildReportsUnexpectedExceptionDuringPhase() throws Exception {
		doTopLevelBuildTest(new RunnableCallback() {
			public void run(BuildContext buildContext) throws Exception {
				builder.buildDetailCallback.setPhase(BuildPhase.Build);
				throw new RuntimeException("this is probably a bug.");
			}
		});
		
		assertEquals(Status.ERROR, buildStatus.getStatus());
		assertEquals("messages.build.uncaught.exception", buildStatus.getMessageKey());
		assertEquals(Arrays.asList(project.getName(), "this is probably a bug.", BuildPhase.Build.name()), Arrays.asList(buildStatus.getMessageArgs()));
	}
	
	public void testTopLevelBuildReportsBuildToolFailure() throws Exception {
		doTopLevelBuildTest(new RunnableCallback() {
			public void run(BuildContext buildContext) throws Exception {
				throw new BuildFailedException("the build failed", null, 0);
			}
		});
		
		assertEquals(Status.FAIL, buildStatus.getStatus());
		assertEquals("messages.build.failure", buildStatus.getMessageKey());
		assertEquals(Arrays.asList("the build failed"), Arrays.asList(buildStatus.getMessageArgs()));
	}
	
	public void testTopLevelBuildReportsBuildToolFailureWithTarget() throws Exception {
		doTopLevelBuildTest(new RunnableCallback() {
			public void run(BuildContext buildContext) throws Exception {
				throw new BuildFailedException("the build failed", "BuggyTarget", 0);
			}
		});
		
		assertEquals(Status.FAIL, buildStatus.getStatus());
		assertEquals("messages.build.failure.during.target", buildStatus.getMessageKey());
		assertEquals(Arrays.asList("the build failed", "BuggyTarget"), Arrays.asList(buildStatus.getMessageArgs()));
	}

	private void doTopLevelBuildTest(final RunnableCallback runInsideExecuteBuildPhases) {
		builder = new ProjectBuilderImpl() {
			@Override
			protected void initializeBuildStatus(BuildContext context) throws StoreException, ConfigException {
				buildContext = context;
			}
			
			@Override
			protected void executeBuildPhases(BuildContext buildContext) throws Exception {
				if (runInsideExecuteBuildPhases != null) {
					runInsideExecuteBuildPhases.run(buildContext);
				}
			}
		};
		
		builder.setBuildManager(mgr);
		builder.setFileSystem(fileSystem);
		
		builder.log = createNiceMock(Log.class);
		
		expect(fileSystem.directoryExists((File) notNull())).andReturn(false);
		
		mgr.registerBuildStatus(eq(info), eq(builder), eq(project), eq(buildStatus));

		mgr.targetCompleted(info, buildTarget.getProjectConfig(), buildTarget.getStatus());
		
		expectLastCall().andAnswer(new IAnswer<Object>() {
			public Object answer() throws Throwable {
				assertEquals(BuildPhase.Publish, builder.buildDetailCallback.getCurrentPhase());
				return null;
			}
		});
		
		replay();
		
		builder.build(info, buildTarget, buildDetailCallback);
		
		verify();
	}
}
