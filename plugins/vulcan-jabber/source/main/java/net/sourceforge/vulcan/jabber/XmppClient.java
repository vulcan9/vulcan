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

import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

public class XmppClient implements JabberClient {
	private static final Log LOG = LogFactory.getLog(XmppClient.class);
	
	private final Object lock = new Object();
	private EventHandler eventHandler;
	private String connectionString;
	protected XMPPConnection connection;
	
	public void refreshConnection(String server, int port, String username, String password) {
		if (password == null) {
			password = StringUtils.EMPTY;
		}
		final String connectionString = server + ":" + port + ":" + username + ":" + DigestUtils.shaHex(password);
		
		synchronized(lock) {
			if (connectionString.equals(this.connectionString)) {
				return;
			}

			this.connectionString = connectionString;
			
			if (connection != null) {
				connection.disconnect();
				connection = null;
			}

			if (StringUtils.isEmpty(server)) {
				return;
			}
			
			connect(server, port, username, password);
			if (connection == null) {
				this.connectionString = null;
			}
		}			
	}
	
	public void sendMessage(String recipient, String message) {
		synchronized(lock) {
			if (connection == null) {
				return;
			}
			final ChatManager chatManager = connection.getChatManager();
			final Chat chat = chatManager.createChat(recipient, new MessageListener() {
				public void processMessage(Chat arg0, Message arg1) {
				}
			});
			try {
				chat.sendMessage(message);
			} catch (XMPPException e) {
				eventHandler.reportEvent(new ErrorEvent(this, "jabber.errors.send", new Object[] {recipient, e.getMessage()}, e));
			}
		}
	}
	
	public EventHandler getEventHandler() {
		return eventHandler;
	}
	
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	
	void connect(String server, int port, String username, String password) {
		ConnectionConfiguration config = new ConnectionConfiguration(server, port);

		connection = new XMPPConnection(config);

		try {
			connection.connect();
			connection.login(username, password);
			LOG.info("Logged into " + server + ":" + port + " as " + username);
		} catch (XMPPException e) {
			eventHandler.reportEvent(new ErrorEvent(this, "jabber.errors.connect",
					new Object[] {server, port, username, e.getMessage()}, e));
			
			connection = null;
		}
	}
}
