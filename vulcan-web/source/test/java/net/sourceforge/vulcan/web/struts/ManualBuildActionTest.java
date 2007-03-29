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
package net.sourceforge.vulcan.web.struts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.SimplePrincipal;
import net.sourceforge.vulcan.core.DependencyBuildPolicy;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.core.WorkingCopyUpdateStrategy;
import net.sourceforge.vulcan.core.support.DependencyGroupImpl;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.scheduler.BuildDaemon;
import net.sourceforge.vulcan.web.Keys;
import net.sourceforge.vulcan.web.struts.forms.ManualBuildForm;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ManualBuildActionTest extends MockApplicationContextStrutsTestCase {
	ProjectConfigDto[] projects = {new ProjectConfigDto(), new ProjectConfigDto()};
	
	RepositoryAdaptor ra1 = createMock(RepositoryAdaptor.class);
	RepositoryAdaptor ra2 = createMock(RepositoryAdaptor.class);
	
	List<RepositoryTagDto> tags1 = new ArrayList<RepositoryTagDto>();
	List<RepositoryTagDto> tags2 = new ArrayList<RepositoryTagDto>();
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/buildmanagement/manualBuild.do");
		
		context.setAttribute(Keys.STATE_MANAGER, manager);
		
		projects[0].setName("a");
		projects[1].setName("b");
		
		tags1.add(createFakeTag("rc1"));
		tags1.add(createFakeTag("rc2"));
		tags1.add(createFakeTag("rc3"));
		
		tags2.add(createFakeTag("1.1-bug-fix"));
		tags2.add(createFakeTag("1.2"));
		tags2.add(createFakeTag("2.0"));
		
		request.setRemoteHost("localhost");
	}

	public void testBasic() throws Exception {
		addRequestParameter("targets", new String[] {"a", "b"});
		addRequestParameter("updateStrategy", "Default");
		
		expect(manager.getProjectConfig("a")).andReturn(projects[0]);
		expect(manager.getProjectConfig("b")).andReturn(projects[1]);
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[0]);
		dg.addTarget(projects[1]);
		
		expect(manager.buildDependencyGroup(
			aryEq(projects),
			eq(DependencyBuildPolicy.NONE),
			eq(WorkingCopyUpdateStrategy.Default),
			eq(false), eq(false))).andReturn(dg);
		
		final DependencyGroup expectedDg = new DependencyGroupImpl();
		expectedDg.addTarget(projects[0]);
		expectedDg.addTarget(projects[1]);
		
		expectedDg.setManualBuild(true);
		expectedDg.setName(request.getRemoteHost());
		
		buildManager.add(expectedDg);

		expect(manager.getBuildDaemons()).andReturn(Collections.<BuildDaemon>emptyList());
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		verifyForward("dashboard");
	}
	public void testWakesUpBuildDaemon() throws Exception {
		BuildDaemon bd1 = createMock(BuildDaemon.class);
		BuildDaemon bd2 = createMock(BuildDaemon.class);
		BuildDaemon bd3 = createMock(BuildDaemon.class);
		BuildDaemon bd4 = createMock(BuildDaemon.class);
		
		addRequestParameter("targets", new String[] {"a", "b"});
		addRequestParameter("updateStrategy", "Default");
		
		expect(manager.getProjectConfig("a")).andReturn(projects[0]);
		expect(manager.getProjectConfig("b")).andReturn(projects[1]);
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[0]);
		dg.addTarget(projects[1]);
		
		expect(manager.buildDependencyGroup(
			aryEq(projects),
			eq(DependencyBuildPolicy.NONE),
			eq(WorkingCopyUpdateStrategy.Default),
			eq(false), eq(false))).andReturn(dg);
		
		final DependencyGroup expectedDg = new DependencyGroupImpl();
		expectedDg.addTarget(projects[0]);
		expectedDg.addTarget(projects[1]);
		
		expectedDg.setManualBuild(true);
		expectedDg.setName(request.getRemoteHost());
		
		buildManager.add(expectedDg);

		expect(manager.getBuildDaemons()).andReturn(Arrays.asList(bd1, bd2, bd3, bd4));
		
		expect(bd1.isRunning()).andReturn(false);
		
		expect(bd2.isRunning()).andReturn(true);
		expect(bd2.isBuilding()).andReturn(true);
		
		expect(bd3.isRunning()).andReturn(true);
		expect(bd3.isBuilding()).andReturn(false);
		
		bd3.wakeUp();
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		verifyForward("dashboard");
	}
	public void testInvalidUpdateStrategy() throws Exception {
		addRequestParameter("targets", new String[] {"a"});
		addRequestParameter("updateStrategy", "NoneSuch");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		
		verifyInputForward();
		
		assertPropertyHasError("updateStrategy", "errors.enum.invalid");
	}
	public void testSpecifyUpdateStrategy() throws Exception {
		addRequestParameter("targets", new String[] {"a", "b"});
		addRequestParameter("updateStrategy", WorkingCopyUpdateStrategy.Incremental.name());
		
		expect(manager.getProjectConfig("a")).andReturn(projects[0]);
		expect(manager.getProjectConfig("b")).andReturn(projects[1]);
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(projects[0]);
		dg.addTarget(projects[1]);
		
		expect(manager.buildDependencyGroup(
			aryEq(projects),
			eq(DependencyBuildPolicy.NONE),
			eq(WorkingCopyUpdateStrategy.Incremental),
			eq(false), eq(false))).andReturn(dg);
		
		final DependencyGroup expectedDg = new DependencyGroupImpl();
		expectedDg.addTarget(projects[0]);
		expectedDg.addTarget(projects[1]);
		
		expectedDg.setManualBuild(true);
		expectedDg.setName(request.getRemoteHost());
		
		buildManager.add(expectedDg);

		expect(manager.getBuildDaemons()).andReturn(Collections.<BuildDaemon>emptyList());
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		verifyForward("dashboard");
	}
	public void testTargetsRequired() throws Exception {
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		
		verifyInputForward();
		
		assertPropertyHasError("targets", "errors.required");
	}
	public void testForceBuildDeps() throws Exception {
		final String username = "Amy";
		request.setUserPrincipal(new SimplePrincipal(username));
		
		addRequestParameter("targets", new String[] {"a"});
		addRequestParameter("dependencies", DependencyBuildPolicy.FORCE.name());
		
		expect(manager.getProjectConfig("a")).andReturn(projects[0]);
		
		final DependencyGroup dg = new DependencyGroupImpl();

		manager.buildDependencyGroup(
				aryEq(new ProjectConfigDto[] {projects[0]}),
				eq(DependencyBuildPolicy.FORCE),
				eq(WorkingCopyUpdateStrategy.Default),
				eq(false), eq(false));
		expectLastCall().andReturn(dg);

		final DependencyGroup expectedDg = new DependencyGroupImpl();
		
		expectedDg.setManualBuild(true);
		expectedDg.setName(username);
		
		buildManager.add(expectedDg);
		
		expect(manager.getBuildDaemons()).andReturn(Collections.<BuildDaemon>emptyList());
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		verifyForward("dashboard");
	}
	public void testForceBuildDepsAsNeeded() throws Exception {
		addRequestParameter("targets", new String[] {"a"});
		addRequestParameter("buildOnNoUpdates", "true");
		addRequestParameter("buildOnDependencyFailure", "true");
		addRequestParameter("dependencies", DependencyBuildPolicy.AS_NEEDED.name());
		
		expect(manager.getProjectConfig("a")).andReturn(projects[0]);
		
		final DependencyGroup dg = new DependencyGroupImpl();

		manager.buildDependencyGroup(
				aryEq(new ProjectConfigDto[] {projects[0]}),
				eq(DependencyBuildPolicy.AS_NEEDED),
				eq(WorkingCopyUpdateStrategy.Default),
				eq(true), eq(true));
		expectLastCall().andReturn(dg);

		buildManager.add(dg);
		
		expect(manager.getBuildDaemons()).andReturn(Collections.<BuildDaemon>emptyList());
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		verifyForward("dashboard");
	}
	public void testChooseTagsAndIncludeDependencies() throws Exception {
		addRequestParameter("targets", new String[] {"a"});
		addRequestParameter("buildOnNoUpdates", "true");
		addRequestParameter("buildOnDependencyFailure", "true");
		addRequestParameter("dependencies", DependencyBuildPolicy.AS_NEEDED.name());
		addRequestParameter("chooseTags", "true");
		
		final ProjectConfigDto project = projects[0];
		expect(manager.getProjectConfig("a")).andReturn(project);
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(project);
		dg.addTarget(projects[1]);
		
		manager.buildDependencyGroup(
				aryEq(new ProjectConfigDto[] {project}),
				eq(DependencyBuildPolicy.AS_NEEDED),
				eq(WorkingCopyUpdateStrategy.Default),
				eq(true), eq(true));
		expectLastCall().andReturn(dg);

		expect(manager.getRepositoryAdaptor(project)).andReturn(ra1);
		
		expect(ra1.getAvailableTags()).andReturn(tags1);
		
		expect(manager.getRepositoryAdaptor(projects[1])).andReturn(ra2);
		
		expect(ra2.getAvailableTags()).andReturn(tags2);
		
		ProjectStatusDto a = new ProjectStatusDto();
		a.setTagName("rc2");
		
		expect(buildManager.getLatestStatus("a")).andReturn(a);

		ProjectStatusDto b = new ProjectStatusDto();
		b.setTagName("tag-that-was-deleted-later");
		
		expect(buildManager.getLatestStatus("b")).andReturn(b);
		
		replay();
		
		actionPerform();
		
		verifyForward("chooseTags");
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		verify();
		
		final ManualBuildForm form = (ManualBuildForm) request.getSession().getAttribute("manualBuildForm");
		assertNotNull(form);
		
		final List<String> projectNames = form.getProjectNames();
		
		assertEquals(2, projectNames.size());
		assertEquals("a", projectNames.get(0));
		assertEquals("b", projectNames.get(1));
		
		final List<List<RepositoryTagDto>> tags = form.getAvailableTags();
		
		assertEquals(2, tags.size());

		assertSame(tags1, tags.get(0));
		assertSame(tags2, tags.get(1));
		
		final String[] selectedTags = form.getSelectedTags();
		assertEquals("rc2", selectedTags[0]);
		assertEquals("2.0", selectedTags[1]);
	}
	public void testChooseTagsReportsRepositoryError() throws Exception {
		addRequestParameter("targets", new String[] {"a"});
		addRequestParameter("chooseTags", "true");
		addRequestParameter("dependencies", DependencyBuildPolicy.NONE.name());
		
		final ProjectConfigDto project = projects[0];
		expect(manager.getProjectConfig("a")).andReturn(project);
		
		final DependencyGroup dg = new DependencyGroupImpl();
		dg.addTarget(project);
		
		manager.buildDependencyGroup(
				aryEq(new ProjectConfigDto[] {project}),
				eq(DependencyBuildPolicy.NONE),
				eq(WorkingCopyUpdateStrategy.Default),
				eq(false), eq(true));
		expectLastCall().andReturn(dg);

		expect(manager.getRepositoryAdaptor(project)).andReturn(ra1);
		
		expect(ra1.getAvailableTags()).andThrow(new RepositoryException("key.message", null, null));
		
		expect(buildManager.getLatestStatus("a")).andReturn(new ProjectStatusDto());

		replay();
		
		actionPerform();
		
		verifyForward("chooseTags");
		
		verifyNoActionMessages();
		assertPropertyHasError("a", "key.message");
		
		verify();
	}
	public void testTagsSelectedProceeds() throws Exception {
		final DependencyGroupImpl withoutTagNames = new DependencyGroupImpl();
		
		withoutTagNames.addTarget(projects[0]);
		withoutTagNames.addTarget(projects[1]);
		
		final DependencyGroupImpl dg = new DependencyGroupImpl();
		final ProjectConfigDto target = (ProjectConfigDto) projects[1].copy();
		target.setRepositoryTagName("rc1");
		
		dg.addTarget(projects[0]);
		dg.addTarget(target);
		dg.setName(request.getRemoteHost());
		
		final ManualBuildForm form = new ManualBuildForm();
		form.setServlet(getActionServlet());
		
		form.populateTagChoices(Arrays.asList(new String[] {"a", "b"}), null, withoutTagNames);
		
		request.getSession().setAttribute("manualBuildForm", form);
		
		addRequestParameter("chooseTags", "true");
		addRequestParameter("selectedTags", new String[] {"trunk", "rc1"});
		addRequestParameter("targets", new String[] {"a", "b"});
		
		buildManager.add(dg);
		
		replay();
		
		actionPerform();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		verifyForward("dashboard");
		
		verify();
	}
	private RepositoryTagDto createFakeTag(String name) {
		final RepositoryTagDto tag = new RepositoryTagDto();
		
		tag.setName(name);
		
		return tag;
	}
}
