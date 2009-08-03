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

import java.text.MessageFormat;
import java.util.Arrays;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.BuildPhase;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.jabber.JabberPluginConfig.EventsToMonitor;

public class JabberBuildStatusListenerTest extends EasyMockTestCase {
	
	JabberBuildStatusListener listener;
	JabberPluginConfig config = new JabberPluginConfig();
	JabberClient client = createStrictMock(JabberClient.class);
	ScreenNameMapper resolver = createStrictMock(ScreenNameMapper.class);
	ProjectBuilder projectBuilder = createStrictMock(ProjectBuilder.class);
	ProjectStatusDto status = new ProjectStatusDto();
	BuildMessageDto error = new BuildMessageDto();
	ChangeLogDto changeLog = new ChangeLogDto();
	ChangeSetDto commit1;
	ChangeSetDto commit2;
	ChangeSetDto commit3;
	ChangeSetDto commit4;
	
	String url = "http://example.com/vulcan/projects/foo/LATEST/";
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	
		config.setVulcanUrl("http://localhost.localdomain:8080/vulcan");
		config.setMessageFormat("You broke the build (way to go).  See {Link} for more info.\n" +
				"These jokers also got notice: {Users}.");
		config.setBuildMasterMessageFormat("One of these users prolly broke the build: {Users}.");
		
		listener = new JabberBuildStatusListener(projectBuilder, client, resolver, config, status);
		
		commit1 = makeChangeSet("Sam");
		commit2 = makeChangeSet("Jesse");
		commit3 = makeChangeSet("Sam");
		commit4 = makeChangeSet("Sydney");
		
		status.setName("my project");
		status.setBuildNumber(24);
		
		error.setMessage("expected ;");
		error.setCode("FC1024");
		error.setFile("MyModule.fcs");
		error.setLineNumber(37);
		
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

	public void testMapsAuthorsToRecipientsOnBuildStartedRemovesDupesCaseInsensitive() throws Exception {
		status.setChangeLog(changeLog);
		changeLog.setChangeSets(Arrays.asList(commit1, commit2, commit3, commit4));
		
		listener.addRecipients("permanentjoe");
		
		expect(resolver.lookupByAuthor(Arrays.asList("Sam", "Jesse", "Sydney"))).andReturn(Arrays.asList("sam82", "PermanentJoe"));
		
		replay();
		
		listener.onBuildPhaseChanged(BuildPhase.Build);
		
		verify();
		
		assertEquals(Arrays.asList("permanentjoe", "sam82"), listener.getRecipients());
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
	
	public void testFormatNoticeBuildMaster() throws Exception {
		listener.addCommitters(Arrays.asList("two", "three"));
		
		replay();

		String message = listener.formatNotificationMessage("permanentjoe", "errors", error);
		
		verify();
		
		assertEquals("One of these users prolly broke the build: two, three.", message);
	}
	
	public void testFormatNoticeMutliUser() throws Exception {
		listener.addCommitters(Arrays.asList("two", "three"));
		
		replay();

		String message = listener.formatNotificationMessage("two", "errors", error);

		verify();
		
		assertEquals("You broke the build (way to go).  See " +
				"http://localhost.localdomain:8080/vulcan/projects/my+project/24/errors " +
				"for more info.\n" +
				"These jokers also got notice: three.", message);
	}
	
	public void testFormatNoticeMutliUserDropsGateway() throws Exception {
		listener.addRecipients("Build Master");
		listener.addCommitters(Arrays.asList("two@aim.example.com"));
		
		replay();

		String message = listener.formatNotificationMessage("permanentjoe", "errors", error);

		verify();
		
		assertEquals("One of these users prolly broke the build: two.", message);
	}
	
	public void testFormatMessage() throws Exception {
		final String pattern = "{0,choice,0#|0<We also notified {1}.}";
		
		assertEquals("", MessageFormat.format(pattern, new Object[] {0, ""}));
		assertEquals("We also notified Fred.", MessageFormat.format(pattern, new Object[] {1, "Fred"}));
		assertEquals("We also notified Fred, Bill.", MessageFormat.format(pattern, new Object[] {2, "Fred, Bill"}));
	}
	
	public void testFormatMessageSimple() throws Exception {
		assertEquals(error.getMessage(), JabberBuildStatusListener.substituteParameters("{Message}", url, "", error, status));
	}
	
	public void testFormatNumber() throws Exception {
		error.setLineNumber(1024);
		assertEquals("1,024", JabberBuildStatusListener.substituteParameters("{LineNumber,number}", url, "", error, status));
	}
	
	public void testFormatParamNameCaseInsensitive() throws Exception {
		error.setLineNumber(1024);
		assertEquals("1,024", JabberBuildStatusListener.substituteParameters("{linenumber,number}", url, "", error, status));
	}
	
	public void testFormatChoice() throws Exception {
		final String format = "{LineNumber,choice,-1#|0<Line {LineNumber, number}}";
		
		error.setLineNumber(null);
		
		assertEquals("", JabberBuildStatusListener.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(0);
		
		assertEquals("", JabberBuildStatusListener.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(1);
		
		assertEquals("Line 1", JabberBuildStatusListener.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(1024);
		
		assertEquals("Line 1,024", JabberBuildStatusListener.substituteParameters(format, url, "", error, status));
	}
	
	public void testFormatBlankable() throws Exception {
		final String format = "{Users?,choice,0#|0<We also notified {Users}.}";
		
		assertEquals("", JabberBuildStatusListener.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(0);
		
		assertEquals("We also notified Sam.", JabberBuildStatusListener.substituteParameters(format, url, "Sam", error, status));
	}
	
	public void testFormatBlankableSimplified() throws Exception {
		final String format = "{Users?,We also notified {Users}.}";
		
		assertEquals("", JabberBuildStatusListener.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(0);
		
		assertEquals("We also notified Sam.", JabberBuildStatusListener.substituteParameters(format, url, "Sam", error, status));
	}
	
	public void testFormatAllParams() throws Exception {
		final String format =
			"You broke ''{ProjectName}''{Users?, (or {Users} did)}.\n" +
			"{File?,{File}}{LineNumber?,': line '{LineNumber}}{Code?,': '{Code}}: {Message}\n" +
			"See {Link} for more info.  This was build {BuildNumber}.";
		
		final String s = JabberBuildStatusListener.substituteParameters(format, url, "Sam", error, status);
		
		assertEquals("You broke 'my project' (or Sam did).\n" +
				"MyModule.fcs: line 37: FC1024: expected ;\n" +
				"See http://example.com/vulcan/projects/foo/LATEST/ for more info.  This was build 24.", s);
	}
	
	public void testFormatBlankableSimplifiedCompound() throws Exception {
		final String format = "{File?,In {File}}{LineNumber?,:line {LineNumber}}";
		
		error.setFile("");
		error.setLineNumber(null);
		
		assertEquals("", JabberBuildStatusListener.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(123);
		error.setFile("Foo.java");
		assertEquals("In Foo.java:line 123", JabberBuildStatusListener.substituteParameters(format, url, "Sam", error, status));
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
	
	public void testSendsMessageOnErrorsEnabled() throws Exception {
		config.setEventsToMonitor(new EventsToMonitor[] {EventsToMonitor.Errors});
		client.sendMessage(eq("user"), (String) notNull());
		
		expect(projectBuilder.removeBuildStatusListener(listener)).andReturn(true);
		
		replay();

		listener.addRecipients("user");
		listener.onErrorLogged(new BuildMessageDto());
		
		verify();
	}
	
	public void testDoesNotSendMessageOnErrorsDisabled() throws Exception {
		config.setEventsToMonitor(new EventsToMonitor[] {});
		
		replay();

		listener.addRecipients("user");
		listener.onErrorLogged(new BuildMessageDto());
		
		verify();
	}
	
	public void testSendsMessageOnWarningEnabled() throws Exception {
		config.setEventsToMonitor(new EventsToMonitor[] {EventsToMonitor.Warnings});
		client.sendMessage(eq("user"), (String) notNull());
		
		expect(projectBuilder.removeBuildStatusListener(listener)).andReturn(true);
		
		replay();

		listener.addRecipients("user");
		listener.onWarningLogged(new BuildMessageDto());
		
		verify();
	}
	
	public void testDoesNotSendMessageOnWarningDisabled() throws Exception {
		config.setEventsToMonitor(new EventsToMonitor[] {});
		
		replay();

		listener.addRecipients("user");
		listener.onWarningLogged(new BuildMessageDto());
		
		verify();
	}
	
	public void testDoesNotSendMessageOnWarningNoMatch() throws Exception {
		config.setWarningRegex("no match my friend");
		config.setEventsToMonitor(new EventsToMonitor[] {EventsToMonitor.Warnings});
		
		replay();

		listener.addRecipients("user");
		final BuildMessageDto warning = new BuildMessageDto();
		warning.setMessage("I thought I warned you.");
		listener.onWarningLogged(warning);
		
		verify();
	}
	
	public void testSendsMessageOnWarningEnabledMatchPattern() throws Exception {
		config.setWarningRegex("Match");
		config.setEventsToMonitor(new EventsToMonitor[] {EventsToMonitor.Warnings});
		client.sendMessage(eq("user"), (String) notNull());
		
		expect(projectBuilder.removeBuildStatusListener(listener)).andReturn(true);
		
		replay();

		listener.addRecipients("user");
		final BuildMessageDto warning = new BuildMessageDto();
		warning.setMessage("Got a match?");
		listener.onWarningLogged(warning);

		
		verify();
	}

}
