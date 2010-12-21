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

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.DependencyBuildPolicy;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.core.WorkingCopyUpdateStrategy;
import net.sourceforge.vulcan.dto.LockDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto.UpdateStrategy;
import net.sourceforge.vulcan.exception.ProjectsLockedException;

import org.easymock.EasyMock;

public class DependencyGroupBuilderTest extends EasyMockTestCase {
	ProjectManager projectMgr = createStrictMock(ProjectManager.class);
	BuildManager buildManager = createStrictMock(BuildManager.class);
	
	ProjectConfigDto[] projects = {new ProjectConfigDto(), new ProjectConfigDto()};
	
	@Override
	public void setUp() throws Exception {
		checkOrder(false);
		expect(buildManager.getLatestStatus((String)notNull())).andReturn(null).anyTimes();
		
		projects[0].setName("a");
		projects[0].setAutoIncludeDependencies(true);
		projects[0].setUpdateStrategy(UpdateStrategy.CleanDaily);
		projects[1].setName("b");
	}
	
	public void testAddsProjects() throws Exception {
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[0]);
		dg.addTarget(projects[1]);
		
		check(dg, DependencyBuildPolicy.AS_NEEDED, true, false, WorkingCopyUpdateStrategy.Default);
	}
	
	public void testThrowsOnLockedProject() throws Exception {
		projects[0].addLock(new LockDto());
		try {
			check(null, DependencyBuildPolicy.AS_NEEDED, true, false, WorkingCopyUpdateStrategy.Default);
			fail("Expected exception");
		} catch (ProjectsLockedException e) {
			assertEquals(1, e.getLockedProjectNames().size());
			assertEquals(projects[0].getName(), e.getLockedProjectNames().get(0));
		}
	}
	
	public void testThrowsOnLockedProjectIncludesAllLocked() throws Exception {
		projects[0].addLock(new LockDto());
		projects[1].addLock(new LockDto());
		try {
			check(null, DependencyBuildPolicy.AS_NEEDED, true, false, WorkingCopyUpdateStrategy.Default);
			fail("Expected exception");
		} catch (ProjectsLockedException e) {
			assertEquals(2, e.getLockedProjectNames().size());
			assertEquals(projects[0].getName(), e.getLockedProjectNames().get(0));
			assertEquals(projects[1].getName(), e.getLockedProjectNames().get(1));
		}
	}
	
	public void testThrowsOnLockedProjectDependency() throws Exception {
		EasyMock.expect(projectMgr.getProjectConfig("b")).andReturn(projects[1]);
		
		projects[0].setDependencies(new String[] {"b"});
		projects[1].addLock(new LockDto());
		
		try {
			check(null, DependencyBuildPolicy.FORCE, false, false, WorkingCopyUpdateStrategy.Default);
			fail("Expected exception");
		} catch (ProjectsLockedException e) {
			assertEquals(1, e.getLockedProjectNames().size());
			assertEquals(projects[1].getName(), e.getLockedProjectNames().get(0));
		}
	}
	
	public void testSkipsOnLockedProjectDependencyAsNeeded() throws Exception {
		EasyMock.expect(projectMgr.getProjectConfig("b")).andReturn(projects[1]);
		
		projects[0].setDependencies(new String[] {"b"});
		projects[1].addLock(new LockDto());
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[0]);
		
		check(dg, DependencyBuildPolicy.AS_NEEDED, false, false, WorkingCopyUpdateStrategy.Default);
	}
	
	public void testOverrideOptions() throws Exception {
		final DependencyGroup dg = new DependencyGroupImpl();
		
		ProjectConfigDto copy = (ProjectConfigDto) projects[0].copy();
		copy.setBuildOnDependencyFailure(true);
		copy.setBuildOnNoUpdates(true);
		final ProjectStatusDto buildStatus = new ProjectStatusDto();
		buildStatus.setBuildReasonArgs(new String[0]);
		buildStatus.setBuildReasonKey("messages.build.reason.forced");
		dg.addTarget(copy, buildStatus);
		
		check(dg, DependencyBuildPolicy.AS_NEEDED, false, true, WorkingCopyUpdateStrategy.Default);
	}
	
	public void testOverrideUpdateStrategyIncremental() throws Exception {
		final DependencyGroup dg = new DependencyGroupImpl();
		
		ProjectConfigDto copy = (ProjectConfigDto) projects[0].copy();
		copy.setUpdateStrategy(ProjectConfigDto.UpdateStrategy.IncrementalAlways);
		
		dg.addTarget(copy);
		
		check(dg, DependencyBuildPolicy.AS_NEEDED, false, false, WorkingCopyUpdateStrategy.Incremental);
	}
	
	public void testOverrideUpdateStrategyClean() throws Exception {
		final DependencyGroup dg = new DependencyGroupImpl();
		
		ProjectConfigDto copy = (ProjectConfigDto) projects[0].copy();
		copy.setUpdateStrategy(ProjectConfigDto.UpdateStrategy.CleanAlways);
		
		dg.addTarget(copy);
		
		check(dg, DependencyBuildPolicy.AS_NEEDED, false, false, WorkingCopyUpdateStrategy.Full);
	}
	
	public void testSkipsDependenciesWhenAutoIncludeFalse() throws Exception {
		projects[0].setDependencies(new String[] {"b"});
		projects[0].setAutoIncludeDependencies(false);
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[0]);
		
		check(dg, DependencyBuildPolicy.AS_NEEDED, false, false, WorkingCopyUpdateStrategy.Default);
	}

	public void testAddsDependenciesWhenAutoSet() throws Exception {
		EasyMock.expect(projectMgr.getProjectConfig("b")).andReturn(projects[1]);
		
		projects[0].setDependencies(new String[] {"b"});
		projects[0].setAutoIncludeDependencies(true);
		
		final DependencyGroup dg = new DependencyGroupImpl();
		
		dg.addTarget(projects[0]);
		dg.addTarget(projects[1]);
		
		check(dg, DependencyBuildPolicy.AS_NEEDED, false, false, WorkingCopyUpdateStrategy.Default);
	}
	
	public void testForceBuildDependenciesWhenAutoSet() throws Exception {
		EasyMock.expect(projectMgr.getProjectConfig("b")).andReturn(projects[1]);
		
		projects[0].setDependencies(new String[] {"b"});
		projects[0].setAutoIncludeDependencies(true);
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[0]);
		
		ProjectConfigDto copy = (ProjectConfigDto) projects[1].copy();
		copy.setBuildOnDependencyFailure(true);
		copy.setBuildOnNoUpdates(true);
		
		final ProjectStatusDto buildStatus = new ProjectStatusDto();
		buildStatus.setBuildReasonArgs(new String[0]);
		buildStatus.setBuildReasonKey("messages.build.reason.forced");
		dg.addTarget(copy, buildStatus);

		
		check(dg, DependencyBuildPolicy.FORCE, false, false, WorkingCopyUpdateStrategy.Default);
	}

	public void testForceNotAppliedWhenAutoNotSet() throws Exception {
		projects[0].setDependencies(new String[] {"b"});
		projects[0].setAutoIncludeDependencies(false);
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[0]);
		
		check(dg, DependencyBuildPolicy.FORCE, false, false, WorkingCopyUpdateStrategy.Default);
	}

	public void testSkipsDependenciesWhenPolicyNone() throws Exception {
		projects[0].setDependencies(new String[] {"b"});
		projects[0].setAutoIncludeDependencies(true);
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[0]);
		
		check(dg, DependencyBuildPolicy.NONE, false, false, WorkingCopyUpdateStrategy.Default);
	}

	public void testAutoIncludeLevel2() throws Exception {
		projects = new ProjectConfigDto[] {new ProjectConfigDto(), new ProjectConfigDto(),
				new ProjectConfigDto()};
		
		projects[0].setName("a");
		projects[0].setAutoIncludeDependencies(true);
		projects[0].setDependencies(new String[] {"b"});
		projects[1].setName("b");
		projects[1].setAutoIncludeDependencies(true);
		projects[1].setDependencies(new String[] {"c"});
		projects[2].setName("c");
		
		EasyMock.expect(projectMgr.getProjectConfig("b")).andReturn(projects[1]);
		EasyMock.expect(projectMgr.getProjectConfig("c")).andReturn(projects[2]);
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[2]);
		dg.addTarget(projects[1]);
		dg.addTarget(projects[0]);
		
		check(dg, DependencyBuildPolicy.AS_NEEDED, false, false, WorkingCopyUpdateStrategy.Default);
	}
	
	public void testAddsSingleWhenOneProjectNoDeps() throws Exception {
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[0]);
		
		check(dg, DependencyBuildPolicy.AS_NEEDED, false, false, WorkingCopyUpdateStrategy.Default);
	}
	
	public void testNoDuplicates() throws Exception {
		EasyMock.expect(projectMgr.getProjectConfig("b")).andReturn(projects[1]);
		
		projects[0].setDependencies(new String[] {"b"});
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[0]);
		dg.addTarget(projects[1]);
		
		check(dg, DependencyBuildPolicy.AS_NEEDED, true, false, WorkingCopyUpdateStrategy.Default);
	}
	
	private void check(DependencyGroup expected, DependencyBuildPolicy policy, boolean useAll, boolean override, WorkingCopyUpdateStrategy updateStrategyOverride) throws Exception {
		replay();
		
		final ProjectConfigDto[] projects;
		
		if (useAll) {
			projects = this.projects;
		} else {
			projects = new ProjectConfigDto[] {this.projects[0]};
		}

		assertEquals(expected, DependencyGroupBuilder.buildDependencyGroup(projects, projectMgr, buildManager, policy, updateStrategyOverride, override, override));
		
		verify();
	}
}
