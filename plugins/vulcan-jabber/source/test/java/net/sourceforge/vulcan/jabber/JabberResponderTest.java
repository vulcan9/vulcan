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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.dto.ProjectStatusDto;

import org.apache.commons.lang.StringUtils;

public class JabberResponderTest extends EasyMockTestCase {
	JabberResponder responder = new JabberResponder() {
		@Override
		protected long getCurrentTimeMillis() {
			return fakeTime;
		}
	};
	JabberClient client = createStrictMock(JabberClient.class);
	BuildManager buildManager = createStrictMock(BuildManager.class);
	JabberPluginConfig config = new JabberPluginConfig();
	
	long fakeTime = 1000l;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		config.getTemplateConfig().setPithyRetortTemplate("Hi there.");
		config.getTemplateConfig().setBrokenBuildAcknowledgementTemplate("Ok thanks!");
		config.getTemplateConfig().setBrokenBuildClaimedByTemplate("Somebody claimed it, so don't worry.");
		config.getTemplateConfig().setClaimKeywords("mine\nmy bad");
		
		responder.setClient(client);
		responder.setBuildManager(buildManager);
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
	
	public void testClaimBrokenBuild() throws Exception {
		String projectName = "example";
		int buildNumber = 1134;
		
		expect(buildManager.claimBrokenBuild(projectName, buildNumber, "committer_sam")).andReturn(true);
		
		client.sendMessage("iamsam82", "Ok thanks!");
		
		replay();
		
		responder.linkUsersToBrokenBuild(projectName, buildNumber, Collections.singletonMap("committer_sam", "iamsam82"));
		
		responder.messageReceived("iamsam82", "mine");
		
		verify();
	}
	
	public void testClaimBrokenBuildFiltersOnKeywords() throws Exception {
		String projectName = "example";
		int buildNumber = 1134;
		
		client.sendMessage("iamsam82", "Hi there.");
		
		replay();
		
		responder.linkUsersToBrokenBuild(projectName, buildNumber, Collections.singletonMap("committer_sam", "iamsam82"));
		
		responder.messageReceived("iamsam82", "I'm not here right now");
		
		verify();
	}
	
	public void testClaimBrokenBuildFiltersOnKeywordsDoesNotRemoveTicket() throws Exception {
		String projectName = "example";
		int buildNumber = 1134;
		
		client.sendMessage("iamsam82", "Hi there.");
		expect(buildManager.claimBrokenBuild(projectName, buildNumber, "committer_sam")).andReturn(true);
		
		client.sendMessage("iamsam82", "Ok thanks!");
		
		replay();
		
		responder.linkUsersToBrokenBuild(projectName, buildNumber, Collections.singletonMap("committer_sam", "iamsam82"));
		
		responder.messageReceived("iamsam82", "I'm not here right now");
		responder.messageReceived("iamsam82", "mine");
		
		verify();
	}
	
	public void testClaimBrokenBuildCaseInsensitive() throws Exception {
		String projectName = "example";
		int buildNumber = 1134;
		
		expect(buildManager.claimBrokenBuild(projectName, buildNumber, "committer_sam")).andReturn(true);
		
		client.sendMessage("iamsam82", "Ok thanks!");
		
		replay();
		
		responder.linkUsersToBrokenBuild(projectName, buildNumber, Collections.singletonMap("committer_sam", "IAmSam82"));
		
		responder.messageReceived("iamSAM82", "Mine.");
		
		verify();
	}
	
	public void testClaimBrokenBuildDropsExtraInfoFromScreenName() throws Exception {
		String projectName = "example";
		int buildNumber = 1134;
		
		expect(buildManager.claimBrokenBuild(projectName, buildNumber, "committer_sam")).andReturn(true);
		
		client.sendMessage("iamsam82@gmail.com", "Ok thanks!");
		
		replay();
		
		responder.linkUsersToBrokenBuild(projectName, buildNumber, Collections.singletonMap("committer_sam", "iamsam82@gmail.com"));
		
		responder.messageReceived("iamsam82@gmail.com/talk113423", "mine");
		
		verify();
	}
	
	public void testClaimBrokenBuildNotifiesOthers() throws Exception {
		String projectName = "example";
		int buildNumber = 1134;
		
		Map<String, String> users = new HashMap<String, String>();
		users.put("committer_sam", "iamsam82");
		users.put("committer_bob", "bob69");
		
		expect(buildManager.claimBrokenBuild(projectName, buildNumber, "committer_bob")).andReturn(true);
		
		client.sendMessage("bob69", "Ok thanks!");
		client.sendMessage("iamsam82", "Somebody claimed it, so don't worry.");
		
		replay();
		
		responder.linkUsersToBrokenBuild(projectName, buildNumber, users);
		
		responder.messageReceived("bob69", "my bad");
		
		responder.notifyBuildClaimed(projectName, buildNumber, "bob69");
		
		verify();
	}
	
	public void testClaimBrokenBuildAlreadyClaimedNotifiesOthers() throws Exception {
		config.getTemplateConfig().setBrokenBuildClaimedByTemplate("{ClaimUser} got it.");
		
		String projectName = "example";
		int buildNumber = 1134;
		
		Map<String, String> users = new HashMap<String, String>();
		users.put("committer_sam", "iamsam82");
		users.put("committer_bob", "bob69");
		
		expect(buildManager.claimBrokenBuild(projectName, buildNumber, "committer_bob")).andReturn(false);

		ProjectStatusDto status = new ProjectStatusDto();
		status.setBrokenBy("committer_otherguy");
		
		expect(buildManager.getStatusByBuildNumber(projectName, buildNumber)).andReturn(status );
		
		client.sendMessage("bob69", "committer_otherguy got it.");
		client.sendMessage("iamsam82", "committer_otherguy got it.");
		
		replay();
		
		responder.linkUsersToBrokenBuild(projectName, buildNumber, users);
		
		responder.messageReceived("bob69", "my bad");
		
		responder.notifyBuildClaimed(projectName, buildNumber, "committer_otherguy");
		verify();
	}
	
	public void testParameterizesMessages() throws Exception {
		config.getTemplateConfig().setBrokenBuildAcknowledgementTemplate("Ok you claimed {BuildNumber}!");
		config.getTemplateConfig().setBrokenBuildClaimedByTemplate("{ClaimUser} got it.");
		String projectName = "example";
		int buildNumber = 1134;
		
		Map<String, String> users = new HashMap<String, String>();
		users.put("committer_sam", "iamsam82");
		users.put("committer_bob", "bob69");
		
		expect(buildManager.claimBrokenBuild(projectName, buildNumber, "committer_bob")).andReturn(true);
		
		client.sendMessage("bob69", "Ok you claimed 1,134!");
		client.sendMessage("iamsam82", "bob69 got it.");
		
		replay();
		
		responder.linkUsersToBrokenBuild(projectName, buildNumber, users);
		
		responder.messageReceived("bob69", "my bad");
		
		responder.notifyBuildClaimed(projectName, buildNumber, "bob69");
		verify();
	}
	
	public void testCannotClaimAlreadyClaimedBuild() throws Exception {
		String projectName = "example";
		int buildNumber = 1134;
		
		Map<String, String> users = new HashMap<String, String>();
		users.put("committer_sam", "iamsam82");
		users.put("committer_bob", "bob69");
		
		expect(buildManager.claimBrokenBuild(projectName, buildNumber, "committer_bob")).andReturn(true);
		
		client.sendMessage("bob69", "Ok thanks!");
		client.sendMessage("iamsam82", "Somebody claimed it, so don't worry.");
		client.sendMessage("iamsam82", "Hi there.");
		
		replay();
		
		responder.linkUsersToBrokenBuild(projectName, buildNumber, users);
		
		responder.messageReceived("bob69", "my bad");
		
		responder.notifyBuildClaimed(projectName, buildNumber, "bob69");
		
		responder.messageReceived("iamsam82", "mine");
		
		verify();
	}
	
	public void testClaimBrokenBuildNotifiesOthersOnSameProject() throws Exception {
		String projectName = "example";
		int buildNumber = 1134;
		
		Map<String, String> users = new HashMap<String, String>();
		users.put("committer_sam", "iamsam82");
		users.put("committer_bob", "bob69");
		
		expect(buildManager.claimBrokenBuild(projectName, buildNumber, "committer_bob")).andReturn(true);
		
		client.sendMessage("bob69", "Ok thanks!");
		client.sendMessage("iamsam82", "Somebody claimed it, so don't worry.");
		
		replay();
		
		responder.linkUsersToBrokenBuild(projectName, buildNumber, users);
		responder.linkUsersToBrokenBuild("other project", buildNumber, Collections.singletonMap("otherguy", "committer_otherguy"));
		
		responder.messageReceived("bob69", "my bad");
		responder.notifyBuildClaimed(projectName, buildNumber, "bob69");
		
		verify();
	}
	
	public void testNotifyDoesNotNotifySameUser() throws Exception {
		String projectName = "example";
		int buildNumber = 1134;
		
		Map<String, String> users = new HashMap<String, String>();
		users.put("committer_sam", "iamsam82");
		users.put("committer_bob", "bob69");
		
		client.sendMessage("iamsam82", "Somebody claimed it, so don't worry.");
		
		replay();
		
		responder.linkUsersToBrokenBuild(projectName, buildNumber, users);
		responder.notifyBuildClaimed(projectName, buildNumber, "committer_bob");
		
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
