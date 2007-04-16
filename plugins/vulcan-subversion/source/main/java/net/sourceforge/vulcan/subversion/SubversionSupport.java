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

import java.util.Map;

import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.integration.support.PluginSupport;
import net.sourceforge.vulcan.subversion.dto.SubversionProjectConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionRepositoryProfileDto;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

public abstract class SubversionSupport extends PluginSupport {
	static {
		// Prefer Basic in case server offers to use NTLM first.
		System.setProperty("svnkit.http.methods", "Basic");

		// Enable support for various protocols.
		FSRepositoryFactory.setup();
		DAVRepositoryFactory.setup();
		SVNRepositoryFactoryImpl.setup();
	}
	
	protected static final String BUGTRAQ_LOGREGEX = "bugtraq:logregex";
	protected static final String BUGTRAQ_MESSAGE = "bugtraq:message";
	protected static final String BUGTRAQ_URL = "bugtraq:url";

	protected final Log log = LogFactory.getLog(getClass());
	protected final SubversionProjectConfigDto config;
	protected final SubversionRepositoryProfileDto profile;
	protected final SVNRepository svnRepository;
	
	protected final DefaultSVNOptions options;

	protected SubversionSupport(SubversionProjectConfigDto config, SubversionRepositoryProfileDto profile, SVNRepository svnRepository) throws ConfigException {
		this.config = config;
		
		this.profile = profile;
		
		this.svnRepository = svnRepository;
		
		if (svnRepository == null) {
			this.options = null;
			return;
		}
		
		if (StringUtils.isNotBlank(profile.getUsername())) {
			svnRepository.setAuthenticationManager(
					new BasicAuthenticationManager(
							profile.getUsername(),
							profile.getPassword()));
		}
		
		this.options = new DefaultSVNOptions();
		this.options.setAuthStorageEnabled(false);
	}

	boolean isFatal(SVNException e) {
		final String message = e.getMessage();
		
		if (isNotBlank(message) && message.indexOf("does not exist in the repository or refers to an unrelated object") > 0) {
			log.error("Got non-fatal subversion exception with code " + e.getErrorMessage().getErrorCode(), e);
			return false;
		}
		
		return true;
	}

	protected String combinePatterns(String logRegex, String messagePattern) {
		final StringBuilder sb = new StringBuilder();
		
		if (isNotBlank(logRegex)) {
			sb.append(logRegex);
		}
		
		if (isNotBlank(messagePattern)) {
			if (sb.length() > 0) {
				sb.append('|');
			}
			
			sb.append(messagePattern.replaceAll("%BUGID%", "(\\\\d+)"));
		}
		
		return sb.toString();
	}

	protected void configureBugtraq(final ProjectConfigDto projectConfig, final Map<String, String> bugtraqProps) {
		final String logRegex = bugtraqProps.get(BUGTRAQ_LOGREGEX);
		final String logRegex1;
		final String logRegex2;
	
		if (isNotBlank(logRegex)) {
			final String value = logRegex.replaceAll("\r", "");
			final String[] patterns = value.split("\n");
			
			logRegex1 = patterns[0];
			
			if (patterns.length > 1) {
				logRegex2 = patterns[1];
			} else {
				logRegex2 = "";
			}
		} else {
			logRegex1 = "";
			logRegex2 = "";
		}
		
		String bugtraqUrl = bugtraqProps.get(BUGTRAQ_URL);
		if (bugtraqUrl == null) {
			bugtraqUrl = StringUtils.EMPTY;
		}
		
		if (isNotBlank(bugtraqUrl)) {
			projectConfig.setBugtraqUrl(bugtraqUrl);
			projectConfig.setBugtraqLogRegex1(combinePatterns(logRegex1, bugtraqProps.get(BUGTRAQ_MESSAGE)));
			projectConfig.setBugtraqLogRegex2(logRegex2);
		}
	}

	protected static SVNRepository createRepository(SubversionRepositoryProfileDto profile, boolean init) throws ConfigException {
		if (!init) {
			return null;
		}
		
		if (profile == null) {
			throw new ConfigException("svn.profile.not.selected", null);
		}
		try {
			return SVNRepositoryFactory.create(SVNURL.parseURIEncoded(profile.getRootUrl()));
		} catch (Exception e) {
			throw new RepositoryException(e);
		}
	}
}
