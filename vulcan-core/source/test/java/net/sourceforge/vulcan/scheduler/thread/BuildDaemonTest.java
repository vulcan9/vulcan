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
package net.sourceforge.vulcan.scheduler.thread;

import java.net.InetAddress;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.core.support.ProjectBuilderImpl;
import net.sourceforge.vulcan.core.support.StoreStub;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id = "$Id$", url = "$HeadURL$")
public class BuildDaemonTest extends EasyMockTestCase {
	boolean fastMode;
	
	class ProjectBuilderStub extends ProjectBuilderImpl {
		@Override
		protected void buildProject(ProjectConfigDto currentTarget) throws Exception {
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
	ProjectBuilder builder = createStrictMock(ProjectBuilder.class);
	
	ProjectBuilderStub builderStub = new ProjectBuilderStub();
	
	StoreStub store = new StoreStub(null);
	
	SchedulerConfigDto config = new SchedulerConfigDto();

	BuildDaemonInfoDto info = new BuildDaemonInfoDto();

	boolean interrupted;
	
	@Override
	public void setUp() throws Exception {
		config.setInterval(30000);
		config.setName("mock");

		builderStub.setBuildManager(mgr);
		builderStub.setStore(store);
		
		bd.setBuildManager(mgr);

		bd.init(config);

		info.setHostname(InetAddress.getLocalHost());
		info.setName(config.getName());
	}

	public void testGetsProjectSkipsOnNull() throws Exception {
		mgr.getTarget(info);
		expectLastCall().andReturn(null);

		replay();

		bd.execute();

		verify();
	}

	public void testKillProjectBeingBuilt() throws Exception {
		builder = builderStub;
		
		final ProjectConfigDto project = new ProjectConfigDto();


		expect(mgr.getTarget(info)).andReturn(project);
		expect(mgr.getLatestStatus(null)).andReturn(null).anyTimes();

		mgr.targetCompleted(
				(BuildDaemonInfoDto) anyObject(),
				(ProjectConfigDto) anyObject(),
				(ProjectStatusDto) anyObject());

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

		synchronized (builder) {
			builder.wait(100);
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
		builder = builderStub;
		
		config.setTimeout(10);
		bd.thread = Thread.currentThread();

		final ProjectConfigDto project = new ProjectConfigDto();

		expect(mgr.getTarget(info)).andReturn(project);

		expect(mgr.getLatestStatus(null)).andReturn(null).anyTimes();
		
		mgr.targetCompleted(
				(BuildDaemonInfoDto) anyObject(),
				(ProjectConfigDto) anyObject(),
				(ProjectStatusDto) anyObject());
		
		replay();

		bd.execute();

		verify();
	}
	public void testWatchDogTerminatesOnBuildComplete() throws Exception {
		builder = builderStub;
		
		config.setTimeout(10000);
		bd.thread = Thread.currentThread();

		final ProjectConfigDto project = new ProjectConfigDto();

		expect(mgr.getTarget(info)).andReturn(project);
		expect(mgr.getLatestStatus(null)).andReturn(null).anyTimes();
		
		mgr.targetCompleted(
				(BuildDaemonInfoDto) anyObject(),
				(ProjectConfigDto) anyObject(),
				(ProjectStatusDto) anyObject());

		replay();

		bd.execute();

		verify();
	}
	public void testWatchDogTerminatesOnBuildCompleteRaceConditionLots() throws Exception {
		for (int i=0; i<1000; i++) {
			doTestWatchDogTerminatesOnBuildCompleteRaceCondition();
			reset();
		}
	}
	public void doTestWatchDogTerminatesOnBuildCompleteRaceCondition() throws Exception {
		fastMode = true;
		
		builder = builderStub;
		
		config.setTimeout(10000);
		bd.thread = Thread.currentThread();

		final ProjectConfigDto project = new ProjectConfigDto();

		expect(mgr.getTarget(info)).andReturn(project);
		expect(mgr.getLatestStatus(null)).andReturn(null).anyTimes();
		
		mgr.targetCompleted(
				(BuildDaemonInfoDto) anyObject(),
				(ProjectConfigDto) anyObject(),
				(ProjectStatusDto) anyObject());
		
		replay();

		bd.execute();

		verify();
	}
}
