/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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

import java.net.InetAddress;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.BuildTarget;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.core.support.BuildTargetImpl;
import net.sourceforge.vulcan.core.support.ProjectBuilderImpl;
import net.sourceforge.vulcan.core.support.StoreStub;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.SchedulerConfigDto;

public class BuildDaemonTest extends EasyMockTestCase {
	boolean fastMode;
	
	class ProjectBuilderStub extends ProjectBuilderImpl {
		@Override
		protected void buildInternal(BuildDaemonInfoDto info, BuildTarget currentTarget, BuildDetailCallback buildDetailCallback) {
			if (fastMode) {
				return;
			}
			
			synchronized(this) {
				notifyAll();
			}
			
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
		
		public String getKilledBy() {
			return killedBy;
		}
	}

	BuildDaemonImpl bd = new BuildDaemonImpl() {
		@Override
		protected ProjectBuilder createBuilder() {
			return builder;
		}
	};

	BuildManager mgr = createStrictMock(BuildManager.class);
	
	ProjectBuilderStub builderStub = new ProjectBuilderStub();
	ProjectBuilder builder = builderStub;
	
	StoreStub store = new StoreStub(null);
	
	SchedulerConfigDto config = new SchedulerConfigDto();

	BuildDaemonInfoDto info = new BuildDaemonInfoDto();

	boolean interrupted;
	
	@Override
	public void setUp() throws Exception {
		config.setInterval(30000);
		config.setName("mock");

		builderStub.setBuildManager(mgr);
		builderStub.setConfigurationStore(store);
		builderStub.setBuildOutcomeStore(store);
		
		bd.setBuildManager(mgr);

		bd.init(config);

		info.setHostname(InetAddress.getLocalHost());
		info.setName(config.getName());
	}

	public void testGetsProjectSkipsOnNull() throws Exception {
		builder = createStrictMock(ProjectBuilder.class);
		
		mgr.getTarget(info);
		expectLastCall().andReturn(null);

		replay();

		bd.execute();

		verify();
	}

	public void testKillProjectBeingBuilt() throws Exception {
		final ProjectConfigDto project = new ProjectConfigDto();
		
		expect(mgr.getTarget(info)).andReturn(new BuildTargetImpl(project, new ProjectStatusDto()));

		replay();

		assertFalse(bd.isBuilding());
		assertNull(bd.getCurrentTarget());

		final Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					bd.execute();
				} catch (Exception e) {
				}
			}
		};
		bd.thread = thread;

		thread.start();

		synchronized (builderStub) {
			builderStub.wait(100);
		}

		assertTrue(bd.isBuilding());
		assertSame(project, bd.getCurrentTarget());

		bd.abortCurrentBuild("a user");

		thread.join();
		
		assertFalse(bd.isBuilding());
		assertNull(bd.getCurrentTarget());

		verify();
		
		assertEquals("a user", builderStub.getKilledBy());
		
		assertTrue("did not interrupt thread", interrupted);
	}
	
	public void testWatchDogKills() throws Exception {
		config.setTimeout(10);
		bd.thread = Thread.currentThread();

		final ProjectConfigDto project = new ProjectConfigDto();

		expect(mgr.getTarget(info)).andReturn(new BuildTargetImpl(project, new ProjectStatusDto()));

		replay();

		bd.execute();

		verify();
	}
	
	public void testWatchDogTerminatesOnBuildComplete() throws Exception {
		config.setTimeout(10000);
		bd.thread = Thread.currentThread();

		final ProjectConfigDto project = new ProjectConfigDto();

		expect(mgr.getTarget(info)).andReturn(new BuildTargetImpl(project, new ProjectStatusDto()));

		replay();

		bd.execute();

		verify();
	}
	
	// attempt to find thread synchronization problems.  only run manually.
	public void _testWatchDogTerminatesOnBuildCompleteRaceConditionLots() throws Exception {
		for (int i=0; i<10000; i++) {
			doTestWatchDogTerminatesOnBuildCompleteRaceCondition();
			reset();
		}
	}
	
	public void doTestWatchDogTerminatesOnBuildCompleteRaceCondition() throws Exception {
		fastMode = true;
		
		config.setTimeout(10000);
		bd.thread = Thread.currentThread();

		final ProjectConfigDto project = new ProjectConfigDto();

		expect(mgr.getTarget(info)).andReturn(new BuildTargetImpl(project, new ProjectStatusDto()));
		
		replay();

		bd.execute();

		verify();
	}
}
