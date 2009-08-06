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

import org.apache.commons.lang.StringUtils;

import net.sourceforge.vulcan.EasyMockTestCase;

public class JabberResponderTest extends EasyMockTestCase {
	JabberResponder responder = new JabberResponder() {
		@Override
		protected long getCurrentTimeMillis() {
			return fakeTime;
		}
	};
	JabberClient client = createStrictMock(JabberClient.class);
	JabberPluginConfig config = new JabberPluginConfig();
	
	long fakeTime = 1000l;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		config.getTemplateConfig().setPithyRetortTemplate("Hi there.");
		
		responder.setClient(client);
		responder.setConfiguration(config);
		responder.setIdleThreshold(30);
	}
	
	public void testInitAddsListener() throws Exception {
		client.addMessageReceivedListener(responder);
		
		replay();
		
		responder.init();
		
		verify();
	}
	
	public void testTalksBackOnMessageReceived() throws Exception {
		client.sendMessage("Sam", "Hi there.");
		
		replay();
		
		responder.messageReceived("Sam", "Hello");
		
		verify();
	}
	
	public void testTalksBackLoopsThroughMessages() throws Exception {
		config.getTemplateConfig().setPithyRetortTemplate("1\r\n2");
		responder.setConfiguration(config);
		
		doChatTest("1", "2", "1");
	}
	
	public void testTalksBackLoopsThroughMessagesIgnoresBlankLines() throws Exception {
		config.getTemplateConfig().setPithyRetortTemplate("1\n\n3");
		responder.setConfiguration(config);
		
		doChatTest("1", "", "3");
	}
	
	public void testKeepTrackOfDifferentUsers() throws Exception {
		config.getTemplateConfig().setPithyRetortTemplate("1\r\n2");
		responder.setConfiguration(config);
		
		client.sendMessage("Sam", "1");
		client.sendMessage("Joe", "1");
		client.sendMessage("Sam", "2");
		
		replay();
		
		responder.messageReceived("Sam", "Hello");
		responder.messageReceived("Joe", "Sup?");
		responder.messageReceived("Sam", "I like you!");
		
		verify();
	}
	
	public void testResetsAfterIdleThreshold() throws Exception {
		config.getTemplateConfig().setPithyRetortTemplate("1\r\n2");
		responder.setConfiguration(config);
		
		client.sendMessage("Sam", "1");
		client.sendMessage("Sam", "1");
		
		replay();
		
		responder.messageReceived("Sam", "Hello");
		
		fakeTime += 1000l;
		
		responder.messageReceived("Sam", "I like you!");
		
		verify();
	}
	
	void doChatTest(String... expectedChats) {
		for (String s : expectedChats) {
			reset();
			if (StringUtils.isNotBlank(s)) {
				client.sendMessage("Sam", s);
			}
			replay();
			responder.messageReceived("Sam", "Hello");
			verify();
		}
	}
}
