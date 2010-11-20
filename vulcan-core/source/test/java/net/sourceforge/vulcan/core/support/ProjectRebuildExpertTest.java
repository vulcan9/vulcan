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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.support.ProjectRebuildExpert.FullBuildAfterIncrementalBuildRule;
import net.sourceforge.vulcan.core.support.ProjectRebuildExpert.WorkingCopyNotUsingSameTagRule;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto.UpdateStrategy;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ProjectRebuildExpertTest extends EasyMockTestCase {
	ProjectConfigDto project = new ProjectConfigDto();
	RevisionTokenDto currentRevision = new RevisionTokenDto();
	ProjectStatusDto buildStatus = new ProjectStatusDto();
	RevisionTokenDto previousRevision = new RevisionTokenDto();
	ProjectStatusDto previousStatus = new ProjectStatusDto();

	boolean isDailyFullBuildRequired = false;
	
	WorkingCopyUpdateExpert wce = new WorkingCopyUpdateExpert() {
		@Override
		boolean isDailyFullBuildRequired(ProjectConfigDto currentTarget, ProjectStatusDto previousStatus) {
			return isDailyFullBuildRequired;
		}
	};

	ProjectRebuildExpertStub expert = new ProjectRebuildExpertStub();
	
	ProjectManager projectManager;
	BuildManager buildManager;
	RepositoryAdaptor repositoryAdaptor;
	
	Date day1;
	Date day2;
	Date now;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	
		projectManager = createStrictMock(ProjectManager.class);
		buildManager = createStrictMock(BuildManager.class);
		repositoryAdaptor = createStrictMock(RepositoryAdaptor.class);
		
		expert.setProjectManager(projectManager);
		expert.setBuildManager(buildManager);
		expert.setWorkingCopyUpdateExpert(wce);
		
		DateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");
		day1 = fmt.parse("1/1/2000");
		day2 = fmt.parse("2/1/2000");
		
		now = day2;
		
		project.setName("A");
		project.setWorkDir("workdir");
		previousStatus.setWorkDir("workdir");
		previousStatus.setCompletionDate(new Date(0));
	}

	public void testBuildProjectWhenPluginConfigIsNewer() throws Exception {
		project.setName("a name");
		project.setWorkDir("a");
		project.setLastModificationDate(new Date(0));
		project.setRepositoryAdaptorPluginId("a.b.c.RepoPlugin");
		project.setBuildToolPluginId("a.b.c.BuildPlugin");
		project.setBuildToolConfig(new FakeBuildToolConfig());
		
		expect(projectManager.getPluginModificationDate("a.b.c.RepoPlugin")).andReturn(new Date(0));
		expect(projectManager.getPluginModificationDate("a.b.c.BuildPlugin")).andReturn(day1);
		
		replay();
		
		assertTrue("shouldBuild", expert.shouldBuild(project, previousStatus));
		
		assertEquals("messages.build.reason.plugin.config", expert.getMessageKey());
		
		assertEquals(Arrays.asList("a fake plugin"), Arrays.asList(expert.getMessageArgs()));
		
		verify();
	}

	public void testBuildProjectUpToDateForceFlag() throws Exception {
		project.setBuildOnNoUpdates(true);

		replay();
		
		assertTrue("shouldBuild", expert.shouldBuild(project, previousStatus));
		
		assertEquals("messages.build.reason.forced", expert.getMessageKey());
		assertEquals(Arrays.asList(), Arrays.asList(expert.getMessageArgs()));
		
		verify();
	}
	
	public void testBuildProjectUpToDateDependencyUpdated() throws Exception {
		project.setDependencies(new String[] {"dep"});
		
		final UUID depId = UUID.randomUUID(); 
		previousStatus.setDependencyIds(Collections.singletonMap("dep", depId));
		previousStatus.setCompletionDate(new Date(0));
		
		ProjectStatusDto currentDepStatus = new ProjectStatusDto();
		currentDepStatus.setId(UUID.randomUUID());

		expect(buildManager.getLatestStatus("dep")).andReturn(currentDepStatus);
		
		replay();
		
		assertTrue("isSatisfiedBy", expert.createDependencyTimestampChangeRule().isSatisfiedBy(project, previousStatus));
		
		assertEquals("messages.build.reason.dependency", expert.getMessageKey());
		assertEquals(Arrays.asList("dep"), Arrays.asList(expert.getMessageArgs()));
		
		verify();
	}
	
	public void testBuildProjectUpToDateDependencyNotUpdated() throws Exception {
		project.setDependencies(new String[] {"dep"});
		
		final UUID depId = UUID.randomUUID(); 
		previousStatus.setDependencyIds(Collections.singletonMap("dep", depId));
		previousStatus.setCompletionDate(new Date(1000));
		
		ProjectStatusDto currentDepStatus = new ProjectStatusDto();
		currentDepStatus.setId(depId);

		expect(buildManager.getLatestStatus("dep")).andReturn(currentDepStatus);
		
		replay();
		
		assertFalse("shouldBuild", expert.createDependencyTimestampChangeRule().isSatisfiedBy(project, previousStatus));
		
		verify();
	}
	
	public void testBuildProjectWhenConfigIsNewer() throws Exception {
		project.setLastModificationDate(new Date(1));
		
		previousStatus.setCompletionDate(new Date(0));
		
		assertTrue("shouldBuild", expert.shouldBuild(project, previousStatus));
		
		assertEquals("messages.build.reason.project.config", expert.getMessageKey());
		
		assertEquals(Collections.emptyList(), Arrays.asList(expert.getMessageArgs()));
	}
	
	public void testDoesNotRebuildWhenPreviousWasFull() throws Exception {
		final FullBuildAfterIncrementalBuildRule rule = expert.createFullBuildAfterIncrementalBuildRule();
		
		previousStatus.setUpdateType(UpdateType.Full);
		project.setUpdateStrategy(UpdateStrategy.CleanAlways);
		
		assertFalse(rule.isSatisfiedBy(project, previousStatus));
	}

	public void testDoesNotRebuildOnIncrementalBuildRequested() throws Exception {
		final FullBuildAfterIncrementalBuildRule rule = expert.createFullBuildAfterIncrementalBuildRule();
		
		previousStatus.setUpdateType(UpdateType.Incremental);
		project.setUpdateStrategy(UpdateStrategy.IncrementalAlways);
		
		assertFalse(rule.isSatisfiedBy(project, previousStatus));
	}
	
	public void testRebuildOnFullBuildRequestedPreviousIncremental() throws Exception {
		final FullBuildAfterIncrementalBuildRule rule = expert.createFullBuildAfterIncrementalBuildRule();
		
		previousStatus.setUpdateType(UpdateType.Incremental);
		project.setUpdateStrategy(UpdateStrategy.CleanAlways);
		
		assertTrue(rule.isSatisfiedBy(project, previousStatus));
	}
	
	public void testRebuildOnDailyFullBuildRequestedPreviousIncremental() throws Exception {
		final FullBuildAfterIncrementalBuildRule rule = expert.createFullBuildAfterIncrementalBuildRule();
		
		previousStatus.setUpdateType(UpdateType.Incremental);
		project.setUpdateStrategy(UpdateStrategy.CleanDaily);
		
		isDailyFullBuildRequired = true;
		
		assertTrue(rule.isSatisfiedBy(project, previousStatus));
	}
	
	public void testDoesNotRebuildOnDailyFullBuildRequestedPreviousIncrementalSameDay() throws Exception {
		final FullBuildAfterIncrementalBuildRule rule = expert.createFullBuildAfterIncrementalBuildRule();
		
		previousStatus.setUpdateType(UpdateType.Incremental);
		project.setUpdateStrategy(UpdateStrategy.CleanDaily);
		
		isDailyFullBuildRequired = false;
		
		assertFalse(rule.isSatisfiedBy(project, previousStatus));
	}
	
	public void testDoesNotRebuildOnDailyFullBuildRequestedPreviousIncrementalPreviousWasFull() throws Exception {
		final FullBuildAfterIncrementalBuildRule rule = expert.createFullBuildAfterIncrementalBuildRule();
		
		previousStatus.setUpdateType(UpdateType.Full);
		project.setUpdateStrategy(UpdateStrategy.CleanDaily);
		
		isDailyFullBuildRequired = true;
		
		assertFalse(rule.isSatisfiedBy(project, previousStatus));
	}
	
	public void testDoesNotRebuildOnSameTag() throws Exception {
		project.setRepositoryTagName("default");
		previousStatus.setTagName("default");
		
		final WorkingCopyNotUsingSameTagRule rule = expert.createWorkingCopyNotUsingSameTagRule();
		
		replay();
		
		assertFalse("rule.isSatisfiedBy", rule.isSatisfiedBy(project, previousStatus));
		
		verify();
	}
	
	public void testRebuildsOnDifferentTag() throws Exception {
		project.setRepositoryTagName("default");
		previousStatus.setTagName("rc1");
		
		final WorkingCopyNotUsingSameTagRule rule = expert.createWorkingCopyNotUsingSameTagRule();
		
		replay();
		
		assertTrue("rule.isSatisfiedBy", rule.isSatisfiedBy(project, previousStatus));
		assertEquals("messages.build.reason.different.tag", expert.getMessageKey());
		assertEquals(Arrays.asList("workdir", "default", "rc1"), Arrays.asList(expert.getMessageArgs()));
		
		verify();
	}
	
	public void testGetsDefaultTagNameFromRepositoryAdaptor() throws Exception {
		project.setRepositoryTagName(null);
		previousStatus.setTagName("default");
		
		final WorkingCopyNotUsingSameTagRule rule = expert.createWorkingCopyNotUsingSameTagRule();
		
		expect(projectManager.getRepositoryAdaptor(project)).andReturn(repositoryAdaptor);
		expect(repositoryAdaptor.getTagName()).andReturn("default");
		
		replay();
		
		assertFalse("rule.isSatisfiedBy", rule.isSatisfiedBy(project, previousStatus));
		
		verify();
	}
	
	public void testGetsLastBuildFromSameWorkDirReturnsNull() throws Exception {
		project.setRepositoryTagName("default");
		previousStatus.setWorkDir("other");
		previousStatus.setTagName("rc1");
		
		final WorkingCopyNotUsingSameTagRule rule = expert.createWorkingCopyNotUsingSameTagRule();
		
		expect(buildManager.getMostRecentBuildByWorkDir(project.getName(), project.getWorkDir())).andReturn(null);
				
		replay();
		
		assertFalse("rule.isSatisfiedBy", rule.isSatisfiedBy(project, previousStatus));
		
		verify();
	}
	
	public void testGetsLastBuildFromSameWorkDirUsesDifferentTag() throws Exception {
		project.setRepositoryTagName("default");
		
		ProjectStatusDto fromSameWorkDir = (ProjectStatusDto) previousStatus.copy();
		
		previousStatus.setWorkDir("other");
		previousStatus.setTagName("default");
		
		fromSameWorkDir.setTagName("rc1");
		
		final WorkingCopyNotUsingSameTagRule rule = expert.createWorkingCopyNotUsingSameTagRule();
		
		expect(buildManager.getMostRecentBuildByWorkDir(project.getName(), project.getWorkDir())).andReturn(fromSameWorkDir);
				
		replay();
		
		assertTrue("rule.isSatisfiedBy", rule.isSatisfiedBy(project, previousStatus));
		
		verify();
	}
	
	class ProjectRebuildExpertStub extends ProjectRebuildExpert {
		FullBuildAfterIncrementalBuildRule createFullBuildAfterIncrementalBuildRule() {
			return new FullBuildAfterIncrementalBuildRule();
		}
		DependencyTimestampChangeRule createDependencyTimestampChangeRule() {
			return new DependencyTimestampChangeRule();
		}
		WorkingCopyNotUsingSameTagRule createWorkingCopyNotUsingSameTagRule() {
			return new WorkingCopyNotUsingSameTagRule();
		}
	}
	
	class FakeBuildToolConfig extends BuildToolConfigDto {
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
