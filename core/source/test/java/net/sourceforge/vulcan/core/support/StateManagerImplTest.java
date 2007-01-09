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
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dto.BuildManagerConfigDto;
import net.sourceforge.vulcan.dto.Date;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.exception.ProjectNeedsDependencyException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class StateManagerImplTest extends StateManagerTestBase
{
	public void testThrowsWhenListIsNull() {
		try {
			stateMgr.getProjectConfig("name");
			fail("Didn't throw on non-existent project");
		} catch (NoSuchProjectException e) {
			assertEquals("name", e.getMessage());
		}
	}
	public void testGetProjectConfig() {
		ProjectConfigDto proj = new ProjectConfigDto();
		proj.setName("a");
		
		stateMgr.getConfig().setProjects(new ProjectConfigDto[] {proj});
		
		assertSame(proj, stateMgr.getProjectConfig("a"));
		
		try {
			stateMgr.getProjectConfig("name");
			fail("Didn't throw on non-existent project");
		} catch (NoSuchProjectException e) {
			assertEquals("name", e.getMessage());
		}
	}
	public void testShutdownDoesWriteConfig() throws StoreException {
		stateMgr.running = true;
		
		assertFalse(store.isCommitCalled());
		
		stateMgr.shutdown();
		
		assertTrue(store.isCommitCalled());
	}
	public void testShutdownIgnoredWhenNotRunning() throws StoreException {
		stateMgr.running = false;
		
		assertFalse(store.isCommitCalled());
		
		stateMgr.shutdown();
		
		assertFalse(store.isCommitCalled());
	}
	public void testStartInitsBuildMgr() throws Exception {
		BuildManagerConfigDto mgrConfig = new BuildManagerConfigDto();
		stateManagerConfig.setBuildManagerConfig(mgrConfig);

		buildManagerConfig = null;

		stateMgr.start();
		assertSame(mgrConfig, buildManagerConfig);
	}
	public void testAddProject() throws Exception {
		final ProjectConfigDto config = createProjectDto("a");
		
		assertNull(config.getLastModificationDate());
		
		assertEquals(0, stateMgr.getConfig().getProjects().length);
		
		stateMgr.addProjectConfig(config);
		
		assertEquals(1, stateMgr.getConfig().getProjects().length);
		
		assertTrue(store.isCommitCalled());
		
		assertNotNull(config.getLastModificationDate());
	}
	public void testAddProjectThrowsOnDuplicateName() throws Exception {
		final ProjectConfigDto config = createProjectDto("a");
		
		assertEquals(0, stateMgr.getConfig().getProjects().length);
		
		stateMgr.addProjectConfig(config);
		
		assertEquals(1, stateMgr.getConfig().getProjects().length);
		
		final ProjectConfigDto copy = (ProjectConfigDto) config.copy();
		copy.setLastModificationDate(null);

		try {
			stateMgr.addProjectConfig(copy);
			fail("expected exception");
		} catch (DuplicateNameException e) {
		}
		assertEquals(1, stateMgr.getConfig().getProjects().length);
		assertNull(copy.getLastModificationDate());
	}
	public void testUpdateProjectThrowsOnDuplicateName() throws Exception {
		final ProjectConfigDto a = createProjectDto("a");
		final ProjectConfigDto b = createProjectDto("b");
		
		b.setLastModificationDate(new Date());
		
		stateMgr.getConfig().setProjects(new ProjectConfigDto[] {a, b});
		
		final ProjectConfigDto c = (ProjectConfigDto) b.copy();
		c.setName(a.getName());

		try {
			stateMgr.updateProjectConfig(b.getName(), c, true);
			fail("expected exception");
		} catch (DuplicateNameException e) {
		}
		assertEquals(a, stateMgr.getConfig().getProjects()[0]);
		assertEquals(b, stateMgr.getConfig().getProjects()[1]);
		assertSame(b.getLastModificationDate(), c.getLastModificationDate());
	}
	public void testRenameProjectUpdatesDependencies() throws Exception {
		final ProjectConfigDto a = createProjectDto("a");
		final ProjectConfigDto b = createProjectDto("b");
		
		a.setLastModificationDate(new Date(0));
		
		b.setDependencies(new String[] {"a"});
		
		stateMgr.getConfig().setProjects(new ProjectConfigDto[] {a, b});

		final ProjectConfigDto update = (ProjectConfigDto) a.copy();
		update.setName("a+");
		
		pluginMgr.projectNameChanged(a.getName(), update.getName());
		replay();
		
		stateMgr.updateProjectConfig(a.getName(), update, true);
		
		verify();
		
		final String[] dependencies = stateMgr.getProjectConfig(b.getName()).getDependencies();
		assertEquals(1, dependencies.length);
		assertEquals(update.getName(), dependencies[0]);
		
		assertTrue("Did not set new modification date on update.", a.getLastModificationDate().before(update.getLastModificationDate()));
	}
	public void testSortsOnAddProject() throws Exception {
		final ProjectConfigDto a = createProjectDto("a");
		final ProjectConfigDto b = createProjectDto("b");
		final ProjectConfigDto c = createProjectDto("c");
		
		stateMgr.addProjectConfig(c);
		assertSame(c, stateMgr.getConfig().getProjects()[0]);
		
		stateMgr.addProjectConfig(a);
		assertSame(a, stateMgr.getConfig().getProjects()[0]);
		assertSame(c, stateMgr.getConfig().getProjects()[1]);
		
		stateMgr.addProjectConfig(b);
		assertSame(a, stateMgr.getConfig().getProjects()[0]);
		assertSame(b, stateMgr.getConfig().getProjects()[1]);
		assertSame(c, stateMgr.getConfig().getProjects()[2]);
	}
	public void testUpdateProjectThrowsOnNotFound() throws Exception {
		final ProjectConfigDto a = createProjectDto("a");
		final ProjectConfigDto a2 = (ProjectConfigDto) a.copy();

		try {
			stateMgr.updateProjectConfig(a.getName(), a2, true);
			fail("exected exception");
		} catch (NoSuchProjectException e) {
			assertEquals("a", e.getMessage());
		}
		
		assertFalse(store.isCommitCalled());

		try {
			stateMgr.updateProjectConfig(a.getName(), a2, true);
			fail("exected exception");
		} catch (NoSuchProjectException e) {
			assertEquals("a", e.getMessage());
		}
		assertFalse(store.isCommitCalled());
	}
	public void testUpdateProjectSaves() throws Exception {
		final ProjectConfigDto a = createProjectDto("a");
		final ProjectConfigDto a2 = (ProjectConfigDto) a.copy();
		
		stateMgr.addProjectConfig(a);
		
		store.setCommitCalled(false);
		
		stateMgr.updateProjectConfig(a.getName(), a2, true);
		
		assertTrue(store.isCommitCalled());
	}
	public void testDeleteProject() throws Exception {
		final ProjectConfigDto a = createProjectDto("a");
		stateMgr.config.setProjects(new ProjectConfigDto[] {a});
		
		store.setCommitCalled(false);

		assertEquals(1, stateMgr.config.getProjects().length);
		stateMgr.deleteProjectConfig(a.getName());
		assertEquals(0, stateMgr.config.getProjects().length);
		assertTrue(store.isCommitCalled());
	}
	public void testDeleteProjectLeavesOthers() throws Exception {
		final ProjectConfigDto a = createProjectDto("a");
		final ProjectConfigDto b = createProjectDto("b");
		final ProjectConfigDto c = createProjectDto("c");
		
		stateMgr.addProjectConfig(a);
		stateMgr.addProjectConfig(b);
		stateMgr.addProjectConfig(c);
		
		assertSame(a, stateMgr.config.getProjects()[0]);
		assertSame(b, stateMgr.config.getProjects()[1]);
		assertSame(c, stateMgr.config.getProjects()[2]);
		
		stateMgr.deleteProjectConfig(b.getName());
		
		assertEquals(2, stateMgr.config.getProjects().length);
		assertSame(a, stateMgr.config.getProjects()[0]);
		assertSame(c, stateMgr.config.getProjects()[1]);

		stateMgr.deleteProjectConfig(c.getName());
		stateMgr.deleteProjectConfig(a.getName());
		
		assertEquals(0, stateMgr.config.getProjects().length);
	}
	public void testDeleteProjectThrowsOnDependency() throws Exception {
		final ProjectConfigDto a = createProjectDto("a");
		final ProjectConfigDto b = createProjectDto("b");

		b.setDependencies(new String[] {"a"});
		
		stateMgr.addProjectConfig(a);
		stateMgr.addProjectConfig(b);
		
		try {
			stateMgr.deleteProjectConfig(a.getName());
			fail("expected exception");
		} catch (ProjectNeedsDependencyException e) {
			assertEquals(a.getName(), e.getProjectToDelete());
			assertEquals(b.getName(), e.getDependantProject());
		}
		
		assertEquals(2, stateMgr.config.getProjects().length);
	}
	public void testUpdateProject() throws Exception {
		final ProjectConfigDto a = createProjectDto("a");
		final ProjectConfigDto a2 = (ProjectConfigDto) a.copy();
		
		stateMgr.addProjectConfig(a);
		assertSame(a, stateMgr.getConfig().getProjects()[0]);
		
		store.setCommitCalled(false);
		assertTrue(a.equals(a));
		assertEquals(1, stateMgr.getConfig().getProjects().length);
		stateMgr.updateProjectConfig(a.getName(), a2, true);
		assertEquals(1, stateMgr.getConfig().getProjects().length);
		assertSame(a2, stateMgr.getConfig().getProjects()[0]);
		
		assertTrue(store.isCommitCalled());
		assertNotNull(a2.getLastModificationDate());
	}
	public void testUpdateProjectSorts() throws Exception {
		final ProjectConfigDto a = createProjectDto("a");
		final ProjectConfigDto b = createProjectDto("b");
		final ProjectConfigDto c = createProjectDto("c");
		final ProjectConfigDto d = (ProjectConfigDto) a.copy();
		
		stateMgr.addProjectConfig(a);
		stateMgr.addProjectConfig(b);
		stateMgr.addProjectConfig(c);

		assertSame(a, stateMgr.getConfig().getProjects()[0]);
		assertSame(b, stateMgr.getConfig().getProjects()[1]);
		assertSame(c, stateMgr.getConfig().getProjects()[2]);
		
		d.setName("d");
		
		pluginMgr.projectNameChanged(a.getName(), d.getName());
		replay();

		stateMgr.updateProjectConfig(a.getName(), d, true);

		verify();
		
		assertSame(b, stateMgr.getConfig().getProjects()[0]);
		assertSame(c, stateMgr.getConfig().getProjects()[1]);
		assertSame(d, stateMgr.getConfig().getProjects()[2]);
	}
	public void testFlushDelegates() throws Exception {
		assertEquals(0, buildMgrClearCallCount);
		stateMgr.flushBuildQueue();
		assertEquals(1, buildMgrClearCallCount);
	}
	public void testUpdatePluginConfig() throws Exception {
		final PluginConfigDto pluginConfig = new FakePluginConfig();
		
		assertNull(pluginConfig.getLastModificationDate());
		
		stateMgr.updatePluginConfig(pluginConfig);
		
		assertNotNull(pluginConfig.getLastModificationDate());
	}
	public void testGetPluginModificationDateNull() throws Exception {
		final PluginConfigDto cfg = new FakePluginConfig();
		
		stateMgr.config.getPluginConfigs().put(cfg.getPluginId(), cfg);
		
		assertEquals(null, stateMgr.getPluginModificationDate(cfg.getPluginId()));
	}
	public void testGetPluginModificationDateNoConfig() throws Exception {
		assertEquals(null, stateMgr.getPluginModificationDate("none.such"));
	}
	public void testGetPluginModificationDate() throws Exception {
		final PluginConfigDto cfg = new FakePluginConfig();
		cfg.setLastModificationDate(new Date());
		
		stateMgr.config.getPluginConfigs().put(cfg.getPluginId(), cfg);
		
		assertEquals(cfg.getLastModificationDate(), stateMgr.getPluginModificationDate(cfg.getPluginId()));
	}
	private ProjectConfigDto createProjectDto(String name) {
		final ProjectConfigDto config = new ProjectConfigDto();
		config.setName(name);
		return config;
	}
	private class FakePluginConfig extends PluginConfigDto {
		@Override
		public String getPluginId() {
			return "a.b.c.plugin";
		}

		@Override
		public String getPluginName() {
			return "a plugin";
		}

		@Override
		public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
			return null;
		}
	}
}
