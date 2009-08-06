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

import java.util.HashMap;
import java.util.Map;

public class JabberResponder implements JabberChatListener {
	static class RertortStatus {
		String user;
		long lastMessage;
		int position;
	}

	private long idleThreshold;
	
	private final Map<String, RertortStatus> retortCounters = new HashMap<String, RertortStatus>();
	
	private JabberClient client;
	private JabberPluginConfig config;
	private String[] retorts;
	
	public void setClient(JabberClient client) {
		this.client = client;
	}
	
	public void setIdleThreshold(long idleThreshold) {
		this.idleThreshold = idleThreshold;
	}
	
	public synchronized void setConfiguration(JabberPluginConfig config) {
		this.config = config;
		if (config != null) {
			retorts = config.getTemplateConfig().getPithyRetortTemplate().split("\r?\n");
		}
	}
	
	public void init() {
		client.addMessageReceivedListener(this);
	}
	
	public synchronized void messageReceived(String from, String message) {
		if (isBlank(message) || config == null) {
			return;
		}
		
		RertortStatus status = retortCounters.get(from);
		if (status == null || getCurrentTimeMillis() - status.lastMessage > idleThreshold) {
			status = new RertortStatus();
			status.position = 0;
			retortCounters.put(from, status);
		}
		
		if (status.position >= retorts.length) {
			status.position = 0;
		}
		
		if (isNotBlank(retorts[status.position])) {
			client.sendMessage(from, TemplateFormatter.substituteParameters(retorts[status.position], config.getVulcanUrl(), "", null, null));
		}
		
		status.lastMessage = getCurrentTimeMillis();
		status.position++;
	}

	protected long getCurrentTimeMillis() {
		return System.currentTimeMillis();
	}
}
