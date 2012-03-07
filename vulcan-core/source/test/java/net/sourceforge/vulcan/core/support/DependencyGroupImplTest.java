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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;


public class DependencyGroupImplTest extends TestCase {
	DependencyGroup dg = new DependencyGroupImpl();
	
	public void testConstructFromProjectConfig() throws Exception {
		ProjectConfigDto config = createConfigDto("a");
		
		assertTrue(dg.isEmpty());
		
		dg.addTarget(config);
		
		assertFalse(dg.isEmpty());
		
		assertSame(config, dg.getNextTarget().getProjectConfig());
		
		assertTrue(dg.isEmpty());
		
		try {
			dg.getNextTarget();
			fail("expected exception");
		} catch (IllegalStateException e) {
		}
	}
	
	public void testBlocksOnUnbuiltDependency() throws Exception {
		ProjectConfigDto a = createConfigDto("a");
		ProjectConfigDto b = createConfigDto("b", new String[] {"a"});
		
		dg.addTarget(b);
		dg.addTarget(a);
		
		assertFalse(dg.isBlocked());
		
		assertSame(a, dg.getNextTarget().getProjectConfig());
		
		assertTrue(dg.isBlocked());
		
		try {
			dg.getNextTarget();
			fail("expected exception");
		} catch (PendingDependencyException e) {
		}
		
		dg.targetCompleted(a, true);
		
		assertFalse(dg.isBlocked());
		
		assertSame(b, dg.getNextTarget().getProjectConfig());
		
		assertTrue(dg.isEmpty());
	}

	public void testThrowsWhenDependencyFailed() throws Exception {
		ProjectConfigDto a = createConfigDto("a");
		ProjectConfigDto b = createConfigDto("b", new String[] {"a"});
		
		dg.addTarget(b);
		dg.addTarget(a);
		
		assertFalse(dg.isBlocked());
		
		assertSame(a, dg.getNextTarget().getProjectConfig());
		
		assertTrue(dg.isBlocked());
		
		dg.targetCompleted(a, false);
		
		try {
			dg.isBlocked();
			fail("expected exception");
		} catch (DependencyFailureException e) {
			assertSame(b, e.getProjectConfig());
			assertEquals("a", e.getDependencyName());
		}
		
		assertTrue(dg.isEmpty());
	}
	public void testIgnoresFailedDependencyOnFlag() throws Exception {
		ProjectConfigDto a = createConfigDto("a");
		ProjectConfigDto b = createConfigDto("b", new String[] {"a"});
		
		b.setBuildOnDependencyFailure(true);
		
		dg.addTarget(b);
		dg.addTarget(a);
		
		assertFalse(dg.isBlocked());
		
		assertSame(a, dg.getNextTarget().getProjectConfig());
		
		assertTrue(dg.isBlocked());
		
		dg.targetCompleted(a, false);
		
		assertSame(b, dg.getNextTarget().getProjectConfig());
		
		assertTrue(dg.isEmpty());
	}
	public void testIgnoresMissingDependencyOnFlag() throws Exception {
		ProjectConfigDto a = createConfigDto("a", new String[] {"b"});
		
		a.setBuildOnDependencyFailure(true);
		
		dg.addTarget(a);
		
		assertFalse(dg.isBlocked());
		
		assertSame(a, dg.getNextTarget().getProjectConfig());
		
		dg.targetCompleted(a, true);
		
		assertTrue(dg.isEmpty());
	}
	public void testThrowsWhenDependencyNotBuilt() throws Exception {
		ProjectConfigDto b = createConfigDto("b", new String[] {"a"});
		
		dg.addTarget(b);
		
		try {
			dg.getNextTarget();
			fail("expected exception");
		} catch (DependencyMissingException e) {
			assertSame(b, e.getProjectConfig());
			assertEquals("a", e.getDependencyName());
		}
		
		assertTrue(dg.isEmpty());
	}
	public void testPrepopulateBuildStatus() throws Exception {
		ProjectConfigDto a = createConfigDto("a");
		ProjectConfigDto b = createConfigDto("b", new String[] {"a"});
		
		ProjectStatusDto status = new ProjectStatusDto();
		status.setName(a.getName());
		status.setStatus(ProjectStatusDto.Status.PASS);
		
		dg.initializeBuildResults(Collections.singletonMap(a.getName(), status));
		
		dg.addTarget(b);
		
		assertSame(b, dg.getNextTarget().getProjectConfig());

		dg.targetCompleted(b, true);
		
		assertTrue(dg.isEmpty());
	}
	public void testThrowsWhenDependencyCycle() throws Exception {
		ProjectConfigDto a = createConfigDto("a", new String[] {"b"});
		ProjectConfigDto b = createConfigDto("b", new String[] {"a"});
		ProjectConfigDto c = createConfigDto("c", new String[] {"a"});
		
		dg.addTarget(a);
		dg.addTarget(b);
		dg.addTarget(c);
		
		try {
			dg.getNextTarget();
			fail("expected exception");
		} catch (DependencyCycleException e) {
			assertSame(a, e.getProjectConfig());
			assertEquals("b", e.getDependencyName());
		}
		
		try {
			dg.getNextTarget();
			fail("expected exception");
		} catch (DependencyMissingException e) {
			fail("threw wrong exception");
		} catch (DependencyCycleException e) {
			assertEquals("a", e.getDependencyName());
			assertSame(b, e.getProjectConfig());
		}

		try {
			dg.getNextTarget();
			fail("expected exception");
		} catch (DependencyMissingException e) {
			fail("threw wrong exception");
		} catch (DependencyCycleException e) {
			assertEquals("a", e.getDependencyName());
			assertSame(c, e.getProjectConfig());
		}
		assertTrue(dg.isEmpty());
	}

	public void testPutsPendingDependenciesFirst() throws Exception {
		final ProjectConfigDto a = createConfigDto("a");
		final ProjectConfigDto b = createConfigDto("b", new String[] {"a"});
		final ProjectConfigDto c = createConfigDto("c", new String[] {"b"});
		
		dg.addTarget(a);
		dg.addTarget(c);
		dg.addTarget(b);
		
		ProjectConfigDto t = dg.getNextTarget().getProjectConfig();
		assertSame(a, t);
		
		try {
			dg.getNextTarget();
			fail("expected exception");
		} catch (PendingDependencyException e) {
		}
		dg.targetCompleted(t, true);
		
		t = dg.getNextTarget().getProjectConfig();
		assertSame(b, t);
		
		try {
			dg.getNextTarget();
			fail("expected exception");
		} catch (PendingDependencyException e) {
		}
		dg.targetCompleted(t, true);

		t = dg.getNextTarget().getProjectConfig();
		assertSame(c, t);
		
		assertTrue(dg.isEmpty());
	}
	public void testGetPendingTargets() throws Exception {
		final ProjectConfigDto a = createConfigDto("a");
		final ProjectConfigDto b = createConfigDto("b", new String[] {"a"});
		final ProjectConfigDto c = createConfigDto("c", new String[] {"b"});
		
		dg.addTarget(a);
		dg.addTarget(c);
		dg.addTarget(b);
		
		final List<ProjectStatusDto> pending = dg.getPendingTargets();
		
		assertNotNull(pending);
		assertEquals(3, pending.size());
		
		assertTrue(pending.get(0).isInQueue());
		assertTrue(pending.get(1).isInQueue());
		assertTrue(pending.get(2).isInQueue());
		
		ProjectConfigDto cfg = dg.getNextTarget().getProjectConfig();
		
		assertEquals(2, dg.getPendingTargets().size());
		
		dg.targetCompleted(cfg, true);
		
		assertEquals(2, dg.getPendingTargets().size());
		
		cfg = dg.getNextTarget().getProjectConfig();
		
		assertEquals(1, dg.getPendingTargets().size());
		
		dg.targetCompleted(cfg, true);
		
		assertEquals(1, dg.getPendingTargets().size());
		
		cfg = dg.getNextTarget().getProjectConfig();
		
		assertEquals(0, dg.getPendingTargets().size());
	}
	public void testSortStable() throws Exception {
		final ProjectConfigDto a = createConfigDto("a");
		final ProjectConfigDto b = createConfigDto("b", new String[] {"a"});
		final ProjectConfigDto c = createConfigDto("c", new String[] {"b"});
		
		dg.addTarget(b);
		dg.addTarget(c);
		dg.addTarget(a);
		
		assertEquals(Arrays.asList(a, b, c), dg.getPendingProjects());
	}
	public void testSortMovesNonDependentUp() throws Exception {
		final ProjectConfigDto a = createConfigDto("a", new String[] {"b", "c"});
		final ProjectConfigDto b = createConfigDto("b");
		final ProjectConfigDto c = createConfigDto("c");
		final ProjectConfigDto d = createConfigDto("d", new String[] {"e"});
		final ProjectConfigDto e = createConfigDto("e");
		
		dg.addTarget(a);
		dg.addTarget(d);
		dg.addTarget(b);
		dg.addTarget(c);
		dg.addTarget(e);
		
		assertEquals(Arrays.asList(b, c, e, d, a), dg.getPendingProjects());
	}
	public void testSortDualDep() throws Exception {
		final ProjectConfigDto a = createConfigDto("a", new String[] {"b", "c"});
		final ProjectConfigDto b = createConfigDto("b", new String[] {"c"});
		final ProjectConfigDto c = createConfigDto("c");
		
		dg.addTarget(a);
		dg.addTarget(b);
		dg.addTarget(c);
		
		assertEquals(Arrays.asList(c, b, a), dg.getPendingProjects());
	}
	public void testSortChainedDep() throws Exception {
		final ProjectConfigDto a = createConfigDto("a", new String[] {"b"});
		final ProjectConfigDto b = createConfigDto("b", new String[] {"c"});
		final ProjectConfigDto c = createConfigDto("c");
		
		dg.addTarget(a);
		dg.addTarget(b);
		dg.addTarget(c);
		
		assertEquals(Arrays.asList(c, b, a), dg.getPendingProjects());
	}
	public void testSortChained2() throws Exception {
		final ProjectConfigDto a = createConfigDto("a");
		final ProjectConfigDto b = createConfigDto("b", new String[] {"a"});
		final ProjectConfigDto c = createConfigDto("c", new String[] {"b"});
		
		dg.addTarget(a);
		dg.addTarget(c);
		
		assertEquals(Arrays.asList(a, c), dg.getPendingProjects());
		
		dg.addTarget(b);
		
		assertEquals(Arrays.asList(a, b, c), dg.getPendingProjects());
	}
	public void testSortChained3() throws Exception {
		final ProjectConfigDto a = createConfigDto("a", new String[] {"b"});
		final ProjectConfigDto c = createConfigDto("c", new String[] {"d", "a"});
		final ProjectConfigDto b = createConfigDto("b", new String[] {"d", "e"});
		final ProjectConfigDto d = createConfigDto("d");
		final ProjectConfigDto e = createConfigDto("e");
		
		dg.addTarget(a);
		dg.addTarget(c);
		
		assertEquals(Arrays.asList(a, c), dg.getPendingProjects());
		
		dg.addTarget(b);

		assertEquals(Arrays.asList(b, a, c), dg.getPendingProjects());

		dg.addTarget(d);
		
		assertEquals(Arrays.asList(d, b, a, c), dg.getPendingProjects());
		
		dg.addTarget(e);
		
		assertEquals(Arrays.asList(d, e, b, a, c), dg.getPendingProjects());
	}

	// Issue 71 http://code.google.com/p/vulcan/issues/detail?id=71
	public void testGetsNextOnPendingDepSingleGroup() throws Exception {
		final ProjectConfigDto a = createConfigDto("a");
		final ProjectConfigDto b = createConfigDto("b", new String[] {"a"});
		final ProjectConfigDto c = createConfigDto("c", new String[] {"b"});
		final ProjectConfigDto d = createConfigDto("d", new String[] {"a"});
		
		dg.addTarget(a);
		dg.addTarget(b);
		dg.addTarget(c);
		dg.addTarget(d);
		
		assertEquals(Arrays.asList(a, b, c, d), dg.getPendingProjects());
		
		assertSame(a, dg.getNextTarget().getProjectConfig());
		dg.targetCompleted(a, true);

		assertSame(b, dg.getNextTarget().getProjectConfig());
		
		assertSame(d, dg.getNextTarget().getProjectConfig());
		
		dg.targetCompleted(b, true);
		
		assertSame(c, dg.getNextTarget().getProjectConfig());
	}

	private ProjectConfigDto createConfigDto(String name) {
		return createConfigDto(name, new String[0]);
	}
	private ProjectConfigDto createConfigDto(String name, String[] dependencies) {
		ProjectConfigDto config = new ProjectConfigDto() {
			@Override
			public String toString() {
				return getName();
			}
		};
		config.setName(name);
		config.setDependencies(dependencies);
		return config;
	}
}
