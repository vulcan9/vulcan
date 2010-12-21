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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import junit.framework.AssertionFailedError;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.dto.BaseDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.PluginProfileDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.exception.CannotCreateDirectoryException;
import net.sourceforge.vulcan.exception.DuplicatePluginIdException;
import net.sourceforge.vulcan.exception.InvalidPluginLayoutException;
import net.sourceforge.vulcan.exception.PluginLoadFailureException;
import net.sourceforge.vulcan.exception.PluginNotConfigurableException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.integration.PluginConfigStub;
import net.sourceforge.vulcan.web.struts.ManageProjectConfigActionTest.MockBuildToolDto;
import net.sourceforge.vulcan.web.struts.forms.PluginConfigForm;
import net.sourceforge.vulcan.web.struts.forms.ProjectConfigForm;

import org.apache.struts.action.ActionMessages;


public class ManagePluginActionTest extends MockApplicationContextStrutsTestCase {
	StateManagerConfigDto stateManagerConfig = new StateManagerConfigDto();
	PluginManager mgr;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/admin/setup/managePlugin.do");
		
		mgr = createStrictMock(PluginManager.class);
		
		expect(manager.getProjectConfigNames()).andReturn(null);
		
		expect(manager.getPluginManager()).andReturn(mgr).anyTimes();
		
		PluginConfigStub.validateCalled = false;
		PluginConfigStub.helpTopic = "fake topic";
		PluginConfigStub.helpUrl = "fake url";
	}
	
	public void testDelete() throws Exception {
		addRequestParameter("action", "delete");
		addRequestParameter("pluginId", "a.b.c.plugin");

		manager.removePlugin("a.b.c.plugin");

		replay();
		
		actionPerform();
		
		verify();
		
		verifyActionMessages(new String[] {"messages.save.success"});
		verifyForward("pluginList");
	}
	public void testDeleteNullId() throws Exception {
		addRequestParameter("action", "delete");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		assertPropertyHasError("pluginId", "errors.required");
	}

	public void testDeleteHandlesStoreException() throws Exception {
		addRequestParameter("action", "delete");
		addRequestParameter("pluginId", "a.b.c.plugin");
		
		manager.removePlugin("a.b.c.plugin");
		expectLastCall().andThrow(new StoreException(null, null));
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("pluginLocked");
	}
	public void testDeleteHandlesNestedFileNotFoundException() throws Exception {
		addRequestParameter("action", "delete");
		addRequestParameter("pluginId", "a.b.c.plugin");
		
		manager.removePlugin("a.b.c.plugin");
		expectLastCall().andThrow(new StoreException(null, new FileNotFoundException()));
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.plugin.not.found");
	}
	public void testUploadNoFile() throws Exception {
		addRequestParameter("action", "Upload");
		
		replay();
		
		actionPerform();
		
		verify();

		assertPropertyHasError("pluginFile", "errors.required");
	}
	public void testUpload() throws Exception {
		setMultipartRequestHandlerStub();
		
		final InputStream is = uploadFile("pluginFile", "foo.zip", "ababa").getInputStream();
		addRequestParameter("action", "Upload");

		mgr.importPluginZip(is);

		replay();
		
		actionPerform();
		
		verify();
		
		verifyActionMessages(new String[] {"messages.save.success"});
	}

	public void testUploadThrowsInvalidLayout() throws Exception {
		setMultipartRequestHandlerStub();
		
		final InputStream is = uploadFile("pluginFile", "foo.zip", "ababa").getInputStream();
		addRequestParameter("action", "Upload");

		mgr.importPluginZip(is);
		expectLastCall().andThrow(new InvalidPluginLayoutException());

		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.plugin.layout");
	}
	public void testUploadThrowsDuplicate() throws Exception {
		setMultipartRequestHandlerStub();
		
		final InputStream is = uploadFile("pluginFile", "foo.zip", "ababa").getInputStream();
		addRequestParameter("action", "Upload");

		mgr.importPluginZip(is);
		expectLastCall().andThrow(new DuplicatePluginIdException("a"));
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.plugin.duplicate");
	}
	public void testUploadThrowsCannotCreateException() throws Exception {
		setMultipartRequestHandlerStub();
		
		final InputStream is = uploadFile("pluginFile", "foo.zip", "ababa").getInputStream();
		addRequestParameter("action", "Upload");

		mgr.importPluginZip(is);
		expectLastCall().andThrow(new CannotCreateDirectoryException(new File("poofy")));
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.plugin.store.failure.dir");
	}
	public void testUploadThrowsStoreException() throws Exception {
		setMultipartRequestHandlerStub();
		
		final InputStream is = uploadFile("pluginFile", "foo.zip", "ababa").getInputStream();
		addRequestParameter("action", "Upload");

		mgr.importPluginZip(is);
		expectLastCall().andThrow(new StoreException(null, null));

		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.plugin.store.failure");
	}
	public void testUploadThrowsPluginLoadException() throws Exception {
		setMultipartRequestHandlerStub();
		
		final InputStream is = uploadFile("pluginFile", "foo.zip", "ababa").getInputStream();
		addRequestParameter("action", "Upload");

		mgr.importPluginZip(is);
		expectLastCall().andThrow(new PluginLoadFailureException("foo", new FileNotFoundException()));
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.plugin.load.failure");
	}
	public void testConfigurePluginNotSupported() throws Exception {
		addRequestParameter("action", "Configure");
		addRequestParameter("pluginId", "a.b.c.plugin");
		
		mgr.getPluginConfigInfo("a.b.c.plugin");
		expectLastCall().andThrow(new PluginNotConfigurableException());
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.plugin.not.configurable");
		verifyInputForward();
	}
	public void testConfigurePlugin() throws Exception {
		final PluginConfigForm form = new PluginConfigForm();
		
		form.setProjectPlugin(true);
		
		request.getSession().setAttribute("pluginConfigForm", form);
		
		addRequestParameter("action", "Configure");
		addRequestParameter("pluginId", "a.b.c.plugin");
		
		final PluginConfigStub config = new PluginConfigStub();
		config.setValue("some value");
		config.setPassword("apaswrd1");
		
		expect(mgr.getPluginConfigInfo("a.b.c.plugin")).andReturn(config);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		final PluginConfigStub copy = (PluginConfigStub) config.copy();
		copy.setPassword(PluginConfigForm.HIDDEN_PASSWORD_VALUE);
		
		assertEquals(copy, form.getPluginConfig());
		assertNotSame(config, form.getPluginConfig());
		assertFalse(form.isProjectPlugin());
		
		assertEquals("Setup > Plugins > Mock Plugin", request.getAttribute("location"));
		
		assertEquals(config.getHelpUrl(), request.getAttribute("helpUrl"));
		assertEquals(config.getHelpTopic(), request.getAttribute("helpTopic"));
	}
	public void testConfigurePluginBlankPasswordStaysBlank() throws Exception {
		final PluginConfigForm form = new PluginConfigForm();
		
		form.setProjectPlugin(true);
		
		request.getSession().setAttribute("pluginConfigForm", form);
		
		addRequestParameter("action", "Configure");
		addRequestParameter("pluginId", "a.b.c.plugin");
		
		final PluginConfigStub config = new PluginConfigStub();
		config.setValue("some value");
		config.setPassword(null);
		
		expect(mgr.getPluginConfigInfo("a.b.c.plugin")).andReturn(config);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		assertEquals(config, form.getPluginConfig());
		assertNotSame(config, form.getPluginConfig());
		assertFalse(form.isProjectPlugin());
	}
	public void testUpdate() throws Exception {
		addRequestParameter("action", "Update");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("pluginConfig.value", "foo");
		addRequestParameter("pluginConfig.password", "sompswrd");
		
		final PluginConfigForm form = new PluginConfigForm();
		form.setProjectPlugin(true);
		
		final PluginConfigStub cfg = new PluginConfigStub();
		cfg.setBool(true);
		form.setPluginConfig(request, (PluginConfigDto) cfg.copy(), false);
		request.getSession().setAttribute("pluginConfigForm", form);
		
		cfg.setValue("foo");
		cfg.setBool(false);
 		cfg.setPassword("sompswrd");
 		cfg.validate();
 		
 		manager.updatePluginConfig(cfg, Collections.<PluginProfileDto>emptySet());
 		replay();
 		
 		actionPerform();

 		verify();
 		
 		verifyForward("pluginList");
	
 		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));
 		final PluginConfigStub formPluginConfig = ((PluginConfigStub)form.getPluginConfig());
		assertFalse(formPluginConfig.getBool());
		assertTrue(formPluginConfig.isValidateCalled());
	}		
	public void testUpdateNoChanges() throws Exception {
		addRequestParameter("action", "Update");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("pluginConfig.value", "foo");
		addRequestParameter("pluginConfig.password", PluginConfigForm.HIDDEN_PASSWORD_VALUE);
		
		final PluginConfigForm form = new PluginConfigForm();
		form.setProjectPlugin(true);
		
		final PluginConfigStub cfg = new PluginConfigStub();
		cfg.setBool(Boolean.FALSE);
		cfg.setPassword("something");
		cfg.setValue("foo");
		
		form.setPluginConfig(request, (PluginConfigDto) cfg.copy(), false);
		request.getSession().setAttribute("pluginConfigForm", form);
		
		cfg.setValue("foo");
 		cfg.setPassword("something");
 		cfg.validate();
 		
 		replay();
 		
 		actionPerform();

 		verify();
 		
 		verifyForward("pluginList");
	
 		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));
 		final PluginConfigStub formPluginConfig = ((PluginConfigStub)form.getPluginConfig());
		assertFalse(formPluginConfig.getBool());
		assertTrue(formPluginConfig.isValidateCalled());
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "warnings.no.change.made", getActionMessages("warnings"));
	}		
 	public void testUpdateDontChangePassword() throws Exception {
 		addRequestParameter("action", "Update");
 		addRequestParameter("pluginId", "a.b.c.plugin");
 		addRequestParameter("pluginConfig.value", "foo");
 		addRequestParameter("pluginConfig.password", PluginConfigForm.HIDDEN_PASSWORD_VALUE);
 		
 		final PluginConfigForm form = new PluginConfigForm();
 		form.setProjectPlugin(true);
 		
 		final PluginConfigStub cfg = new PluginConfigStub();
 		cfg.setBool(true);
 		cfg.setPassword("sompswrd");
 		
 		form.setPluginConfig(request, (PluginConfigDto) cfg.copy(), false);
 		request.getSession().setAttribute("pluginConfigForm", form);
 		
 		cfg.setValue("foo");
 		cfg.setBool(false);
 		cfg.setPassword("sompswrd");
 		
		cfg.validate();
		
		manager.updatePluginConfig(cfg, Collections.<PluginProfileDto>emptySet());
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("pluginList");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));
		assertFalse(((PluginConfigStub)form.getPluginConfig()).getBool());
		assertTrue("PluginConfigDto.validate was not called", ((PluginConfigStub)form.getPluginConfig()).isValidateCalled());
	}
	public void testUpdateValidationError() throws Exception {
		addRequestParameter("action", "Update");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("pluginConfig.value", "bad");
		
		final PluginConfigForm form = new PluginConfigForm();
		form.setProjectPlugin(true);
		
		final PluginConfigStub cfg = new PluginConfigStub();
		cfg.setBool(true);
		form.setPluginConfig(request, (PluginConfigDto) cfg.copy(), false);
		request.getSession().setAttribute("pluginConfigForm", form);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));
		assertFalse(((PluginConfigStub)form.getPluginConfig()).getBool());
		assertTrue("PluginConfigDto.validate was not called", ((PluginConfigStub)form.getPluginConfig()).isValidateCalled());
		
		assertPropertyHasError("pluginConfig.value", "fake.error.key");
		
		assertEquals(cfg.getHelpUrl(), request.getAttribute("helpUrl"));
		assertEquals(cfg.getHelpTopic(), request.getAttribute("helpTopic"));
	}
	public void testUpdateValidationErrors() throws Exception {
		addRequestParameter("action", "Update");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("pluginConfig.value", "worse");
		
		final PluginConfigForm form = new PluginConfigForm();
		form.setProjectPlugin(true);
		
		final PluginConfigStub cfg = new PluginConfigStub();
		cfg.setBool(true);
		form.setPluginConfig(request, (PluginConfigDto) cfg.copy(), false);
		request.getSession().setAttribute("pluginConfigForm", form);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));
		assertFalse(((PluginConfigStub)form.getPluginConfig()).getBool());
		assertTrue("PluginConfigDto.validate was not called", ((PluginConfigStub)form.getPluginConfig()).isValidateCalled());
		
		assertPropertyHasError("pluginConfig.value", "fake.error.key");
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "other.fake.error.key");
	}
	public void testBackGoesToProjectViewAtTopLevel() throws Exception {
		addRequestParameter("action", "Back");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("pluginConfig.value", "foo");
		addRequestParameter("projectPlugin", "true");
		
		final PluginConfigForm form = new PluginConfigForm();
		form.setPluginConfig(request, new PluginConfigStub(), false);
		request.getSession().setAttribute("pluginConfigForm", form);
		
		final ProjectConfigForm projectForm = new ProjectConfigForm();
		projectForm.populate(new ProjectConfigDto(), false);
		request.getSession().setAttribute("projectConfigForm", projectForm);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("projectDetails");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));
		assertSame(projectForm, request.getSession().getAttribute("projectConfigForm"));
		assertSame(form.getPluginConfig(), projectForm.getProjectConfig().getRepositoryAdaptorConfig());
		
		assertEquals(null, request.getAttribute("helpUrl"));
		assertEquals(null, request.getAttribute("helpTopic"));
	}
	public void testBackGoesToProjectViewAtTopLevelSetsBuildTool() throws Exception {
		addRequestParameter("action", "Back");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("pluginConfig.url", "foo");
		addRequestParameter("projectPlugin", "true");
		
		final PluginConfigForm form = new PluginConfigForm();
		form.setPluginConfig(request, new MockBuildToolDto(), false);
		request.getSession().setAttribute("pluginConfigForm", form);
		
		final ProjectConfigForm projectForm = new ProjectConfigForm();
		projectForm.populate(new ProjectConfigDto(), false);
		projectForm.setFocus("config.buildTool");
		request.getSession().setAttribute("projectConfigForm", projectForm);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("projectDetails");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));
		assertSame(projectForm, request.getSession().getAttribute("projectConfigForm"));
		assertSame(form.getPluginConfig(), projectForm.getProjectConfig().getBuildToolConfig());
	}
	public void testBackGoesUpWhenNested() throws Exception {
		addRequestParameter("action", "Back");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("projectPlugin", "true");
		addRequestParameter("focus", "pluginConfig.value");
		
		final PluginConfigForm form = new PluginConfigForm();
		final PluginConfigStub cfg = new PluginConfigStub();
		form.setPluginConfig(request, cfg, false);
		form.setFocus("pluginConfig.value");
		request.getSession().setAttribute("pluginConfigForm", form);
		assertEquals("Setup > Plugins > Mock Plugin", request.getAttribute("location"));
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));
		assertEquals("pluginConfig", form.getFocus());
		
		assertEquals(cfg.getHelpUrl(), request.getAttribute("helpUrl"));
		assertEquals(cfg.getHelpTopic(), request.getAttribute("helpTopic"));
	}
	public void testConfigureNestedObject() throws Exception {
		addRequestParameter("action", "Configure");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("pluginConfig.value", "foo");
		addRequestParameter("focus", "pluginConfig.obj");
		
		final PluginConfigForm form = new PluginConfigForm();
		final PluginConfigStub cfg = new PluginConfigStub();
		form.setPluginConfig(request, cfg, false);
		request.getSession().setAttribute("pluginConfigForm", form);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));

		assertEquals("foo", ((PluginConfigStub)form.getPluginConfig()).getValue());
		assertEquals("pluginConfig.obj.nestedValue", form.getAllProperties().get(1).getName());
		assertEquals("Setup > Plugins > Mock Plugin > Nested Object", request.getAttribute("location"));
		
		assertEquals(cfg.getHelpUrl(), request.getAttribute("helpUrl"));
		assertEquals(cfg.getHelpTopic(), request.getAttribute("helpTopic"));
	}
	public void testConfigureNestedObjectBack() throws Exception {
		addRequestParameter("action", "Back");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("pluginConfig.obj.nestedValue", "foobar");
		addRequestParameter("focus", "pluginConfig.obj");
		
		final PluginConfigForm form = new PluginConfigForm();
		
		final PluginConfigStub cfg = new PluginConfigStub();
		form.setPluginConfig(request, cfg, false);
		form.setFocus("pluginConfig.obj");
		form.introspect(request);
		assertEquals("Setup > Plugins > Mock Plugin > Nested Object", request.getAttribute("location"));
		
		request.getSession().setAttribute("pluginConfigForm", form);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));

		assertEquals("foobar", ((PluginConfigStub)form.getPluginConfig()).getObj().getNestedValue());
		assertEquals("pluginConfig", form.getFocus());
		assertEquals("Setup > Plugins > Mock Plugin", request.getAttribute("location"));
		assertEquals(cfg.getHelpUrl(), request.getAttribute("helpUrl"));
		assertEquals(cfg.getHelpTopic(), request.getAttribute("helpTopic"));
	}
	public void testBackSortsArray() throws Exception {
		addRequestParameter("action", "Back");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("pluginConfig.arr[1].value", "5");
		addRequestParameter("focus", "pluginConfig.arr[1]");
		
		final PluginConfigForm form = new PluginConfigForm();
		
		final PluginConfigWithArray cfg = new PluginConfigWithArray();
		final NestedArrayElement[] arr = new NestedArrayElement[2];
		final NestedArrayElement lastElem = new NestedArrayElement();
		final NestedArrayElement firstElem = new NestedArrayElement();
		arr[0] = lastElem;
		arr[0].setValue(10);
		arr[1] = firstElem;
		
		cfg.setArr(arr);
		form.setPluginConfig(request, cfg, false);
		form.setFocus("pluginConfig.arr[1]");
		form.introspect(request);
		
		request.getSession().setAttribute("pluginConfigForm", form);
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals(5, firstElem.getValue());
		assertEquals(10, lastElem.getValue());

		assertSame(firstElem, cfg.getArr()[0]);
		assertSame(lastElem, cfg.getArr()[1]);
	}
	public void testConfigureRenamableNestedObjectBack() throws Exception {
		addRequestParameter("action", "Back");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("pluginConfig.renameable.name", "b");
		addRequestParameter("focus", "pluginConfig.renameable");
		
		final PluginConfigForm form = new PluginConfigForm();
		
		final PluginConfigStub pluginConfig = new PluginConfigStub();
		pluginConfig.getRenameable().setName("a");
		
		form.setPluginConfig(request, pluginConfig, false);
		form.setFocus("pluginConfig.renameable");
		form.introspect(request);
		
		request.getSession().setAttribute("pluginConfigForm", form);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));

		final PluginConfigStub configured = ((PluginConfigStub)form.getPluginConfig());
		
		assertEquals("b", configured.getRenameable().getName());
		assertEquals("pluginConfig", form.getFocus());
		assertTrue(configured.getRenameable().isRenamed());
		
		assertEquals(1, form.getRenamedProfiles().size());
	}
	public void testConfigureRenamableNestedObjectNotRenamedBack() throws Exception {
		addRequestParameter("action", "Back");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("pluginConfig.renameable.name", "a");
		addRequestParameter("focus", "pluginConfig.renameable");
		
		final PluginConfigForm form = new PluginConfigForm();
		
		final PluginConfigStub pluginConfig = new PluginConfigStub();
		pluginConfig.getRenameable().setName("a");
		
		form.setPluginConfig(request, pluginConfig, false);
		form.setFocus("pluginConfig.renameable");
		form.introspect(request);
		
		request.getSession().setAttribute("pluginConfigForm", form);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));

		final PluginConfigStub configured = ((PluginConfigStub)form.getPluginConfig());
		
		assertEquals("a", configured.getRenameable().getName());
		assertEquals("pluginConfig", form.getFocus());
		assertFalse(configured.getRenameable().isRenamed());
		
		assertEquals(0, form.getRenamedProfiles().size());
	}
	public void testConfigureAddIndexedNestedObjectNullArray() throws Exception {
		addRequestParameter("action", "Add");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("focus", "pluginConfig");
		addRequestParameter("target", "pluginConfig.arr");
		
		final PluginConfigForm form = new PluginConfigForm();
		form.setPluginConfig(request, new PluginConfigWithArray(), false);
		request.getSession().setAttribute("pluginConfigForm", form);
		assertEquals("Setup > Plugins > Mock Plugin", request.getAttribute("location"));

		expect(mgr.createObject("a.b.c.plugin", NestedArrayElement.class.getName()))
			.andReturn(new NestedArrayElement());
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));

		assertEquals("pluginConfig.arr[0]", form.getFocus());
		assertEquals("Setup > Plugins > Mock Plugin > Nested List of Objects", request.getAttribute("location"));
	}
	public void testConfigureAddIndexedNestedObject() throws Exception {
		addRequestParameter("action", "Add");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("focus", "pluginConfig");
		addRequestParameter("target", "pluginConfig.arr");
		
		final PluginConfigForm form = new PluginConfigForm();
		final PluginConfigWithArray pluginConfigWithArray = new PluginConfigWithArray();
		pluginConfigWithArray.setArr(new NestedArrayElement[] {new NestedArrayElement(321) });
		
		form.setPluginConfig(request, pluginConfigWithArray, false);
		request.getSession().setAttribute("pluginConfigForm", form);
		
		expect(mgr.createObject("a.b.c.plugin", NestedArrayElement.class.getName()))
			.andReturn(new NestedArrayElement());

		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));

		assertEquals("pluginConfig.arr[1]", form.getFocus());
		assertEquals(321, ((PluginConfigWithArray)form.getPluginConfig()).getArr()[0].getValue());
		assertEquals(2, ((PluginConfigWithArray)form.getPluginConfig()).getArr().length);
	}
	public void testConfigureRemoveIndexedNestedObject() throws Exception {
		addRequestParameter("action", "Remove");
		addRequestParameter("pluginId", "a.b.c.plugin");
		addRequestParameter("focus", "pluginConfig");
		addRequestParameter("target", "pluginConfig.arr[2]");
		
		final PluginConfigForm form = new PluginConfigForm();
		final PluginConfigWithArray pluginConfigWithArray = new PluginConfigWithArray();
		pluginConfigWithArray.setArr(new NestedArrayElement[] {
				new NestedArrayElement(0),
				new NestedArrayElement(1),
				new NestedArrayElement(2),
				new NestedArrayElement(3)
				});
		
		form.setPluginConfig(request, pluginConfigWithArray, false);
		request.getSession().setAttribute("pluginConfigForm", form);
		request.removeAttribute("location");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("configure");
		
		assertSame(form, request.getSession().getAttribute("pluginConfigForm"));

		assertEquals("pluginConfig", form.getFocus());
		final PluginConfigWithArray config = ((PluginConfigWithArray)form.getPluginConfig());
		assertEquals(3, config.getArr().length);
		
		assertEquals(0, config.getArr()[0].getValue());
		assertEquals(1, config.getArr()[1].getValue());
		assertEquals(3, config.getArr()[2].getValue());
		assertEquals("Setup > Plugins > Mock Plugin", request.getAttribute("location"));
	}
	public static class PluginConfigWithArray extends PluginConfigStub {
		private NestedArrayElement[] arr;
		
		@Override
		public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
			try {
				final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>(super.getPropertyDescriptors(locale));
				final PropertyDescriptor pd = new PropertyDescriptor("arr", PluginConfigWithArray.class);
				pd.setDisplayName("Nested List of Objects");
	
				pds.add(pd);
				
				return pds;
			} catch (IntrospectionException e) {
				throw (Error)new AssertionFailedError("").initCause(e);
			}
		}
		public NestedArrayElement[] getArr() {
			return arr;
		}
		public void setArr(NestedArrayElement[] arr) {
			this.arr = arr;
		}
	}
	
	public static class NestedArrayElement extends BaseDto implements Comparable<NestedArrayElement> {
		private int value;

		public NestedArrayElement() {
			value = 0;
		}
		public NestedArrayElement(int i) {
			value = i;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
		public int compareTo(NestedArrayElement o) {
			return ((Integer)value).compareTo(o.value);
		}
		
	}
}
