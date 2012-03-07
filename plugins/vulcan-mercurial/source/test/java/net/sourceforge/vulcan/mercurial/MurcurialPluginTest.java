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
package net.sourceforge.vulcan.mercurial;

import java.io.File;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.dto.ProjectConfigDto;

public class MurcurialPluginTest extends EasyMockTestCase {
	private MercurialPlugin plugin = new MercurialPlugin();
	private ProjectConfigDto projectConfig = new ProjectConfigDto();
	private MercurialProjectConfig settings = new MercurialProjectConfig();
	private PluginManager pluginManager;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		projectConfig.setRepositoryAdaptorConfig(settings);
		
		pluginManager = createMock(PluginManager.class);
		plugin.setPluginManager(pluginManager);
	}
	
	public void testDefaultConfig() throws Exception {
		assertNotNull(plugin.getConfiguration());
	}
	
	public void testDefaultProjectConfig() throws Exception {
		assertNotNull(plugin.getDefaultConfig());
	}
	
	public void testGetRepositoryAdapter() throws Exception {
		File dir = new File(".");
		expect(pluginManager.getPluginDirectory(plugin.getId())).andReturn(dir);
		
		replay();
		
		final MercurialRepository repo = plugin.createInstance(projectConfig);
		
		verify();
		
		assertEquals(plugin.getConfiguration(), repo.getGlobals());
		assertEquals(projectConfig, repo.getProjectConfig());
		assertEquals(projectConfig.getRepositoryAdaptorConfig(), repo.getSettings());
		assertEquals(new File(dir, "vulcan-xml.template").getAbsolutePath(), repo.getChangeLogTemplatePath());
	}

}
