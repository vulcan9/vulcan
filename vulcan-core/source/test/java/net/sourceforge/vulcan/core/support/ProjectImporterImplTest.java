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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.ProjectRepositoryConfigurator;
import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.core.Store;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.PluginNotConfigurableException;
import net.sourceforge.vulcan.integration.BuildToolPlugin;
import net.sourceforge.vulcan.integration.PluginStub;
import net.sourceforge.vulcan.integration.RepositoryAdaptorPlugin;

public class ProjectImporterImplTest extends EasyMockTestCase {
	ProjectImporterImpl importer = new ProjectImporterImpl() {
		@Override
		protected File createTempFile() throws IOException {
			return tmpFile;
		}
	};
	
	PluginManager pluginManager = createMock(PluginManager.class);
	StateManager stateManager = createMock(StateManager.class);
	Store store = createMock(Store.class);
	
	RepositoryAdaptorPlugin rap1 = createMock(RepositoryAdaptorPlugin.class);
	RepositoryAdaptorPlugin rap2 = createMock(RepositoryAdaptorPlugin.class);
	
	ProjectRepositoryConfigurator repoConfigurator = createMock(ProjectRepositoryConfigurator.class);
	
	BuildToolPlugin btp1 = createMock(BuildToolPlugin.class);
	BuildToolPlugin btp2 = createMock(BuildToolPlugin.class);
	
	List<String> existingProjectNames = Arrays.asList("fakeExistingProject1");
	
	ProjectBuildConfigurator buildConfiguratorMock = createMock(ProjectBuildConfigurator.class);
	ProjectBuildConfigurator buildConfigurator = new ProjectBuildConfigurator() {
		public void applyConfiguration(ProjectConfigDto projectConfig, List<String> existingProjectNames, boolean createSubprojects) {
			buildConfiguratorMock.applyConfiguration(projectConfig, existingProjectNames, false);
			if (playback) {
				projectConfig.setName(projectName);
				projectConfig.setWorkDir(workDir);
			}
		}
		public boolean isStandaloneProject() {
			return buildConfiguratorMock.isStandaloneProject();
		}
		public List<String> getSubprojectUrls() {
			return buildConfiguratorMock.getSubprojectUrls();
		}
	};
	
	List<RepositoryAdaptorPlugin> repositoryPlugins = Arrays.asList(rap1, rap2);
	List<BuildToolPlugin> buildToolPlugins = Arrays.asList(btp1, btp2);
	
	String url = "http://localhost/";
	
	File tmpFile;
	
	IOException ioException = new IOException("foo");
	
	String defaultWorkDirPattern = "a workdir ${projectName} location";
	
	String projectName = "importedProject";
	String workDir = null;
	
	boolean playback = false;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		tmpFile = File.createTempFile("vulcan-ProjectImporterImplTest", ".tmp");
		
		importer.setPluginManager(pluginManager);
		importer.setStateManager(stateManager);
		importer.setStore(store);
		
		expect(store.getWorkingCopyLocationPattern()).andReturn(defaultWorkDirPattern).anyTimes();
		expect(stateManager.getProjectConfigNames()).andReturn(existingProjectNames).anyTimes();
	}

	@Override
	public void replay() {
		super.replay();
		playback = true;
	}
	
	public void trainNoRepositorySupportsUrl() throws Exception {
		expect(pluginManager.getPlugins(RepositoryAdaptorPlugin.class))
			.andReturn(repositoryPlugins);
		expect(pluginManager.getPlugins(BuildToolPlugin.class))
			.andReturn(buildToolPlugins);

		expect(rap1.createProjectConfigurator(url)).andReturn(null);
		expect(rap2.createProjectConfigurator(url)).andReturn(null);
	}
	
	@TrainingMethod("trainNoRepositorySupportsUrl")
	public void testNoRepositorySupportsUrl() throws Exception {
		try {
			importer.createProjectsForUrl(url, false);
			fail("Expected exception");
		} catch (ConfigException e) {
			assertEquals("errors.url.unsupported", e.getKey());
		}
	}
	
	public void trainWrapsIOExceptionDuringDownload() throws Exception {
		expect(pluginManager.getPlugins(RepositoryAdaptorPlugin.class))
			.andReturn(repositoryPlugins);
		expect(pluginManager.getPlugins(BuildToolPlugin.class))
			.andReturn(buildToolPlugins);

		expect(rap1.createProjectConfigurator(url)).andReturn(repoConfigurator);
		expect(rap1.getId()).andReturn("a.fake.repo.plugin");
		
		repoConfigurator.download(tmpFile);
		expectLastCall().andThrow(ioException);
	}

	@TrainingMethod("trainWrapsIOExceptionDuringDownload")
	public void testWrapsIOExceptionDuringDownload() throws Exception {
		try {
			importer.createProjectsForUrl(url, false);
			fail("Expected exception");
		} catch (ConfigException e) {
			assertSame(ioException, e.getCause());
			assertEquals("errors.import.download", e.getKey());
			assertEquals(ioException.getMessage(), e.getArgs()[0]);
		}
		assertFalse(tmpFile.exists());
	}
	
	public void trainNoBuildToolSupportsFile() throws Exception {
		expect(pluginManager.getPlugins(RepositoryAdaptorPlugin.class))
			.andReturn(repositoryPlugins);
		expect(pluginManager.getPlugins(BuildToolPlugin.class))
			.andReturn(buildToolPlugins);

		expect(rap1.createProjectConfigurator(url)).andReturn(repoConfigurator);
		expect(rap1.getId()).andReturn("a.fake.repo.plugin");
		
		repoConfigurator.download(tmpFile);
		
		expect(btp1.createProjectConfigurator(tmpFile)).andReturn(null);
		expect(btp2.createProjectConfigurator(tmpFile)).andReturn(null);
	}

	@TrainingMethod("trainNoBuildToolSupportsFile")
	public void testNoBuildToolSupportsFile() throws Exception {
		try {
			importer.createProjectsForUrl(url, false);
			fail("expected exception");
		} catch (ConfigException e) {
			assertEquals("errors.build.file.unsupported", e.getKey());
		}
		
		assertFalse(tmpFile.exists());
	}
	
	public void trainConfigures() throws Exception {
		expect(pluginManager.getPlugins(RepositoryAdaptorPlugin.class))
			.andReturn(repositoryPlugins);
	
		expect(rap1.createProjectConfigurator(url)).andReturn(repoConfigurator);
		expect(rap1.getId()).andReturn("a.fake.repo.plugin");
		
		repoConfigurator.download(tmpFile);
		
		expect(pluginManager.getPlugins(BuildToolPlugin.class))
			.andReturn(buildToolPlugins);
		
		expect(btp1.createProjectConfigurator(tmpFile)).andReturn(null);
		expect(btp2.createProjectConfigurator(tmpFile)).andReturn(buildConfigurator);
		expect(btp2.getId()).andReturn("a.fake.build.plugin");
		
		final ProjectConfigDto projectConfig = new ProjectConfigDto();
		
		projectConfig.setRepositoryAdaptorPluginId("a.fake.repo.plugin");
		projectConfig.setBuildToolPluginId("a.fake.build.plugin");
		
		buildConfigurator.applyConfiguration(projectConfig, existingProjectNames, false);
		
		final ProjectConfigDto projectConfigWithName = (ProjectConfigDto) projectConfig.copy();
		projectConfigWithName.setName(projectName);
		projectConfigWithName.setWorkDir(workDir);
		
		repoConfigurator.applyConfiguration(projectConfigWithName);
	}

	public void trainSaves() throws Exception {
		expect(pluginManager.getPluginConfigInfo("a.fake.repo.plugin"))
			.andThrow(new PluginNotConfigurableException());

		final ProjectConfigDto projectConfig = new ProjectConfigDto();
		
		projectConfig.setRepositoryAdaptorPluginId("a.fake.repo.plugin");
		projectConfig.setBuildToolPluginId("a.fake.build.plugin");
		projectConfig.setName(projectName);
		projectConfig.setWorkDir("a workdir importedProject location");
		
		stateManager.addProjectConfig(projectConfig);
	}
	
	public void trainSavesWithAlternateWorkDir() throws Exception {
		expect(pluginManager.getPluginConfigInfo("a.fake.repo.plugin"))
			.andThrow(new PluginNotConfigurableException());

		final ProjectConfigDto projectConfig = new ProjectConfigDto();
		
		projectConfig.setRepositoryAdaptorPluginId("a.fake.repo.plugin");
		projectConfig.setBuildToolPluginId("a.fake.build.plugin");
		projectConfig.setName(projectName);
		projectConfig.setWorkDir(workDir);
		
		stateManager.addProjectConfig(projectConfig);
	}
	
	@TrainingMethod("trainConfigures,trainSaves")
	public void testConfiguresAndSaves() throws Exception {
		importer.createProjectsForUrl(url, false);
	}
	
	public void trainStandalone() throws Exception {
		expect(buildConfigurator.isStandaloneProject()).andReturn(true);
		repoConfigurator.setNonRecursive();
		
		expect(buildConfigurator.getSubprojectUrls()).andReturn(null);
	}
	
	@TrainingMethod("trainConfigures,trainStandalone,trainSaves")
	public void testConfiguresAndSavesStandalone() throws Exception {
		importer.createProjectsForUrl(url, true);
	}

	public void trainNotStandalone() throws Exception {
		expect(buildConfigurator.isStandaloneProject()).andReturn(false);
		expect(buildConfigurator.getSubprojectUrls()).andReturn(null);
	}

	@TrainingMethod("trainConfigures,trainNotStandalone,trainSaves")
	public void testConfiguresAndSavesNotStandalone() throws Exception {
		importer.createProjectsForUrl(url, true);
	}

	public void trainSetupAlternateWorkDir() throws Exception {
		workDir = "alternate work dir";
	}
	
	@TrainingMethod("trainSetupAlternateWorkDir,trainConfigures,trainNotStandalone,trainSavesWithAlternateWorkDir")
	public void testConfiguresAndSavesBuildToolSetsWorkDir() throws Exception {
		importer.createProjectsForUrl(url, true);
	}
	
	public void trainGetSubprojects() throws Exception {
		expect(buildConfigurator.isStandaloneProject()).andReturn(false);
		
		expect(buildConfigurator.getSubprojectUrls()).andReturn(
				Collections.singletonList("http://localhost/a-sub-project"));
		
		expect(rap1.createProjectConfigurator("http://localhost/a-sub-project")).andReturn(
				repoConfigurator);
		
		expect(rap1.getId()).andReturn("a.fake.repo.plugin");
		
		repoConfigurator.download(tmpFile);
		
		expect(btp1.createProjectConfigurator(tmpFile)).andReturn(buildConfigurator);
		expect(btp1.getId()).andReturn("a.fake.build.plugin");
		
		final ProjectConfigDto projectConfig = new ProjectConfigDto();
		
		projectConfig.setRepositoryAdaptorPluginId("a.fake.repo.plugin");
		projectConfig.setBuildToolPluginId("a.fake.build.plugin");

		final List<String> updatedExistingProjectNames = new ArrayList<String>(existingProjectNames);
		updatedExistingProjectNames.add(projectName);
		
		buildConfigurator.applyConfiguration(projectConfig, updatedExistingProjectNames, true);
		repoConfigurator.applyConfiguration((ProjectConfigDto)notNull());
		
		expect(buildConfigurator.isStandaloneProject()).andReturn(false);
		expect(buildConfigurator.getSubprojectUrls()).andReturn(null);
	}
	
	public void trainSavesSubproject() throws Exception {
		PluginStub pluginConfig = new PluginStub();
		
		expect(pluginManager.getPluginConfigInfo("a.fake.repo.plugin"))
			.andReturn(pluginConfig);
		
		repoConfigurator.updateGlobalConfig(pluginConfig);
		
		final ProjectConfigDto projectConfig = new ProjectConfigDto();
		
		projectConfig.setRepositoryAdaptorPluginId("a.fake.repo.plugin");
		projectConfig.setBuildToolPluginId("a.fake.build.plugin");
		projectConfig.setName(projectName);
		projectConfig.setWorkDir("a workdir importedProject location");
		
		stateManager.addProjectConfig(projectConfig);
	}
	
	@TrainingMethod("trainConfigures,trainGetSubprojects,trainSaves,trainSavesSubproject")
	public void testCreatesProjectsRecursivelyOnFlag() throws Exception {
		importer.createProjectsForUrl(url, true);
	}
}
