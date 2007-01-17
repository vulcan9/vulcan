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
package net.sourceforge.vulcan.ant.buildlistener;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.vulcan.ant.AntBuildEvent;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class SerializingBuildEventPublisherTest extends TestCase {
	final List<AntEventSummary> events = new ArrayList<AntEventSummary>();
	
	final SerializingBuildEventPublisher publisher = new SerializingBuildEventPublisher() {
		@Override
		protected void transmit(byte[] serializedData) {
		}
		@Override
		protected void transmitEvent(AntEventSummary buildEventSummary) {
			events.add(buildEventSummary);
		}
	};
	
	public void testTranslate() {
		final BuildEvent be = new BuildEvent(new Task() {
			@Override
			public String getTaskName() {
				return "MockTask";
			}
		});
		
		final AntEventSummary trans = publisher.translate(AntBuildEvent.TARGET_STARTED.name(), be);
		
		assertNotSame(be, trans);
		assertNotSame(be.getTask(), trans.getTaskName());
		assertEquals(be.getTask().getTaskName(), trans.getTaskName());
	}
	public void testTruncates() throws Exception {
		final StringBuffer buf = new StringBuffer();
		
		while (buf.length() <= Constants.MAX_MESSAGE_LENGTH) {
			buf.append("0123456789ABCDEF");
		}
		
		final Task task = new Task() {
			@Override
			public String getTaskName() {
				return "MockTask";
			}
		};
		final BuildEvent be = new BuildEvent(task) {
			@Override
			public String getMessage() {
				return buf.toString();
			}
		};
		
		final AntEventSummary trans = publisher.translate(AntBuildEvent.TARGET_STARTED.name(), be);
		
		assertTrue(trans.getMessage().endsWith(Constants.MESSAGE_TRUNCATED_SUFFIX));
		assertEquals(Constants.MAX_MESSAGE_LENGTH, trans.getMessage().length());
	}
	public void testProject() throws Exception {
		final Project p = new Project();
		p.setName("mockProj");
		
		Target tgt = new Target();
		tgt.setName("mockTarget");
		
		final BuildException buildException = new BuildException("whoops", new IllegalStateException());
		
		final Task task = new TaskStub(buildException);
		task.setProject(p);
		
		tgt.addTask(task);
		
		p.addTarget(tgt);
		
		p.addBuildListener(publisher);
		
		try {
			p.executeTarget("mockTarget");
			fail("expected exception");
		} catch (BuildException e) {
			assertSame(buildException, e);
		}
		
		for (AntEventSummary event : events) {
			assertNotNull(event);
			assertNotNull("Event has null project", event.getProjectName());
			
		}
	}
	public static final class TaskStub extends Task {
		private final BuildException exception;

		public TaskStub(BuildException exception) {
			this.exception = exception;
		}

		@Override
		public String getTaskName() {
			return "mockTask";
		}

		@Override
		public String getDescription() {
			return "mockTaskDescription";
		}

		@Override
		public void execute() throws BuildException {
			throw exception;
		}
	}
}
