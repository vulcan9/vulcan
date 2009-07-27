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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import net.sourceforge.vulcan.core.BuildPhase;
import net.sourceforge.vulcan.core.BuildStatusListener;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.jabber.JabberPluginConfig.EventsToMonitor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


class JabberBuildStatusListener implements BuildStatusListener {
	final static Log LOG = LogFactory.getLog(JabberPlugin.class);
	
	private final JabberClient client;
	private final ScreenNameMapper screenNameResolver;
	private final ProjectStatusDto status;

	private final List<String> recipients = new ArrayList<String>();

	private final ProjectBuilder projectBuilder;

	private final JabberPluginConfig config;
	
	private Pattern errorRegex;
	private Pattern warningRegex;
	
	public JabberBuildStatusListener(JabberClient client, ProjectBuilder projectBuilder, ScreenNameMapper screenNameResolver, JabberPluginConfig config, ProjectStatusDto status) {
		this.client = client;
		this.projectBuilder = projectBuilder;
		this.screenNameResolver = screenNameResolver;
		this.config = config;
		this.status = status;
		
		addRecipients(config.getRecipients());
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
		if (errorRegex == null && StringUtils.isNotBlank(config.getErrorRegex())) {
			errorRegex = Pattern.compile(config.getErrorRegex(), Pattern.CASE_INSENSITIVE);
		}
		
		onBuildMessageLogged(EventsToMonitor.Errors, errorRegex, error, "errors");
	}

	public void onWarningLogged(BuildMessageDto warning) {
		if (warningRegex == null && StringUtils.isNotBlank(config.getWarningRegex())) {
			warningRegex = Pattern.compile(config.getWarningRegex(), Pattern.CASE_INSENSITIVE);
		}
		
		onBuildMessageLogged(EventsToMonitor.Warnings, warningRegex, warning, "warnings");
	}

	public void attach() {
		projectBuilder.addBuildStatusListener(this);
	}

	public void detach() {
		projectBuilder.removeBuildStatusListener(this);
	}
	
	public void addRecipients(String... recipients) {
		addRecipients(Arrays.asList(recipients));
	}
	
	public void addRecipients(List<String> recipients) {
		this.recipients.addAll(recipients);
	}
	
	public List<String> getRecipients() {
		return recipients;
	}
	
	public String getVulcanUrl() {
		return config.getVulcanUrl();
	}
	
	public String getMessageFormat() {
		return config.getMessageFormat();
	}

	public String getOtherUsersMessageFormat() {
		return config.getOtherUsersMessageFormat();
	}
	
	String formatNotificationMessage(BuildMessageDto error, String recipient, String view) {
		String url = generateBuildReportUrl(view);
		
		final List<String> others = new ArrayList<String>();
		for (String s : recipients) {
			if (!s.equals(recipient)) {
				final int index = s.indexOf('@');
				if (index > 0) {
					s = s.substring(0, index);
				}
				
				others.add(s);
			}
		}

		final String users = StringUtils.join(others.iterator(), ", ");
		
		final StringBuilder sb = new StringBuilder();
		
		sb.append(substituteParameters(getMessageFormat(), url, users));

		if (!others.isEmpty()) {
			sb.append("\n");
			
			sb.append(substituteParameters(getOtherUsersMessageFormat(), url, users));
		}
		
		return sb.toString();
	}

	String substituteParameters(String template, String url, String users) {
		return template.
			replace("{url}", url).
			replace("{users}", users);
	}

	String generateBuildReportUrl(String view) {
		final StringBuilder sb = new StringBuilder(getVulcanUrl());
		if (sb.charAt(sb.length()-1) != '/') {
			sb.append('/');
		}
		sb.append("projects/");
		
		try {
			sb.append(URLEncoder.encode(status.getName(), "utf-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		sb.append("/");
		sb.append(status.getBuildNumber());
		sb.append("/");
		sb.append(view);
		
		String url = sb.toString();
		return url;
	}

	private void onBuildMessageLogged(EventsToMonitor type, Pattern regex, BuildMessageDto message, String view) {
		if (!isEventMonitored(type)) {
			return;
		}
		if (regex != null && !regex.matcher(message.getMessage()).find()) {
			return;
		}
		for (String recipient : recipients) {
			client.sendMessage(recipient, formatNotificationMessage(message, recipient, view));
		}
		
		detach();
	}

	private boolean isEventMonitored(EventsToMonitor type) {
		for (EventsToMonitor e : config.getEventsToMonitor()) {
			if (e == type) {
				return true;
			}
		}
		return false;
	}
}