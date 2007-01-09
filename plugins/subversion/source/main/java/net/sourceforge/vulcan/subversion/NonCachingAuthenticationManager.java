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

import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;

public class NonCachingAuthenticationManager
		extends DefaultSVNAuthenticationManager {

	final String username;
	final String password;
	
	static {
		// Prefer Basic over NTLM.
		System.setProperty("svnkit.http.methods", "Basic");
	}
	public NonCachingAuthenticationManager(String username, String password) {
		super(null, false, username, password);
		this.username = username;
		this.password = password;
	}

	@Override
	public void setAuthenticationProvider(ISVNAuthenticationProvider provider) {
	}

	@Override
	public SVNAuthentication getFirstAuthentication(String kind, String realm,
			SVNURL url) throws SVNException {
		return new SVNPasswordAuthentication(username, password, false);
	}

	@Override
	public SVNAuthentication getNextAuthentication(String kind, String realm,
			SVNURL url) throws SVNException {
		throw new SVNAuthenticationException(
				SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE,
						"Invalid username/password for '" + realm + "'"));
	}

	@Override
	public void acknowledgeAuthentication(boolean accepted, String kind, String realm, SVNErrorMessage errorMessage, SVNAuthentication authentication) throws SVNException {
		if (!accepted) {
			super.acknowledgeAuthentication(accepted, kind, realm, errorMessage, authentication);
		}
	}
}
