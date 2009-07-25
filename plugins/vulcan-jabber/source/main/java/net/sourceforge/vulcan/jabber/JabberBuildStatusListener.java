/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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
package net.sourceforge.vulcan.jabber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sourceforge.vulcan.core.BuildPhase;
import net.sourceforge.vulcan.core.BuildStatusListener;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class JabberBuildStatusListener implements BuildStatusListener {
	final static Log LOG = LogFactory.getLog(JabberPlugin.class);
	
	private final JabberClient client;
	private final ScreenNameResolver screenNameResolver;
	private final ProjectStatusDto status;

	private final List<String> recipients = new ArrayList<String>();
	
	public JabberBuildStatusListener(JabberClient client, ScreenNameResolver screenNameResolver, ProjectStatusDto status) {
		this.client = client;
		this.screenNameResolver = screenNameResolver;
		this.status = status;
	}
	
	public void onBuildPhaseChanged(BuildPhase phase) {
		if (phase != BuildPhase.Build || status.getChangeLog() == null || status.getChangeLog().getChangeSets() == null) {
			return;
		}
		
		final List<String> uniques = new ArrayList<String>();
		
		for (ChangeSetDto commit : status.getChangeLog().getChangeSets()) {
			final String author = commit.getAuthor();
		
			if (!StringUtils.isEmpty(author) && !uniques.contains(author)) {
				uniques.add(author);
			}
		}
		
		addRecipients(screenNameResolver.lookupByAuthor(uniques));
	}
	
	public void onErrorLogged(BuildMessageDto error) {
		for (String recipient : recipients) {
			client.sendMessage(recipient, error.getMessage());
		}
	}
	
	public void onWarningLogged(BuildMessageDto warning) {
	}

	public void addRecipients(String... recipients) {
		addRecipients(Arrays.asList(recipients));
	}
	
	private void addRecipients(List<String> recipients) {
		this.recipients.addAll(recipients);
	}
	
	public List<String> getRecipients() {
		return recipients;
	}
}