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
package net.sourceforge.vulcan.scheduler.quartz;

import java.util.Arrays;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.DependencyBuildPolicy;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.core.support.DependencyGroupImpl;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.event.InfoEvent;
import net.sourceforge.vulcan.exception.AlreadyScheduledException;

import org.easymock.EasyMock;

public class ScheduleProjectsJobTest extends EasyMockTestCase {
	ScheduleProjectsJob job = new ScheduleProjectsJob();
	
	ProjectManager projectManager = createStrictMock(ProjectManager.class);
	BuildManager buildManager = createStrictMock(BuildManager.class);
	EventHandler eventHandler = createStrictMock(EventHandler.class);
	
	ProjectConfigDto[] projects = { new ProjectConfigDto() };
	DependencyGroup depGroup = new DependencyGroupImpl();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		job.setProjectManager(projectManager);
		job.setBuildManager(buildManager);
		job.setEventHandler(eventHandler);
		
		// Add a target so isEmpty will be false.
		depGroup.addTarget(new ProjectConfigDto());
	}
	
	public void testSimple() throws Exception {
		projectManager.getProjectsForScheduler("job");
		EasyMock.expectLastCall().andReturn(projects);
		
		projectManager.buildDependencyGroup(projects, DependencyBuildPolicy.AS_NEEDED, null, false, false);
		EasyMock.expectLastCall().andReturn(depGroup);
		
		buildManager.add(depGroup);
		
		replay();
		
		job.executeInternal("job");
		
		verify();
	}
	
	public void testDoesNotScheduleEmptyGroup() throws Exception {
		projectManager.getProjectsForScheduler("job");
		EasyMock.expectLastCall().andReturn(projects);
		
		projectManager.buildDependencyGroup(projects, DependencyBuildPolicy.AS_NEEDED, null, false, false);
		depGroup = new DependencyGroupImpl();
		EasyMock.expectLastCall().andReturn(depGroup);
		
		replay();
		
		job.executeInternal("job");
		
		verify();
	}
	
	public void testAlreadyScheduled() throws Exception {
		projectManager.getProjectsForScheduler("job");
		EasyMock.expectLastCall().andReturn(projects);
		
		projectManager.buildDependencyGroup(projects, DependencyBuildPolicy.AS_NEEDED, null, false, false);
		EasyMock.expectLastCall().andReturn(depGroup);
		
		final AlreadyScheduledException e = new AlreadyScheduledException("job");

		buildManager.add(depGroup);
		EasyMock.expectLastCall().andThrow(e);
		
		eventHandler.reportEvent(new InfoEvent(job, "Scheduler.interval.too.short", new Object[] {"job"}) {
			@Override
			public boolean equals(Object obj) {
				final InfoEvent o = (InfoEvent) obj;
				if (o.getKey().equals(getKey()) && o.getSource().equals(getSource())
						&& Arrays.equals(o.getArgs(), getArgs())) {
					return true;
				}
				return false;
			}
		});
		
		replay();
		
		job.executeInternal("job");
		
		verify();
	}

	public void testShortCircuitOnNoProjects() throws Exception {
		projectManager.getProjectsForScheduler("job");
		EasyMock.expectLastCall().andReturn((new ProjectConfigDto[0]));
		
		replay();
		
		job.executeInternal("job");
		
		verify();
		
	}
}
