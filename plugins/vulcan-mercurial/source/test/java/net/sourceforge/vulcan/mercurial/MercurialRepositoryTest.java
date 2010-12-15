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
package net.sourceforge.vulcan.mercurial;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.core.support.FileSystem;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.RepositoryException;

import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.filefilter.IOFileFilter;

public class MercurialRepositoryTest extends EasyMockTestCase {
	MercurialConfig globals;
	ProjectConfigDto project;
	MercurialRepository repo;
	FileSystem fileSystem;
	
	Invoker invoker;
	BuildDetailCallback buildDetail;
	
	File workDir;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		globals = new MercurialConfig();
		
		MercurialProjectConfig settings = new MercurialProjectConfig();
		settings.setRemoteRepositoryUrl("http://localhost/hg/repo1");
		
		project = new ProjectConfigDto();
		project.setWorkDir("work_dir");
		
		workDir = new File(project.getWorkDir());
		
		project.setRepositoryAdaptorConfig(settings);
		invoker = createMock(Invoker.class);
		buildDetail = createMock(BuildDetailCallback.class);
		
		repo = new MercurialRepository(project, globals) {
			@Override
			protected Invoker createInvoker() {
				return invoker;
			}
		};
		
		fileSystem = createMock(FileSystem.class);
		repo.setFileSystem(fileSystem);
		
		expect(fileSystem.directoryExists(workDir)).andReturn(true).anyTimes();
	}
	
	public void testCreateInvokerSetsExecutable() throws Exception {
		globals.setExecutable("custom-hg.exe");
		
		ProcessInvoker invoker = (ProcessInvoker) new MercurialRepository(project, globals).createInvoker();
		
		assertEquals("custom-hg.exe", invoker.getExecutable());
	}
	
	public void testGetSelectedBranchDefaultWhenBlank() throws Exception {
		repo.setTagOrBranch("");
		
		assertEquals("default", repo.getSelectedBranch());
	}
	
	public void testGetSelectedBranchOther() throws Exception {
		repo.setTagOrBranch("other");
		
		assertEquals("other", repo.getSelectedBranch());
	}
	
	public void testHasIncomingChangesFromRemote() throws Exception {
		expectIncomingCommand();
		
		returnSuccess();
		
		replay();
		
		assertEquals("hasIncomingChangesFromRemote", true, repo.hasIncomingChangesFromRemote());
		
		verify();
	}

	public void testHasIncomingChangesFromRemoteUsesTagNoChanges() throws Exception {
		repo.getSettings().setBranch("v2");
		
		expectIncomingCommand();
		
		returnFailure();
		
		replay();
		
		assertEquals("hasIncomingChangesFromRemote", false, repo.hasIncomingChangesFromRemote());
		
		verify();
	}

	public void testHasIncomingChangesFromRemoteFalseOnNoRemoteRepo() throws Exception {
		repo.getSettings().setRemoteRepositoryUrl("");
		
		replay();
		
		assertEquals("", repo.getSettings().getRemoteRepositoryUrl());
		assertEquals("hasIncomingChangesFromRemote", false, repo.hasIncomingChangesFromRemote());
		
		verify();
	}

	public void testGetLatestRevision() throws Exception {
		expectLogCommand();
		
		returnSuccessWithOutput("8:9bd7475fd513\n");
		
		replay();
		
		RevisionTokenDto rev = repo.getLatestRevision(null);
		
		verify();
		
		assertNotNull("getLatestRevision", rev);
		assertEquals(8, rev.getRevision().longValue());
		assertEquals("8:9bd7475fd513", rev.getLabel());
	}
	
	public void testHasIncomingChangesComparesRevisions() throws Exception {
		expectLogCommand();
		
		returnSuccessWithOutput("8:9bd7475fd513\n");
		
		replay();
		
		ProjectStatusDto previous = new ProjectStatusDto();
		previous.setRevision(new RevisionTokenDto(7L, "7:different"));
		
		assertEquals("hasIncomingChanges", true, repo.hasIncomingChanges(previous));
		
		verify();
	}
	
	public void testHasIncomingChangesGetsIncoming() throws Exception {
		expectLogCommand();
		
		returnSuccessWithOutput("8:9bd7475fd513\n");
		
		expectIncomingCommand();
		
		returnSuccess();
		
		replay();
		
		ProjectStatusDto previous = new ProjectStatusDto();
		previous.setRevision(new RevisionTokenDto(8L, "8:9bd7475fd513"));
		
		assertEquals("hasIncomingChanges", true, repo.hasIncomingChanges(previous));
		
		verify();
	}
	
	public void testHasIncomingChangesFalseOnNoRemoteRepository() throws Exception {
		repo.getSettings().setRemoteRepositoryUrl("");
		
		expectLogCommand();
		
		returnSuccessWithOutput("8:9bd7475fd513\n");
		
		replay();
		
		ProjectStatusDto previous = new ProjectStatusDto();
		previous.setRevision(new RevisionTokenDto(8L, "8:9bd7475fd513"));
		
		assertEquals("hasIncomingChanges", false, repo.hasIncomingChanges(previous));
		
		verify();
	}

	public void testIsWorkingCopy() throws Exception {
		expectSummaryCommand();
		
		returnSuccess();
		
		replay();
		
		assertEquals("isWorkingCopy", true, repo.isWorkingCopy());
		
		verify();
	}

	public void testIsWorkingCopyFalseOnMissingDir() throws Exception {
		reset();
		
		expect(fileSystem.directoryExists(workDir)).andReturn(false);
		
		replay();
		
		assertEquals("isWorkingCopy", false, repo.isWorkingCopy());
		
		verify();
	}

	public void testIsWorkingCopyFalseOnCommandFailure() throws Exception {
		expectSummaryCommand();
		
		expectLastCall().andThrow(new IOException("not a working copy"));
		
		replay();
		
		assertEquals("isWorkingCopy", false, repo.isWorkingCopy());
		
		verify();
	}
	
	public void testPrepareMakesClone() throws Exception {
		expectSummaryCommand();
		
		returnFailure();
	
		expectCloneCommand();
		
		returnSuccess();
		
		replay();
		
		repo.prepareRepository(buildDetail);
		
		verify();
	}
	
	public void testPrepareMakesCloneWithFlags() throws Exception {
		repo.getSettings().setUncompressed(true);
		repo.getSettings().setCloneWithPullProtocol(true);
		
		testPrepareMakesClone();
	}
	
	public void testPrepareThrowsOnMissingLocalRepoAndNoRemoteRepo() throws Exception {
		repo.getSettings().setRemoteRepositoryUrl("");
		
		expectSummaryCommand();
		
		returnFailure();
	
		replay();
		
		try {
			repo.prepareRepository(buildDetail);
			fail("expected RepositoryException");
		} catch (RepositoryException e) {
			
		}
		
		verify();
	}
	
	public void testPrepareDoesNotPullOnNoRemoteRepo() throws Exception {
		repo.getSettings().setRemoteRepositoryUrl("");
		
		expectSummaryCommand();
		
		returnSuccess();
	
		replay();
		
		repo.prepareRepository(buildDetail);
		
		verify();
	}

	public void testPreparePullsWhenRepositoryPresent() throws Exception {
		expectSummaryCommand();
		
		returnSuccess();
	
		expectPullCommand();
		
		returnSuccess();
		
		replay();
		
		repo.prepareRepository(buildDetail);
		
		verify();
	}
	
	public void testUpdateWorkingCopy() throws Exception {
		expectUpdateCommand();
		
		returnSuccess();
		
		replay();
		
		repo.updateWorkingCopy(buildDetail);
		
		verify();
	}
	
	public void testCreatePristineWorkingCopyUsingPurge() throws Exception {
		globals.setPurgeEnabled(true);
		
		expectUpdateCommandWithBuildDetail("--clean");
		
		returnSuccess();
		
		expectPurgeAllCommand();
		
		returnSuccess();
		
		replay();
		
		repo.createPristineWorkingCopy(buildDetail);
		
		verify();
	}
	
	public void testCreatePristineWorkingCopy() throws Exception {
		globals.setPurgeEnabled(false);
		
		buildDetail.setDetailMessage("hg.activity.remove.working.copy", null);
		
		invoker.invoke("update", workDir, "--clean", "null");
		returnSuccess();
		
		buildDetail.setDetailMessage("hg.activity.purge", null);
		fileSystem.cleanDirectory(eq(workDir), (IOFileFilter) notNull());
		
		expectUpdateCommandWithBuildDetail();
		
		returnSuccess();

		replay();
		
		repo.createPristineWorkingCopy(buildDetail);
		
		verify();
	}
	
	public void testGetChangeLogEmpty() throws Exception {
		invoker.invoke("log", workDir, "--style", "xml", "-r", "124:456");
		returnSuccessWithOutput("<xml/>");
		
		replay();
		
		final ChangeLogDto changeLog = repo.getChangeLog(new RevisionTokenDto(123L, "123:9bd7475fd513"), new RevisionTokenDto(456L, "456:9bd7475fd513"), null);
		
		verify();
		
		assertNotNull("return value", changeLog);
		assertNotNull("change sets", changeLog.getChangeSets());
	}
	
	public void testGetTagsAndBranchesOneBranch() throws Exception {
		invoker.invoke("branches", workDir);
		returnSuccessWithOutput("first draft                 1234:9bd7475fd513\n");
		invoker.invoke("tags", workDir);
		returnSuccessWithOutput("");
		invoker.invoke("heads", workDir, "--quiet", "--topo");
		returnSuccessWithOutput("");
		
		replay();
		
		final List<RepositoryTagDto> actual = repo.getAvailableTagsAndBranches();
		
		verify();
		
		final List<RepositoryTagDto> expected = Arrays.asList(
				new RepositoryTagDto("first draft", "first draft"));
		
		assertEquals(expected, actual);
	}
	
	public void testGetTagsAndBranchesShowsAnonymousHeads() throws Exception {
		invoker.invoke("branches", workDir);
		returnSuccessWithOutput("");
		invoker.invoke("tags", workDir);
		returnSuccessWithOutput("");
		invoker.invoke("heads", workDir, "--quiet", "--topo");
		returnSuccessWithOutput("5679:315df5747db");
		
		replay();
		
		final List<RepositoryTagDto> actual = repo.getAvailableTagsAndBranches();
		
		verify();
		
		final List<RepositoryTagDto> expected = Arrays.asList(
				new RepositoryTagDto("5679:315df5747db", "5679:315df5747db"));
		
		assertEquals(expected, actual);
	}
	
	public void testGetTagsAndBranchesExcludesNamedHeads() throws Exception {
		invoker.invoke("branches", workDir);
		returnSuccessWithOutput("b1             1234:9bd7475fd513");
		invoker.invoke("tags", workDir);
		returnSuccessWithOutput("t1             5679:315df5747db\nt2             8765:2a56f5747db");
		invoker.invoke("heads", workDir, "--quiet", "--topo");
		returnSuccessWithOutput("1234:9bd7475fd513\n5679:315df5747db");
		
		replay();
		
		final List<RepositoryTagDto> actual = repo.getAvailableTagsAndBranches();
		
		verify();
		
		final List<RepositoryTagDto> expected = Arrays.asList(
				new RepositoryTagDto("b1", "b1"),
				new RepositoryTagDto("t1", "t1"),
				new RepositoryTagDto("t2", "t2")
				);
		
		assertEquals(expected, actual);
	}
	
	public void testInvokeIncludesRemoteOptions() throws Exception {
		repo.getSettings().setSshCommand("/opt/bin/ssh2");
		repo.getSettings().setRemoteCommand("/opt/hg/bin/hg");
		
		invoker.invoke("incoming", workDir, "--ssh", "/opt/bin/ssh2", "--remotecmd", "/opt/hg/bin/hg", "some", "extra", "args");
		returnSuccess();
		
		replay();
		
		repo.tryInvoke(MercurialRepository.Command.incoming, "some", "extra", "args");
		
		verify();
	}
	
	public void testInvokeCreatesWorkDirWhenMissing() throws Exception {
		reset();
		
		expect(fileSystem.directoryExists(workDir)).andReturn(false);
		fileSystem.createDirectory(workDir);
		
		invoker.invoke("incoming", workDir);
		returnSuccess();
		
		replay();
		
		repo.tryInvoke(MercurialRepository.Command.incoming);
		
		verify();
	}
	
	public void testInvokeCreatesWorkDirWhenMissingWrapsIOException() throws Exception {
		final IOException ioe = new IOException();
		
		reset();
		
		expect(fileSystem.directoryExists(workDir)).andReturn(false);
		fileSystem.createDirectory(workDir);
		expectLastCall().andThrow(ioe);
		
		replay();
		
		try {
			repo.tryInvoke(MercurialRepository.Command.incoming);
			fail("expected RepositoryException");
		} catch (RepositoryException e) {
			assertSame("e.getCause()", ioe, e.getCause());
		}
		
		verify();
	}
	
	public void testInvokeWrapsExecuteException() throws Exception {	
		final ExecuteException ee = new ExecuteException("oops", 2);
		expect(invoker.invoke("incoming", workDir)).andThrow(ee);
		expect(invoker.getErrorText()).andReturn("unrecognized option");
		expect(invoker.getExitCode()).andReturn(2);
		
		replay();
		
		try {
			repo.tryInvoke(MercurialRepository.Command.incoming);
			fail("expected RepositoryException");
		} catch (RepositoryException e) {
			assertSame("e.getCause()", ee, e.getCause());
		}
		
		verify();
	}
	
	private void expectPurgeAllCommand() throws IOException {
		buildDetail.setDetailMessage("hg.activity.purge", null);
		invoker.invoke("purge", workDir, "--all", "--config", "extensions.purge=");
	}

	private void expectUpdateCommandWithBuildDetail(String... extraArgs) throws IOException {
		buildDetail.setDetailMessage("hg.activity.update", null);
		expectUpdateCommand(extraArgs);
	}
	
	private void expectUpdateCommand(String... extraArgs) throws IOException {
		List<String> args = new ArrayList<String>();
		args.addAll(Arrays.asList(extraArgs));
		args.add(repo.getSelectedBranch());
		
		invoker.invoke("update", workDir, args.toArray(new String[args.size()]));
	}

	private void expectCloneCommand() throws IOException {
		buildDetail.setDetailMessage("hg.activity.clone", null);
		
		List<String> args = new ArrayList<String>();
		if (repo.getSettings().isCloneWithPullProtocol()) {
			args.add("--pull");
		}
		if (repo.getSettings().isUncompressed()) {
			args.add("--uncompressed");
		}
		args.add("--noupdate");
		args.add(repo.getSettings().getRemoteRepositoryUrl());
		args.add(".");
		
		invoker.invoke("clone", workDir, args.toArray(new String[args.size()]));
	}
	
	private void expectPullCommand() throws IOException {
		buildDetail.setDetailMessage("hg.activity.pull", null);
		invoker.invoke("pull", workDir, repo.getSettings().getRemoteRepositoryUrl());
	}

	private void expectSummaryCommand() throws IOException {
		invoker.invoke("summary", workDir, "--quiet");
	}

	private void expectLogCommand() throws IOException {
		invoker.invoke("log", workDir, new String[] {"--rev", repo.getSelectedBranch(), "--limit", "1", "--quiet"});
	}

	private void expectIncomingCommand() throws IOException {
		invoker.invoke("incoming", workDir, new String[] {"--rev", repo.getSelectedBranch(), "--limit", "1", "--quiet", repo.getSettings().getRemoteRepositoryUrl()});
	}

	private void returnSuccess() {
		returnSuccessWithOutput("");
	}

	private void returnSuccessWithOutput(String output) {
		expectLastCall().andReturn(new InvocationResult(output, "", true));
	}
	
	private void returnFailure() {
		expectLastCall().andReturn(new InvocationResult("", "", false));
	}
}
