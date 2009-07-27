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

import java.util.Arrays;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.BuildPhase;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;

public class JabberBuildStatusListenerTest extends EasyMockTestCase {
	
	JabberBuildStatusListener listener;
	JabberClient client = createStrictMock(JabberClient.class);
	ScreenNameMapper resolver = createStrictMock(ScreenNameMapper.class);
	ProjectBuilder projectBuilder = createStrictMock(ProjectBuilder.class);
	ProjectStatusDto status = new ProjectStatusDto();
	ChangeLogDto changeLog = new ChangeLogDto();
	ChangeSetDto commit1;
	ChangeSetDto commit2;
	ChangeSetDto commit3;
	ChangeSetDto commit4;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	
		listener = new JabberBuildStatusListener(client, projectBuilder, resolver, status);
		listener.setVulcanUrl("http://localhost.localdomain:8080/vulcan");
		listener.setMessageFormat("You broke the build (way to go).  See {url} for more info.");
		listener.setOtherUsersMessageFormat("These jokers also got notice: {users}.");
		
		commit1 = makeChangeSet("Sam");
		commit2 = makeChangeSet("Jesse");
		commit3 = makeChangeSet("Sam");
		commit4 = makeChangeSet("Sydney");
	}
	
	private ChangeSetDto makeChangeSet(String author) {
		final ChangeSetDto dto = new ChangeSetDto();
		dto.setAuthor(author);
		return dto;
	}

	public void testMapsAuthorsToRecipientsOnBuildStarted() throws Exception {
		status.setChangeLog(changeLog);
		changeLog.setChangeSets(Arrays.asList(commit1, commit2, commit3, commit4));
		
		listener.addRecipients("permanentjoe");
		
		expect(resolver.lookupByAuthor(Arrays.asList("Sam", "Jesse", "Sydney"))).andReturn(Arrays.asList("sam82", "sidthekid"));
		
		replay();
		
		listener.onBuildPhaseChanged(BuildPhase.Build);
		
		verify();
		
		assertEquals(Arrays.asList("permanentjoe", "sam82", "sidthekid"), listener.getRecipients());
	}

	public void testSkipsNullAndBlank() throws Exception {
		status.setChangeLog(changeLog);
		commit2.setAuthor("");
		commit4.setAuthor(null);
		changeLog.setChangeSets(Arrays.asList(commit1, commit2, commit3, commit4));
		
		listener.addRecipients("permanentjoe");
		
		expect(resolver.lookupByAuthor(Arrays.asList("Sam"))).andReturn(Arrays.asList("sam82"));
		
		replay();
		
		listener.onBuildPhaseChanged(BuildPhase.Build);
		
		verify();
		
		assertEquals(Arrays.asList("permanentjoe", "sam82"), listener.getRecipients());
	}
	
	public void testMapsAuthorsToRecipientsOnBuildStartedNullChangeLog() throws Exception {
		listener.addRecipients("permanentjoe");
		
		status.setChangeLog(null);
		
		replay();
		
		listener.onBuildPhaseChanged(BuildPhase.Build);
		
		verify();
		
		assertEquals(Arrays.asList("permanentjoe"), listener.getRecipients());
	}
	
	public void testMapsAuthorsToRecipientsOnBuildStartedNullChangeSets() throws Exception {
		listener.addRecipients("permanentjoe");
		
		changeLog.setChangeSets(null);
		status.setChangeLog(changeLog);
		
		replay();
		
		listener.onBuildPhaseChanged(BuildPhase.Build);
		
		verify();
		
		assertEquals(Arrays.asList("permanentjoe"), listener.getRecipients());
	}
	
	public void testDoesNotLookupOnOtherPhases() throws Exception {
		listener.addRecipients("permanentjoe");
		
		replay();
		
		listener.onBuildPhaseChanged(BuildPhase.GetChangeLog);
		listener.onBuildPhaseChanged(BuildPhase.Publish);
		
		verify();
		
		assertEquals(Arrays.asList("permanentjoe"), listener.getRecipients());
	}
	
	public void testFormatNoticeSingleUser() throws Exception {
		status.setName("my project");
		status.setBuildNumber(24);
		
		replay();

		String message = listener.formatNotificationMessage(null, "permanentjoe");
		
		verify();
		
		assertEquals("You broke the build (way to go).  See " +
				"http://localhost.localdomain:8080/vulcan/projects/my+project/24/errors " +
				"for more info.", message);
	}
	
	public void testFormatNoticeMutliUser() throws Exception {
		status.setName("my project");
		status.setBuildNumber(24);
		listener.addRecipients("two", "three");
		
		replay();

		String message = listener.formatNotificationMessage(null, "permanentjoe");

		verify();
		
		assertEquals("You broke the build (way to go).  See " +
				"http://localhost.localdomain:8080/vulcan/projects/my+project/24/errors " +
				"for more info.\n" +
				"These jokers also got notice: two, three.", message);
	}
	
	public void testFormatNoticeMutliUserDropsGateway() throws Exception {
		status.setName("my project");
		status.setBuildNumber(24);
		listener.addRecipients("two@aim.example.com");
		
		replay();

		String message = listener.formatNotificationMessage(null, "permanentjoe");

		verify();
		
		assertEquals("You broke the build (way to go).  See " +
				"http://localhost.localdomain:8080/vulcan/projects/my+project/24/errors " +
				"for more info.\n" +
				"These jokers also got notice: two.", message);
	}
	
	public void testAttach() throws Exception {
		projectBuilder.addBuildStatusListener(listener);
		
		replay();
		
		listener.attach();
		
		verify();
	}
	
	public void testDetach() throws Exception {
		expect(projectBuilder.removeBuildStatusListener(listener)).andReturn(true);
		
		replay();
		
		listener.detach();
		
		verify();
	}
	
	public void testDetachAfterFirstError() throws Exception {
		expect(projectBuilder.removeBuildStatusListener(listener)).andReturn(true);
		
		replay();
		
		listener.onErrorLogged(new BuildMessageDto());
		
		verify();
	}
}
