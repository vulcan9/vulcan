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

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;

public class MercurialRepositoryTest extends EasyMockTestCase {

	MercurialProjectConfig settings;
	ProjectConfigDto project;
	MercurialRepository repo;
	
	Invoker invoker;
	BuildDetailCallback buildDetail;
	
	boolean directoryPresent = true;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		settings = new MercurialProjectConfig();
		settings.setRemoteRepositoryUrl("http://localhost/hg/repo1");
		
		project = new ProjectConfigDto();
		project.setWorkDir("work_dir");
		
		project.setRepositoryAdaptorConfig(settings);
		invoker = createMock(Invoker.class);
		buildDetail = createMock(BuildDetailCallback.class);
		
		repo = new MercurialRepository(project) {
			@Override
			protected Invoker createInvoker() {
				return invoker;
			}
			
			@Override
			protected boolean isDirectoryPresent(File absolutePath) {
				return directoryPresent;
			}
		};
	}
	
	public void testGetSelectedBranchDefaultWhenBlank() throws Exception {
		settings.setBranch("");
		
		assertEquals("default", repo.getSelectedBranch());
	}
	
	public void testGetSelectedBranchOther() throws Exception {
		settings.setBranch("other");
		
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
		settings.setBranch("v2");
		
		expectIncomingCommand();
		
		returnFailure();
		
		replay();
		
		assertEquals("hasIncomingChangesFromRemote", false, repo.hasIncomingChangesFromRemote());
		
		verify();
	}

	public void testHasIncomingChangesFromRemoteFalseOnNoRemoteRepo() throws Exception {
		settings.setRemoteRepositoryUrl("");
				
		replay();
		
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
	
	public void testHasIncomingChangesFalse() throws Exception {
		settings.setRemoteRepositoryUrl("");
		
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

	public void testIsWorkingCopyFalse() throws Exception {
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
	
	public void testPreparePullsWhenRepositoryPresent() throws Exception {
		expectSummaryCommand();
		
		returnSuccess();
	
		expectPullCommand();
		
		returnSuccess();
		
		replay();
		
		repo.prepareRepository(buildDetail);
		
		verify();
	}
	
	private void expectCloneCommand() throws IOException {
		buildDetail.setDetailMessage("hg.activity.clone", null);
		invoker.invoke("clone", new File(project.getWorkDir()), "--noupdate", settings.getRemoteRepositoryUrl(), ".");
	}
	
	private void expectPullCommand() throws IOException {
		buildDetail.setDetailMessage("hg.activity.pull", null);
		invoker.invoke("pull", new File(project.getWorkDir()), settings.getRemoteRepositoryUrl());
	}

	private void expectSummaryCommand() throws IOException {
		invoker.invoke("summary", new File(project.getWorkDir()), "--quiet");
	}

	private void expectLogCommand() throws IOException {
		invoker.invoke("log", new File(project.getWorkDir()), new String[] {"--rev", repo.getSelectedBranch(), "--limit", "1", "--quiet"});
	}

	private void expectIncomingCommand() throws IOException {
		invoker.invoke("incoming", new File(project.getWorkDir()), new String[] {"--rev", repo.getSelectedBranch(), "--limit", "1", "--quiet", settings.getRemoteRepositoryUrl()});
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
