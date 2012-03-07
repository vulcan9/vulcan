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
package net.sourceforge.vulcan.scheduler.thread;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.DependencyBuildPolicy;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.core.support.DependencyGroupImpl;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.AlreadyScheduledException;

public class ProjectSchedulerTest extends EasyMockTestCase {
	ProjectSchedulerImpl ps = new ProjectSchedulerImpl();
	
	ProjectManager projectMgr = createStrictMock(ProjectManager.class);
	BuildManager mgr = createStrictMock(BuildManager.class);
	
	SchedulerConfigDto config = new SchedulerConfigDto();
	ProjectConfigDto[] projects = {new ProjectConfigDto(), new ProjectConfigDto()};
	EventHandler handler;
	Event event;
	
	@Override
	public void setUp() throws Exception {
		projects[0].setName("a");
		projects[0].setAutoIncludeDependencies(true);
		projects[1].setName("b");
		
		config.setName("mock scheduler");
		ps.setProjectManager(projectMgr);
		ps.setBuildManager(mgr);
		
		handler = new EventHandler() {
			public void reportEvent(Event event) {
				ProjectSchedulerTest.this.event = event;
			};
		};
		ps.setEventHandler(handler);

		ps.init(config);
		
	}
	public void testAddsProjects() throws Exception {
		expect(projectMgr.getProjectsForScheduler(config.getName())).andReturn(projects);
		
		DependencyGroup dg = createStrictMock(DependencyGroup.class);
		
		expect(projectMgr.buildDependencyGroup(projects, DependencyBuildPolicy.AS_NEEDED, null, false, false)).andReturn(dg);
		
		dg.setName(ps.getName());
		
		mgr.add(dg);
		
		check();
	}
	public void testHandlesAlreadyScheduledException() throws Exception {
		expect(projectMgr.getProjectsForScheduler(config.getName())).andReturn(projects);
		
		projects[0].setDependencies(new String[] {"b"});
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.setName(ps.getName());
		dg.addTarget(projects[0]);
		dg.addTarget(projects[1]);
		
		expect(projectMgr.buildDependencyGroup(projects, DependencyBuildPolicy.AS_NEEDED, null, false, false)).andReturn(dg);
		
		mgr.add(dg);
		expectLastCall().andThrow(new AlreadyScheduledException(ps.getName()));
		
		assertNull(event);
		
		check();
		
		assertNotNull(event);
		assertEquals("Scheduler.interval.too.short", event.getKey());
	}

	private void check() throws Exception {
		replay();
		
		ps.execute();

		verify();
	}
}
