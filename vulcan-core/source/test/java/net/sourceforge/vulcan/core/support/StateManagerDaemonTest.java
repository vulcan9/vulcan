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

import java.util.Date;
import java.util.List;

import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.scheduler.BuildDaemon;
import net.sourceforge.vulcan.scheduler.ProjectScheduler;
import net.sourceforge.vulcan.scheduler.Scheduler;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class StateManagerDaemonTest extends StateManagerTestBase {
	int startCallCount = 0;
	int stopCallCount = 0;
	boolean running = false;
	SchedulerConfigDto configArg = null;
	
	public class MockScheduler implements Scheduler, BuildDaemon {
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
		public Date getNextExecutionDate() {
			return null;
		}
		public String getName() {
			return name;
		}
		public void abortCurrentBuild(String requestUsername) {
		}
		public ProjectConfigDto getCurrentTarget() {
			return null;
		}
		public String getDetail() {
			return null;
		}
		public String getPhase() {
			return null;
		}
		public boolean isBuilding() {
			return false;
		}
		public boolean isKilling() {
			return false;
		}
		public void wakeUp() {
		}
	};
	@Override
	public void setUp() {
		stateMgr = new StateManagerImpl() {
			@Override
			public BuildDaemon createBuildDaemon() {
				return new MockScheduler();
			}
			@Override
			protected ProjectScheduler createProjectScheduler() {
				return null;
			}
		};
		super.setUp();
	}
	public void testRunStartsBuildDaemons() throws Exception {
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		this.stateManagerConfig.setBuildDaemons(new SchedulerConfigDto[] {scheduler});
		
		assertEquals(0, startCallCount);
		stateMgr.start();
		assertEquals(1, startCallCount);
	}
	public void testShutdownStopsBuildDaemons() throws Exception {
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		this.stateManagerConfig.setBuildDaemons(new SchedulerConfigDto[] {scheduler});
		
		assertEquals(0, startCallCount);
		stateMgr.start();
		assertEquals(1, startCallCount);

		assertEquals(0, stopCallCount);
		assertEquals(1, stateMgr.buildDaemons.size());
		stateMgr.shutdown();
		assertEquals(1, stopCallCount);
		assertEquals(0, stateMgr.buildDaemons.size());
	}
	public void testSortsDaemons() throws Exception {
		stateMgr.start();
		
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		stateMgr.addBuildDaemonConfig(scheduler);

		scheduler = new SchedulerConfigDto();
		scheduler.setName("a");
		scheduler.setInterval(30000);
		stateMgr.addBuildDaemonConfig(scheduler);
		
		final List<BuildDaemon> list = stateMgr.getBuildDaemons();
		assertEquals(2, list.size());
		assertEquals("a", ((Scheduler)list.get(0)).getName());
		assertEquals("default", ((Scheduler)list.get(1)).getName());
	}
	public void testGetDaemonByName() throws Exception {
		stateMgr.start();
		
		SchedulerConfigDto scheduler = new SchedulerConfigDto();
		scheduler.setName("default");
		scheduler.setInterval(60000);
		stateMgr.addBuildDaemonConfig(scheduler);

		scheduler = new SchedulerConfigDto();
		scheduler.setName("a");
		scheduler.setInterval(30000);
		stateMgr.addBuildDaemonConfig(scheduler);

		assertEquals("a", stateMgr.getBuildDaemons().get(0).getName());
		assertSame(stateMgr.getBuildDaemons().get(0), stateMgr.getBuildDaemon("a"));
		
		assertNull(stateMgr.getBuildDaemon("none"));
		assertNull(stateMgr.getBuildDaemon(null));
	}
	public void testUpdateBuildDaemonConfigNotifiesScheduler() throws Exception {
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");

		stateMgr.addBuildDaemonConfig(config);

		stateMgr.startBuildDaemons();
		
		configArg = null;
		
		final SchedulerConfigDto clone = (SchedulerConfigDto)config.copy();
		clone.setName("other");
		
		stateMgr.updateBuildDaemonConfig(config.getName(), clone);
		
		assertSame(clone, configArg);
		assertEquals(0, stopCallCount);
		
		stateMgr.stopBuildDaemons();
		
		assertEquals(1, stopCallCount);
	}
	public void testDeleteSchedulerConfigThrowsOnNotFound() throws Exception {
		try {
			stateMgr.deleteBuildDaemonConfig("none");
			fail("expected exception");
		} catch (IllegalArgumentException e) {
		}
	}
	public void testDeleteSchedulerConfigStopsIfRunning() throws Exception {
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");

		stateMgr.addBuildDaemonConfig(config);

		stateMgr.startBuildDaemons();

		assertEquals(0, stopCallCount);
		
		assertEquals(1, stateMgr.getConfig().getBuildDaemons().length);
		
		stateMgr.deleteBuildDaemonConfig(config.getName());
		
		assertEquals(0, stateMgr.getConfig().getBuildDaemons().length);
		assertEquals(1, stopCallCount);
	}
	public void testUpdateSchedulerConfigThrowsDuplicateName() throws Exception {
		final SchedulerConfigDto a = new SchedulerConfigDto();
		final SchedulerConfigDto b = new SchedulerConfigDto();
		a.setName("a");
		b.setName("b");
		
		stateMgr.addBuildDaemonConfig(a);
		stateMgr.addBuildDaemonConfig(b);
		store.setCommitCalled(false);
		
		final String oldName = b.getName();
		b.setName("a");
		
		try {
			stateMgr.updateBuildDaemonConfig(oldName, b);
			fail("expected exception");
		} catch (DuplicateNameException e) {
		}
		assertFalse(store.isCommitCalled());
	}
	public void testAddSchedulerConfigThrowsDuplicateName() throws Exception {
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");
		assertEquals(0, stateMgr.getConfig().getSchedulers().length);
		stateMgr.addBuildDaemonConfig(config);
		
		assertEquals(1, stateMgr.getConfig().getBuildDaemons().length);
		store.setCommitCalled(false);
		
		try {
			stateMgr.addBuildDaemonConfig(config);
			fail("expected exception");
		} catch (DuplicateNameException e) {
		}
		
		assertFalse(store.isCommitCalled());
		
		assertEquals(1, stateMgr.getConfig().getBuildDaemons().length);
	}
	public void testAddSchedulerWhileNotRunningDoesNotStart() throws Exception {
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");

		stateMgr.shutdown();
		
		assertEquals(false, stateMgr.running);
		assertEquals(0, startCallCount);
		stateMgr.addBuildDaemonConfig(config);
		assertEquals(0, startCallCount);
	}
	public void testAddScheduleWhileRunningStarts() throws Exception {
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("test");

		stateMgr.running = true;
		
		assertEquals(0, startCallCount);
		stateMgr.addBuildDaemonConfig(config);
		assertEquals(1, startCallCount);
	}
}
