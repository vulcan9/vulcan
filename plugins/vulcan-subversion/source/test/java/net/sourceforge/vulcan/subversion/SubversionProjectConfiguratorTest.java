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
package net.sourceforge.vulcan.subversion;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionProjectConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionRepositoryProfileDto;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;

public class SubversionProjectConfiguratorTest extends TestCase {
	SubversionConfigDto globalConfig = new SubversionConfigDto();
	SubversionProjectConfigDto repoConfig = new SubversionProjectConfigDto();
	ProjectConfigDto projectConfig = new ProjectConfigDto();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		repoConfig.setRepositoryProfile("a");
		repoConfig.setPath("");
		
		SubversionRepositoryProfileDto profile = new SubversionRepositoryProfileDto();
		profile.setDescription("a");
		profile.setRootUrl("http://localhost/svn");
		
		globalConfig.setProfiles(new SubversionRepositoryProfileDto[] {profile});
	}
	
	public void testCreateInstanceForUrlUnsupported() throws Exception {
		assertNull(SubversionProjectConfigurator.createInstance("cvs:localhost:/cvsroot", null, null));
	}
	
	public void testGetRepositoryProfileForUrlCreatesOnMissing() throws Exception {
		SVNRepository fakeRepo = new FakeRepo("http://localhost/root");
		ProjectConfigDto project = new ProjectConfigDto();
		SubversionProjectConfigDto raProjectConfig = new SubversionProjectConfigDto();
		
		final SubversionRepositoryProfileDto profile =
			SubversionProjectConfigurator.findOrCreateProfile(fakeRepo, globalConfig, project, raProjectConfig, "http://localhost/root/pom.xml");
		
		assertNotNull(profile);
		
		assertEquals("http://localhost/root", profile.getRootUrl());
		assertEquals("http://localhost/root", profile.getDescription());
		assertEquals("", profile.getUsername());
		assertEquals("", profile.getPassword());
		
		assertEquals(SubversionConfigDto.PLUGIN_ID, project.getRepositoryAdaptorPluginId());
		assertSame(raProjectConfig, project.getRepositoryAdaptorConfig());
		assertEquals(profile.getDescription(), raProjectConfig.getRepositoryProfile());
		assertEquals("", raProjectConfig.getPath());
	}

	
	public void testGetRepositoryProfileForUrlReuseOnMatchRoot() throws Exception {
		SVNRepository fakeRepo = new FakeRepo("http://localhost/svn");
		ProjectConfigDto project = new ProjectConfigDto();
		SubversionProjectConfigDto raProjectConfig = new SubversionProjectConfigDto();
		
		final SubversionRepositoryProfileDto profile =
			SubversionProjectConfigurator.findOrCreateProfile(fakeRepo, globalConfig,
					project, raProjectConfig, "http://localhost/svn/trunk/pom.xml");
		
		assertNotNull(profile);
		assertSame(globalConfig.getProfiles()[0], profile);
		
		assertEquals("http://localhost/svn", profile.getRootUrl());
		assertEquals("a", profile.getDescription());
		
		assertEquals(SubversionConfigDto.PLUGIN_ID, project.getRepositoryAdaptorPluginId());
		assertSame(raProjectConfig, project.getRepositoryAdaptorConfig());
		assertEquals("a", raProjectConfig.getRepositoryProfile());
		assertEquals("/trunk", raProjectConfig.getPath());
	}
	
	public void testUpdateConfigAddsIfMissing() throws Exception {
		final SubversionConfigDto globalConfig = new SubversionConfigDto();
		final SubversionProjectConfigDto projectRaConfig = new SubversionProjectConfigDto();
		final SubversionRepositoryProfileDto profile = new SubversionRepositoryProfileDto();
		
		profile.setRootUrl("http://localhost/svn");
		projectRaConfig.setPath("/trunk");
		
		final SubversionProjectConfigurator cfgr = new SubversionProjectConfigurator(
				globalConfig, projectRaConfig, profile, null);
		
		cfgr.updateGlobalConfig(globalConfig);
		
		assertEquals(1, globalConfig.getProfiles().length);
		assertSame(profile, globalConfig.getProfiles()[0]);
	}
	
	public void testUpdateConfigDoesNotAddWhenPresent() throws Exception {
		final SubversionConfigDto globalConfig = new SubversionConfigDto();
		final SubversionProjectConfigDto projectRaConfig = new SubversionProjectConfigDto();
		final SubversionRepositoryProfileDto profile1 = new SubversionRepositoryProfileDto();
		final SubversionRepositoryProfileDto profile2 = new SubversionRepositoryProfileDto();
		
		globalConfig.setProfiles(new SubversionRepositoryProfileDto[] {profile2});
		
		profile1.setRootUrl("http://localhost/svn");
		profile2.setRootUrl("http://localhost/svn");
		
		projectRaConfig.setPath("/trunk");
		
		final SubversionProjectConfigurator cfgr = new SubversionProjectConfigurator(
				globalConfig, projectRaConfig, profile1, null);
		
		cfgr.updateGlobalConfig(globalConfig);
		
		assertEquals(1, globalConfig.getProfiles().length);
		assertSame(profile2, globalConfig.getProfiles()[0]);
	}
	
	public static class FakeRepo extends SVNRepositoryImpl {
		final String root;
		
		public FakeRepo(String root) {
			super(null, null);
			this.root = root;
		}
		@Override
		public SVNURL getRepositoryRoot(boolean forceConnection) throws SVNException {
			assertTrue("Expected true but was false", forceConnection);
			return SVNURL.parseURIEncoded(root);
		}
	}
}
