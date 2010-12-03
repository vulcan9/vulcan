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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import net.sourceforge.vulcan.ProjectRepositoryConfigurator;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.AuthenticationRequiredRepositoryException;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.subversion.dto.CheckoutDepth;
import net.sourceforge.vulcan.subversion.dto.SubversionConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionProjectConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionRepositoryProfileDto;

import org.springframework.context.ApplicationContext;
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class SubversionProjectConfigurator extends SubversionSupport
		implements ProjectRepositoryConfigurator {

	private final String buildSpecPath;

	protected SubversionProjectConfigurator(SubversionProjectConfigDto config, SubversionRepositoryProfileDto profile, String buildSpecPath) throws ConfigException {
		this(config, profile, buildSpecPath, createRepository(profile, true));
	}
	
	protected SubversionProjectConfigurator(SubversionProjectConfigDto config, SubversionRepositoryProfileDto profile, String buildSpecPath, SVNRepository repository) throws ConfigException {
		super(config, profile, repository);
		this.buildSpecPath = buildSpecPath;
	}
	
	public static SubversionProjectConfigurator createInstance(String url, String username, String password, SubversionConfigDto globalConfig, ApplicationContext appCtx) throws ConfigException {
		final SVNURL svnurl;
		final SVNRepository repo;
		
		try {
			svnurl = SVNURL.parseURIEncoded(url);
			repo = SVNRepositoryFactory.create(svnurl);
		} catch (SVNException e) {
			return null;
		}
		
		final SubversionProjectConfigDto raProjectConfig = new SubversionProjectConfigDto();
		final ProjectConfigDto project = new ProjectConfigDto();
		
		final SubversionRepositoryProfileDto profile;
		try {
			profile = findOrCreateProfile(repo, globalConfig, project, raProjectConfig, url, username, password);
		} catch (SVNException e) {
			final int errorCode = e.getErrorMessage().getErrorCode().getCode();
			if (errorCode == 180001 || errorCode == 175002) {
				// Erros which mean the url did not point to a Subversion repository.
				// 18001: Unable to open an ra_local session to URL (happens with file protocol)
				// 175002: RA layer request failed (happens with http/https protocols).
				return null;
			}
			
			if (errorCode == 170001) {
				throw new AuthenticationRequiredRepositoryException();
			}
			
			throw new ConfigException("svn.error", new Object[] {e.getErrorMessage().getFullMessage()});
		}
		
		raProjectConfig.setApplicationContext(appCtx);
		profile.setApplicationContext(appCtx);
		
		final String buildSpecPath = url.substring(profile.getRootUrl().length());
		
		return new SubversionProjectConfigurator(raProjectConfig, profile, buildSpecPath);
	}
	
	public void download(File target) throws RepositoryException, IOException {
		final OutputStream os = new FileOutputStream(target);
		try {
			svnRepository.getFile(buildSpecPath, SVNRevision.HEAD.getNumber(), null, os);
		} catch (SVNException e) {
			throw new RepositoryException("svn.error", new Object[] {e.getErrorMessage().getFullMessage()}, e);
		} finally {
			os.close();
		}
	}
	
	public void applyConfiguration(ProjectConfigDto projectConfig, String projectBasedirUrl) {
		final String relativeProjectBasedirUrl =
			projectBasedirUrl.substring(profile.getRootUrl().length());
		config.setPath(relativeProjectBasedirUrl);
		
		projectConfig.setRepositoryAdaptorConfig(config);
		
		applyBugtraqConfiguration(projectConfig);
	}

	public void setNonRecursive() {
		config.setCheckoutDepth(CheckoutDepth.Files);
	}
	
	public boolean updateGlobalConfig(PluginConfigDto globalRaConfig) {
		final SubversionConfigDto globalConfig = (SubversionConfigDto) globalRaConfig;
		if (findProfileByUrlPrefix(globalConfig, profile.getRootUrl()) != null) {
			return false;
		}
		
		final SubversionRepositoryProfileDto[] profiles = 
			new SubversionRepositoryProfileDto[globalConfig.getProfiles().length + 1];
		
		System.arraycopy(globalConfig.getProfiles(), 0, profiles, 0, profiles.length - 1);
		
		profiles[profiles.length - 1] = profile;
		
		globalConfig.setProfiles(profiles);
		
		return true;
	}
	
	protected static SubversionRepositoryProfileDto findOrCreateProfile(SVNRepository repo, SubversionConfigDto globalConfig, ProjectConfigDto project, SubversionProjectConfigDto raProjectConfig, String absoluteUrl, String username, String password) throws SVNException {
		SubversionRepositoryProfileDto profile = findProfileByUrlPrefix(globalConfig, absoluteUrl);
		
		if (profile == null) {
			if (isNotBlank(username)) {
				repo.setAuthenticationManager(
						new BasicAuthenticationManager(
								username,
								password));
			}
			
			final String root = repo.getRepositoryRoot(true).toString();
			
			profile = new SubversionRepositoryProfileDto();
			profile.setRootUrl(root);
			profile.setUsername(username);
			profile.setPassword(password);
			
			try {
				profile.setDescription(new URL(root).getHost());
			} catch (MalformedURLException e) {
				profile.setDescription(root);
			}

		}
		
		project.setRepositoryAdaptorPluginId(SubversionConfigDto.PLUGIN_ID);
		project.setRepositoryAdaptorConfig(raProjectConfig);

		raProjectConfig.setRepositoryProfile(profile.getDescription());
		
		final StringBuilder relativePath = new StringBuilder(
				absoluteUrl.substring(profile.getRootUrl().length()));
		
		relativePath.delete(relativePath.lastIndexOf("/"), relativePath.length());
		
		raProjectConfig.setPath(relativePath.toString());
		
		return profile;
	}

	protected static SubversionRepositoryProfileDto findProfileByUrlPrefix(SubversionConfigDto globalConfig, final String root) {
		SubversionRepositoryProfileDto profile = 
			getSelectedEnvironment(
					globalConfig.getProfiles(),
					new RootUrlMatcher(root));
		return profile;
	}
	
	protected static final class RootUrlMatcher implements Visitor<SubversionRepositoryProfileDto> {
		private final String root;

		protected RootUrlMatcher(String root) {
			this.root = root.toLowerCase();
		}

		public boolean isMatch(SubversionRepositoryProfileDto node) {
			return root.startsWith(node.getRootUrl().toLowerCase());
		}
	}

	private void applyBugtraqConfiguration(ProjectConfigDto projectConfig) {
		final SVNProperties properties = new SVNProperties();
		
		try {
			svnRepository.getDir(config.getPath(), SVNRevision.HEAD.getNumber(), properties, (ISVNDirEntryHandler)null);
		} catch (SVNException e) {
		}
		
		configureBugtraq(projectConfig, properties);
	}
}
