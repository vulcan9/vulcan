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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sourceforge.vulcan.core.BuildManager;

public class JabberResponder implements JabberChatListener {
	static class RertortStatus {
		long lastMessageTime;
		int index;
	}

	static class BuildClaimTicket {
		String screenName;
		String userName;
		String projectName;
		int buildNumber;
	}
	
	private long idleThreshold;
	
	private final Map<String, RertortStatus> retortCounters = new HashMap<String, RertortStatus>();
	private final Map<String, BuildClaimTicket> tickets = new HashMap<String, BuildClaimTicket>();
	
	private JabberClient client;
	private BuildManager buildManager;
	private JabberPluginConfig config;
	private String[] retorts;
	private Set<String> claimKeywords;
	
	public void setClient(JabberClient client) {
		this.client = client;
	}
	
	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
	
	public void setIdleThreshold(long idleThreshold) {
		this.idleThreshold = idleThreshold;
	}
	
	public synchronized void setConfiguration(JabberPluginConfig config) {
		this.config = config;
		if (config != null) {
			retorts = config.getTemplateConfig().getPithyRetortTemplate().split("\r?\n");
			claimKeywords = new HashSet<String>(Arrays.asList(config.getTemplateConfig().getClaimKeywords().toLowerCase().split("\r?\n")));
		}
	}
	
	public void init() {
		client.addMessageReceivedListener(this);
	}

	/**
	 * @param projectName
	 * @param buildNumber
	 * @param users Map of commit usernames to screen names
	 */
	public synchronized void linkUsersToBrokenBuild(String projectName, int buildNumber, Map<String, String> users) {
		for (Map.Entry<String, String> kvp : users.entrySet()) {
			final BuildClaimTicket ticket = new BuildClaimTicket();
			ticket.projectName = projectName;
			ticket.buildNumber = buildNumber;
			ticket.userName = kvp.getKey();
			ticket.screenName = kvp.getValue().toLowerCase();
			
			tickets.put(ticket.screenName, ticket);
		}
	}

	public synchronized void notifyBuildClaimed(String projectName,	int buildNumber, String claimUser) {
		for (Iterator<BuildClaimTicket> itr = tickets.values().iterator(); itr.hasNext();) {
			final BuildClaimTicket tkt = itr.next();
			if (tkt.projectName == projectName) {
				if (!tkt.userName.equals(claimUser)) {
					client.sendMessage(tkt.screenName, TemplateFormatter.substituteParameters(config.getTemplateConfig().getBrokenBuildClaimedByTemplate(), "", "", claimUser, null, projectName, buildNumber));
				}
				itr.remove();
			}
		}
	}

	public synchronized void messageReceived(String from, String message) {
		if (isBlank(message) || config == null) {
			return;
		}
		
		if (claimBuildIfApplicable(from, message)) {
			return;
		}
		
		sendPithyRetort(from);
	}

	private boolean claimBuildIfApplicable(String from, String message) {
		// Remove punctuation and lowercase.
		final String scrubbed = message.toLowerCase().replaceAll("[^\\s\\w]", "");
		
		if (!claimKeywords.contains(scrubbed)) {
			return false;
		}
		
		// talk.google.com uses names like user@gmail.com/Talk.v104AB7 to distinguish sessions.
		// Drop anything after a slash when looking up a user.
		final BuildClaimTicket ticket = tickets.remove(from.split("/")[0].toLowerCase());
		
		if (ticket == null) {
			return false;
		}
		
		final String claimUser;
		
		final String projectName = ticket.projectName;
		final int buildNumber = ticket.buildNumber;
		
		if (buildManager.claimBrokenBuild(projectName, buildNumber, ticket.userName)) {
			claimUser = ticket.screenName;
			
			final String template = config.getTemplateConfig().getBrokenBuildAcknowledgementTemplate();
			
			client.sendMessage(ticket.screenName, TemplateFormatter.substituteParameters(template, "", "", claimUser, null, projectName, buildNumber));
		} else {
			claimUser = buildManager.getStatusByBuildNumber(projectName, buildNumber).getBrokenBy();
			client.sendMessage(ticket.screenName, TemplateFormatter.substituteParameters(config.getTemplateConfig().getBrokenBuildClaimedByTemplate(), "", "", claimUser, null, projectName, buildNumber));
		}
		
		return true;
	}

	private void sendPithyRetort(String from) {
		RertortStatus status = retortCounters.get(from);
		if (status == null || getCurrentTimeMillis() - status.lastMessageTime > idleThreshold) {
			status = new RertortStatus();
			status.index = 0;
			retortCounters.put(from, status);
		}
		
		if (status.index >= retorts.length) {
			status.index = 0;
		}
		
		if (isNotBlank(retorts[status.index])) {
			client.sendMessage(from, TemplateFormatter.substituteParameters(retorts[status.index], config.getVulcanUrl(), "", null, null));
		}
		
		status.lastMessageTime = getCurrentTimeMillis();
		status.index++;
	}

	protected long getCurrentTimeMillis() {
		return System.currentTimeMillis();
	}
}
