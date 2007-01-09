/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2006 Chris Eldredge
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

import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.Date;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id = "$Id$", url = "$HeadURL$")
public class ProjectBuilderTest extends EasyMockTestCase {
	boolean createWorkingDirectoriesSuccess = true;
	long sleepTime = -1;
	
	ProjectConfigDto project;

	ProjectStatusDto buildToolStatus = new ProjectStatusDto();
	
	Exception re;
	BuildFailedException be;
	
	boolean gotInterrupt;
	boolean invokedBuild;
	boolean suppressStartDate = true;
	
	File logFile = new File("fakeBuildLog.log");
	
	String errorMessage;
	String warningMessage;
	
	UpdateType updateType = null;
	
	BuildDetailCallback buildDetailCallback = new BuildDetailCallback() {
		public void setPhase(String phase) {};
		public void setDetail(String detail) {};
		public void reportError(String message, String file, Integer lineNumber, String code) {
		}
		public void reportWarning(String message, String file, Integer lineNumber, String code) {
		}
		@Override
		public boolean equals(Object obj) {
			return true;
		}
	};
	
	ProjectBuilderImpl builder = new ProjectBuilderImpl() {
		@Override
		protected void buildProject(ProjectConfigDto currentTarget) throws Exception {
			if (re != null) {
				doPhase(BuildPhase.Build, new PhaseCallback() {
					public void execute() throws Exception {
						throw re;						
					}
				});
			}
			super.buildProject(currentTarget);
		}
		@Override
		protected boolean createWorkingDirectories(File path) throws ConfigException {
			return createWorkingDirectoriesSuccess;
		}
		@Override
		protected void invokeBuilder(ProjectConfigDto currentTarget) throws TimeoutException, KilledException, BuildFailedException, ConfigException, IOException, StoreException {
			if (be != null) {
				throw be;
			}
			synchronized(this) {
				invokedBuild = true;
				notifyAll();
			}
			if (errorMessage != null) {
				buildDetailCallback.reportError(errorMessage, null, null, null);
			}
			if (warningMessage != null) {
				buildDetailCallback.reportWarning(warningMessage, null, null, null);
			}
			if (sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					gotInterrupt = true;
					Thread.currentThread().interrupt();
				}
			} else {
				super.invokeBuilder(currentTarget);
			}
		}
		@Override
		protected File createTempFile() throws IOException {
			return logFile;
		}
		@Override
		protected void createBuildStatus(ProjectConfigDto currentTarget) {
			super.createBuildStatus(currentTarget);
			if (suppressStartDate) {
				buildStatus.setStartDate(null);
			}
		}
		@Override
		protected void determineUpdateType(ProjectConfigDto currentTarget) {
			if (ProjectBuilderTest.this.updateType != null) {
				this.updateType = ProjectBuilderTest.this.updateType; 
			} else {
				super.determineUpdateType(currentTarget);
			}
		}
	};
	
	BuildManager mgr = createStrictMock(BuildManager.class);
	ProjectManager projectMgr = createStrictMock(ProjectManager.class);
	RepositoryAdaptor ra = createStrictMock(RepositoryAdaptor.class);
	BuildTool tool = createStrictMock(BuildTool.class);

	UUID id = UUID.randomUUID();
	
	EqualByteArrayOutputStream changeLogOS = new EqualByteArrayOutputStream();
	EqualByteArrayOutputStream buildLogOS = new EqualByteArrayOutputStream();
	
	StoreStub store = new StoreStub(null) {
		@Override
		public ProjectStatusDto createBuildOutcome(String projectName) {
			final ProjectStatusDto dto = super.createBuildOutcome(projectName);
			dto.setId(id);
			dto.setDiffId(id);
			return dto;
		}
		@Override
		public OutputStream getChangeLogOutputStream(String projectName, UUID diffId) throws StoreException {
			return changeLogOS;
		}
		@Override
		public OutputStream getBuildLogOutputStream(String projectConfig, UUID buildLogId) {
			return buildLogOS;
		}
	};
	
	public static class EqualByteArrayOutputStream extends ByteArrayOutputStream {
		@Override
		public boolean equals(Object obj) {
			return true;
		}
	};
	
	BuildDaemonInfoDto info = new BuildDaemonInfoDto();
	ProjectStatusDto previousStatus = new ProjectStatusDto();

	RevisionTokenDto rev0 = new RevisionTokenDto(0l);
	RevisionTokenDto rev1 = new RevisionTokenDto(1l);

	Throwable error;
	
	@Override
	public void setUp() throws Exception {
		checkOrder(false);
		
		builder.setStore(store);
		builder.setBuildManager(mgr);
		builder.setProjectManager(projectMgr);

		builder.init();
		
		expect(ra.getRepositoryUrl()).andReturn("http://localhost").anyTimes();
		expect(projectMgr.getPluginModificationDate((String)anyObject())).andReturn(null).anyTimes();

		info.setHostname(InetAddress.getLocalHost());
		info.setName("mock");

		previousStatus.setStatus(Status.PASS);
		previousStatus.setRevision(rev0);
		previousStatus.setBuildNumber(42);
		
		buildToolStatus.setName("a name");
		buildToolStatus.setRevision(rev0);
		buildToolStatus.setStatus(Status.PASS);
		buildToolStatus.setTagName("trunk");
		buildToolStatus.setId(id);
		buildToolStatus.setDiffId(id);
		buildToolStatus.setRepositoryUrl("http://localhost");
		buildToolStatus.setErrors(new ArrayList<BuildMessageDto>());
		buildToolStatus.setWarnings(new ArrayList<BuildMessageDto>());
		buildToolStatus.setBuildNumber(0);
	}

	public void testSetsStartDate() throws Exception {
		suppressStartDate = false;
		
		project = new ProjectConfigDto();
		project.setName("foo");

		assertNull(builder.buildStatus);
		builder.createBuildStatus(project);
		assertNotNull(builder.buildStatus.getStartDate());
	}
	
	public void testKillProjectDuringBuild() throws Throwable {
		sleepTime = 10000;
		
		project = new ProjectConfigDto();
		project.setName("foo");
		project.setWorkDir("dir");
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(mgr.getLatestStatus("foo")).andReturn(null);
		expect(ra.getLatestRevision()).andReturn(rev1);
		
		ra.createWorkingCopy(new File(project.getWorkDir()).getAbsoluteFile(), buildDetailCallback);
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev1, "trunk", Status.ERROR, "messages.build.killed", new Object[] {"a user"}, null, "http://localhost", true, null, "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Full));

		replay();

		assertFalse(builder.isBuilding());

		final Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					builder.build(info, project, buildDetailCallback);
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

	public void testInformsBuildManagerWhenProjectUpToDate() throws Exception {
		project = new ProjectConfigDto();
		project.setWorkDir("a");
		project.setName("foo");

		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);
		
		expect(mgr.getLatestStatus("foo")).andReturn(previousStatus);
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 43, rev0, "trunk", Status.UP_TO_DATE, null, null, null, "http://localhost", false, null, null, null, ProjectStatusDto.UpdateType.Full));

		checkBuild();
	}

	public void testBuildProjectPreviousNull() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");

		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);

		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.repository.changes");
		
		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev0, "trunk", Status.PASS, null, null, null, "http://localhost", true, null, "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Full));
		
		checkBuild();
		
		assertTrue(invokedBuild);
	}
	public void testBuildProjectRequestedByUser() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		project.setRequestedBy("Deborah");

		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.repository.changes");
		buildToolStatus.setRequestedBy("Deborah");
		
/*		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());
*/		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev0, "trunk", Status.PASS, null, null, null, "http://localhost", true, "Deborah", "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Full));
		
		checkBuild();
	}
	public void testCapturesErrorsAndWarnings() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		project.setRequestedBy("Deborah");

		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);

		expect(projectMgr.getBuildTool(project)).andReturn(tool);

		errorMessage = "An error occurred!";
		warningMessage = "This api is deprecated.";
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.repository.changes");
		buildToolStatus.setRequestedBy("Deborah");
		buildToolStatus.getErrors().add(new BuildMessageDto(errorMessage, null, null, null));
		buildToolStatus.getWarnings().add(new BuildMessageDto(warningMessage, null, null, null));
		
		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());
		

		final ProjectStatusDto outcome = createFakeBuildOutcome(project.getName(), 0, rev0, "trunk", Status.PASS, null, null, null, "http://localhost", true, "Deborah", "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Full);
		
		outcome.getErrors().add(new BuildMessageDto(errorMessage, null, null, null));
		outcome.getWarnings().add(new BuildMessageDto(warningMessage, null, null, null));
		
		mgr.targetCompleted(info, project, outcome);
		
		checkBuild();
	}
	public void testSupressErrors() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		project.setRequestedBy("Deborah");
		project.setSuppressErrors(true);
		
		expect(projectMgr
				.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);

		expect(projectMgr.getBuildTool(project)).andReturn(tool);

		errorMessage = "An error occurred!";
		warningMessage = "This api is deprecated.";
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.repository.changes");
		buildToolStatus.setRequestedBy("Deborah");
		buildToolStatus.getWarnings().add(new BuildMessageDto(warningMessage, null, null, null));
		
		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());
		

		final ProjectStatusDto outcome = createFakeBuildOutcome(project.getName(), 0, rev0, "trunk", Status.PASS, null, null, null, "http://localhost", true, "Deborah", "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Full);
		
		outcome.getWarnings().add(new BuildMessageDto(warningMessage, null, null, null));
		
		mgr.targetCompleted(info, project, outcome);
		
		checkBuild();
	}
	public void testGetsChangeLog() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		
		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev1);
		
		final ProjectStatusDto prevStatus = new ProjectStatusDto();
		prevStatus.setRevision(rev0);
		prevStatus.setTagName("trunk");
		prevStatus.setStatus(Status.ERROR);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(prevStatus);

		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);
		
		ra.getChangeLog(rev0, rev1, store.getChangeLogOutputStream("a name", id));
		expectLastCall().andReturn(new ChangeLogDto());

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.repository.changes");
		buildToolStatus.setTagName("trunk");
		buildToolStatus.setRevision(rev1);
		buildToolStatus.setChangeLog(new ChangeLogDto());
		
		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev1, "trunk", Status.PASS, null, null, new ChangeLogDto(), "http://localhost", true, null, "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Full));
		
		checkBuild();
	}
	public void testIncremental() throws Exception {
		updateType = UpdateType.Incremental;
		
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		project.setUpdateStrategy(ProjectConfigDto.UpdateStrategy.IncrementalAlways);
		
		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev1);
		
		final ProjectStatusDto prevStatus = new ProjectStatusDto();
		prevStatus.setRevision(rev0);
		prevStatus.setTagName("trunk");
		prevStatus.setStatus(Status.ERROR);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(prevStatus);

		ra.updateWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);
		
		ra.getChangeLog(rev0, rev1, store.getChangeLogOutputStream("a name", id));
		expectLastCall().andReturn(new ChangeLogDto());

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.repository.changes");
		buildToolStatus.setTagName("trunk");
		buildToolStatus.setRevision(rev1);
		buildToolStatus.setUpdateType(ProjectStatusDto.UpdateType.Incremental);
		buildToolStatus.setChangeLog(new ChangeLogDto());
		
		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev1, "trunk", Status.PASS, null, null, new ChangeLogDto(), "http://localhost", true, null, "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Incremental));
		
		checkBuild();
	}
	public void testChangeLogUsesLastKnownRevisionWhenPrevNull() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		
		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev1);
		
		final ProjectStatusDto prevStatus = new ProjectStatusDto();
		prevStatus.setLastKnownRevision(rev0);
		prevStatus.setRevision(null);
		prevStatus.setTagName("trunk");
		prevStatus.setStatus(Status.ERROR);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(prevStatus);

		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);
		
		ra.getChangeLog(rev0, rev1, store.getChangeLogOutputStream("a name", id));
		expectLastCall().andReturn(new ChangeLogDto());

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.repository.changes");
		buildToolStatus.setTagName("trunk");
		buildToolStatus.setRevision(rev1);
		buildToolStatus.setChangeLog(new ChangeLogDto());
		
		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());

		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev1, "trunk", Status.PASS, null, null, new ChangeLogDto(), "http://localhost", true, null, "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Full));
		
		checkBuild();
	}
	public void testBuildProjectWithTag() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		project.setRepositoryTagName("rc1");
		
		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		ra.setTagName("rc1");
		expect(ra.getLatestRevision()).andReturn(rev1);
		
		final ProjectStatusDto prevStatus = new ProjectStatusDto();
		prevStatus.setRevision(rev0);
		prevStatus.setStatus(Status.PASS);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(prevStatus);

		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);
		
		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.repository.changes");
		buildToolStatus.setTagName("rc1");
		buildToolStatus.setRevision(rev1);
		
		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev1, "rc1", Status.PASS, null, null, null, "http://localhost", false, null, "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Full));
		
		checkBuild();
	}
	public void testBuildProjectCopiesLogOnFail() throws Exception {
		project = new ProjectConfigDto();
		project.setWorkDir("a");

		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);

		tool = new BuildTool() {
			public void buildProject(ProjectConfigDto projectConfig, ProjectStatusDto buildStatus, File logFile, BuildDetailCallback buildDetailCallback) throws BuildFailedException, ConfigException {
				try {
					OutputStream os = new FileOutputStream(logFile);
					os.write("hello".getBytes());
					os.close();
				} catch (IOException e) {
				}
				throw new BuildFailedException("none", "target", 0);
			}
		};
		
		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev0, "trunk", Status.FAIL, "messages.build.failure.during.target", new String[] {"none", "target"}, null, "http://localhost", true, null, "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Full));
		
		sleepTime = 0;
		
		checkBuild();
		
		assertEquals("hello", buildLogOS.toString());
	}
	public void testBuildFailsNoTargetAvailable() throws Exception {
		project = new ProjectConfigDto();
		project.setWorkDir("a");

		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);

		tool = new BuildTool() {
			public void buildProject(ProjectConfigDto projectConfig, ProjectStatusDto buildStatus, File logFile, BuildDetailCallback buildDetailCallback) throws BuildFailedException, ConfigException {
				try {
					OutputStream os = new FileOutputStream(logFile);
					os.write("hello".getBytes());
					os.close();
				} catch (IOException e) {
				}
				throw new BuildFailedException("none", null, 0);
			}
		};
		
		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev0, "trunk", Status.FAIL, "messages.build.failure", new String[] {"none"}, null, "http://localhost", true, null, "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Full));
		
		sleepTime = 0;
		
		checkBuild();
		
		assertEquals("hello", buildLogOS.toString());
	}
	public void testBuildProjectNullOrBlankWorkDir() throws Exception {
		project = new ProjectConfigDto();
		project.setWorkDir("");

		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));
		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, null,
				null, Status.ERROR, "messages.build.null.work.dir", null, null, null, true, null, null, null, ProjectStatusDto.UpdateType.Full));
		

		checkBuild();
	}
	public void testBuildProjectCannotCreateWorkDir() throws Exception {
		createWorkingDirectoriesSuccess = false;

		project = new ProjectConfigDto();
		project.setWorkDir("hello");

		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(((ProjectStatusDto) null));

		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 0, rev0,
				"trunk", Status.ERROR, "errors.cannot.create.dir", new Object[] {new File("hello").getCanonicalPath()}, null, "http://localhost", true, null, "messages.build.reason.repository.changes", null, ProjectStatusDto.UpdateType.Full));
		
		checkBuild();
	}
	public void testBuildProjectUpToDateForceFlag() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a");
		project.setWorkDir("a");
		project.setBuildOnNoUpdates(true);

		buildToolStatus.setName("a");
		
		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);

		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.forced");
		buildToolStatus.setBuildNumber(43);
		
		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());

		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 43, rev0,
				"trunk", Status.PASS, null, null, null, "http://localhost", false, null, "messages.build.reason.forced", null, ProjectStatusDto.UpdateType.Full));

		checkBuild();
	}
	public void testBuildProjectUpToDateDependencyUpdated() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a");
		project.setWorkDir("a");
		project.setDependencies(new String[]{"dep"});

		buildToolStatus.setName("a");
		
		expect(projectMgr
		.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);
		
		final UUID depId = UUID.randomUUID(); 
		previousStatus.setDependencyIds(Collections.singletonMap("dep",
				depId));

		ProjectStatusDto currentDepStatus = new ProjectStatusDto();
		currentDepStatus.setId(UUID.randomUUID());

		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);
		expect(mgr.getLatestStatus("dep")).andReturn(currentDepStatus);
		
		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);

		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.dependency");
		buildToolStatus.setBuildReasonArgs(new Object[] {"dep"});
		buildToolStatus.setBuildNumber(43);
		
		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());

		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 43, rev0,
				"trunk", Status.PASS, null, null, null, "http://localhost", false, null, "messages.build.reason.dependency", "dep", ProjectStatusDto.UpdateType.Full));

		checkBuild();
	}
	public void testBuildProjectUpToDateDependencyNotUpdated() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a");
		project.setWorkDir("a");
		project.setDependencies(new String[]{"dep"});
		project.setLastModificationDate(new Date(0));
		
		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);
		
		final UUID depId = UUID.randomUUID(); 
		previousStatus.setDependencyIds(Collections.singletonMap("dep",
				depId));
		previousStatus.setCompletionDate(new Date(1000));
		
		ProjectStatusDto currentDepStatus = new ProjectStatusDto();
		currentDepStatus.setId(depId);

		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);
		expect(mgr.getLatestStatus("dep")).andReturn(currentDepStatus);

		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 43, rev0,
				"trunk", Status.UP_TO_DATE, null, null, null, "http://localhost", false, null, null, null, ProjectStatusDto.UpdateType.Full));
		
		checkBuild();
	}
	public void testBuildProjectWhenConfigIsNewer() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		project.setLastModificationDate(new Date(1));
		
		previousStatus.setCompletionDate(new Date(0));
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);

		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);

		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);
		
		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.project.config");
		buildToolStatus.setBuildNumber(43);
		
		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());

		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 43, rev0,
				"trunk", Status.PASS, null, null, null, "http://localhost", false, null, "messages.build.reason.project.config", null, ProjectStatusDto.UpdateType.Full));
		
		checkBuild();
	}
	public void testBuildProjectWhenPluginConfigIsNewer() throws Exception {
		reset();
		checkOrder(true);
		
		project = new ProjectConfigDto();
		project.setName("a name");
		project.setWorkDir("a");
		project.setLastModificationDate(new Date(0));
		project.setRepositoryAdaptorPluginId("a.b.c.RepoPlugin");
		project.setBuildToolPluginId("a.b.c.BuildPlugin");
		project.setBuildToolConfig(new FakeBuildToolConfig());
		
		previousStatus.setCompletionDate(new Date(1));
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);

		expect(projectMgr.getRepositoryAdaptor(project)).andReturn(ra);

		expect(ra.getTagName()).andReturn("trunk");
		expect(ra.getLatestRevision()).andReturn(rev0);
		expect(ra.getRepositoryUrl()).andReturn("http://localhost");
		
		expect(projectMgr.getPluginModificationDate("a.b.c.RepoPlugin")).andReturn(new Date(0));
		expect(projectMgr.getPluginModificationDate("a.b.c.BuildPlugin")).andReturn(new Date(2));
		
		ra.createWorkingCopy(new File("a").getAbsoluteFile(), buildDetailCallback);
		
		expect(projectMgr.getBuildTool(project)).andReturn(tool);
		
		buildToolStatus.setBuildReasonKey("messages.build.reason.plugin.config");
		buildToolStatus.setBuildReasonArgs(new String[] {"a fake plugin"});
		buildToolStatus.setBuildNumber(43);
		
		tool.buildProject(
				(ProjectConfigDto) eq(project),
				(ProjectStatusDto) eq(buildToolStatus),
				(File) eq(logFile),
				(BuildDetailCallback)notNull());

		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 43, rev0,
				"trunk", Status.PASS, null, null, null, "http://localhost", false, null, "messages.build.reason.plugin.config", "a fake plugin", ProjectStatusDto.UpdateType.Full));
		
		checkBuild();
	}
	public void testHandlesRepositoryException() throws Exception {
		project = new ProjectConfigDto();
		project.setName("a");
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);
		
		re = new RepositoryException(new SQLException(
				"table or view does not exist"));

		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 43, null,
				null,
				Status.ERROR, "messages.repository.error", new Object[]{"table or view does not exist"}, null, null, true, null, null, null, ProjectStatusDto.UpdateType.Full));

		checkBuild();
	}
	public void testTreatsGeneralExceptionAsError()
			throws Exception {
		project = new ProjectConfigDto();
		project.setName("a name");
		
		expect(mgr.getLatestStatus(project.getName())).andReturn(previousStatus);

		re = new RuntimeException("this should not have happened");

		mgr.targetCompleted(info, project, createFakeBuildOutcome(project.getName(), 43, null,
				null,
				Status.ERROR, "messages.build.uncaught.exception", new Object[]{project.getName(), re.getMessage(), ProjectBuilder.BuildPhase.Build.name()}, null, null, true, null, null, null, ProjectStatusDto.UpdateType.Full));

		checkBuild();
	}

	private void checkBuild() throws Exception {
		replay();

		try {
			builder.build(info, project, buildDetailCallback);
		} catch (Exception e) {
			verify();
			throw e;
		}
		verify();
	}

	private ProjectStatusDto createFakeBuildOutcome(String name, int buildNumber, RevisionTokenDto rev, String tagName, Status status, String key, Object[] args, ChangeLogDto changeLog, String repoUrl, boolean statusChanged, String requestedBy, String reasonKey, String reasonArg, UpdateType updateType) {
		final ProjectStatusDto dto = new ProjectStatusDto();
		dto.setBuildNumber(buildNumber);
		dto.setId(id);
		dto.setDiffId(id);
		dto.setName(name);
		dto.setRevision(rev);
		dto.setStatus(status);
		dto.setMessageKey(key);
		dto.setMessageArgs(args);
		dto.setChangeLog(changeLog);
		dto.setTagName(tagName);
		dto.setRepositoryUrl(repoUrl);
		dto.setStatusChanged(statusChanged);
		dto.setRequestedBy(requestedBy);
		dto.setErrors(new ArrayList<BuildMessageDto>());
		dto.setWarnings(new ArrayList<BuildMessageDto>());
		dto.setBuildReasonKey(reasonKey);
		dto.setUpdateType(updateType);
		if (reasonArg != null) {
			dto.setBuildReasonArgs(new Object[] {reasonArg});
		}
		
		return dto;
	}
	private class FakeBuildToolConfig extends BuildToolConfigDto {
		@Override
		public String getPluginId() {
			return "a.b.c.BuildPlugin";
		}
		@Override
		public String getPluginName() {
			return "a fake plugin";
		}
		@Override
		public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
			return null;
		}
		@Override
		public boolean equals(Object obj) {
			return true;
		}
	}
}
