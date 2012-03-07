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

import java.util.Date;
import java.util.List;

import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.dto.LockDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.scheduler.BuildDaemon;
import net.sourceforge.vulcan.scheduler.ProjectScheduler;
import net.sourceforge.vulcan.scheduler.Scheduler;


public class StateManagerScheduleTest extends StateManagerTestBase {
	int startCallCount = 0;
	int stopCallCount = 0;
	boolean running = false;
	SchedulerConfigDto configArg = null;
	
	@Override
	public void setUp() {
		stateMgr = new StateManagerImpl()  {
			@Override
			public ProjectScheduler createProjectScheduler() {
				return new MockScheduler();
			}
			@Override
			protected BuildDaemon createBuildDaemon() {
				return null;
			}
		};
		
		super.setUp();
	}
	
	public class MockScheduler implements ProjectScheduler {
		private String name;
		public void init(SchedulerConfigDto config) {
			configArg = config;
			name = config.getName();
		}
		public void configurationChanged(SchedulerConfigDto config) {
			configArg = config;
			name = config.getName();
		}
		public boolean isRunning() {
			return running;
		}
		public void start() {
			startCallCount++;
		}
		public void stop() {
			stopCallCount++;
		}
		public void wakeUp() {
		}
		public Date getNextExecutionDate() {
			return null;
		}
		public String getName() {
			return name;
		}
		public ProjectManager getProjectManager() {
			return null;
		}
	};
	public void testRunStartsSchedulers() throws Exception {
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		this.stateManagerConfig.setSchedulers(new SchedulerConfigDto[] {scheduler});
		
		assertEquals(0, startCallCount);
		stateMgr.start();
		assertEquals(1, startCallCount);
	}
	public void testRunDoesNotStartSchedulersIfBuildManagerDisabled() throws Exception {
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		this.stateManagerConfig.setSchedulers(new SchedulerConfigDto[] {scheduler});
		
		stateManagerConfig.getBuildManagerConfig().setEnabled(false);
		
		assertEquals(0, startCallCount);
		stateMgr.start();
		assertEquals(0, startCallCount);
	}
	public void testStartSchedulersIfBuildManagerEnabledLater() throws Exception {
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		this.stateManagerConfig.setSchedulers(new SchedulerConfigDto[] {scheduler});
		
		stateManagerConfig.getBuildManagerConfig().setEnabled(false);
		
		assertEquals(0, startCallCount);
		
		stateManagerConfig.getBuildManagerConfig().setEnabled(true);
		
		assertEquals(1, startCallCount);
	}
	public void testStopSchedulersIfBuildManagerDisabledLater() throws Exception {
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		stateMgr.addSchedulerConfig(scheduler);
		
		assertEquals(0, stopCallCount);
		assertEquals(1, stateMgr.schedulers.size());
		
		stateManagerConfig.getBuildManagerConfig().setEnabled(false);
		
		assertEquals(1, stopCallCount);
		assertEquals(0, stateMgr.schedulers.size());
	}
	public void testAddSchedulerDoesNotStartIfManagerDisabled() throws Exception {
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		stateManagerConfig.getBuildManagerConfig().setEnabled(false);

		assertEquals(0, startCallCount);
		stateMgr.addSchedulerConfig(scheduler);
		assertEquals(0, startCallCount);
	}
	public void testShutdownStopsSchedulers() throws Exception {
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		this.stateManagerConfig.setSchedulers(new SchedulerConfigDto[] {scheduler});
		
		assertEquals(0, startCallCount);
		stateMgr.start();
		assertEquals(1, startCallCount);

		assertEquals(0, stopCallCount);
		stateMgr.shutdown();
		assertEquals(1, stopCallCount);
	}
	public void testSortsSchedulers() throws Exception {
		stateMgr.start();
		
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		stateMgr.addSchedulerConfig(scheduler);

		scheduler = new SchedulerConfigDto();
		scheduler.setName("a");
		scheduler.setInterval(30000);
		stateMgr.addSchedulerConfig(scheduler);
		
		final List<ProjectScheduler> list = stateMgr.getSchedulers();
		assertEquals(2, list.size());
		assertEquals("a", ((Scheduler)list.get(0)).getName());
		assertEquals("default", ((Scheduler)list.get(1)).getName());
	}
	public void testStartSchedulers() throws Exception {
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		this.stateManagerConfig.setSchedulers(new SchedulerConfigDto[] {scheduler});
		
		assertEquals(0, startCallCount);
		stateMgr.startSchedulers();
		assertEquals(1, startCallCount);
		
		assertSame(scheduler, configArg);
		
		assertEquals(0, stopCallCount);
		stateMgr.stopSchedulers();
		assertEquals(1, startCallCount);
		assertEquals(1, stopCallCount);
	}
	public void testUpdateSchedulerConfigNotifiesScheduler() throws Exception {
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");

		stateMgr.addSchedulerConfig(config);

		stateMgr.startSchedulers();
		
		configArg = null;
		
		final SchedulerConfigDto clone = (SchedulerConfigDto)config.copy();
		clone.setName("other");
		
		stateMgr.updateSchedulerConfig(config.getName(), clone, true);
		
		assertSame(clone, configArg);
		assertEquals(0, stopCallCount);
		
		stateMgr.stopSchedulers();
		
		assertEquals(1, stopCallCount);
	}
	public void testRenameSchedulerConfigUpdatesProjects() throws Exception {
		final ProjectConfigDto a = new ProjectConfigDto();
		a.setName("a");
		a.setSchedulerNames(new String[] {"test"});
		
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");

		stateMgr.addSchedulerConfig(config);
		stateMgr.addProjectConfig(a);
		
		final SchedulerConfigDto clone = (SchedulerConfigDto)config.copy();
		clone.setName("other");
		
		stateMgr.updateSchedulerConfig(config.getName(), clone, true);

		assertEquals("other", stateMgr.getProjectConfig("a").getSchedulerNames()[0]);
	}
	public void testDeleteSchedulerConfigThrowsOnNotFound() throws Exception {
		try {
			stateMgr.deleteSchedulerConfig("none");
			fail("expected exception");
		} catch (IllegalArgumentException e) {
		}
		
		assertFalse(store.isCommitCalled());
	}
	public void testDeleteSchedulerConfigStopsIfRunning() throws Exception {
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");

		stateMgr.addSchedulerConfig(config);

		stateMgr.startSchedulers();

		assertEquals(0, stopCallCount);
		
		stateMgr.deleteSchedulerConfig(config.getName());
		
		assertEquals(1, stopCallCount);
	}
	public void testDeleteSchedulerConfigRemovesFromProject() throws Exception {
		final ProjectConfigDto a = new ProjectConfigDto();
		a.setSchedulerNames(new String[] {"test"});
		
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");

		stateMgr.addProjectConfig(a);
		stateMgr.addSchedulerConfig(config);

		stateMgr.deleteSchedulerConfig(config.getName());
		
		assertEquals(0, a.getSchedulerNames().length);
	}
	public void testUpdateSchedulerConfigThrowsDuplicateName() throws Exception {
		final SchedulerConfigDto a = new SchedulerConfigDto();
		final SchedulerConfigDto b = new SchedulerConfigDto();
		a.setName("a");
		b.setName("b");
		
		stateMgr.addSchedulerConfig(a);
		stateMgr.addSchedulerConfig(b);
		store.clearCommitCount();
		
		final String oldName = b.getName();
		b.setName("a");
		
		try {
			stateMgr.updateSchedulerConfig(oldName, b, true);
			fail("expected exception");
		} catch (DuplicateNameException e) {
		}
		assertFalse(store.isCommitCalled());
	}
	public void testAddSchedulerConfigThrowsDuplicateName() throws Exception {
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");
		assertEquals(0, stateMgr.getConfig().getSchedulers().length);
		stateMgr.addSchedulerConfig(config);
		
		assertEquals(1, stateMgr.getConfig().getSchedulers().length);
		store.clearCommitCount();
		
		try {
			stateMgr.addSchedulerConfig(config);
			fail("expected exception");
		} catch (DuplicateNameException e) {
		}
		
		assertFalse(store.isCommitCalled());
		
		assertEquals(1, stateMgr.getConfig().getSchedulers().length);
	}
	public void testAddSchedulerWhileNotRunningDoesNotStart() throws Exception {
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");

		stateMgr.shutdown();
		
		assertEquals(false, stateMgr.running);
		assertEquals(0, startCallCount);
		stateMgr.addSchedulerConfig(config);
		assertEquals(0, startCallCount);
	}
	public void testAddScheduleWhileRunningStarts() throws Exception {
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");

		stateMgr.running = true;
		
		assertEquals(0, startCallCount);
		stateMgr.addSchedulerConfig(config);
		assertEquals(1, startCallCount);
	}
	public void testGetProjectsForScheduler() throws Exception {
		final SchedulerConfigDto sched = new SchedulerConfigDto();
		sched.setName("mock");
		
		final ProjectConfigDto a = new ProjectConfigDto();
		a.setName("a");
		a.setSchedulerNames(new String[] {"mock"});

		final ProjectConfigDto b = new ProjectConfigDto();
		b.setName("b");
		b.setSchedulerNames(new String[] {"other"});
		
		final ProjectConfigDto c = new ProjectConfigDto();
		c.setName("c");
		c.setSchedulerNames(new String[] {"mock"});

		final ProjectConfigDto d = new ProjectConfigDto();
		d.setName("d");
		d.setSchedulerNames(new String[] {"mock"});
		d.addLock(new LockDto());

		stateMgr.addSchedulerConfig(sched);
		stateMgr.addProjectConfig(a);
		stateMgr.addProjectConfig(b);
		stateMgr.addProjectConfig(c);
		stateMgr.addProjectConfig(d);
		
		ProjectConfigDto[] ps = stateMgr.getProjectsForScheduler(sched.getName());
		
		assertNotNull(ps);
		assertEquals(2, ps.length);
		assertSame(a, ps[0]);
		assertSame(c, ps[1]);
	}
}
