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
import java.util.Arrays;
import java.util.List;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.core.ProjectBuildConfigurator;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.PluginNotConfigurableException;
import net.sourceforge.vulcan.integration.BuildToolPlugin;
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
	
	RepositoryAdaptorPlugin rap1 = createMock(RepositoryAdaptorPlugin.class);
	RepositoryAdaptorPlugin rap2 = createMock(RepositoryAdaptorPlugin.class);
	
	RepositoryAdaptor ra = createMock(RepositoryAdaptor.class);
	
	BuildToolPlugin btp1 = createMock(BuildToolPlugin.class);
	BuildToolPlugin btp2 = createMock(BuildToolPlugin.class);
	
	ProjectBuildConfigurator buildConfigurator = createMock(ProjectBuildConfigurator.class);
	
	List<RepositoryAdaptorPlugin> repositoryPlugins = Arrays.asList(rap1, rap2);
	List<BuildToolPlugin> buildToolPlugins = Arrays.asList(btp1, btp2);
	
	ProjectConfigDto projectConfig = new ProjectConfigDto();
	
	String url = "http://localhost/";
	
	File tmpFile;
	
	IOException ioException = new IOException("foo");
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		tmpFile = TestUtils.resolveRelativeFile("pom.xml");
		
		importer.setPluginManager(pluginManager);
		importer.setStateManager(stateManager);
		
		projectConfig.setRepositoryAdaptorPluginId("fake.repo.id");
	}

	public void trainNoRepositorySupportsUrl() throws Exception {
		expect(pluginManager.getPlugins(RepositoryAdaptorPlugin.class))
			.andReturn(repositoryPlugins);
	
		expect(rap1.createInstanceForUrl(url)).andReturn(null);
		expect(rap2.createInstanceForUrl(url)).andReturn(null);
	}
	
	@TrainingMethod("trainNoRepositorySupportsUrl")
	public void testNoRepositorySupportsUrl() throws Exception {
		try {
			importer.createProjectsForUrl(url);
			fail("Expected exception");
		} catch (ConfigException e) {
			assertEquals("errors.url.unsupported", e.getKey());
		}
	}
	
	public void trainWrapsIOExceptionDuringDownload() throws Exception {
		expect(pluginManager.getPlugins(RepositoryAdaptorPlugin.class))
			.andReturn(repositoryPlugins);

		expect(rap1.createInstanceForUrl(url)).andReturn(ra);
		
		ra.download(tmpFile);
		expectLastCall().andThrow(ioException);
	}

	@TrainingMethod("trainWrapsIOExceptionDuringDownload")
	public void testWrapsIOExceptionDuringDownload() throws Exception {
		try {
			importer.createProjectsForUrl(url);
			fail("Expected exception");
		} catch (ConfigException e) {
			assertSame(ioException, e.getCause());
			assertEquals("errors.import.download", e.getKey());
			assertEquals(ioException.getMessage(), e.getArgs()[0]);
		}
	}
	
	public void trainNoBuildToolSupportsFile() throws Exception {
		expect(pluginManager.getPlugins(RepositoryAdaptorPlugin.class))
			.andReturn(repositoryPlugins);

		expect(rap1.createInstanceForUrl(url)).andReturn(ra);
		
		ra.download(tmpFile);
		
		expect(pluginManager.getPlugins(BuildToolPlugin.class))
			.andReturn(buildToolPlugins);
		
		expect(btp1.createProjectConfigurator(tmpFile)).andReturn(null);
		expect(btp2.createProjectConfigurator(tmpFile)).andReturn(null);
	}

	@TrainingMethod("trainNoBuildToolSupportsFile")
	public void testNoBuildToolSupportsFile() throws Exception {
		try {
			importer.createProjectsForUrl(url);
			fail("expected exception");
		} catch (ConfigException e) {
			assertEquals("errors.build.file.unsupported", e.getKey());
		}
	}
	
	public void trainConfigures() throws Exception {
		expect(pluginManager.getPlugins(RepositoryAdaptorPlugin.class))
			.andReturn(repositoryPlugins);
	
		expect(rap1.createInstanceForUrl(url)).andReturn(ra);
		
		ra.download(tmpFile);
		
		expect(pluginManager.getPlugins(BuildToolPlugin.class))
			.andReturn(buildToolPlugins);
		
		expect(btp1.createProjectConfigurator(tmpFile)).andReturn(null);
		expect(btp2.createProjectConfigurator(tmpFile)).andReturn(buildConfigurator);
		
		expect(ra.getProjectConfig()).andReturn(projectConfig);
		
		buildConfigurator.applyConfiguration(projectConfig);
	}

	public void trainSaves() throws Exception {
		expect(pluginManager.getPluginConfigInfo("fake.repo.id"))
			.andThrow(new PluginNotConfigurableException());
		
		stateManager.addProjectConfig(projectConfig);
	}
	
	public void trainStandalone() throws Exception {
		expect(buildConfigurator.isStandaloneProject()).andReturn(true);
		ra.setNonRecursive();
	}
	
	@TrainingMethod("trainConfigures,trainStandalone,trainSaves")
	public void testConfiguresAndSavesStandalone() throws Exception {
		importer.createProjectsForUrl(url);
	}

	public void trainNotStandalone() throws Exception {
		expect(buildConfigurator.isStandaloneProject()).andReturn(false);
	}

	@TrainingMethod("trainConfigures,trainNotStandalone,trainSaves")
	public void testConfiguresAndSavesNotStandalone() throws Exception {
		importer.createProjectsForUrl(url);
	}
}
