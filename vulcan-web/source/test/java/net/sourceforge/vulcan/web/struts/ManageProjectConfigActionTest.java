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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.Date;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.ProjectNeedsDependencyException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.struts.forms.PluginConfigForm;
import net.sourceforge.vulcan.web.struts.forms.ProjectConfigForm;

import org.apache.struts.action.ActionMessages;
import org.easymock.EasyMock;
import org.easymock.IAnswer;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ManageProjectConfigActionTest extends MockApplicationContextStrutsTestCase {
	PluginManager pluginMgr;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/admin/setup/manageProjectConfig.do");

		pluginMgr = createStrictMock(PluginManager.class);
		
		expect(store.isWorkingCopyLocationInvalid((String)anyObject())).andAnswer(new IAnswer<Boolean>() {
			public Boolean answer() throws Throwable {
				final String arg = (String)EasyMock.getCurrentArguments()[0];
				if (arg != null && arg.indexOf("invalid") >= 0) {
					return Boolean.TRUE;
				}
				return Boolean.FALSE;
			}
		}).anyTimes();
	}
	
	public void testThrowsOnNoName() {
		addRequestParameter("action", "edit");
		
		EasyMock.expect(manager.getProjectConfig(null)).andThrow(new IllegalArgumentException());
		replay();
		
		try {
			actionPerform();
			fail("expected exception");
		} catch (IllegalArgumentException e) {
		}
		verify();
	}

	public void testPopulatesForm() {
		final ProjectConfigDto p = new ProjectConfigDto();
		p.setName("example");
		
		EasyMock.expect(manager.getProjectConfig(p.getName())).andReturn(p);
		
		addRequestParameter("action", "edit");
		addRequestParameter("config.name", "example");
		
		assertNull(request.getSession().getAttribute("projectConfigForm"));

		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("projectDetails");
		
		final ProjectConfigForm form = (ProjectConfigForm) request.getSession().getAttribute("projectConfigForm");
		assertNotNull(form);
		
		assertEquals(form.getConfig(), p);
		assertNotSame(form.getConfig(), p);
	}
	public void testResetsToCreateNew() {
		ProjectConfigForm form = new ProjectConfigForm();
		
		final ProjectConfigDto p = new ProjectConfigDto();
		p.setName("example");
		
		form.populate(p, false);
		
		request.getSession().setAttribute("projectConfigForm", form);

		addRequestParameter("action", "edit");
		addRequestParameter("createNew", "true");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("projectDetails");
		
		form = (ProjectConfigForm) request.getSession().getAttribute("projectConfigForm");
		assertNotNull(form);
		
		assertNotSame(form.getConfig(), p);
		final ProjectConfigDto newProj = new ProjectConfigDto();
		newProj.setName("");
		assertEquals(form.getConfig(), newProj);
		assertEquals(true, form.isCreateNew());
	}

	public void testCreateNewProject() throws Exception {
		initializeForm();
		
		addRequestParameter("action", "create");
		addRequestParameter("commit", "true");
		addRequestParameter("config.name", "newName");
		addRequestParameter("config.workDir", "dir");
		addRequestParameter("config.schedulerNames", new String[] {"daily", "weekly"});
		
		ProjectConfigDto config = new ProjectConfigDto();
		config.setName("newName");
		config.setWorkDir("dir");
		config.setSchedulerNames(new String[] {"daily", "weekly"});
		
		manager.addProjectConfig(config);
		
		replay();
		
		actionPerform();
		
		verifyNoActionErrors();
		
		verify();
		
		verifyForward("projectList");
		verifyActionMessages(new String[] {"messages.save.success"});
	}
	public void testCreateNewProjectHandlesDuplicateName() throws Exception {
		initializeForm();
		
		addRequestParameter("action", "create");
		addRequestParameter("commit", "true");
		addRequestParameter("config.name", "newName");
		addRequestParameter("config.workDir", "dir");
		
		ProjectConfigDto config = new ProjectConfigDto();
		config.setName("newName");
		config.setWorkDir("dir");
		
		manager.addProjectConfig(config);
		EasyMock.expectLastCall().andThrow(new DuplicateNameException("newName"));
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		verifyActionErrors(new String[] {"errors.unique"});
		
		final ProjectConfigForm form = (ProjectConfigForm) request.getSession().getAttribute("projectConfigForm");
		assertTrue(form.isCreateNew());
	}
	public void testUpdateProject() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setName("my project");
		config.setWorkDir("dir");
		config.setAutoIncludeDependencies(true);
		config.setSchedulerNames(new String[] {"a"});
		
		ProjectConfigForm form = new ProjectConfigForm();
		form.populate(config, false);
		form.setStore(store);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("action", "Update Project");
		addRequestParameter("commit", "true");
		addRequestParameter("config.workDir", "dirt");
		addRequestParameter("config.name", "my project new name");
		
		ProjectConfigDto updated = (ProjectConfigDto) config.copy();
		updated.setName("my project new name");
		updated.setWorkDir("dirt");
		updated.setAutoIncludeDependencies(false);
		updated.setSchedulerNames(new String[0]);
		
		manager.updateProjectConfig(config.getName(), updated, true);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("projectList");
		verifyActionMessages(new String[] {"messages.save.success"});
	}
	public void testCopyProject() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setName("my project");
		config.setWorkDir("dir");

		ProjectConfigForm form = new ProjectConfigForm();
		form.populate(config, false);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("action", "Copy Project");
		addRequestParameter("commit", "true");
		addRequestParameter("config.workDir", "dirt");
		addRequestParameter("config.name", "my project");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("projectDetails");
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		ProjectConfigDto copy = (ProjectConfigDto) config.copy();
		copy.setName("my project-copy");
		copy.setWorkDir("dirt-copy");
		
		assertEquals(copy, form.getConfig());
		assertTrue(form.isCreateNew());
	}
	public void testUpdateProjectValidatesName() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setName("my project");
		
		ProjectConfigForm form = new ProjectConfigForm();
		form.populate(config, false);
		form.setStore(store);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("action", "Update Project");
		addRequestParameter("config.name", "");
		addRequestParameter("commit", "true");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		assertPropertyHasError("config.name", "errors.required");
		assertPropertyHasError("config.workDir", "errors.required");
	}
	public void testUpdateProjectValidatesWorkingCopy() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setName("my project");
		
		ProjectConfigForm form = new ProjectConfigForm();
		form.populate(config, false);
		form.setStore(store);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("action", "Update Project");
		addRequestParameter("config.name", "dafsd");
		addRequestParameter("config.workDir", "invalid");
		addRequestParameter("commit", "true");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		assertPropertyHasError("config.workDir", "errors.work.dir.reserved");
	}
	public void testUpdateProjectValidatesIssueTrackerURL() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setName("my project");

		ProjectConfigForm form = new ProjectConfigForm();
		form.populate(config, false);
		form.setStore(store);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("action", "Update Project");
		addRequestParameter("config.bugtraqUrl", "nosuchprotocol://localhost/ticket/%BUGID%");
		addRequestParameter("commit", "true");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		assertPropertyHasError("config.bugtraqUrl", "errors.url");
	}
	public void testUpdateProjectIssueTrackerURLContainsBugId() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setName("my project");

		ProjectConfigForm form = new ProjectConfigForm();
		form.populate(config, false);
		form.setStore(store);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("action", "Update Project");
		addRequestParameter("config.bugtraqUrl", "http://localhost");
		addRequestParameter("commit", "true");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		assertPropertyHasError("config.bugtraqUrl", "errors.bugtraq.must.contain.id");
	}
	public void testUpdateProjectValidatesRegex() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setName("my project");

		ProjectConfigForm form = new ProjectConfigForm();
		form.populate(config, false);
		form.setStore(store);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("action", "Update Project");
		addRequestParameter("config.bugtraqUrl", "http://localhost/%BUGID%");
		addRequestParameter("config.bugtraqLogRegex1", "(.*");
		addRequestParameter("config.bugtraqLogRegex2", "(+");
		addRequestParameter("commit", "true");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		assertPropertyHasError("config.bugtraqLogRegex1", "errors.regex");
		assertPropertyHasError("config.bugtraqLogRegex2", "errors.regex");
	}
	public void testUpdateProjectNoChanges() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setName("my project");
		config.setWorkDir("dir");
		config.setSchedulerNames(new String[0]);
		config.setLastModificationDate(new Date());
		
		ProjectConfigForm form = new ProjectConfigForm();
		form.populate(config, false);
		form.setStore(store);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("action", "Update Project");
		addRequestParameter("config.name", "my project");
		addRequestParameter("config.workDir", "dir");
		addRequestParameter("commit", "true");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("projectList");
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "warnings.no.change.made", getActionMessages("warnings"));
	}
	public void testDeleteProject() throws Exception {
		addRequestParameter("config.name", "my project");
		addRequestParameter("action", "Delete Project");
		
		manager.deleteProjectConfig("my project");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("projectList");
		verifyActionMessages(new String[] {"messages.save.success"});
	}
	public void testDeleteProjectHandlesDependencyException() throws Exception {
		addRequestParameter("config.name", "my project");
		addRequestParameter("action", "Delete Project");
		
		manager.deleteProjectConfig("my project");
		EasyMock.expectLastCall().andThrow(new ProjectNeedsDependencyException("my project", "building block"));
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("projectList");
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "messages.dependency.required");
	}
	public void testUpdateHandlesDuplicateNameException() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setName("my project");
		config.setWorkDir("dir");
		
		ProjectConfigForm form = new ProjectConfigForm();
		form.populate(config, false);
		form.setStore(store);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("action", "Update Project");
		addRequestParameter("config.workDir", "dir");
		addRequestParameter("config.name", "my project2");
		addRequestParameter("commit", "true");
		
		ProjectConfigDto updated = (ProjectConfigDto) config.copy();
		updated.setName("my project2");
		
		manager.updateProjectConfig(config.getName(), updated, true);
		EasyMock.expectLastCall().andThrow(new DuplicateNameException("my project"));
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		verifyActionErrors(new String[] {"errors.unique"});
	}
	public void testConfigureRepositoryAdaptor() throws Exception {
		addRequestParameter("config.repositoryAdaptorPluginId", "com.example.plugin");
		addRequestParameter("action", "Configure");
		
		EasyMock.expect(manager.getProjectConfigNames()).andReturn(((List<String>)null));
		EasyMock.expect(manager.getPluginManager()).andReturn(pluginMgr);
		
		final MockRACDto rac = new MockRACDto();
		rac.helpTopic = "foo/bar.html";
		rac.helpUrl = "http://example.com/";
		
		expect(pluginMgr.getRepositoryAdaptorDefaultConfig("com.example.plugin"))
			.andReturn(rac);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		verifyNoActionErrors();
		verifyNoActionMessages();
		
		final PluginConfigForm form = (PluginConfigForm) request.getSession().getAttribute("pluginConfigForm");
		assertNotNull(form);
		
		assertTrue(form.isProjectPlugin());
		assertTrue("Form should be forced dirty if projectConfig is dirty.", form.isDirty());
		
		assertEquals("pluginConfig.url", form.getAllProperties().get(0).getName());
		
		assertEquals(rac.helpUrl, request.getAttribute("helpUrl"));
		assertEquals(rac.helpTopic, request.getAttribute("helpTopic"));
	}
	public void testConfigureRepositoryAdaptorPreviouslyConfigured() throws Exception {
		final ProjectConfigForm form = new ProjectConfigForm();
		final ProjectConfigDto projectConfigDto = new ProjectConfigDto();
		final MockRACDto pluginConfigDto = new MockRACDto();
		pluginConfigDto.setUrl("hello");
		projectConfigDto.setRepositoryAdaptorConfig(pluginConfigDto);
		projectConfigDto.setRepositoryAdaptorPluginId("com.example.plugin");
		form.populate(projectConfigDto, false);
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("config.repositoryAdaptorPluginId", "com.example.plugin");
		addRequestParameter("action", "Configure");
		
		EasyMock.expect(manager.getProjectConfigNames()).andReturn(((List<String>)null));
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		verifyNoActionErrors();
		verifyNoActionMessages();
		
		final PluginConfigForm pluginForm = (PluginConfigForm) request.getSession().getAttribute("pluginConfigForm");
		assertNotNull(pluginForm);
		
		assertEquals("pluginConfig.url", pluginForm.getAllProperties().get(0).getName());
		assertEquals("hello", ((MockRACDto)pluginForm.getPluginConfig()).getUrl());
	}
	public void testConfigureRepositoryAdaptorSwitchType() throws Exception {
		final ProjectConfigForm form = new ProjectConfigForm();
		final ProjectConfigDto projectConfigDto = new ProjectConfigDto();
		final MockRACDto pluginConfigDto = new MockRACDto();
		pluginConfigDto.setUrl("hello");
		projectConfigDto.setRepositoryAdaptorConfig(pluginConfigDto);
		projectConfigDto.setRepositoryAdaptorPluginId("com.example.plugin");
		form.populate(projectConfigDto, false);
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("config.repositoryAdaptorPluginId", "com.example.other");
		addRequestParameter("action", "Configure");

		EasyMock.expect(manager.getProjectConfigNames()).andReturn(((List<String>)null));
		EasyMock.expect(manager.getPluginManager()).andReturn(pluginMgr);
		
		expect(pluginMgr.getRepositoryAdaptorDefaultConfig("com.example.other"))
			.andReturn(new MockRACDto());

		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		verifyNoActionErrors();
		verifyNoActionMessages();
		
		final PluginConfigForm pluginForm = (PluginConfigForm) request.getSession().getAttribute("pluginConfigForm");
		assertNotNull(pluginForm);
		
		assertEquals("pluginConfig.url", pluginForm.getAllProperties().get(0).getName());
		assertEquals(null, ((MockRACDto)pluginForm.getPluginConfig()).getUrl());
	}
	public void testConfigureRepositoryAdaptorSwitchToNone() throws Exception {
		final ProjectConfigForm form = new ProjectConfigForm();
		final ProjectConfigDto projectConfigDto = new ProjectConfigDto();
		final MockRACDto pluginConfigDto = new MockRACDto();
		pluginConfigDto.setUrl("hello");
		projectConfigDto.setRepositoryAdaptorConfig(pluginConfigDto);
		projectConfigDto.setRepositoryAdaptorPluginId("com.example.plugin");
		form.populate(projectConfigDto, false);
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("config.repositoryAdaptorPluginId", "");
		addRequestParameter("focus", "config.repositoryAdaptorPluginId");
		addRequestParameter("action", "Configure");

		EasyMock.expect(manager.getProjectConfigNames()).andReturn(((List<String>)null));
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		assertPropertyHasError("config.repositoryAdaptorPluginId", "errors.no.plugin.selected");
		verifyNoActionMessages();
		
		final PluginConfigForm pluginForm = (PluginConfigForm) request.getSession().getAttribute("pluginConfigForm");
		assertNotNull(pluginForm);
	}
	public void testRepositoryAdaptorSwitchTypeUpdateWithoutConfigure() throws Exception {
		final ProjectConfigForm form = new ProjectConfigForm();
		final ProjectConfigDto projectConfigDto = new ProjectConfigDto();
		final MockRACDto pluginConfigDto = new MockRACDto();
		pluginConfigDto.setUrl("hello");
		projectConfigDto.setRepositoryAdaptorConfig(pluginConfigDto);
		projectConfigDto.setRepositoryAdaptorPluginId("com.example.plugin");
		form.populate(projectConfigDto, false);
		form.setStore(store);
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("config.repositoryAdaptorPluginId", "com.example.other");
		addRequestParameter("action", "Update");

		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		assertPropertyHasError("config.repositoryAdaptorPluginId", "errors.plugin.not.configured");
		verifyNoActionMessages();
	}
	public void testRepositoryAdaptorSwitchTypeToNoneUpdateWithoutConfigure() throws Exception {
		final ProjectConfigForm form = new ProjectConfigForm();
		final ProjectConfigDto projectConfigDto = new ProjectConfigDto();
		final MockRACDto pluginConfigDto = new MockRACDto();
		pluginConfigDto.setUrl("hello");
		projectConfigDto.setName("name");
		projectConfigDto.setRepositoryAdaptorConfig(pluginConfigDto);
		projectConfigDto.setRepositoryAdaptorPluginId("com.example.plugin");
		form.populate(projectConfigDto, false);
		form.setStore(store);
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("config.repositoryAdaptorPluginId", "");
		addRequestParameter("action", "Update");
		addRequestParameter("commit", "true");
		addRequestParameter("config.workDir", "dirt");
		addRequestParameter("config.name", "my project new name");

		ProjectConfigDto updated = (ProjectConfigDto) projectConfigDto.copy();
		updated.setName("my project new name");
		updated.setWorkDir("dirt");
		updated.setAutoIncludeDependencies(false);
		updated.setRepositoryAdaptorPluginId("");
		updated.setRepositoryAdaptorConfig(null);
		
		manager.updateProjectConfig(projectConfigDto.getName(), updated, true);

		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		verifyForward("projectList");
		
		assertEquals(null, form.getProjectConfig().getRepositoryAdaptorConfig());
	}
	public void testConfigureBuildTool() throws Exception {
		addRequestParameter("config.buildToolPluginId", "com.example.build.plugin");
		addRequestParameter("focus", "config.buildToolPluginId");
		addRequestParameter("action", "Configure");
		
		EasyMock.expect(manager.getProjectConfigNames()).andReturn(((List<String>)null));
		EasyMock.expect(manager.getPluginManager()).andReturn(pluginMgr);
		
		expect(pluginMgr.getBuildToolDefaultConfig("com.example.build.plugin"))
			.andReturn(new MockBuildToolDto());
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		verifyNoActionErrors();
		verifyNoActionMessages();
		
		final PluginConfigForm form = (PluginConfigForm) request.getSession().getAttribute("pluginConfigForm");
		assertNotNull(form);
		
		assertTrue(form.isProjectPlugin());
		
		assertEquals("pluginConfig.url", form.getAllProperties().get(0).getName());
	}
	public void testConfigureBuildToolPreviouslyConfigured() throws Exception {
		final ProjectConfigForm form = new ProjectConfigForm();
		final ProjectConfigDto projectConfigDto = new ProjectConfigDto();
		final MockBuildToolDto pluginConfigDto = new MockBuildToolDto();
		pluginConfigDto.setUrl("hello");
		projectConfigDto.setBuildToolConfig(pluginConfigDto);
		projectConfigDto.setBuildToolPluginId("com.example.build.plugin");
		
		form.populate(projectConfigDto, false);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("config.buildToolPluginId", "com.example.build.plugin");
		addRequestParameter("focus", "config.buildToolPluginId");
		addRequestParameter("action", "Configure");
		
		EasyMock.expect(manager.getProjectConfigNames()).andReturn(((List<String>)null));
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		verifyNoActionErrors();
		verifyNoActionMessages();
		
		final PluginConfigForm pluginForm = (PluginConfigForm) request.getSession().getAttribute("pluginConfigForm");
		assertNotNull(pluginForm);
		
		assertEquals("pluginConfig.url", pluginForm.getAllProperties().get(0).getName());
		assertEquals("hello", ((MockBuildToolDto)pluginForm.getPluginConfig()).getUrl());
	}
	public void testConfigureBuildToolSwitchType() throws Exception {
		final ProjectConfigForm form = new ProjectConfigForm();
		final ProjectConfigDto projectConfigDto = new ProjectConfigDto();

		final MockBuildToolDto pluginConfigDto = new MockBuildToolDto();
		pluginConfigDto.setUrl("hello");
		projectConfigDto.setBuildToolConfig(pluginConfigDto);
		projectConfigDto.setBuildToolPluginId("com.example.build.plugin");
		
		form.populate(projectConfigDto, false);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("config.buildToolPluginId", "com.example.build.plugin.other");
		addRequestParameter("focus", "config.buildToolPluginId");
		addRequestParameter("action", "Configure");

		EasyMock.expect(manager.getProjectConfigNames()).andReturn(((List<String>)null));
		EasyMock.expect(manager.getPluginManager()).andReturn(pluginMgr);
		
		expect(pluginMgr.getBuildToolDefaultConfig("com.example.build.plugin.other"))
			.andReturn(new MockBuildToolDto());

		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		verifyNoActionErrors();
		verifyNoActionMessages();
		
		final PluginConfigForm pluginForm = (PluginConfigForm) request.getSession().getAttribute("pluginConfigForm");
		assertNotNull(pluginForm);
		
		assertEquals("pluginConfig.url", pluginForm.getAllProperties().get(0).getName());
		assertEquals(null, ((MockBuildToolDto)pluginForm.getPluginConfig()).getUrl());
	}
	public void testConfigureBuildToolSwitchToNone() throws Exception {
		final ProjectConfigForm form = new ProjectConfigForm();
		final ProjectConfigDto projectConfigDto = new ProjectConfigDto();

		final MockBuildToolDto pluginConfigDto = new MockBuildToolDto();
		pluginConfigDto.setUrl("hello");
		projectConfigDto.setBuildToolConfig(pluginConfigDto);
		projectConfigDto.setBuildToolPluginId("com.example.build.plugin");
		
		form.populate(projectConfigDto, false);
		
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("config.buildToolPluginId", "");
		addRequestParameter("focus", "config.buildToolPluginId");
		addRequestParameter("action", "Configure");

		EasyMock.expect(manager.getProjectConfigNames()).andReturn(((List<String>)null));
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		assertPropertyHasError("config.buildToolPluginId", "errors.no.plugin.selected");
		verifyNoActionMessages();
		
		final PluginConfigForm pluginForm = (PluginConfigForm) request.getSession().getAttribute("pluginConfigForm");
		assertNotNull(pluginForm);
	}
	public void testBuildToolSwitchTypeUpdateWithoutConfigure() throws Exception {
		final ProjectConfigForm form = new ProjectConfigForm();
		final ProjectConfigDto projectConfigDto = new ProjectConfigDto();

		final MockBuildToolDto pluginConfigDto = new MockBuildToolDto();
		pluginConfigDto.setUrl("hello");
		projectConfigDto.setBuildToolConfig(pluginConfigDto);
		projectConfigDto.setBuildToolPluginId("com.example.build.plugin");
		
		form.populate(projectConfigDto, false);
		form.setStore(store);
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("config.buildToolPluginId", "com.example.build.plugin.other");
		addRequestParameter("action", "Update");

		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		assertPropertyHasError("config.buildToolPluginId", "errors.plugin.not.configured");
		verifyNoActionMessages();
	}
	public void testBuildToolSwitchTypeToNoneUpdateWithoutConfigure() throws Exception {
		final ProjectConfigForm form = new ProjectConfigForm();
		final ProjectConfigDto projectConfigDto = new ProjectConfigDto();
		final MockBuildToolDto pluginConfigDto = new MockBuildToolDto();
		
		pluginConfigDto.setUrl("hello");
		projectConfigDto.setBuildToolConfig(pluginConfigDto);
		projectConfigDto.setBuildToolPluginId("com.example.build.plugin");
		projectConfigDto.setName("name");

		form.populate(projectConfigDto, false);
		form.setStore(store);
		request.getSession().setAttribute("projectConfigForm", form);
		
		addRequestParameter("config.buildToolPluginId", "");
		addRequestParameter("action", "Update");
		addRequestParameter("commit", "true");
		addRequestParameter("config.workDir", "dirt");
		addRequestParameter("config.name", "my project new name");

		ProjectConfigDto updated = (ProjectConfigDto) projectConfigDto.copy();
		updated.setName("my project new name");
		updated.setWorkDir("dirt");
		updated.setAutoIncludeDependencies(false);
		updated.setBuildToolPluginId("");
		updated.setBuildToolConfig(null);
		
		manager.updateProjectConfig(projectConfigDto.getName(), updated, true);

		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionErrors();
		verifyActionMessages(new String[] {"messages.save.success"});
		verifyForward("projectList");
		
		assertEquals(null, form.getProjectConfig().getBuildToolConfig());
	}

	private void initializeForm() {
		ProjectConfigForm form = new ProjectConfigForm();
		form.setStore(store);
		
		request.getSession().setAttribute("projectConfigForm", form);
	}
	
	public static class MockRACDto extends RepositoryAdaptorConfigDto {
		String url;
		String helpTopic;
		String helpUrl;
		
		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		@Override
		public String getHelpTopic() {
			return helpTopic;
		}
		
		@Override
		public String getHelpUrl() {
			return helpUrl;
		}
		
		@Override
		public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
			try {
				return Collections.singletonList(new PropertyDescriptor("url", MockRACDto.class));
			} catch (IntrospectionException e) {
				throw new RuntimeException(e);
			}
		}
		@Override
		public String getPluginId() {
			return "com.example.plugin";
		}
		@Override
		public String getPluginName() {
			return "Example Plugin";
		}
	}
	public static class MockBuildToolDto extends BuildToolConfigDto {
		String url;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		@Override
		public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
			try {
				return Collections.singletonList(new PropertyDescriptor("url", MockRACDto.class));
			} catch (IntrospectionException e) {
				throw new RuntimeException(e);
			}
		}
		@Override
		public String getPluginId() {
			return "com.example.build.plugin";
		}
		@Override
		public String getPluginName() {
			return "Example Build Tool";
		}
	}
}

