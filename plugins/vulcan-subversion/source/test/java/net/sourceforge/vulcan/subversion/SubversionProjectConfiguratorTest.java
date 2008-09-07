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

import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;

public class SubversionProjectConfiguratorTest extends TestCase {
	SubversionConfigDto globalConfig = new SubversionConfigDto();
	SubversionProjectConfigDto repoConfig = new SubversionProjectConfigDto();
	ProjectConfigDto projectConfig = new ProjectConfigDto();
	SubversionRepositoryProfileDto profile = new SubversionRepositoryProfileDto();
	
	SVNProperties bugtraqProps = new SVNProperties();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		repoConfig.setRepositoryProfile("a");
		repoConfig.setPath("");
		
		profile.setDescription("a");
		profile.setRootUrl("http://localhost/svn");
		
		globalConfig.setProfiles(new SubversionRepositoryProfileDto[] {profile});
	}
	
	public void testCreateInstanceForUrlUnsupported() throws Exception {
		assertNull(SubversionProjectConfigurator.createInstance("cvs:localhost:/cvsroot", null, null, null, null));
	}
	
	public void testGetRepositoryProfileForUrlCreatesOnMissing() throws Exception {
		SVNRepository fakeRepo = new FakeRepo("http://localhost/root", "");
		ProjectConfigDto project = new ProjectConfigDto();
		SubversionProjectConfigDto raProjectConfig = new SubversionProjectConfigDto();
		
		final SubversionRepositoryProfileDto profile =
			SubversionProjectConfigurator.findOrCreateProfile(fakeRepo, globalConfig, project, raProjectConfig,
					"http://localhost/root/pom.xml", "kelly", "p@ssw0rd");
		
		assertNotNull(profile);
		
		assertEquals("http://localhost/root", profile.getRootUrl());
		assertEquals("localhost", profile.getDescription());
		assertEquals("kelly", profile.getUsername());
		assertEquals("p@ssw0rd", profile.getPassword());
		
		assertEquals(SubversionConfigDto.PLUGIN_ID, project.getRepositoryAdaptorPluginId());
		assertSame(raProjectConfig, project.getRepositoryAdaptorConfig());
		assertEquals(profile.getDescription(), raProjectConfig.getRepositoryProfile());
		assertEquals("", raProjectConfig.getPath());
	}

	public void testGetRepositoryProfileForUrlReuseOnMatchRoot() throws Exception {
		SVNRepository fakeRepo = new FakeRepo("http://localhost/svn", "/trunk");
		ProjectConfigDto project = new ProjectConfigDto();
		SubversionProjectConfigDto raProjectConfig = new SubversionProjectConfigDto();
		
		final SubversionRepositoryProfileDto profile =
			SubversionProjectConfigurator.findOrCreateProfile(fakeRepo, globalConfig,
					project, raProjectConfig, "http://localhost/svn/trunk/pom.xml", null, null);
		
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
				projectRaConfig, profile, null);
		
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
				projectRaConfig, profile1, null);
		
		cfgr.updateGlobalConfig(globalConfig);
		
		assertEquals(1, globalConfig.getProfiles().length);
		assertSame(profile2, globalConfig.getProfiles()[0]);
	}
	
	public void testApplyBugtraqPropsNull() throws Exception {
		final SubversionProjectConfigurator cfgr = new SubversionProjectConfigurator(
				repoConfig, profile, null, new FakeRepo(profile.getRootUrl(), repoConfig.getPath()));
		
		cfgr.applyConfiguration(projectConfig, "http://localhost/svn");

		assertEquals("", projectConfig.getBugtraqUrl());
		assertEquals("", projectConfig.getBugtraqLogRegex1());
		assertEquals("", projectConfig.getBugtraqLogRegex2());
	}
	
	public void testApplyBugtraqPropsDoesNotOverwriteWhenNull() throws Exception {
		final SubversionProjectConfigurator cfgr = new SubversionProjectConfigurator(
				repoConfig, profile, null, new FakeRepo(profile.getRootUrl(), repoConfig.getPath()));

		projectConfig.setBugtraqUrl("something manually configured.");
		
		cfgr.applyConfiguration(projectConfig, "http://localhost/svn");

		assertEquals("something manually configured.", projectConfig.getBugtraqUrl());
	}
	
	public void testApplyBugtraqProps() throws Exception {
		final SubversionProjectConfigurator cfgr = new SubversionProjectConfigurator(
				repoConfig, profile, null, new FakeRepo(profile.getRootUrl(), repoConfig.getPath()));
	
		bugtraqProps.put(SubversionSupport.BUGTRAQ_URL, "http://localhost");
		bugtraqProps.put(SubversionSupport.BUGTRAQ_LOGREGEX, "bug (\\d+)");
		
		cfgr.applyConfiguration(projectConfig, "http://localhost/svn");

		assertEquals("http://localhost", projectConfig.getBugtraqUrl());
		assertEquals("bug (\\d+)", projectConfig.getBugtraqLogRegex1());
		assertEquals("", projectConfig.getBugtraqLogRegex2());
	}
	
	public void testApplyBugtraqPropsWithMessage() throws Exception {
		final SubversionProjectConfigurator cfgr = new SubversionProjectConfigurator(
				repoConfig, profile, null, new FakeRepo(profile.getRootUrl(), repoConfig.getPath()));
	
		bugtraqProps.put(SubversionSupport.BUGTRAQ_URL, "http://localhost");
		bugtraqProps.put(SubversionSupport.BUGTRAQ_MESSAGE, "Bug: %BUGID%");
		
		cfgr.applyConfiguration(projectConfig, "http://localhost/svn");

		assertEquals("http://localhost", projectConfig.getBugtraqUrl());
		assertEquals("Bug: (\\d+)", projectConfig.getBugtraqLogRegex1());
		assertEquals("", projectConfig.getBugtraqLogRegex2());
	}
	
	public void testApplyBugtraqPropsAll() throws Exception {
		final SubversionProjectConfigurator cfgr = new SubversionProjectConfigurator(
				repoConfig, profile, null, new FakeRepo(profile.getRootUrl(), repoConfig.getPath()));
	
		bugtraqProps.put(SubversionSupport.BUGTRAQ_URL, "http://localhost");
		bugtraqProps.put(SubversionSupport.BUGTRAQ_LOGREGEX, "[Ii]ssue \\d+\r\n\\d+");
		bugtraqProps.put(SubversionSupport.BUGTRAQ_MESSAGE, "Bug: %BUGID%");
		
		cfgr.applyConfiguration(projectConfig, "http://localhost/svn");

		assertEquals("http://localhost", projectConfig.getBugtraqUrl());
		assertEquals("[Ii]ssue \\d+|Bug: (\\d+)", projectConfig.getBugtraqLogRegex1());
		assertEquals("\\d+", projectConfig.getBugtraqLogRegex2());
	}
	
	public class FakeRepo extends SVNRepositoryImpl {
		final String root;
		final String path;
		
		public FakeRepo(String root, String path) {
			super(null, null);
			this.root = root;
			this.path = path;
		}
		@Override
		public SVNURL getRepositoryRoot(boolean forceConnection) throws SVNException {
			assertTrue("Expected true but was false", forceConnection);
			return SVNURL.parseURIEncoded(root);
		}
		@Override
		@SuppressWarnings("unchecked")
		public long getDir(String path, long revision, SVNProperties properties, ISVNDirEntryHandler handler) throws SVNException {
			assertEquals(this.path, path);
			assertNull(handler);
			assertNotNull(properties);
			
			
			properties.putAll(bugtraqProps);
			
			return 11;
		}
	}
}
