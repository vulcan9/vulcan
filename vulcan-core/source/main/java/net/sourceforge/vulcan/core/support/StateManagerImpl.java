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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.ConfigurationStore;
import net.sourceforge.vulcan.core.DependencyBuildPolicy;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.core.ProjectNameChangeListener;
import net.sourceforge.vulcan.core.WorkingCopyUpdateStrategy;
import net.sourceforge.vulcan.dto.BuildManagerConfigDto;
import net.sourceforge.vulcan.dto.ComponentVersionDto;
import net.sourceforge.vulcan.dto.ConfigUpdatesDto;
import net.sourceforge.vulcan.dto.NamedObject;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.PluginProfileDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.exception.PluginNotFoundException;
import net.sourceforge.vulcan.exception.ProjectNeedsDependencyException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.scheduler.BuildDaemon;
import net.sourceforge.vulcan.scheduler.ProjectScheduler;
import net.sourceforge.vulcan.scheduler.Scheduler;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;


@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class StateManagerImpl implements StateManager, ProjectManager {
	/* Dependencies */
	ConfigurationStore configurationStore;
	BuildManager buildManager;
	PluginManager pluginManager;
	String version;
	
	/* State */
	boolean running;
	StateManagerConfigDto config;
	Map<SchedulerConfigDto, ProjectScheduler> schedulers = new HashMap<SchedulerConfigDto, ProjectScheduler>();
	Map<SchedulerConfigDto, BuildDaemon> buildDaemons = new HashMap<SchedulerConfigDto, BuildDaemon>();

	List<ProjectNameChangeListener> projectNameChangeListeners = new ArrayList<ProjectNameChangeListener>();
	
	/* Locks for State information */
	final Lock readLock;
	final Lock writeLock;
	
	public StateManagerImpl() {
		final ReadWriteLock lock = new ReentrantReadWriteLock();
		
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}
	
	public void start() throws StoreException {
		try {
			writeLock.lock();
			
			config = configurationStore.loadConfiguration();
			
			applyPluginConfigurations();
			
			final BuildManagerConfigDto buildManagerConfig = config.getBuildManagerConfig();
			
			buildManagerConfig.addPropertyChangeListener("enabled", new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					final Object newValue = evt.getNewValue();
					if (!evt.getOldValue().equals(newValue)) {
						if (Boolean.TRUE.equals(newValue)) {
							startSchedulers();	
						} else {
							stopSchedulers();
						}
					}
				}
			});
			buildManager.init(buildManagerConfig);
			
			running = true;
			
			if (buildManagerConfig.isEnabled()) {
				startSchedulers();
			}
			
			startBuildDaemons();
		} finally {
			writeLock.unlock();
		}
	}
	public void shutdown() throws StoreException {
		try {
			readLock.lock();
			
			if (!this.running) {
				return;
			}
		} finally {
			readLock.unlock();
		}
		
		try {
			writeLock.lock();
			stopSchedulers();
			stopBuildDaemons();
			
			this.running = false;
			
			save();
		} finally {
			writeLock.unlock();
		}
	}
	public void save() throws StoreException {
		try {
			readLock.lock();
			configurationStore.storeConfiguration(this.config);
		} finally {
			readLock.unlock();
		}
			
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public List<ComponentVersionDto> getComponentVersions() {
		final List<ComponentVersionDto> versions = new ArrayList<ComponentVersionDto>();
		
		versions.addAll(pluginManager.getPluginVersions());
		
		return versions;
	}
	public void applyMultipleUpdates(ConfigUpdatesDto updates) throws DuplicateNameException, StoreException, PluginNotFoundException {
		try {
			writeLock.lock();
			
			final Collection<ProjectConfigDto> newProjectConfigs = updates.getNewProjectConfigs();
			if (newProjectConfigs != null && newProjectConfigs.size() > 0) {
				addOrReplaceProjectConfigInternal(false, false, newProjectConfigs.toArray(new ProjectConfigDto[newProjectConfigs.size()]));
			}
			
			if (updates.getModifiedPluginConfigs() != null) {
				for (PluginConfigDto pluginConfig : updates.getModifiedPluginConfigs().values()) {
					updatePluginConfigInternal(pluginConfig, null, false);
				}
			}
			
			save();
		} finally {
			writeLock.unlock();
		}
	}
	public List<String> getProjectConfigNames() {
		final List<String> list = new ArrayList<String>();
		
		try {
			readLock.lock();
			for (ProjectConfigDto project : config.getProjects()) {
				list.add(project.getName());
			}
		} finally {
			readLock.unlock();
		}
		return list;
	}
	public ProjectConfigDto getProjectConfig(String name) throws NoSuchProjectException {
		try {
			readLock.lock();
			final ProjectConfigDto config = (ProjectConfigDto) getConfigOrNull(name, this.config.getProjects());
			if (config == null) {
				throw new NoSuchProjectException(name);
			}
			return config;
		} finally {
			readLock.unlock();
		}
	}
	public void addProjectConfig(ProjectConfigDto... configs) throws DuplicateNameException, StoreException {
		addOrReplaceProjectConfigInternal(true, true, configs);
	}
	public void addOrReplaceProjectConfig(ProjectConfigDto... configs) throws StoreException {
		try {
			addOrReplaceProjectConfigInternal(false, true, configs);
		} catch (DuplicateNameException e) {
			throw new RuntimeException(e);
		}
	}
	public void updateProjectConfig(String oldName, ProjectConfigDto updatedConfig, boolean setLastModifiedDate) throws DuplicateNameException, NoSuchProjectException, StoreException {
		try {
			writeLock.lock();
			if (!oldName.equals(updatedConfig.getName())) {
				if (((ProjectConfigDto) getConfigOrNull(updatedConfig.getName(), this.config.getProjects())) != null) {
					throw new DuplicateNameException(updatedConfig.getName());
				}
				projectNameChanged(oldName, updatedConfig.getName());
			}

			if (setLastModifiedDate) {
				updatedConfig.setLastModificationDate(new Date());
			}
			
			final ProjectConfigDto[] all = this.config.getProjects();
			for (int i=0; i<all.length; i++) {
				if (all[i].getName().equals(oldName)) {
					all[i] = updatedConfig;
					projectsUpdated(all);
					save();
					return;
				}
			}
			
			throw new NoSuchProjectException(oldName);
		} finally {
			writeLock.unlock();
		}
	}
	public void deleteProjectConfig(String... names) throws ProjectNeedsDependencyException, StoreException {
		try {
			writeLock.lock();
			
			final ProjectConfigDto[] all = config.getProjects();
			
			final Set<String> deletedProjectNames = new HashSet<String>(
					Arrays.asList(names));
			
			List<String> dependentProjectNames = null;
			List<String> projectsWithDependencies = null;
			
			for (int i=0; i<names.length; i++) {
				final String name = names[i];
				
				for (int j=0; j<all.length; j++) {
					if (deletedProjectNames.contains(all[j].getName())) {
						continue;
					}
					
					if (Arrays.asList(all[j].getDependencies()).contains(name)) {
						if (projectsWithDependencies == null) {
							projectsWithDependencies = new ArrayList<String>();
						}
						if (!projectsWithDependencies.contains(name)) {
							projectsWithDependencies.add(name);
						}
						
						if (dependentProjectNames == null) {
							dependentProjectNames = new ArrayList<String>();
						}
						if (!dependentProjectNames.contains(all[j].getName())) {
							dependentProjectNames.add(all[j].getName());
						}
					}
				}
			}
			
			if (dependentProjectNames != null) {
				throw new ProjectNeedsDependencyException(
						projectsWithDependencies.toArray(new String[projectsWithDependencies.size()]), 
						dependentProjectNames.toArray(new String[dependentProjectNames.size()]));	
			}
			
			ProjectConfigDto[] prunedProjects = all;
			
			for (String projectName : names) {
				prunedProjects = removeByName(projectName, prunedProjects);	
			}
			
			config.setProjects(prunedProjects);
			
			save();
		} finally {
			writeLock.unlock();
		}
	}
	public void applyProjectLabel(String label, Collection<String> projectNames) throws StoreException {
		try {
			writeLock.lock();
			
			final Set<ProjectConfigDto> labeled = new HashSet<ProjectConfigDto>();
			
			for (String name : projectNames) {
				final ProjectConfigDto config = (ProjectConfigDto) getConfigOrNull(name, this.config.getProjects());
				if (config == null) {
					throw new NoSuchProjectException(name);
				}
				labeled.add(config);
			}
			
			for (ProjectConfigDto config : this.config.getProjects()) {
				if (labeled.contains(config)) {
					config.getLabels().add(label);
				} else {
					config.getLabels().remove(label);
				}
			}
			
			save();
		} finally {
			writeLock.unlock();
		}
	}
	public List<String> getProjectConfigNamesByLabel(String label) {
		try {
			readLock.lock();
			
			final List<String> names = new ArrayList<String>();
			
			for (ProjectConfigDto config : this.config.getProjects()) {
				if (config.getLabels().contains(label)) {
					names.add(config.getName());
				}
			}
			
			return names;
		} finally {
			readLock.unlock();
		}
	}
	public List<String> getProjectLabels() {
		final Set<String> labels = new HashSet<String>();

		try {
			readLock.lock();
			
			for (ProjectConfigDto config : this.config.getProjects()) {
				labels.addAll(config.getLabels());
			}
		} finally {
			readLock.unlock();
		}
		
		final ArrayList<String> list = new ArrayList<String>(labels);
		Collections.sort(list);
		return list;
	}
	public SchedulerConfigDto getSchedulerConfig(String name) {
		try {
			readLock.lock();
			final SchedulerConfigDto config = (SchedulerConfigDto) getConfigOrNull(name, this.config.getSchedulers());
			if (config == null) {
				throw new IllegalArgumentException(name);
			}
			return config;
		} finally {
			readLock.unlock();
		}
	}
	public void addSchedulerConfig(SchedulerConfigDto config) throws DuplicateNameException, StoreException {
		try {
			writeLock.lock();
			final SchedulerConfigDto[] previous = this.config.getSchedulers();
			final String name = config.getName();
			if (getConfigOrNull(name, previous) != null) {
				throw new DuplicateNameException(name);
			}
			
			final SchedulerConfigDto[] all = new SchedulerConfigDto[previous.length+1];
			
			System.arraycopy(previous, 0, all, 0, previous.length);
			all[previous.length] = config;
			
			schedulesUpdated(all);
			save();
			
			if (this.running && this.config.getBuildManagerConfig().isEnabled()) {
				startScheduler(config);
			}
		} finally {
			writeLock.unlock();
		}
	}
	public void updateSchedulerConfig(String oldName, SchedulerConfigDto updatedConfig, boolean save) throws DuplicateNameException, StoreException {
		try {
			writeLock.lock();
			if (!oldName.equals(updatedConfig.getName())) {
				schedulerNameChanged(oldName, updatedConfig.getName());			
			}
			final SchedulerConfigDto[] all = this.config.getSchedulers();
			updateSchedulerConfig(oldName, updatedConfig, all, schedulers);
			schedulesUpdated(all);
			
			if (save) {
				save();
			}
		} finally {
			writeLock.unlock();
		}
	}
	public void deleteSchedulerConfig(String name) {
		try {
			writeLock.lock();
			final SchedulerConfigDto config = (SchedulerConfigDto) getConfigOrNull(name, this.config.getSchedulers());
			
			final SchedulerConfigDto[] schedules = 
				removeByName(name, this.config.getSchedulers());
			this.config.setSchedulers(schedules);
			
			for (ProjectConfigDto project : this.config.getProjects()) {
				final List<String> asList = Arrays.asList(project.getSchedulerNames());
				
				if (asList.contains(name)) {
					List<String> pruned = new ArrayList<String>(asList);
					pruned.remove(name);
					project.setSchedulerNames(pruned.toArray(new String[pruned.size()]));
				}
			}
			
			if (config != null) {
				final Scheduler scheduler = schedulers.remove(config);
				if (scheduler != null) {
					scheduler.stop();
				}
			}
		} finally {
			writeLock.unlock();
		}
	}
	
	public ProjectConfigDto[] getProjectsForScheduler(String schedulerName) {
		final List<ProjectConfigDto> list = new ArrayList<ProjectConfigDto>(Arrays.asList(this.config.getProjects()));
		
		try {
			readLock.lock();
			
			for (Iterator<ProjectConfigDto> itr = list.iterator(); itr.hasNext();) {
				final ProjectConfigDto config = itr.next();
				
				if (!Arrays.asList(config.getSchedulerNames()).contains(schedulerName)) {
					itr.remove();
				}
			}
		} finally {
			readLock.unlock();
		}
		
		return list.toArray(new ProjectConfigDto[list.size()]);
	}

	public SchedulerConfigDto getBuildDaemonConfig(String name) {
		final SchedulerConfigDto config;
		
		try {
			readLock.lock();
			config = (SchedulerConfigDto) getConfigOrNull(name, this.config.getBuildDaemons());
		} finally {
			readLock.unlock();
		}
		
		if (config == null) {
			throw new IllegalArgumentException(name);
		}
		return config;
	}
	public void updateBuildDaemonConfig(String oldName, SchedulerConfigDto updatedConfig) throws DuplicateNameException, StoreException {
		try {
			writeLock.lock();
			final SchedulerConfigDto[] all = this.config.getBuildDaemons();
			updateSchedulerConfig(oldName, updatedConfig, all, buildDaemons);
			buildDaemonsUpdated(all);
			save();
		} finally {
			writeLock.unlock();
		}
	}
	public void addBuildDaemonConfig(SchedulerConfigDto scheduler) throws DuplicateNameException, StoreException {
		try {
			writeLock.lock();
			final SchedulerConfigDto[] previous = this.config.getBuildDaemons();
			final String name = scheduler.getName();
			if (getConfigOrNull(name, previous) != null) {
				throw new DuplicateNameException(name);
			}
			
			final SchedulerConfigDto[] all = new SchedulerConfigDto[previous.length+1];
			
			System.arraycopy(previous, 0, all, 0, previous.length);
			all[previous.length] = scheduler;
			
			buildDaemonsUpdated(all);
			save();
			
			if (this.running) {
				startBuildDaemon(scheduler);
			}
		} finally {
			writeLock.unlock();
		}
	}
	public void deleteBuildDaemonConfig(String name) throws StoreException {
		try {
			writeLock.lock();
			
			SchedulerConfigDto[] buildDaemons = this.config.getBuildDaemons();
			final SchedulerConfigDto config = (SchedulerConfigDto) getConfigOrNull(name, buildDaemons);
			
			final SchedulerConfigDto[] schedules = 
				removeByName(name, buildDaemons);
			this.config.setBuildDaemons(schedules);
			
			if (config != null) {
				final Scheduler scheduler = this.buildDaemons.remove(config);
				if (scheduler != null) {
					scheduler.stop();
				}
			}
			
			save();
		} finally {
			writeLock.unlock();
		}
	}
	public void updatePluginConfig(PluginConfigDto pluginConfig, Set<PluginProfileDto> renamedProfiles) throws PluginNotFoundException, StoreException {
		updatePluginConfigInternal(pluginConfig, renamedProfiles, true);
	}
	public void removePlugin(String pluginId) throws StoreException, PluginNotFoundException {
		try {
			writeLock.lock();
			config.getPluginConfigs().remove(pluginId);
		} finally {
			writeLock.unlock();
		}

		pluginManager.removePlugin(pluginId);
		save();
	}
	public Date getPluginModificationDate(String pluginId) {
		try {
			readLock.lock();
			
			final PluginConfigDto pluginConfig = config.getPluginConfigs().get(pluginId);
			
			if (pluginConfig != null) {
				return pluginConfig.getLastModificationDate();
			}
			
			return null;
		} finally {
			readLock.unlock();
		}
	}
	public void flushBuildQueue() {
		buildManager.clear();
	}
	public List<ProjectScheduler> getSchedulers() {
		final List<ProjectScheduler> list;
		try {
			readLock.lock();
			list = new ArrayList<ProjectScheduler>(schedulers.values());
		} finally {
			readLock.unlock();
		}
		
		sortSchedulers(list);
		
		return list;
	}
	public List<BuildDaemon> getBuildDaemons() {
		final List<BuildDaemon> list;
		
		try {
			readLock.lock();
			list = new ArrayList<BuildDaemon>(buildDaemons.values());
		} finally {
			readLock.unlock();
		}
		
		sortSchedulers(list);
		
		return list;
	}
	public BuildDaemon getBuildDaemon(String name) {
		try {
			readLock.lock();
			for (BuildDaemon bd : buildDaemons.values()) {
				if (bd.getName().equals(name)) {
					return bd;
				}
			}
			return null;
		} finally {
			readLock.unlock();
		}
	}
	public RepositoryAdaptor getRepositoryAdaptor(final ProjectConfigDto projectConfig) throws ConfigException {
		final String pluginId = projectConfig.getRepositoryAdaptorPluginId();
		
		if (!StringUtils.isBlank(pluginId)) {
			try {
				return pluginManager.createRepositoryAdaptor(pluginId, projectConfig);
			} catch (PluginNotFoundException e) {
			}
		}
		
		throw new ConfigException("messages.repository.not.configured", null);
	}
	public BuildTool getBuildTool(ProjectConfigDto projectConfig) throws ConfigException {
		final String pluginId = projectConfig.getBuildToolPluginId();
		
		if (!StringUtils.isBlank(pluginId)) {
			try {
				return pluginManager.createBuildTool(pluginId, projectConfig.getBuildToolConfig());
			} catch (PluginNotFoundException e) {
			}
		}
		
		throw new ConfigException("messages.build.tool.not.configured", null);
	}
	public DependencyGroup buildDependencyGroup(ProjectConfigDto[] projects, DependencyBuildPolicy policy, WorkingCopyUpdateStrategy updateStrategyOverride, boolean buildOnDependencyFailure, boolean buildOnNoUpdates) {
		return DependencyGroupBuilder.buildDependencyGroup(projects, this, policy, updateStrategyOverride, buildOnDependencyFailure, buildOnNoUpdates);
	}
	public StateManagerConfigDto getConfig() {
		return config;
	}
	public boolean isRunning() {
		try {
			readLock.lock();
			
			return running;
		} finally {
			readLock.unlock();
		}
	}
	public ConfigurationStore getConfigurationStore() {
		return configurationStore;
	}
	public void setConfigurationStore(ConfigurationStore store) {
		this.configurationStore = store;
	}
	public void setProjectNameChangeListeners(
			List<ProjectNameChangeListener> projectNameChangeListeners) {
		this.projectNameChangeListeners = projectNameChangeListeners;
	}
	public BuildManager getBuildManager() {
		return buildManager;
	}
	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
	public PluginManager getPluginManager() {
		return pluginManager;
	}
	public void setPluginManager(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}

	
	protected abstract ProjectScheduler createProjectScheduler();
	protected abstract BuildDaemon createBuildDaemon();
	
	protected void startSchedulers() {
		try {
			writeLock.lock();
			final SchedulerConfigDto[] configs = config.getSchedulers();
			
			for (int i=0; i<configs.length; i++) {
				startScheduler(configs[i]);
			}
		} finally {
			writeLock.unlock();
		}
	}
	protected void stopSchedulers() {
		try {
			writeLock.lock();
			for (Iterator<? extends Scheduler> itr = schedulers.values().iterator(); itr.hasNext();) {
				Scheduler scheduler = itr.next();
				scheduler.stop();
				itr.remove();
			}
		} finally {
			writeLock.unlock();
		}
	}
	protected void startBuildDaemons() {
		try {
			writeLock.lock();
			final SchedulerConfigDto[] configs = config.getBuildDaemons();
			
			for (int i=0; i<configs.length; i++) {
				startBuildDaemon(configs[i]);
			}
		} finally {
			writeLock.unlock();
		}
	}
	protected void stopBuildDaemons() {
		try {
			writeLock.lock();

			for (Iterator<? extends Scheduler> itr = buildDaemons.values().iterator(); itr.hasNext();) {
				Scheduler scheduler = itr.next();
				scheduler.stop();
				itr.remove();
			}
		} finally {
			writeLock.unlock();
		}
	}
	private void addOrReplaceProjectConfigInternal(boolean throwOnDuplicate, boolean saveOnSuccess, ProjectConfigDto... configs) throws DuplicateNameException, StoreException {
		try {
			writeLock.lock();	
			
			final ProjectConfigDto[] previous = this.config.getProjects();

			final List<ProjectConfigDto> allConfigs = new ArrayList<ProjectConfigDto>(
					Arrays.asList(previous));
			
			for (ProjectConfigDto config : configs) {
				final NamedObject existingProject = getConfigOrNull(config.getName(), previous);
				if (existingProject != null) {
					if (throwOnDuplicate) {
						throw new DuplicateNameException(config.getName());
					}
					if (!allConfigs.remove(existingProject)) {
						throw new IllegalStateException();
					}
				}
				
				config.setLastModificationDate(new Date());
				
				allConfigs.add(config);
			}
			
			projectsUpdated(allConfigs.toArray(new ProjectConfigDto[allConfigs.size()]));
			
			if (saveOnSuccess) {
				save();
			}
		} finally {
			writeLock.unlock();
		}
	}
	private void updatePluginConfigInternal(PluginConfigDto pluginConfig, Set<PluginProfileDto> renamedProfiles, boolean saveOnSuccess) throws PluginNotFoundException, StoreException {
		try {
			writeLock.lock();
			
			pluginConfig.setLastModificationDate(new Date());
			config.getPluginConfigs().put(pluginConfig.getPluginId(), pluginConfig);
			
			if (renamedProfiles != null) {
				updateRenamedProfiles(renamedProfiles);
			}
		} finally {
			writeLock.unlock();
		}
		
		pluginManager.configurePlugin(pluginConfig);
		
		if (saveOnSuccess) {
			save();
		}
	}
	private void projectsUpdated(ProjectConfigDto[] projects) {
		Arrays.sort(projects);
		this.config.setProjects(projects);
	}
	private void schedulesUpdated(SchedulerConfigDto[] schedulers) {
		Arrays.sort(schedulers);
		this.config.setSchedulers(schedulers);
	}
	private void buildDaemonsUpdated(SchedulerConfigDto[] buildDaemons) {
		Arrays.sort(buildDaemons);
		this.config.setBuildDaemons(buildDaemons);
	}
	private void startScheduler(final SchedulerConfigDto config) {
		final ProjectScheduler scheduler = createProjectScheduler();
		scheduler.init(config);
		scheduler.start();
		schedulers.put(config, scheduler);
	}
	private void startBuildDaemon(final SchedulerConfigDto config) {
		final BuildDaemon scheduler = createBuildDaemon();
		scheduler.init(config);
		scheduler.start();
		buildDaemons.put(config, scheduler);
	}
	private NamedObject getConfigOrNull(String name, NamedObject[] elements) {
		final NamedObject[] elems = elements;
		if (elems == null) {
			return null;
		}
		for (int i=0; i<elems.length; i++) {
			if (name.equals(elems[i].getName())) {
				return elems[i];
			}
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	private <T extends NamedObject> T[] removeByName(String name, T[] elements) {
		int pos=-1;
		for (int i=0; i<elements.length; i++) {
			if (name.equals(elements[i].getName())) {
				pos = i;
				break;
			}
		}
		if (pos < 0) {
			throw new IllegalArgumentException(name);
		}
		
		final T[] adjusted = (T[])Array.newInstance(elements[0].getClass(), elements.length-1);
		System.arraycopy(elements, 0, adjusted, 0, pos);
		System.arraycopy(elements, pos+1, adjusted, pos, elements.length - pos - 1);
		
		return adjusted;
	}	
	private void projectNameChanged(String oldName, String newName) {
		final ProjectConfigDto[] projects = this.config.getProjects();
		
		for (ProjectConfigDto project : projects) {
			final List<String> deps = new ArrayList<String>(Arrays.asList(project.getDependencies()));
			if (deps.remove(oldName)) {
				deps.add(newName);
				Collections.sort(deps);
				project.setDependencies(deps.toArray(new String[deps.size()]));
			}
		}
		
		for (ProjectNameChangeListener listener : projectNameChangeListeners) {
			listener.projectNameChanged(oldName, newName);
		}
	}
	private void schedulerNameChanged(String oldName, String newName) {
		final ProjectConfigDto[] projects = this.config.getProjects();
		
		for (ProjectConfigDto project : projects) {
			final String[] schedulerNames = project.getSchedulerNames();
			final int index = Arrays.asList(schedulerNames).indexOf(oldName);
			if (index >= 0) {
				schedulerNames[index] = newName;
			}
		}
	}
	private static void sortSchedulers(final List<? extends Scheduler> list) {
		Collections.sort(list, new Comparator<Scheduler>() {
			public int compare(Scheduler o1, Scheduler o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
	}
	private <S extends Scheduler> void updateSchedulerConfig(String oldName, SchedulerConfigDto updatedConfig,
			final SchedulerConfigDto[] all, Map<SchedulerConfigDto, S> map) throws DuplicateNameException, StoreException {
		if (!oldName.equals(updatedConfig.getName())) {
			if (getConfigOrNull(updatedConfig.getName(), all) != null) {
				throw new DuplicateNameException(updatedConfig.getName());
			}
		}
		for (int i=0; i<all.length; i++) {
			if (all[i].getName().equals(oldName)) {
				final S scheduler = map.remove(all[i]);
				if (scheduler != null) {
					scheduler.configurationChanged(updatedConfig);
					map.put(updatedConfig, scheduler);
				}
				all[i] = updatedConfig;
			}
		}
	}
	private void applyPluginConfigurations() {
		final Map<String, PluginConfigDto> configs = config.getPluginConfigs();
		
		for (Iterator<String> itr = configs.keySet().iterator(); itr.hasNext();) {
			final String pluginId = itr.next();
			final PluginConfigDto config = configs.get(pluginId);
			if (config == null) {
				itr.remove();
				continue;
			}
			try {
				pluginManager.configurePlugin(config);
			} catch (PluginNotFoundException e) {
				itr.remove();
			}
		}
	}
	private void updateRenamedProfiles(Set<PluginProfileDto> renamedProfiles) {
		final ProjectConfigDto[] projects = config.getProjects();
		
		for (PluginProfileDto profile : renamedProfiles) {
			final String pluginId = profile.getPluginId();
			
			for (ProjectConfigDto project : projects) {
				if (pluginId.equals(project.getBuildToolPluginId())) {
					updateProfileNameIfNecessary(project.getBuildToolConfig(), profile);
				}
				
				if (pluginId.equals(project.getRepositoryAdaptorPluginId())) {
					updateProfileNameIfNecessary(project.getRepositoryAdaptorConfig(), profile);
				}
			}
		}
	}
	private void updateProfileNameIfNecessary(PluginConfigDto buildToolConfig, PluginProfileDto profile) {
		final String projectConfigProfilePropertyName = profile.getProjectConfigProfilePropertyName();
		try {
			final String projectSetting = (String) PropertyUtils.getProperty(
					buildToolConfig, projectConfigProfilePropertyName);
			
			if (profile.getOldName().equals(projectSetting)) {
				PropertyUtils.setProperty(
						buildToolConfig, projectConfigProfilePropertyName, profile.getName());
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
}	
