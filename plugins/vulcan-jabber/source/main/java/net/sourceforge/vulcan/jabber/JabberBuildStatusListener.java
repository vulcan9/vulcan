/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	
	private final ProjectBuilder projectBuilder;
	private final JabberClient client;
	private final JabberResponder responder;
	private final ScreenNameMapper screenNameResolver;
	private final JabberPluginConfig config;
	private final ProjectStatusDto status;

	private final List<String> recipients = new ArrayList<String>();
	private final List<String> committers = new ArrayList<String>();
	
	private Pattern errorRegex;
	private Pattern warningRegex;

	private boolean attached;

	private List<ProjectStatusDto> previousFailures = Collections.emptyList();

	private Map<String, String> screenNameMap = Collections.emptyMap();
	
	public JabberBuildStatusListener(ProjectBuilder projectBuilder, JabberClient client, JabberResponder responder, ScreenNameMapper screenNameResolver, JabberPluginConfig config, ProjectStatusDto status) {
		this.projectBuilder = projectBuilder;
		this.client = client;
		this.responder = responder;
		this.screenNameResolver = screenNameResolver;
		this.config = config;
		this.status = status;
		
		addRecipients(config.getRecipients());
	}
	
	public void onBuildPhaseChanged(Object source, BuildPhase phase) {
		if (phase != BuildPhase.Build || status.getChangeLog() == null || status.getChangeLog().getChangeSets() == null) {
			return;
		}
		
		final List<String> uniques = new ArrayList<String>();
		
		final List<ProjectStatusDto> outcomes = new ArrayList<ProjectStatusDto>(previousFailures);
		outcomes.add(status);
		
		for (ProjectStatusDto outcome : outcomes) {
			if (outcome.getChangeLog() == null || outcome.getChangeLog().getChangeSets() == null) {
				continue;
			}
			
			for (ChangeSetDto commit : outcome.getChangeLog().getChangeSets()) {
				final String author = commit.getAuthorName();
			
				if (!StringUtils.isEmpty(author) && !uniques.contains(author)) {
					uniques.add(author);
				}
			}
		}
		
		screenNameMap = screenNameResolver.lookupByAuthor(uniques);
		
		addCommitters(screenNameMap.values());
	}
	
	public void onErrorLogged(Object source, BuildMessageDto error) {
		if (errorRegex == null && StringUtils.isNotBlank(config.getErrorRegex())) {
			errorRegex = Pattern.compile(config.getErrorRegex(), Pattern.CASE_INSENSITIVE);
		}
		
		onBuildMessageLogged(EventsToMonitor.Errors, errorRegex, error, "errors");
	}

	public void onWarningLogged(Object source, BuildMessageDto warning) {
		if (warningRegex == null && StringUtils.isNotBlank(config.getWarningRegex())) {
			warningRegex = Pattern.compile(config.getWarningRegex(), Pattern.CASE_INSENSITIVE);
		}
		
		onBuildMessageLogged(EventsToMonitor.Warnings, warningRegex, warning, "warnings");
	}

	public void attach() {
		projectBuilder.addBuildStatusListener(this);
		attached = true;
	}

	public void detach() {
		projectBuilder.removeBuildStatusListener(this);
		attached = false;
	}
	
	public void addCommitters(Collection<String> committers) {
		this.committers.addAll(committers);
		addRecipients(committers);
	}
	public void addRecipients(String... recipients) {
		addRecipients(Arrays.asList(recipients));
	}
	
	public void addRecipients(Collection<String> recipients) {
		final Set<String> uniques = new HashSet<String>();
		
		for (String s : this.recipients) {
			uniques.add(s.toLowerCase());
		}
		
		for (String s : recipients) {
			if (!uniques.contains(s.toLowerCase())) {
				this.recipients.add(s);				
			}
		}
	}
	
	public List<String> getRecipients() {
		return recipients;
	}
	
	public String getVulcanUrl() {
		return config.getVulcanUrl();
	}
	
	String formatNotificationMessage(String recipient, String view, BuildMessageDto message) {
		String url = generateBuildReportUrl(view);
		
		final List<String> others = new ArrayList<String>();
		for (String s : committers) {
			if (!s.equals(recipient)) {
				final int index = s.indexOf('@');
				if (index > 0) {
					s = s.substring(0, index);
				}
				
				others.add(s);
			}
		}

		final String users = StringUtils.join(others.iterator(), ", ");
		final String messageFormat;
		
		if (committers.contains(recipient)) {
			messageFormat = config.getTemplateConfig().getNotifyCommitterTemplate();
		} else {
			messageFormat = config.getTemplateConfig().getNotifyBuildMasterTemplate();
		}
		
		return TemplateFormatter.substituteParameters(messageFormat, url, users, message, status);
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

	protected void onBuildMessageLogged(EventsToMonitor type, Pattern regex, BuildMessageDto message, String view) {
		if (!isEventMonitored(type)) {
			return;
		}
		if (regex != null && !regex.matcher(message.getMessage()).find()) {
			return;
		}
		
		responder.linkUsersToBrokenBuild(status.getName(), status.getBuildNumber(), screenNameMap);
		
		for (String recipient : recipients) {
			client.sendMessage(recipient, formatNotificationMessage(recipient, view, message));
		}
		
		detach();
	}

	void setScreenNameMap(Map<String, String> screenNameMap) {
		this.screenNameMap = screenNameMap;
	}
	
	private boolean isEventMonitored(EventsToMonitor type) {
		for (EventsToMonitor e : config.getEventsToMonitor()) {
			if (e == type) {
				return true;
			}
		}
		return false;
	}

	public boolean isAttached() {
		return attached;
	}

	public List<ProjectStatusDto> getPreviousFailures() {
		return previousFailures;
	}
	
	public void setPreviousFailures(List<ProjectStatusDto> previousFailures) {
		this.previousFailures = previousFailures;
	}
}