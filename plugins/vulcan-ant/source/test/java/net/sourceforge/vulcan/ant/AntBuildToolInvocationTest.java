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
package net.sourceforge.vulcan.ant;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import net.sourceforge.vulcan.ant.receiver.EventListener;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;

public class AntBuildToolInvocationTest extends AntBuildToolTestBase {
	ProjectStatusDto buildStatus = new ProjectStatusDto();
	
	List<AntEventSummary> events = new ArrayList<AntEventSummary>();
	
	List<String> details = new ArrayList<String>();
	
	List<String> errors = new ArrayList<String>();
	List<String> warnings = new ArrayList<String>();
	
	BuildDetailCallback detailCallback = new BuildDetailCallback() {
		public void setDetail(String detail) {
			details.add(detail);
		}
		public void setDetailMessage(String messageKey, Object[] args) {
			fail("should not call this method");
		}
		public void setPhaseMessageKey(String phase) {
			fail("should not call this method");
		}
		public void reportError(String message, String arg1, Integer arg2, String arg3) {
			errors.add(message);
		}
		public void reportWarning(String message, String arg1, Integer arg2, String arg3) {
			warnings.add(message);
		}
	};
	
	public AntBuildToolInvocationTest() {
		super(true);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		buildStatus.setBuildNumber(792);
		buildStatus.setRevision(new RevisionTokenDto(11432l, "11-43/2"));
		buildStatus.setTagName("1.2rc4");
	}
	public void testExecute() throws Exception {
		tool.execute(new String[] {"sh", "-c", "exit 0"}, projectConfig.getWorkDir());
	}
	public void testExecuteReturnsNonZero() throws Exception {
		try {
			tool.execute(new String[] {"sh", "-c", "exit 1"}, projectConfig.getWorkDir());
			fail("should have thrown BuildFailedException");
		} catch (BuildFailedException e) {
			assertEquals("unknown", e.getMessage());
		}
	}
	public void testExecuteBadCommand() throws Exception {
		try {
			tool.execute(new String[] {"no.such.program"}, projectConfig.getWorkDir());
			fail("should have thrown ConfigException");
		} catch (ConfigException e) {
			assertEquals("ant.exec.failure", e.getKey());
			assertEquals(1, e.getArgs().length);
			assertNotNull(e.getArgs()[0]);
		}
	}
	public void testInterrupt() throws Exception {
		Thread.currentThread().interrupt();
		
		final Date date = new Date();
		
		tool.execute(new String[] {"sh", "-c", "sleep 3"}, projectConfig.getWorkDir());
		
		assertTrue("Did not interrupt", new Date().getTime() - date.getTime() < 2000);
	}
	
	public void testGetsEvents() throws Exception {
		config.setTargets("");

		tool.getEventSource().addEventListener(new EventListener() {
			public void eventReceived(AntEventSummary event) {
				AntBuildToolInvocationTest.this.events.add(event);
			}
		});
		
		assertEquals(0, events.size());
		
		tool.buildProject(projectConfig, buildStatus, null, detailCallback);
		
		assertTrue(events.size() > 0);
		
		assertEquals(2, details.size());
		assertEquals("all", details.get(0));
		assertEquals(null, details.get(1));
	}
	
	public void testMonitorsJUnit() throws Exception {
		config.setTargets("run-test");

		tool.getEventSource().addEventListener(new EventListener() {
			public void eventReceived(AntEventSummary event) {
				AntBuildToolInvocationTest.this.events.add(event);
			}
		});
		
		assertEquals(0, events.size());
		
		tool.buildProject(projectConfig, buildStatus, null, detailCallback);
		
		assertTrue(events.size() > 0);
		
		assertEquals("run-test", details.get(0));
		assertEquals("run-test (2/0/0)", details.get(1));
		assertEquals("run-test (5/1/0)", details.get(2));
		assertEquals("run-test (6/1/1)", details.get(3));
		assertEquals(null, details.get(4));
	}
	
	public void testMonitorsJUnitResetsOnNewTarget() throws Exception {
		config.setTargets("run-test run-test2");

		tool.getEventSource().addEventListener(new EventListener() {
			public void eventReceived(AntEventSummary event) {
				AntBuildToolInvocationTest.this.events.add(event);
			}
		});
		
		assertEquals(0, events.size());
		
		tool.buildProject(projectConfig, buildStatus, null, detailCallback);
		
		assertTrue(events.size() > 0);
		
		assertEquals("run-test", details.get(0));
		assertEquals("run-test (2/0/0)", details.get(1));
		assertEquals("run-test (5/1/0)", details.get(2));
		assertEquals("run-test (6/1/1)", details.get(3));

		assertEquals(null, details.get(4));
		
		assertEquals("run-test2", details.get(5));
		assertEquals("run-test2 (5/0/0)", details.get(6));
		assertEquals("run-test2 (20/1/14)", details.get(7));
		assertEquals("run-test2 (31/1/25)", details.get(8));
		
		assertEquals(null, details.get(9));
	}
	
	public void testAntSeesBuildProperties() throws Exception {
		antConfig.setBuildNumberPropertyName("build.number");
		antConfig.setRevisionPropertyName("repo.revision");
		antConfig.setNumericRevisionPropertyName("repo.revision.numeric");
		antConfig.setTagNamePropertyName("repo.tag");
		antConfig.setBuildUserPropertyName("build.user");
		antConfig.setBuildSchedulerPropertyName("build.scheduler");
		
		buildStatus.setRequestedBy("Sam");
		
		config.setTargets("echo-build-info-1");
		
		tool.getEventSource().addEventListener(new EventListener() {
			public void eventReceived(AntEventSummary event) {
				AntBuildToolInvocationTest.this.events.add(event);
			}
		});
		
		assertEquals(0, events.size());
		
		tool.buildProject(projectConfig, buildStatus, null, detailCallback);
		
		assertTrue(events.size() > 0);

		assertMessageLogged("revision: 11-43/2 (11432)");
		assertMessageLogged("tag: 1.2rc4");
		assertMessageLogged("build number: 792");
		assertMessageLogged("build user: Sam");
	}
		
	public void testAntSeesBuildPropertiesScheduledBuild() throws Exception {
		antConfig.setBuildNumberPropertyName("build.number");
		antConfig.setRevisionPropertyName("repo.revision");
		antConfig.setNumericRevisionPropertyName("repo.revision.numeric");
		antConfig.setTagNamePropertyName("repo.tag");
		antConfig.setBuildUserPropertyName("build.user");
		antConfig.setBuildSchedulerPropertyName("build.scheduler");
		
		buildStatus.setRequestedBy("Sambot");
		buildStatus.setScheduledBuild(true);
		
		config.setTargets("echo-build-info-1");
		
		tool.getEventSource().addEventListener(new EventListener() {
			public void eventReceived(AntEventSummary event) {
				AntBuildToolInvocationTest.this.events.add(event);
			}
		});
		
		assertEquals(0, events.size());
		
		tool.buildProject(projectConfig, buildStatus, null, detailCallback);
		
		assertTrue(events.size() > 0);

		assertMessageLogged("revision: 11-43/2 (11432)");
		assertMessageLogged("tag: 1.2rc4");
		assertMessageLogged("build number: 792");
		assertMessageLogged("build scheduler: Sambot");
	}
	
	public void testAntSeesRevisionAndTagNameWithDifferentNames() throws Exception {
		antConfig.setBuildNumberPropertyName("pretzel");
		antConfig.setRevisionPropertyName("foo");
		antConfig.setNumericRevisionPropertyName("qwerty");
		antConfig.setTagNamePropertyName("bar");
		
		config.setTargets("echo-build-info-2");
		
		tool.getEventSource().addEventListener(new EventListener() {
			public void eventReceived(AntEventSummary event) {
				AntBuildToolInvocationTest.this.events.add(event);
			}
		});
		
		assertEquals(0, events.size());
		
		tool.buildProject(projectConfig, buildStatus, null, detailCallback);
		
		assertTrue(events.size() > 0);

		assertMessageLogged("revision: 11-43/2 (11432)");
		assertMessageLogged("tag: 1.2rc4");
		assertMessageLogged("build number: 792");
	}
	
	public void testDetailHasSubTask() throws Exception {
		config.setTargets("task");

		tool.getEventSource().addEventListener(new EventListener() {
			public void eventReceived(AntEventSummary event) {
				AntBuildToolInvocationTest.this.events.add(event);
			}
		});
		
		assertEquals(0, events.size());
		
		tool.buildProject(projectConfig, buildStatus, null, detailCallback);
		
		assertTrue(events.size() > 0);
		
		assertEquals(4, details.size());
		assertEquals("task", details.get(0));
		assertEquals("subtask", details.get(1));
		assertEquals("task", details.get(2));
		assertEquals(null, details.get(3));
	}

	public void testGetsFailureReason() throws Exception {
		config.setTargets("fail");

		tool.getEventSource().addEventListener(new EventListener() {
			public void eventReceived(AntEventSummary event) {
				AntBuildToolInvocationTest.this.events.add(event);
			}
		});
		
		assertEquals(0, events.size());
		
		try {
			tool.buildProject(projectConfig, buildStatus, null, detailCallback);
			fail("expected BuildFailedException");
		} catch (BuildFailedException e) {
			final AntEventSummary event = events.get(events.size()-1);
			assertEquals("Can the listener grab this message?", event.getMessage());

			assertEquals(event.getMessage(), e.getMessage());
			assertEquals("fail", e.getTarget());
		}
		
		assertEquals(2, details.size());
		assertEquals("fail", details.get(0));
		assertEquals(null, details.get(1));
	}
	
	public void testGetsFailureReasonAndProperTarget() throws Exception {
		config.setTargets("thing1");

		tool.getEventSource().addEventListener(new EventListener() {
			public void eventReceived(AntEventSummary event) {
				AntBuildToolInvocationTest.this.events.add(event);
			}
		});
		
		assertEquals(0, events.size());
		
		try {
			tool.buildProject(projectConfig, buildStatus, null, detailCallback);
			fail("expected BuildFailedException");
		} catch (BuildFailedException e) {
			final AntEventSummary event = events.get(events.size()-1);
			assertEquals("thing1 doesn't work", event.getMessage());

			assertEquals(event.getMessage(), e.getMessage());
			assertEquals("thing1", e.getTarget());
			
			assertEquals(0, errors.size());
			assertEquals(0, warnings.size());
		}
	}
	
	public void testReportsCompileErrors() throws Exception {
		config.setTargets("compile");

		tool.getEventSource().addEventListener(new EventListener() {
			public void eventReceived(AntEventSummary event) {
				AntBuildToolInvocationTest.this.events.add(event);
			}
		});

		try {
			tool.buildProject(projectConfig, buildStatus, null, detailCallback);
			fail("expected BuildFailedException");
		} catch (BuildFailedException e) {
		}
		
		assertEquals(0, errors.size());
		assertEquals(13, warnings.size());
	}
	
	public void testReportsErrorsAndWarnings() throws Exception {
		config.setTargets("generateWarnings");

		assertEquals(0, warnings.size());
		
		tool.buildProject(projectConfig, buildStatus, null, detailCallback);
		
		assertEquals(2, warnings.size());
		assertEquals("You are allowed to do that but you really shouldn't.", warnings.get(0));
		assertEquals("This is a fake deprecation message.", warnings.get(1));
		
		assertEquals(1, errors.size());
		assertEquals("Foo.java:43: syntax error.", errors.get(0));
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
