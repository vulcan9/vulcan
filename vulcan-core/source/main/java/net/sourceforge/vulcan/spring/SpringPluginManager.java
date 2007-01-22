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
package net.sourceforge.vulcan.spring;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.Store;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.ComponentVersionDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.PluginMetaDataDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.DuplicatePluginIdException;
import net.sourceforge.vulcan.exception.PluginLoadFailureException;
import net.sourceforge.vulcan.exception.PluginNotConfigurableException;
import net.sourceforge.vulcan.exception.PluginNotFoundException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.integration.BuildManagerObserverPlugin;
import net.sourceforge.vulcan.integration.BuildToolPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.Plugin;
import net.sourceforge.vulcan.integration.ProjectNameAwarePlugin;
import net.sourceforge.vulcan.integration.RepositoryAdaptorPlugin;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.Resource;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class SpringPluginManager 
		implements PluginManager, ApplicationContextAware {
	final static Log log = LogFactory.getLog(SpringPluginManager.class);
	
	ApplicationContext ctx;
	
	Store store;
	EventHandler eventHandler;
	BuildEventPluginPublisher buildEventPluginPublisher;
	DelegatingResourceBundleMessageSource messageSource;
	SpringBeanXmlEncoder.FactoryExpert factoryExpert;
	
	String pluginBeanName;
	
	boolean importBundledPlugins;
	String bundledPluginResourcesPattern;
	
	boolean initialized;
	
	final Map<String, PluginState> plugins = new HashMap<String, PluginState>();
	
	class PluginState {
		FileSystemXmlApplicationContext context;
		ClassLoader classLoader;
		PluginMetaDataDto pluginConfig;
		Plugin plugin;
		
		PluginState(PluginMetaDataDto pluginConfig, String contextPath, String beanName, ApplicationContext ctx) throws Exception {
			final ClassLoader tmpLoader = Thread.currentThread().getContextClassLoader();
			
			this.pluginConfig = pluginConfig;
			this.classLoader = URLClassLoader.newInstance(this.pluginConfig.getClassPath(), tmpLoader);
			
			try {
				Thread.currentThread().setContextClassLoader(this.classLoader);

				this.context = new FileSystemXmlApplicationContext(
						new String[] {contextPath}, false, ctx);
				
				this.context.addBeanFactoryPostProcessor(
					new BeanFactoryPostProcessor() {
						public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
							beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
								public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
									if (bean instanceof ResourceBundleMessageSource) {
										((ResourceBundleMessageSource)bean).setBundleClassLoader(classLoader);
									}
									return bean;
								}
								public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
									return bean;
								}
							});
						}
					});
				this.context.refresh();
				this.plugin = (Plugin) context.getBean(beanName);
			} catch (Exception e) {
				this.classLoader = null;
				throw e;
			} finally {
				Thread.currentThread().setContextClassLoader(tmpLoader);
			}
		}
		public void destroy() {
			context.close();
			context = null;
			classLoader = null;
			plugin = null;
		}
		public String getId() {
			return plugin.getId();
		}
	}
	public void init() {
		initialized = true;
		
		importBundledPlugins();
		
		final PluginMetaDataDto[] plugins = store.getPluginConfigs();
		
		for (int i=0; i<plugins.length; i++) {
			try {
				createPlugin(plugins[i], false);
			} catch (PluginLoadFailureException e) {
				log.error(e.getMessage(), e);
				final ErrorEvent errorEvent = new ErrorEvent(this,
						"PluginManager.load.failed",
						new Object[] {plugins[i].getId(), e.getMessage()},
						e);
				eventHandler.reportEvent(errorEvent);
			}
		}
	}
	public void shutdown() {
		while (plugins.size() > 0) {
			final String id = plugins.keySet().iterator().next();
			destroyPlugin(id);
		}
	}

	public synchronized void importPluginZip(InputStream in) throws StoreException, PluginLoadFailureException {
		final PluginMetaDataDto pluginConfig = store.extractPlugin(in);
		createPlugin(pluginConfig, true);
	}
	public synchronized void removePlugin(String id) throws StoreException, PluginNotFoundException {
		destroyPlugin(id);
		
		store.deletePlugin(id);
	}
	public synchronized File getPluginDirectory(String id) throws PluginNotFoundException {
		return findPluginState(id).pluginConfig.getDirectory();
	}
	public synchronized PluginConfigDto getPluginConfigInfo(String id) throws PluginNotConfigurableException, PluginNotFoundException {
		final PluginState state = findPluginState(id);
		
		if (state.plugin instanceof ConfigurablePlugin) {
			final PluginConfigDto configuration = ((ConfigurablePlugin)state.plugin).getConfiguration();
			configuration.setApplicationContext(state.context);
			return configuration;
		}
		throw new PluginNotConfigurableException();
	}
	public void configurePlugin(PluginConfigDto pluginConfig) throws PluginNotFoundException {
		final PluginState state = findPluginState(pluginConfig.getPluginId());
		
		if (state.plugin instanceof ConfigurablePlugin) {
			((ConfigurablePlugin)state.plugin).setConfiguration(pluginConfig);
		}
	}
	public Object createObject(String id, String className) throws PluginNotFoundException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		final PluginState state = findPluginState(id);
		
		final Object object = state.classLoader.loadClass(className).newInstance();
		if (object instanceof ApplicationContextAware) {
			((ApplicationContextAware)object).setApplicationContext(state.context);
		}
		
		return object;
	}
	@SuppressWarnings("unchecked")
	public Enum createEnum(String id, String className, String enumName) throws ClassNotFoundException, PluginNotFoundException {
		final PluginState state = findPluginState(id);
		
		 Class c = state.classLoader.loadClass(className);
		 return Enum.valueOf(c, enumName);
	}

	public synchronized List<Plugin> getPlugins() {
		final List<Plugin> list = new ArrayList<Plugin>();

		for (PluginState state : plugins.values()) {
			list.add(state.plugin);
		}
		Collections.sort(list, new Comparator<Plugin>() {
			public int compare(Plugin o1, Plugin o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return list;
	}
	
	public synchronized List<ComponentVersionDto> getPluginVersions() {
		final ArrayList<ComponentVersionDto> versions = new ArrayList<ComponentVersionDto>();
		
		for (PluginState p : plugins.values()) {
			final ComponentVersionDto v = new ComponentVersionDto(
					ComponentVersionDto.ComponentType.Plugin);
			
			v.setId(p.pluginConfig.getId());
			v.setVersion(p.pluginConfig.getVersion());
			v.setName(p.plugin.getName());
			
			versions.add(v);
		}
		
		Collections.sort(versions, new Comparator<ComponentVersionDto>() {
			public int compare(ComponentVersionDto o1, ComponentVersionDto o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		return versions;
	}
	
	public synchronized List<Plugin> getRepositoryPlugins() {
		final List<Plugin> list = getPlugins();
		
		for (Iterator<Plugin> itr = list.iterator(); itr.hasNext();) {
			final Plugin plugin = itr.next();
			if (!(plugin instanceof RepositoryAdaptorPlugin)) {
				itr.remove();
			}
		}
		return list;
	}
	public List<Plugin> getBuildToolPlugins() {
		final List<Plugin> list = getPlugins();
		
		for (Iterator<Plugin> itr = list.iterator(); itr.hasNext();) {
			final Plugin plugin = itr.next();
			if (!(plugin instanceof BuildToolPlugin)) {
				itr.remove();
			}
		}
		return list;
	}
	public List<Plugin> getObserverPlugins() {
		final List<Plugin> list = getPlugins();
		
		for (Iterator<Plugin> itr = list.iterator(); itr.hasNext();) {
			final Plugin plugin = itr.next();
			if (!(plugin instanceof BuildManagerObserverPlugin)) {
				itr.remove();
			}
		}
		return list;
	}
	public RepositoryAdaptor createRepositoryAdaptor(String id, ProjectConfigDto projectConfig) throws PluginNotFoundException, ConfigException {
		final PluginState state = findPluginState(id);
		
		final RepositoryAdaptorPlugin plugin = (RepositoryAdaptorPlugin) state.plugin;
		
		return plugin.createInstance(projectConfig, projectConfig.getRepositoryAdaptorConfig());
	}
	public BuildTool createBuildTool(String id, BuildToolConfigDto buildToolConfig) throws PluginNotFoundException, ConfigException {
		final PluginState state = findPluginState(id);
		
		final BuildToolPlugin plugin = (BuildToolPlugin) state.plugin;
		
		return plugin.createInstance(buildToolConfig);
	}
	public RepositoryAdaptorConfigDto getRepositoryAdaptorDefaultConfig(String id) throws PluginNotFoundException {
		final PluginState state = findPluginState(id);
		
		final RepositoryAdaptorConfigDto defaultConfig = 
			((RepositoryAdaptorPlugin)state.plugin).getDefaultConfig();
		defaultConfig.setApplicationContext(state.context);
		
		return defaultConfig;
	}
	public BuildToolConfigDto getBuildToolDefaultConfig(String id) throws PluginNotFoundException {
		final PluginState state = findPluginState(id);
		
		final BuildToolConfigDto defaultConfig = ((BuildToolPlugin)state.plugin).getDefaultConfig();
		defaultConfig.setApplicationContext(state.context);
		
		return defaultConfig;
	}
	public void projectNameChanged(String oldName, String newName) {
		for (PluginState state : plugins.values()) {
			if (state.plugin instanceof ProjectNameAwarePlugin) {
				((ProjectNameAwarePlugin)state.plugin).projectNameChanged(oldName, newName);
			}
		}
	}
	public Store getStore() {
		return store;
	}
	public void setStore(Store store) {
		this.store = store;
	}
	public String getPluginBeanName() {
		return pluginBeanName;
	}
	public void setPluginBeanName(String pluginBeanName) {
		this.pluginBeanName = pluginBeanName;
	}
	public EventHandler getEventHandler() {
		return eventHandler;
	}
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	public DelegatingResourceBundleMessageSource getMessageSource() {
		return messageSource;
	}
	public void setMessageSource(DelegatingResourceBundleMessageSource messageSource) {
		this.messageSource = messageSource;
	}
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.ctx = applicationContext;
	}
	public BuildEventPluginPublisher getBuildEventPluginPublisher() {
		return buildEventPluginPublisher;
	}
	public void setBuildEventPluginPublisher(
			BuildEventPluginPublisher buildEventPluginPublisher) {
		this.buildEventPluginPublisher = buildEventPluginPublisher;
	}
	public boolean isImportBundledPlugins() {
		return importBundledPlugins;
	}
	public void setImportBundledPlugins(boolean importBundledPlugins) {
		this.importBundledPlugins = importBundledPlugins;
	}
	public String getBundledPluginResourcesPattern() {
		return bundledPluginResourcesPattern;
	}
	public void setBundledPluginResourcesPattern(
			String bundledPluginResourcesPattern) {
		this.bundledPluginResourcesPattern = bundledPluginResourcesPattern;
	}
	public SpringBeanXmlEncoder.FactoryExpert getFactoryExpert() {
		return factoryExpert;
	}
	public void setFactoryExpert(SpringBeanXmlEncoder.FactoryExpert factoryExpert) {
		this.factoryExpert = factoryExpert;
	}
	synchronized void createPlugin(PluginMetaDataDto plugin, boolean deleteOnFailure) throws PluginLoadFailureException {
		final String id = plugin.getId();
		
		String contextPath = new File(plugin.getDirectory(), "vulcan-plugin.xml").getAbsolutePath();
		
		try {
			final PluginState state = new PluginState(plugin, contextPath, pluginBeanName, ctx);
			
			if (plugins.containsKey(id)) {
				destroyPlugin(id);
			}
			plugins.put(id, state);
			factoryExpert.registerPlugin(state.classLoader, state.getId());
			
			messageSource.addDelegate(state.context);
			if (state.plugin instanceof BuildManagerObserverPlugin) {
				buildEventPluginPublisher.add((BuildManagerObserverPlugin)state.plugin);
			}
		} catch (Exception e) {
			if (deleteOnFailure) {
				try {
					store.deletePlugin(id);
				} catch (Exception e1) {
					eventHandler.reportEvent(new ErrorEvent(
							this, "PluginManager.delete.failed", new Object[] {id}, e1));
				}
			}
			throw new PluginLoadFailureException(e.getMessage(), e);
		}
	}
	synchronized PluginState findPluginState(final String id) throws PluginNotFoundException {
		final PluginState state = plugins.get(id);
		if (state == null) {
			throw new PluginNotFoundException(id);
		}
		return state;
	}
	private void destroyPlugin(String id) {
		final PluginState state = plugins.remove(id);
		if (state != null) {
			if (state.plugin instanceof BuildManagerObserverPlugin) {
				buildEventPluginPublisher.remove((BuildManagerObserverPlugin) state.plugin);
			}
			messageSource.removeDelegate(state.context);
			state.destroy();
		}
	}
	private void importBundledPlugins() {
		if (!importBundledPlugins) {
			return;
		}
		
		final Resource[] resources;
		
		try {
			resources = ctx.getResources(bundledPluginResourcesPattern);
		} catch (IOException e) {
			log.error("Failure looking for bundled plugins", e);
			return;
		}
		
		for (Resource r : resources) {
			try {
				store.extractPlugin(r.getInputStream());
			} catch (DuplicatePluginIdException ignore) {
				// This happens when plugin is up to date.
			} catch (Exception e) {
				log.error("Error extracting bundled plugin", e);
				ErrorEvent errorEvent = new ErrorEvent(this,
						"PluginManager.load.failed",
						new Object[] {r.getFilename(), e.getMessage()},
						e);
				eventHandler.reportEvent(errorEvent);
			}
		}
	}
}
