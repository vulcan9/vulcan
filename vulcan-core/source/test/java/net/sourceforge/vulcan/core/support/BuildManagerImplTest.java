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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import java.net.InetAddress;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.BuildManagerConfigDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.AlreadyScheduledException;
import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class BuildManagerImplTest extends TestCase {
	BuildManagerImpl mgr = new BuildManagerImpl();
	
	BuildDaemonInfoDto info1 = new BuildDaemonInfoDto();
	BuildDaemonInfoDto info2 = new BuildDaemonInfoDto();
	
	ProjectConfigDto a = new ProjectConfigDto();
	ProjectConfigDto b = new ProjectConfigDto();
	ProjectConfigDto c = new ProjectConfigDto();
	
	DependencyGroup dg = new DependencyGroupImpl();
	
	BuildManagerConfigDto mgrConfig = new BuildManagerConfigDto();
	BuildOutcomeCache cache = new BuildOutcomeCache();
	
	RevisionTokenDto rev0 = new RevisionTokenDto(0l, "0");
	RevisionTokenDto rev1 = new RevisionTokenDto(1l, "0");
	
	int fireCount;

	protected Event event;
	
	final EventHandler eventHandler = new EventHandler() {
		public void reportEvent(Event event) {
			fireCount++;
			BuildManagerImplTest.this.event = event;
		}
	};
	
	@Override
	public void setUp() throws Exception {
		info1.setName("mock build daemon 1");
		info1.setHostname(InetAddress.getLocalHost());
		info2.setName("mock build daemon 2");
		info2.setHostname(InetAddress.getLocalHost());
		
		a.setName("a");
		b.setName("b");
		c.setName("c");

		cache.setBuildOutcomeStore(new StoreStub(null) {
			@Override
			public ProjectStatusDto loadBuildOutcome(UUID id) {
				if (id == null) {
					throw new AssertionFailedError("should never call this with null UUID");
				}
				return super.loadBuildOutcome(id);
			}
		});
		
		cache.setCacheSize(100);
		cache.init();
		
		mgr.setEventHandler(eventHandler);
		mgr.setBuildOutcomeCache(cache);
		
		mgrConfig.setEnabled(true);
		mgr.init(mgrConfig);
	}
	
	public void testCannotGetTwoProjectsAtOnce() {
		mgr.add(a);
		mgr.add(b);
		
		assertEquals(null, a.getRequestedBy());
		
		assertSame(a, mgr.getTarget(info1));
		
		assertEquals(null, a.getRequestedBy());
		
		assertEquals(1, mgr.activeDaemons.size());
		
		try {
			mgr.getTarget(info1);
			fail("expected exception");
		} catch (IllegalStateException e) {
		}
		assertEquals(1, mgr.activeDaemons.size());

		assertSame(b, mgr.getTarget(info2));
	}
	public void testTargetCompletedThrowsIfNotFound() {
		try {
			mgr.targetCompleted(info1, a, 
				createFakeStatus(a, rev0, Status.PASS, null, null, null));
			fail("expected exception");
		} catch (IllegalStateException e) {
		}
	}
	public void testGetTarget() {
		ProjectConfigDto proj = mgr.getTarget(info1);
		
		assertEquals(null, proj);
		
		mgr.add(a);
		mgr.add(b);
		
		assertSame(a, mgr.getTarget(info1));
		assertEquals(1, mgr.activeDaemons.size());
		
		assertSame(b, mgr.getTarget(info2));
		assertEquals(2, mgr.activeDaemons.size());
		
		BuildDaemonInfoDto info3 = (BuildDaemonInfoDto) info2.copy();
		info3.setName("other");
		
		assertEquals(null, mgr.getTarget(info3));
	}
	public void testGetProjectsBeingBuilt() {
		assertEquals(Collections.emptyMap(), mgr.getProjectsBeingBuilt());
		
		ProjectConfigDto proj = mgr.getTarget(info1);
		
		assertEquals(null, proj);
		assertEquals(Collections.emptyMap(), mgr.getProjectsBeingBuilt());
		
		mgr.add(a);
		mgr.add(b);
		
		assertSame(a, mgr.getTarget(info1));
		assertEquals(1, mgr.activeDaemons.size());
		assertEquals(Collections.singleton(a.getName()), mgr.getProjectsBeingBuilt().keySet());
		
		assertSame(b, mgr.getTarget(info2));
		assertEquals(2, mgr.activeDaemons.size());
		assertEquals(new HashSet<String>(Arrays.asList(new String[] {a.getName(), b.getName()})), mgr.getProjectsBeingBuilt().keySet());
		
		BuildDaemonInfoDto info3 = (BuildDaemonInfoDto) info2.copy();
		info3.setName("other");
		
		assertEquals(null, mgr.getTarget(info3));
	}
	public void testRegisterStatus() throws Exception {
		mgr.add(a);
		assertSame(a, mgr.getTarget(info1));
		
		assertNotNull(mgr.getProjectsBeingBuilt().get(a.getName()));
		
		final ProjectStatusDto status = new ProjectStatusDto();
		
		mgr.registerBuildStatus(info1, a, status);
		
		assertSame(status, mgr.getProjectsBeingBuilt().get(a.getName()));
	}
	public void testGetTargetDoesNotAddNullToActive() throws Exception {
		b.setDependencies(new String[] {"a"});
		
		dg.addTarget(a);
		dg.addTarget(b);
		
		mgr.add(dg);
		
		assertSame(a, mgr.getTarget(info1));
		
		assertEquals(1, mgr.activeBuilds.size());
		assertEquals(1, mgr.activeDaemons.size());
		
		assertEquals(null, mgr.getTarget(info2));
		
		assertEquals(1, mgr.activeBuilds.size());
		assertEquals(1, mgr.activeDaemons.size());

		mgr.targetCompleted(info1, a, 
				createFakeStatus(a, rev0, Status.FAIL, null, null, null));
		
		assertEquals(0, mgr.activeBuilds.size());
		assertEquals(0, mgr.activeDaemons.size());
		
		assertEquals(null, mgr.getTarget(info2));
		
		assertEquals(0, mgr.activeBuilds.size());
		assertEquals(0, mgr.activeDaemons.size());
	}
	public void testTargetCompletedFiresEvent() throws Exception {
		mgr.add(a);
		mgr.add(b);
		
		ProjectConfigDto config = mgr.getTarget(info1);
		
		assertEquals(0, fireCount);
		assertFalse(mgr.getProjectStatus().containsKey(config.getName()));
		
		mgr.targetCompleted(info1, config, createFakeStatus(config, rev0, Status.PASS, null, null, null));
		
		assertTrue(mgr.getProjectStatus().containsKey(config.getName()));
		ProjectStatusDto status = mgr.getProjectStatus().get(config.getName());
		assertEquals(config.getName(), status.getName());
		assertTrue(status.isPass());

		assertEquals(1, fireCount);
		assertNotNull(event);
		BuildCompletedEvent buildCompletedEvent = ((BuildCompletedEvent)event);
		assertEquals("messages.build.success", buildCompletedEvent.getKey());
		assertEquals(rev0, buildCompletedEvent.getStatus().getRevision());
		assertSame(buildCompletedEvent.getStatus().getBuildNumber(), buildCompletedEvent.getStatus().getLastGoodBuildNumber());
		assertSame(buildCompletedEvent.getStatus().getLastKnownRevision(), buildCompletedEvent.getStatus().getRevision());

		config = mgr.getTarget(info2);
		final ProjectStatusDto st = createFakeStatus(config, rev1, Status.FAIL, "mock",
				new Object[] {"foo"}, null);
		
		mgr.targetCompleted(info2, config, st);
		
		assertEquals(2, fireCount);
		
		status = mgr.getProjectStatus().get(config.getName());
		assertEquals(config.getName(), status.getName());
		assertEquals("mock", status.getMessageKey());
		assertEquals(rev1, status.getRevision());
		
		buildCompletedEvent = ((BuildCompletedEvent)event);
		assertEquals(status, buildCompletedEvent.getStatus());
		
		assertEquals(status.getMessageKey(), buildCompletedEvent.getKey());
		assertTrue(status.isFail());
		assertNotNull(status.getCompletionDate());
		assertNull(status.getLastGoodBuildNumber());
	}
	
	public void testTargetCompletedFiresEventWithDetailedStatus() throws Exception {
		a.setDependencies(new String[] {"b"});
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(a);
		
		mgr.add(dg);
		
		assertNull(mgr.getTarget(info1));
		
		final ProjectStatusDto status = mgr.getProjectStatus().get("a");
		assertEquals("messages.dependency.missing", status.getMessageKey());
		assertEquals(status.getMessageKey(), ((BuildCompletedEvent)event).getKey());
		assertTrue(status.isFail());
		assertNotNull(status.getCompletionDate());
		assertEquals(Status.SKIP, status.getStatus());
		assertEquals((Integer)0, status.getBuildNumber());
	}

	public void testSkipDoesNotFireWhenPreviousSkip() throws Exception {
		final ProjectStatusDto prevStatus = createFakeStatus(a, rev0, Status.SKIP, null, null, null);
		prevStatus.setBuildNumber(67);
		
		mgr.cache.store(prevStatus);
		
		a.setDependencies(new String[] {"b"});

		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(a);
		dg.addTarget(b);
		
		mgr.add(dg);
		
		assertSame(b, mgr.getTarget(info1));

		mgr.targetCompleted(info1, b, createFakeStatus(b, rev0, Status.FAIL, null, null, null));
		
		assertSame(prevStatus, mgr.getLatestStatus(a.getName()));
		
		assertNull(mgr.getTarget(info1));
		
		assertSame(prevStatus, mgr.getLatestStatus(a.getName()));
	}
	
	public void testGetTargetFromDepGroup() throws Exception {
		dg.addTarget(a);
		dg.setName("Catherine");
		
		mgr.add(dg);
		
		assertEquals(null, a.getRequestedBy());
		assertSame(a, mgr.getTarget(info1));
		assertEquals("Catherine", a.getRequestedBy());
		
		assertEquals(null, mgr.getTarget(info2));
	}

	public void testSetsDependencyRev() throws Exception {
		a.setDependencies(new String[] {"b"});
		
		dg.addTarget(a);
		dg.addTarget(b);
		
		mgr.add(dg);
		
		assertNull(cache.getLatestOutcome("b"));
		
		ProjectConfigDto target = mgr.getTarget(info1);
		mgr.targetCompleted(info1, target,
				createFakeStatus(target, rev1, Status.PASS, null, null, null));
		
		assertNotNull(cache.getLatestOutcome("b"));
		assertNotNull(cache.getLatestOutcomeId("b"));
		
		target = mgr.getTarget(info1);
		mgr.targetCompleted(info1, target, 
				createFakeStatus(target, rev1, Status.PASS, null, null, null));
		
		assertEquals(cache.getLatestOutcomeId("b"), mgr.getLatestStatus(a.getName())
				.getDependencyIds().get(b.getName()));
	}

	public void testBlocksOnPendingDep() throws Exception {
		a.setDependencies(new String[] {"b"});
		
		dg.addTarget(a);
		dg.addTarget(b);
		
		mgr.add(dg);
		
		assertSame(b, mgr.getTarget(info1));
		
		assertEquals(null, mgr.getTarget(info2));
		
		mgr.targetCompleted(info1, b, 
				createFakeStatus(b, rev0, Status.PASS, null, null, null));
		
		assertSame(a, mgr.getTarget(info1));
	}

	public void testGetsNextOnPendingDep() throws Exception {
		a.setDependencies(new String[] {"b"});
		
		dg.addTarget(a);
		dg.addTarget(b);
		
		mgr.add(dg);
		mgr.add(c);
		
		assertSame(b, mgr.getTarget(info1));
		
		assertSame(c, mgr.getTarget(info2));
		
		BuildDaemonInfoDto info3 = (BuildDaemonInfoDto) info2.copy();
		info3.setName("other");
		
		assertSame(null, mgr.getTarget(info3));
		
		mgr.targetCompleted(info1, b,
				createFakeStatus(b, rev0, Status.PASS, null, null, null));
		
		assertSame(a, mgr.getTarget(info3));
		
		assertSame(null, mgr.getTarget(info1));
	}
	
	public void testFlush() throws Exception {
		a.setDependencies(new String[] {"b"});
		
		dg.addTarget(a);
		dg.addTarget(b);
		
		mgr.add(dg);
		mgr.add(c);
		
		assertSame(b, mgr.getTarget(info1));
		
		mgr.clear();
		
		assertSame(null, mgr.getTarget(info2));
		
		mgr.targetCompleted(info1, b,
				createFakeStatus(b, rev0, Status.PASS, null, null, null));
		
		assertSame(null, mgr.getTarget(info2));
	}

	public void testFiresOnDependencyFailure() throws Exception {
		a.setDependencies(new String[] {"b"});
		
		dg.addTarget(a);

		mgr.add(dg);

		assertEquals(0, fireCount);
		
		assertEquals(null, mgr.getTarget(info1));
		
		assertEquals(1, fireCount);
		
		final ProjectStatusDto status = mgr.getProjectStatus().get(a.getName());
		assertEquals(a.getName(), status.getName());
		assertTrue(status.isFail());
		assertNotNull(status.getCompletionDate());
		assertEquals("messages.dependency.missing", status.getMessageKey());
		assertEquals(2, status.getMessageArgs().length);
	}

	public void testFiresTwiceOnDependencyCycle() throws Exception {
		b.setDependencies(new String[] {"a"});
		a.setDependencies(new String[] {"b"});
		
		dg.addTarget(b);
		dg.addTarget(a);

		mgr.add(dg);

		assertEquals(0, fireCount);
		assertNull(mgr.getProjectStatus().get("a"));
		assertNull(mgr.getProjectStatus().get("b"));
		
		assertEquals(null, mgr.getTarget(info1));
		
		assertEquals(1, fireCount);
		
		assertEquals(null, mgr.getTarget(info2));
		
		assertEquals(2, fireCount);
		
		assertNotNull(mgr.getProjectStatus().get("a"));
		assertNotNull(mgr.getProjectStatus().get("b"));
	}
	public void testFiresOnDependencyFailed() throws Exception {
		b.setDependencies(new String[] {"a"});
		b.setBuildOnDependencyFailure(false);
		
		dg.addTarget(b);
		dg.addTarget(a);

		mgr.add(dg);

		assertEquals(0, fireCount);
		
		assertSame(a, mgr.getTarget(info1));
		
		assertEquals(0, fireCount);

		mgr.targetCompleted(info1, a,
				createFakeStatus(a, rev0, Status.FAIL, null, null, null));
		
		assertEquals(1, fireCount);
		
		assertEquals(null, mgr.getTarget(info1));
		
		assertEquals(2, fireCount);
	}
	public void testReturnsNullAfterDependencyException() throws Exception {
		a.setDependencies(new String[] {"noexist"});
		b.setDependencies(new String[] {"c"});
		
		dg.addTarget(c);
		dg.addTarget(a);
		dg.addTarget(b);

		mgr.add(dg);

		assertEquals(0, fireCount);
		
		assertSame(c, mgr.getTarget(info1));
		
		assertEquals(0, fireCount);

		mgr.targetCompleted(info1, c, 
				createFakeStatus(c, rev0, Status.PASS, null, null, null));
		
		assertEquals(1, fireCount);
		
		assertSame(b, mgr.getTarget(info1));

		assertEquals(1, fireCount);
		
		mgr.targetCompleted(info1, b, 
				createFakeStatus(b, rev0, Status.PASS, null, null, null));
		
		assertEquals(2, fireCount);
		
		assertSame(null, mgr.getTarget(info1));

		assertEquals(3, fireCount);
	}
	public void testIsBuildingOrInQueue() throws Exception {
		dg.addTarget(a);
		
		mgr.add(dg);
		mgr.add(c);
		
		assertTrue(mgr.isBuildingOrInQueue(a.getName()));
		assertFalse(mgr.isBuildingOrInQueue(b.getName()));
		assertTrue(mgr.isBuildingOrInQueue(c.getName()));
		
		mgr.getTarget(info1);
		
		assertTrue(mgr.isBuildingOrInQueue(a.getName()));
	}
	public void testGetPendingTargets() throws Exception {
		dg.addTarget(a);
		dg.addTarget(b);
		
		mgr.add(dg);
		mgr.add(c);
		
		ProjectStatusDto[] pending = mgr.getPendingTargets();
		assertEquals(3, pending.length);
		
		assertTrue(pending[0].isInQueue());
		assertTrue(pending[1].isInQueue());
		assertTrue(pending[2].isInQueue());
		
		ProjectConfigDto project = mgr.getTarget(info1);
		
		pending = mgr.getPendingTargets();
		assertEquals(3, pending.length);
		
		assertSame(project.getName(), pending[0].getName());
		assertTrue(pending[0].isBuilding());
		assertTrue(pending[1].isInQueue());
		assertTrue(pending[2].isInQueue());
		
		mgr.targetCompleted(info1, project, 
				createFakeStatus(project, rev0, Status.PASS, null, null, null));
		
		project = mgr.getTarget(info1);
		
		pending = mgr.getPendingTargets();
		assertEquals(2, pending.length);
		
		assertSame(project.getName(), pending[0].getName());
		assertTrue(pending[0].isBuilding());
		assertTrue(pending[1].isInQueue());
		
		mgr.targetCompleted(info1, project,
				createFakeStatus(project, rev0, Status.PASS, null, null, null));

		project = mgr.getTarget(info1);
		
		pending = mgr.getPendingTargets();
		assertEquals(1, pending.length);
		
		assertSame(project.getName(), pending[0].getName());
		assertTrue(pending[0].isBuilding());
		
		mgr.targetCompleted(info1, project,
				createFakeStatus(project, rev0, Status.PASS, null, null, null));

		assertEquals(0, mgr.getPendingTargets().length);
	}
	public void testDoesNotAllowGroupWithSameNameTwice() throws Exception {
		dg.addTarget(a);
		dg.addTarget(b);
		
		dg.setName("a");
		
		mgr.add(dg);
		
		try {
			mgr.add(dg);
			fail("expected exception");
		} catch (AlreadyScheduledException e) {
		}
		
		ProjectConfigDto project = mgr.getTarget(info1);
		mgr.targetCompleted(info1, project,
				createFakeStatus(project, rev0, Status.PASS, null, null, null));

		try {
			mgr.add(dg);
			fail("expected exception");
		} catch (AlreadyScheduledException e) {
		}

		project = mgr.getTarget(info1);
		mgr.targetCompleted(info1, project,
				createFakeStatus(project, rev0, Status.PASS, null, null, null));

		dg.addTarget(a);
		dg.addTarget(b);

		try {
			mgr.add(dg);
		} catch (AlreadyScheduledException e) {
			fail("did not expect exception");
		}
	}
	public void testAllowsGroupWithSameNameTwiceOnOverride() throws Exception {
		dg.addTarget(a);
		dg.addTarget(b);
		
		dg.setName("a");
		
		DependencyGroupImpl dg2 = new DependencyGroupImpl();
		dg2.setName("a");
		dg2.addTarget(a);
		
		dg2.setManualBuild(true);
		
		mgr.add(dg);
		mgr.add(dg2);
	}
	public void testTargetNotBuiltLeavesPreviousStatus() throws Exception {
		ProjectStatusDto prev = new ProjectStatusDto();
		prev.setStatus(Status.PASS);
		prev.setBuildNumber(17);
		
		final Map<String, ProjectStatusDto> map = new HashMap<String, ProjectStatusDto>(); 
		map.put(a.getName(), prev);
		map.put(b.getName(), prev);
		
		cache.mergeOutcomes(map);
		mgr.init(mgrConfig);
		
		b.setDependencies(new String[] {a.getName()});
		dg.addTarget(a);
		dg.addTarget(b);
		
		mgr.add(dg);

		final ProjectConfigDto a = mgr.getTarget(info1);
		assertEquals(this.a, a);
		
		assertEquals(0, fireCount);
		
		mgr.targetCompleted(info1, a,
				createFakeStatus(a, null, Status.UP_TO_DATE, null, null, null));
		
		assertEquals(0, fireCount);
		
		assertEquals(b, mgr.getTarget(info1));
		
		mgr.targetCompleted(info1, b, 
				createFakeStatus(b, rev0, Status.PASS, null, null, null));
		
		assertEquals(1, fireCount);
		assertEquals(null, mgr.getTarget(info1));
		
		final ProjectStatusDto newestA = cache.getLatestOutcome(a.getName());
		final ProjectStatusDto newestB = cache.getLatestOutcome(b.getName());
		
		assertSame(prev, newestA);
		assertNotSame(prev, newestB);
	}
	public void testTargetNotPassPreservesLastGoodBuildNumber() throws Exception {
		ProjectStatusDto prev = new ProjectStatusDto();
		prev.setStatus(Status.PASS);
		prev.setBuildNumber(51);
		
		prev.setLastGoodBuildNumber(45);
		prev.setLastKnownRevision(rev1);
		
		final Map<String, ProjectStatusDto> map = new HashMap<String, ProjectStatusDto>(); 
		map.put(a.getName(), prev);
		
		cache.mergeOutcomes(map);
		mgr.init(mgrConfig);
		
		b.setDependencies(new String[] {a.getName()});
		dg.addTarget(a);
		
		mgr.add(dg);

		final ProjectConfigDto a = mgr.getTarget(info1);
		assertEquals(this.a, a);
		
		assertEquals(0, fireCount);
		
		mgr.targetCompleted(info1, a,
				createFakeStatus(a, null, Status.SKIP, null, null, null));
		
		assertEquals(1, fireCount);
		
		assertEquals(null, mgr.getTarget(info1));
		
		assertEquals(1, fireCount);
		assertEquals(null, mgr.getTarget(info1));
		
		final ProjectStatusDto cur = cache.getLatestOutcome(a.getName());
		assertNotSame(prev, cur);
		
		assertSame(prev.getLastGoodBuildNumber(), cur.getLastGoodBuildNumber());
		assertSame(prev.getLastKnownRevision(), cur.getLastKnownRevision());
		assertNull(cur.getRevision());
		assertEquals((Integer)52, cur.getBuildNumber());
	}
	public void testTargetNotPassNoPreviousBuild() throws Exception {
		mgr.init(mgrConfig);
		
		dg.addTarget(a);
		
		mgr.add(dg);

		mgr.getTarget(info1);
		
		mgr.targetCompleted(info1, a,
				createFakeStatus(a, null, Status.FAIL, null, null, null));
		
		final ProjectStatusDto cur = cache.getLatestOutcome(a.getName());
		
		assertEquals(null, cur.getLastGoodBuildNumber());
	}
	public void testTargetNotBuiltMakesDependentsSkip() throws Exception {
		ProjectStatusDto prev = new ProjectStatusDto();
		prev.setStatus(Status.FAIL);
		prev.setBuildNumber(64);
		
		final Map<String, ProjectStatusDto> map = new HashMap<String, ProjectStatusDto>(); 
		map.put(a.getName(), prev);
		map.put(b.getName(), prev);

		cache.mergeOutcomes(map);
		mgr.init(mgrConfig);

		b.setDependencies(new String[] {"a"});
		dg.addTarget(a);
		dg.addTarget(b);
		
		mgr.add(dg);

		final ProjectConfigDto a = mgr.getTarget(info1);
		assertEquals(this.a, a);
		
		assertEquals(0, fireCount);
		
		mgr.targetCompleted(info1, a,
				createFakeStatus(a, null, Status.UP_TO_DATE, null, null, null));
		
		assertEquals(0, fireCount);
		
		assertEquals(null, mgr.getTarget(info1));
		
		assertEquals(1, fireCount);
		
		final ProjectStatusDto newestA = cache.getLatestOutcome(a.getName());
		final ProjectStatusDto newestB = cache.getLatestOutcome(b.getName());
		
		assertSame(prev, newestA);
		assertNotSame(prev, newestB);
		assertEquals((Integer)65, newestB.getBuildNumber());
	}
	
	public void testThrowsOnEmptyGroup() throws Exception {
		DependencyGroup dg = new DependencyGroupImpl();
		
		try {
			mgr.add(dg);
			fail("expected exception");
		} catch (Exception e) {
			
		}
	}
	
	public void testGetLatestStatusShortCircuitsOnNull() throws Exception {
		assertNull(mgr.getLatestStatus("nonesuch"));
	}
	
	public void testGetTargetWarnsOnDuplicateProject() throws Exception {
		dg.addTarget(a);

		DependencyGroupImpl dg2 = new DependencyGroupImpl();
		
		dg2.addTarget(a);
		dg2.addTarget(b);
		dg2.addTarget(c);
		
		
		mgr.add(dg);
		mgr.add(dg2);
		
		assertSame(a, mgr.getTarget(info1));

		assertNull(event);
		
		assertNull(mgr.getTarget(info2));
		
		assertNotNull(event);
		
		assertEquals("warnings.group.removed", event.getKey());
		assertEquals("a", event.getArgs()[0]);
		assertEquals("b, c", event.getArgs()[1]);
	}
	
	public void testGetTargetWarnsOnDuplicateProjectPurgesGroupAndProceeds() throws Exception {
		DependencyGroupImpl dg2 = new DependencyGroupImpl();
		DependencyGroupImpl dg3 = new DependencyGroupImpl();
		
		dg.addTarget(a);
		dg2.addTarget(a);
		dg2.addTarget(b);
		
		dg3.addTarget(c);
		
		mgr.add(dg);
		mgr.add(dg2);
		mgr.add(dg3);
		
		assertSame(a, mgr.getTarget(info1));

		assertSame(c, mgr.getTarget(info2));
		
		assertNotNull(event);
		
		assertEquals("warnings.group.removed", event.getKey());
		assertEquals("a", event.getArgs()[0]);
		assertEquals("b", event.getArgs()[1]);
	}
	
	public void testGetTargetWarnsOnDuplicateProjectProceeds() throws Exception {
		DependencyGroupImpl dg2 = new DependencyGroupImpl();
		DependencyGroupImpl dg3 = new DependencyGroupImpl();
		
		dg.addTarget(a);
		dg2.addTarget(a);
		
		dg3.addTarget(c);
		
		mgr.add(dg);
		mgr.add(dg2);
		mgr.add(dg3);
		
		assertSame(a, mgr.getTarget(info1));

		assertSame(c, mgr.getTarget(info2));
		
		assertNotNull(event);
		
		assertEquals("warnings.duplicate.target.removed", event.getKey());
		assertEquals("a", event.getArgs()[0]);
	}
	
	public void testGetStatusByBuildNumber() throws Exception {
		int buildNumber = 5545;
		ProjectStatusDto status = new ProjectStatusDto();
		
		status.setName(a.getName());
		status.setBuildNumber(buildNumber);
		cache.store(status);
		
		assertSame(status, mgr.getStatusByBuildNumber(a.getName(), buildNumber));
	}
	
	public void testGetStatusByBuildNumberWhenBuilding() throws Exception {
		int buildNumber = 5545;
		ProjectStatusDto status = new ProjectStatusDto();
		
		status.setName(a.getName());
		status.setBuildNumber(buildNumber);

		dg.addTarget(a);

		mgr.add(dg);

		assertSame(a, mgr.getTarget(info1));
		mgr.registerBuildStatus(info1, a, status);
		
		assertSame(status, mgr.getStatusByBuildNumber(a.getName(), buildNumber));
	}
	
	public void testGetStatusByBuildNumberWhenBuildingNoMatch() throws Exception {
		int buildNumber = 5545;
		ProjectStatusDto status = new ProjectStatusDto();
		
		status.setName(a.getName());
		status.setBuildNumber(buildNumber);

		dg.addTarget(a);

		mgr.add(dg);

		assertSame(a, mgr.getTarget(info1));
		mgr.registerBuildStatus(info1, a, status);
		
		assertEquals(null, mgr.getStatusByBuildNumber(a.getName(), buildNumber + 1));
	}
	
	/**
	 * @see http://code.google.com/p/vulcan/issues/detail?id=146
	 */
	public void testBlocksDependencyFromRebuilding() throws Exception {
		ProjectStatusDto status = new ProjectStatusDto();
		status.setName(a.getName());
		status.setStatus(Status.PASS);
		status.setBuildNumber(11);
		
		cache.store(status);
		
		b.setDependencies(new String[] {a.getName()});
		
		dg.addTarget(b);
		
		mgr.add(dg);
		
		assertSame(b, mgr.getTarget(info1));

		dg = new DependencyGroupImpl();
		
		dg.addTarget(a);
		
		mgr.add(dg);

		assertSame(null, mgr.getTarget(info2));
		
		status = new ProjectStatusDto();
		
		status.setName(a.getName());
		status.setBuildNumber(12);
		status.setStatus(Status.PASS);
		
		mgr.targetCompleted(info1, b, status);
		
		assertSame(a, mgr.getTarget(info1));

		mgr.targetCompleted(info1, a, status);

		assertSame(null, mgr.getTarget(info1));
	}
	
	private ProjectStatusDto createFakeStatus(ProjectConfigDto config, RevisionTokenDto rev, Status status1, String key, Object[] args, ChangeLogDto changeLog) {
		final ProjectStatusDto st = new ProjectStatusDto();
		st.setName(config.getName());
		st.setRevision(rev);
		st.setStatus(status1);
		st.setMessageKey(key);
		st.setMessageArgs(args);
		st.setChangeLog(changeLog);
		return st;
	}
}
