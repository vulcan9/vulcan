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

import junit.framework.TestCase;

import org.jivesoftware.smack.XMPPConnection;

public class XmppClientTest extends TestCase {
	int connectCount = 0;
	int disconnectCount = 0;
	boolean failToConnectFlag = false;
	
	XmppClient client = new XmppClient() {
		@Override
		void connect(String server, int port, String username, String password) {
			connectCount++;
			if (!failToConnectFlag) {
				connection = new XMPPConnection("fake") {
					@Override
					public void disconnect() {
						disconnectCount++;
					}
				};
			}
		}
	};
	
	public void testConnect() throws Exception {
		client.refreshConnection("example.com", 5222, "user", "pass");
		
		assertEquals(1, connectCount);
	}
	
	public void testConnectNullPassword() throws Exception {
		client.refreshConnection("example.com", 5222, null, null);
		
		assertEquals(1, connectCount);
	}
	
	public void testDoesNotConnectOnEmptyServer() throws Exception {
		client.refreshConnection("", 5222, null, null);
		
		assertEquals(0, connectCount);
	}
	
	public void testDoesNotReconnectOnUnmodifiedSettings() throws Exception {
		client.refreshConnection("example.com", 5222, "user", "pass");
		client.refreshConnection("example.com", 5222, "user", "pass");
		
		assertEquals(1, connectCount);
	}
	
	public void testDoesReconnectOnUnmodifiedSettingsFailedConnection() throws Exception {
		failToConnectFlag = true;
		client.refreshConnection("example.com", 5222, "user", "pass");
		failToConnectFlag = false;
		client.refreshConnection("example.com", 5222, "user", "pass");
		
		assertEquals(2, connectCount);
	}
	
	public void testDisconectsOldConnectionBeforeMakingNewOne() throws Exception {
		client.refreshConnection("example.com", 5222, "user", "pass");
		client.refreshConnection("example.com", 5222, "user", "fixed");
		
		assertEquals(1, disconnectCount);
		assertEquals(2, connectCount);
	}
	
	public void testDisconectsOldConnection() throws Exception {
		client.refreshConnection("example.com", 5222, "user", "pass");
		client.refreshConnection("", 5222, "", "");
		
		assertEquals(1, disconnectCount);
		assertEquals(1, connectCount);
	}
	
	public void testForgetsOldInfoOnDisconnect() throws Exception {
		client.refreshConnection("example.com", 5222, "user", "pass");
		client.refreshConnection("", 5222, "user", "pass");
		client.refreshConnection("example.com", 5222, "user", "pass");
		
		assertEquals(1, disconnectCount);
		assertEquals(2, connectCount);
	}
}