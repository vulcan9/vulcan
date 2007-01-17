/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2006 Chris Eldredge
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
package net.sourceforge.vulcan.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import net.sourceforge.vulcan.ant.receiver.EventListener;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.BuildFailedException;

public class Maven2BuildToolInvocationTest extends MavenBuildToolTestBase {
	List<AntEventSummary> events = new ArrayList<AntEventSummary>();
	
	List<String> details = new ArrayList<String>();

	List<String> errors = new ArrayList<String>();
	
	File logFile;
	
	ProjectStatusDto status = new ProjectStatusDto();
	
	final EventListener listener = new EventListener() {
		public void eventReceived(AntEventSummary event) {
			Maven2BuildToolInvocationTest.this.events.add(event);
		}
	};

	BuildDetailCallback detailCallback = new BuildDetailCallback() {
		public void setDetail(String detail) {
			details.add(detail);
		}
		public void setPhase(String phase) {
			fail("should not call this method");
		}
		public void reportError(String message, String arg1, Integer arg2, String arg3) {
			errors.add(message);
		}
		public void reportWarning(String arg0, String arg1, Integer arg2, String arg3) {
		}
	};

	public Maven2BuildToolInvocationTest() {
		super(true);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tool = new MavenBuildTool(config, mavenConfig, null, mavenConfig.getMavenHomes()[1]);
		logFile = File.createTempFile("vulcan-maven-junit-build", ".log");
		logFile.deleteOnExit();
		
		status.setBuildNumber(42);
		status.setTagName("someTagName");
		status.setRevision(new RevisionTokenDto(3241l, "3-2-41"));
	}
	
	public void testGetsEvents() throws Exception {
		config.setTargets("clean");

		tool.getEventSource().addEventListener(listener);
		
		assertEquals(0, events.size());
		
		tool.buildProject(projectConfig, status, null, detailCallback);
		
		assertTrue(events.size() > 0);
		
		assertTrue("Did not receive enough events", details.size() > 1);
		assertEquals("clean:clean", details.get(details.size() - 2));
		assertEquals(null, details.get(details.size() - 1));
	}
	
	public void testCompileFails() throws Exception {
		config.setTargets("compile");
		
		try {
			tool.buildProject(projectConfig, status, null, detailCallback);
			fail("expected build to fail");
		} catch (BuildFailedException dontCare) {
		}
		
		assertEquals(1, errors.size());
		assertEquals("Compilation failure", errors.get(0));
	}
	
	void assertMessageLogged(String message) {
		for (AntEventSummary e : events) {
			if (message.equals(e.getMessage())) {
				return;
			}
		}
		fail("Expected message but did not find it: " + message);
	}
}
