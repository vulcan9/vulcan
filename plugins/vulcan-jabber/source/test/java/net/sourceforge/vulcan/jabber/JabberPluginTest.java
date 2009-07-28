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

import java.util.regex.Pattern;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.BuildStatusListener;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.BuildStartingEvent;
import net.sourceforge.vulcan.jabber.JabberPluginConfig.EventsToMonitor;
import net.sourceforge.vulcan.jabber.JabberPluginConfig.ProjectsToMonitor;

public class JabberPluginTest extends EasyMockTestCase {
	JabberPlugin plugin = new JabberPlugin();
	JabberPluginConfig config = new JabberPluginConfig();
	
	JabberClient client = createStrictMock(JabberClient.class);
	BuildManager buildManager = createStrictMock(BuildManager.class);
	ProjectBuilder projectBuilder = createStrictMock(ProjectBuilder.class);
	
	BuildDaemonInfoDto buildDaemonInfo = new BuildDaemonInfoDto();
	ProjectConfigDto target = new ProjectConfigDto();
	ProjectStatusDto status = new ProjectStatusDto();
	
	@Override
	public void setUp() {
		config.setServer("example.com");
		config.setPort(5222);
		config.setUsername("user");
		config.setPassword("pass");
		config.setProjectsToMonitor(ProjectsToMonitor.All);
		plugin.setClient(client);
		plugin.config = config;
		
		target.setName("a");
		status.setName("a");
	}
	
	public void testSetConfigConnectsClient() throws Exception {
		client.refreshConnection("example.com", 5222, "", "user", "pass");
		
		replay();
		
		plugin.setConfiguration(config);
		
		verify();
	}
	
	public void testAddsBuildListener() throws Exception {
		client.refreshConnection("example.com", 5222, "", "user", "pass");
		
		expect(buildManager.getProjectBuilder(status.getName())).andReturn(projectBuilder);
		projectBuilder.addBuildStatusListener((BuildStatusListener) notNull());
		
		replay();
		
		plugin.onBuildStarting(new BuildStartingEvent(buildManager, buildDaemonInfo, target, status));
		
		verify();
	}
	
	public void testDoesNotListenToProjectNotSelected() throws Exception {
		config.setProjectsToMonitor(ProjectsToMonitor.Specify);
		config.setSelectedProjects(new String[] {"not a"});
		
		replay();
		
		plugin.onBuildStarting(new BuildStartingEvent(buildManager, buildDaemonInfo, target, status));
		
		verify();
	}
	
	public void testAddsBuildListenerToSelectedProject() throws Exception {
		config.setProjectsToMonitor(ProjectsToMonitor.Specify);
		config.setSelectedProjects(new String[] {status.getName()});
		client.refreshConnection("example.com", 5222, "", "user", "pass");
		
		expect(buildManager.getProjectBuilder(status.getName())).andReturn(projectBuilder);
		projectBuilder.addBuildStatusListener((BuildStatusListener) notNull());
		
		replay();
		
		plugin.onBuildStarting(new BuildStartingEvent(buildManager, buildDaemonInfo, target, status));
		
		verify();
	}
	
	public void testSendsMessageOnBuildFailureWhenAttached() throws Exception {
		doBuildCompletedTest(Status.FAIL, true, true, true);
	}
	
	public void testSendsMessageOnBuildErrorWhenAttached() throws Exception {
		doBuildCompletedTest(Status.ERROR, true, true, true);
	}
	
	public void testSendsNoMessageOnBuildErrorWhenAttached() throws Exception {
		doBuildCompletedTest(Status.ERROR, false, false, false);
	}
	
	public void testSendsNoMessageOnBuildPassWhenAttached() throws Exception {
		doBuildCompletedTest(Status.PASS, true, false, true);
	}

	private void doBuildCompletedTest(Status result, final boolean isAttached, boolean expectMessage, boolean expectDetach) {
		final boolean[] callFlags = new boolean[2];
		
		plugin.addBuildListener(status.getName(), new JabberBuildStatusListener(null, null, null, config, status) {
			@Override
			protected void onBuildMessageLogged(EventsToMonitor type,
					Pattern regex, BuildMessageDto message, String view) {
				callFlags[0] = true;
			}
			@Override
			public boolean isAttached() {
				return isAttached;
			}
			@Override
			public void detach() {
				callFlags[1] = true;
			}
		});
		
		replay();
		status.setStatus(result);
		plugin.onBuildCompleted(new BuildCompletedEvent(buildManager, buildDaemonInfo, target, status));
		
		verify();
		
		String prefix1 = expectMessage ? "" : "un";
		String prefix2 = expectDetach ? "" : "un";
		
		assertTrue(prefix1 + "expected onBuildMessageLogged()", callFlags[0] == expectMessage);
		assertTrue(prefix2 + "expected detach()", callFlags[1] == expectDetach);
	}
	
	public void testDoesNotRemoveMissingBuildListener() throws Exception {
		replay();
		
		plugin.onBuildCompleted(new BuildCompletedEvent(buildManager, buildDaemonInfo, target, status));
		
		verify();
	}
}
