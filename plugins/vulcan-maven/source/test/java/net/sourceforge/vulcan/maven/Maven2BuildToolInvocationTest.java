/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import net.sourceforge.vulcan.ant.receiver.EventListener;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.BuildFailedException;

public class Maven2BuildToolInvocationTest extends MavenBuildToolTestBase {
	List<AntEventSummary> events = new ArrayList<AntEventSummary>();
	
	List<String> details = new ArrayList<String>();

	List<String> errors = new ArrayList<String>();
	List<String> fileNames = new ArrayList<String>();
	List<Integer> lineNumbers = new ArrayList<Integer>();
	
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
		public void setDetailMessage(String messageKey, Object[] args) {
			fail("should not call this method");
		}
		public void setPhaseMessageKey(String phase) {
			fail("should not call this method");
		}
		public void reportError(String message, String file, Integer line, String arg3) {
			errors.add(message);
			fileNames.add(file);
			lineNumbers.add(line);
		}
		public void reportWarning(String arg0, String arg1, Integer arg2, String arg3) {
		}
		public void addMetric(MetricDto metric) {
		}
	};

	public Maven2BuildToolInvocationTest() {
		super(true);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		config.setOffline(false);
		config.setBuildScript("");
		tool = new MavenBuildTool(config, mavenConfig, null, mavenConfig.getMavenHomes()[1]);
		logFile = File.createTempFile("vulcan-maven-junit-build", ".log");
		logFile.deleteOnExit();
		
		status.setBuildNumber(42);
		status.setTagName("someTagName");
		status.setRevision(new RevisionTokenDto(3241l, "3-2-41"));
	}
	
	public void testGetsEvents() throws Exception {
		config.setTargets("clean");
		config.setOffline(false);
		
		tool.getEventSource().addEventListener(listener);
		
		assertEquals(0, events.size());
		
		tool.buildProject(projectConfig, status, null, detailCallback);
		
		assertTrue(events.size() > 0);
		
		assertTrue("Did not receive enough events", details.size() > 1);
		final String detail1 = details.get(details.size() - 2);
		assertTrue("Expected starts with clean:clean but was " + detail1, detail1.startsWith("clean:clean"));
		assertEquals(null, details.get(details.size() - 1));
	}
	
	public void testDoubleSpace() throws Exception {
		config.setTargets("clean  clean");

		tool.getEventSource().addEventListener(listener);
		
		assertEquals(0, events.size());
		
		tool.buildProject(projectConfig, status, null, detailCallback);
		
		assertTrue(events.size() > 0);
		
		assertTrue("Did not receive enough events", details.size() > 1);
		final String detail1 = details.get(details.size() - 2);
		assertTrue("Expected starts with clean:clean but was " + detail1, detail1.startsWith("clean:clean"));
		assertEquals(null, details.get(details.size() - 1));
	}
	
	public void testCompileFails() throws Exception {
		config.setTargets("compile");
		
		try {
			tool.buildProject(projectConfig, status, null, detailCallback);
			fail("expected build to fail");
		} catch (BuildFailedException dontCare) {
		}
		
		assertEquals(2, errors.size());
		assertEquals("source/main/java/Invalid.java", fileNames.get(0).replaceAll("\\\\", "/"));
		assertTrue(errors.get(0).startsWith("cannot find symbol"));
		assertEquals(3, lineNumbers.get(0).intValue());
		assertEquals("source/main/java/Invalid.java", fileNames.get(1).replaceAll("\\\\", "/"));
		assertTrue(errors.get(1).startsWith("cannot find symbol"));
		assertEquals(6, lineNumbers.get(1).intValue());
	}
	
	public void testSpecifyPomWithMissingDependency() throws Exception {
		config.setBuildScript("pom-missing-dependency.xml");
		config.setTargets("compile");
		
		try {
			tool.buildProject(projectConfig, status, null, detailCallback);
			fail("expected build to fail");
		} catch (BuildFailedException dontCare) {
		}
		
		assertEquals("Actual errors: " + StringUtils.join(errors.iterator(), "\n"), 2, errors.size());
		Collections.sort(errors);
		assertEquals("Missing artifact no.such.group:no-such-artifact:jar:1.0", errors.get(0));
		assertEquals("Missing artifact no.such.group:other-no-such-artifact:jar:1.0", errors.get(1));
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
