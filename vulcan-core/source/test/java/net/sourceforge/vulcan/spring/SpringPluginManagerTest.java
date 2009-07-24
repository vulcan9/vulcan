/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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
package net.sourceforge.vulcan.spring;

import static net.sourceforge.vulcan.TestUtils.resolveRelativeFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.MockApplicationContext;
import net.sourceforge.vulcan.core.ConfigurationStore;
import net.sourceforge.vulcan.core.ProjectNameChangeListener;
import net.sourceforge.vulcan.core.BeanEncoder.FactoryExpert;
import net.sourceforge.vulcan.dto.ComponentVersionDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.PluginMetaDataDto;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.BuildEvent;
import net.sourceforge.vulcan.event.BuildStartingEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.PluginLoadFailureException;
import net.sourceforge.vulcan.exception.PluginNotConfigurableException;
import net.sourceforge.vulcan.exception.PluginNotFoundException;
import net.sourceforge.vulcan.integration.BuildManagerObserverPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.Plugin;
import net.sourceforge.vulcan.integration.PluginStub;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class SpringPluginManagerTest extends EasyMockTestCase {
	BuildEventPluginPublisher buildEventPluginPublisher = new BuildEventPluginPublisher();
	SpringPluginManager mgr = new SpringPluginManager() {
		@SuppressWarnings("unchecked")
		@Override
		public synchronized <T extends Plugin> List<T> getPlugins(Class<T> type) {
			if (fakePlugins != null) {
				return (List<T>) fakePlugins;
			}
			return super.getPlugins(type);
		}
	};
	DelegatingResourceBundleMessageSource loader = new DelegatingResourceBundleMessageSource();
	
	ConfigurationStore store = createMock(ConfigurationStore.class);
	FactoryExpert expert = createStrictMock(FactoryExpert.class);
	
	List<Plugin> fakePlugins;
	
	@Override
	public void setUp() throws Exception {
		mgr.setConfigurationStore(store);
		
		mgr.setPluginBeanName("plugin");
		mgr.setMessageSource(loader);
		mgr.setBuildEventPluginPublisher(buildEventPluginPublisher);
		mgr.setEventHandler(new EventHandler() {
			public void reportEvent(net.sourceforge.vulcan.event.Event event) {};
		});
		mgr.setFactoryExpert(expert);
		
		initCalled = false;
		destroyCalled = false;
		event = null;
		nameChangeCalled = false;
	}
	
	public void testInstallPlugin() throws Exception {
		final InputStream stream = new ByteArrayInputStream("a".getBytes());
		
		expect(store.extractPlugin(stream)).andReturn(createFakePluginConfig(true));
		expert.registerPlugin((ClassLoader)anyObject(), (String)anyObject());

		replay();
		
		mgr.importPluginZip(stream);
		
		verify();
	}

	public void testImportCreatesPluginState() throws Exception {
		final PluginMetaDataDto plugin = createFakePluginConfig(true);

		final InputStream stream1 = new ByteArrayInputStream("a".getBytes());
		expect(store.extractPlugin(stream1)).andReturn(plugin);

		final InputStream stream2 = new ByteArrayInputStream("a".getBytes());
		expect(store.extractPlugin(stream2)).andReturn(plugin);
		
		expert.registerPlugin((ClassLoader)anyObject(), (String)anyObject());
		expectLastCall().times(2);
		
		replay();
		
		assertEquals(0, mgr.plugins.size());
		
		assertFalse(initCalled);
		mgr.importPluginZip(stream1);
		assertTrue(initCalled);
		
		assertEquals(1, mgr.plugins.size());
		assertTrue(mgr.plugins.containsKey("1"));
		
		mgr.importPluginZip(stream2);
		assertEquals(1, mgr.plugins.size());
		assertTrue(mgr.plugins.containsKey("1"));
		
		verify();
	}

	public void testImportThrowsOnBadPlugin() throws Exception {
		final PluginMetaDataDto plugin = createFakePluginConfig(false);
		final InputStream stream = new ByteArrayInputStream("a".getBytes());
		expect(store.extractPlugin(stream)).andReturn(plugin);
		
		store.deletePlugin(plugin.getId());

		replay();
		
		assertEquals(0, mgr.plugins.size());
		
		try {
			mgr.importPluginZip(stream);
			fail("expected exception");
		} catch (PluginLoadFailureException e) {
		}
		
		assertEquals(0, mgr.plugins.size());
		verify();
	}

	public void testRemovePlugin() throws Exception {
		testImportCreatesPluginState();
		destroyCalled = false;
		
		reset();
		
		store.deletePlugin("1");
		replay();

		assertEquals(1, mgr.plugins.size());
		
		assertFalse(destroyCalled);
		mgr.removePlugin("1");
		assertTrue(destroyCalled);
		assertEquals(0, mgr.plugins.size());
		verify();
	}
	
	public void testNoConfig() throws Exception {
		assertEquals(0, mgr.plugins.size());
		try {
			mgr.createPlugin(createFakePluginConfig(false), true);
			fail("expected exception");
		} catch (PluginLoadFailureException e) {
			final String message = e.getMessage();
			assertTrue(message, message.startsWith("IOException parsing XML document"));
		}
		assertEquals(0, mgr.plugins.size());
	}

	public void testConfigBeanNoExist() throws Exception {
		assertEquals(0, mgr.plugins.size());
		try {
			mgr.createPlugin(createFakePluginConfig("source/test/pluginTests/pluginWrongBeanName"), true);
			fail("expected exception");
		} catch (PluginLoadFailureException e) {
			assertTrue(e.getMessage().startsWith("No bean named 'plugin' is defined"));
		}

		assertEquals(0, mgr.plugins.size());
	}
	static boolean initCalled = false;
	static boolean destroyCalled = false;
	static boolean nameChangeCalled = false;
	static BuildEvent event = null;
	static PluginConfigDto configBean = new PluginStub();
	
	public static class MockPlugin implements BuildManagerObserverPlugin, ProjectNameChangeListener {
		String id;
		public void setId(String id) {
			this.id = id;
		}
		public String getId() {
			return id;
		}
		public String getName() {
			return id;
		}
		public void init() {
			initCalled = true;
		}
		public void end() {
			destroyCalled = true;
		}
		public void onBuildStarting(BuildStartingEvent evt) {
			event = evt;
		}
		public void onBuildCompleted(BuildCompletedEvent evt) {
			event = evt;
		}
		public void projectNameChanged(String oldName, String newName) {
			nameChangeCalled = true;
		}
	}
	public static class MockPlugin2 extends MockPlugin implements ConfigurablePlugin, ApplicationContextAware {
		ApplicationContext ac;
		public PluginConfigDto getConfiguration() {
			return configBean;
		}
		public void setConfiguration(PluginConfigDto bean) {
			configBean = bean;
		}
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			ac = applicationContext;
		}
	}
	public void testInitCreatesPluginStates() throws Exception {
		expect(store.getPluginConfigs()).andReturn(
				new PluginMetaDataDto[] {createFakePluginConfig(true)});
		
		expert.registerPlugin((ClassLoader)anyObject(), (String)anyObject());

		replay();
		
		assertEquals(0, mgr.plugins.size());
		
		mgr.init();
		
		verify();
		assertEquals(1, mgr.plugins.size());
		assertTrue(mgr.plugins.containsKey("1"));
	}
	public void testPluginAppCtxGetsParentPropertyPlaceholder() throws Exception {
		final PropertyResourceConfigurer cfgr = new PropertyPlaceholderConfigurer();
		
		final StaticApplicationContext ctx = new StaticApplicationContext();
		
		ctx.getBeanFactory().registerSingleton("foo", cfgr);
		
		ctx.refresh();
		
		mgr.setApplicationContext(ctx);
		
		expect(store.getPluginConfigs()).andReturn(
				new PluginMetaDataDto[] {createFakePluginConfig(true)});
		
		expert.registerPlugin((ClassLoader)anyObject(), (String)anyObject());

		replay();
		
		mgr.init();
		
		verify();
		
		assertTrue("should contain cfgr", mgr.plugins.get("1").context.getBeanFactoryPostProcessors().contains(cfgr));
	}
	public void testInitDoesNotDeleteFailedPlugins() throws Exception {
		final PluginMetaDataDto pluginConfig = createFakePluginConfig(false);

		expect(store.getPluginConfigs()).andReturn(new PluginMetaDataDto[] {pluginConfig});
		
		replay();
		
		assertEquals(0, mgr.plugins.size());
		
		mgr.init();
		
		verify();
		assertEquals(0, mgr.plugins.size());
	}
	public void testInitLoadsBundlesPlugins() throws Exception {
		mgr.setImportBundledPlugins(true);
		final String resourcePattern = "/plugins/*.zip";
		final FileSystemResource resource = new FileSystemResource(resolveRelativeFile("source/test/pluginTests/mockPlugin.zip"));
		
		mgr.setBundledPluginResourcesPattern(resourcePattern);
		final MockApplicationContext ctx = new MockApplicationContext() {
			@Override
			public Resource[] getResources(String locationPattern) throws java.io.IOException {
				assertEquals(resourcePattern, locationPattern);
				return new Resource[] { resource };
			};
		};
		ctx.refresh();
		mgr.setApplicationContext(ctx);
		
		final PluginMetaDataDto pluginConfig = createFakePluginConfig(true);
		
		expect(store.extractPlugin((InputStream)anyObject())).andReturn(pluginConfig);

		expect(store.getPluginConfigs()).andReturn(new PluginMetaDataDto[] {pluginConfig});
		expert.registerPlugin((ClassLoader)anyObject(), (String)anyObject());

		replay();
		
		assertEquals(0, mgr.plugins.size());
		
		mgr.init();
		
		
		verify();
		assertEquals(1, mgr.plugins.size());
	}
	public void testShutdownStopsPlugins() throws Exception {
		assertFalse(destroyCalled);
		
		testInitCreatesPluginStates();
		
		assertFalse(destroyCalled);
		
		mgr.shutdown();
		
		assertTrue(destroyCalled);
	}
	public void testCallsBuildManagerObservers() throws Exception {
		expect(store.getPluginConfigs()).andReturn(new PluginMetaDataDto[] {createFakePluginConfig(true)});
		
		expert.registerPlugin((ClassLoader)anyObject(), (String)anyObject());

		replay();
		
		assertEquals(0, mgr.plugins.size());
		
		mgr.init();
		
		verify();
		assertEquals(1, mgr.plugins.size());
		assertTrue(mgr.plugins.containsKey("1"));
		
		assertEquals(null, event);
		
		final EventBridge e = new EventBridge(new BuildCompletedEvent(this, null, null, null));
		buildEventPluginPublisher.onApplicationEvent(e);
		
		assertSame(e.getEvent(), event);
	}
	public void testCallsProjectNameChanged() throws Exception {
		expect(store.getPluginConfigs()).andReturn(new PluginMetaDataDto[] {createFakePluginConfig(true)});
		expert.registerPlugin((ClassLoader)anyObject(), (String)anyObject());

		replay();
		mgr.init();
		verify();
		
		assertFalse(nameChangeCalled);
		
		mgr.projectNameChanged("oldy", "newy");
		
		assertTrue(nameChangeCalled);
	}
	public void testGetPlugins() throws Exception {
		mgr.createPlugin(createFakePluginConfig("source/test/pluginTests/goodPlugin"), true);
		mgr.createPlugin(createFakePluginConfig("source/test/pluginTests/goodPlugin2"), true);
		
		assertEquals(2, mgr.plugins.size());
		
		final List<Plugin> plugins = mgr.getPlugins(Plugin.class);
		
		assertNotNull(plugins);
		
		assertEquals(2, plugins.size());
		
		assertEquals("1", plugins.get(0).getId());
		assertEquals("2", plugins.get(1).getId());
	}
	public void testGetInfoNoPlugin() throws Exception {
		try {
			mgr.getPluginConfigInfo("1");
			fail("expected exception");
		} catch (PluginNotFoundException e) {
			assertEquals("1", e.getPluginId());
		}
	}
	public void testGetPluginVersions() throws Exception {
		mgr.createPlugin(createFakePluginConfig("source/test/pluginTests/goodPlugin"), true);
		mgr.createPlugin(createFakePluginConfig("source/test/pluginTests/goodPlugin2"), true);

		final List<ComponentVersionDto> versions = mgr.getPluginVersions();
		assertNotNull(versions);
		
		assertEquals(2, versions.size());
		
		assertEquals("1", versions.get(0).getId());
		assertEquals("1", versions.get(0).getName());
		assertEquals("2", versions.get(1).getId());
		assertEquals("2", versions.get(1).getName());
	}
	public void testGetInfoNotConfigurable() throws Exception {
		final PluginMetaDataDto plugin = createFakePluginConfig("source/test/pluginTests/goodPlugin");
		
		mgr.createPlugin(plugin, true);
		
		try {
			mgr.getPluginConfigInfo("1");
			fail("expected exception");
		} catch (PluginNotConfigurableException e) {
		}
	}
	public void testGetInfo() throws Exception {
		final PluginMetaDataDto plugin = createFakePluginConfig("source/test/pluginTests/goodPlugin2");
		
		assertNotNull(configBean);
		
		mgr.createPlugin(plugin, true);
		
		final Object bean = mgr.getPluginConfigInfo(plugin.getId());
		assertSame(configBean, bean);
	}
	public void testCreateObject() throws Exception {
		final PluginMetaDataDto plugin = createFakePluginConfig("source/test/pluginTests/goodPlugin2");
		mgr.createPlugin(plugin, true);
		
		final Object bean = mgr.getPluginConfigInfo(plugin.getId());
		
		final Object bean2 = mgr.createObject(plugin.getId(), bean.getClass().getName());
		
		assertNotSame(bean, bean2);
	}
	public void testCreateApplicationContextAwareObject() throws Exception {
		final PluginMetaDataDto plugin = createFakePluginConfig("source/test/pluginTests/goodPlugin2");
		mgr.createPlugin(plugin, true);
		
		final MockPlugin2 bean2 = (MockPlugin2) 
			mgr.createObject(plugin.getId(), MockPlugin2.class.getName());
		
		assertNotNull(bean2.ac);
		
		assertSame(mgr.findPluginState(plugin.getId()).context, bean2.ac);
	}
	public static enum E { A, B };
	public void testCreateEnum() throws Exception {
		final PluginMetaDataDto plugin = createFakePluginConfig("source/test/pluginTests/goodPlugin2");
		mgr.createPlugin(plugin, true);
		
		final Enum<?> e = mgr.createEnum(plugin.getId(), E.class.getName(), "A");
		
		assertEquals(E.A, e);
	}
	public void testChildContextFormatsMessage() throws Exception {
		final PluginMetaDataDto plugin = createFakePluginConfig("source/test/pluginTests/goodPlugin2");
		mgr.createPlugin(plugin, true);
		
		final SpringPluginManager.PluginState state = mgr.plugins.get(plugin.getId());
		
		assertEquals("good afternoon.", state.context.getMessage("plugin.message", null, null));
	}
	private PluginMetaDataDto createFakePluginConfig(boolean valid) {
		if (valid) 
			return createFakePluginConfig("source/test/pluginTests/goodPlugin");
		
		return createFakePluginConfig("source/test/pluginTests/noSuchPlugin");
	}
	int count = 1;
	private PluginMetaDataDto createFakePluginConfig(String path) {
		final PluginMetaDataDto cfg = new PluginMetaDataDto();
		cfg.setId(Integer.toString(count++));
		final File dir = resolveRelativeFile(path);
		cfg.setDirectory(dir);
		cfg.setClassPath(new URL[] {});
		
		return cfg;
	}
}
