/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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

import net.sourceforge.vulcan.ant.io.ObjectSerializer;
import net.sourceforge.vulcan.ant.io.Serializer;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;

public abstract class SerializingBuildEventPublisher implements BuildListener, Constants {
	private Serializer serializer = new ObjectSerializer();
	
	protected abstract void transmit(final byte[] serializedData);
	
	public final void buildStarted(final BuildEvent buildEvent) {
		processEvent(BUILD_STARTED, buildEvent);
	}
	public final void buildFinished(final BuildEvent buildEvent) {
		processEvent(BUILD_FINISHED, buildEvent);
	}
	public final void targetStarted(final BuildEvent buildEvent) {
		processEvent(TARGET_STARTED, buildEvent);
	}
	public final void targetFinished(final BuildEvent buildEvent) {
		processEvent(TARGET_FINISHED, buildEvent);
	}
	public final void taskStarted(final BuildEvent buildEvent) {
		processEvent(TASK_STARTED, buildEvent);
	}
	public final void taskFinished(final BuildEvent buildEvent) {
		processEvent(TASK_FINISHED, buildEvent);
	}
	public final void messageLogged(final BuildEvent buildEvent) {
		processEvent(MESSAGE_LOGGED, buildEvent);
	}

	protected final AntEventSummary translate(final String type, final BuildEvent buildEvent) {
		final Project project = buildEvent.getProject();
		final Target target = buildEvent.getTarget();
		final Task task = buildEvent.getTask();
		
		String projectName = null;
		
		if (project != null) {
			projectName = project.getName();
		}

		String targetName = null;
		if (target != null) {
			targetName = target.getName();
		}
		
		String taskName = null;
		if (task != null) {
			taskName = task.getTaskName();
		}
		
		String message = buildEvent.getMessage();
		if (message == null && buildEvent.getException() != null) {
			message = buildEvent.getException().getMessage();
		}

		message = truncateIfNeeded(message);
		
		return new AntEventSummary(type, projectName,
				targetName, taskName, message, buildEvent.getPriority());
	}

	protected void transmitEvent(final AntEventSummary buildEventSummary) {
		transmit(serializer.serialize(buildEventSummary));
	}
	private final void processEvent(String type, final BuildEvent buildEvent) {
		transmitEvent(translate(type, buildEvent));
	}
	private String truncateIfNeeded(String message) {
		if (message != null && message.length() > Constants.MAX_MESSAGE_LENGTH) {
			final StringBuffer buf = new StringBuffer(
					message.substring(0,
							Constants.MAX_MESSAGE_LENGTH - Constants.MESSAGE_TRUNCATED_SUFFIX.length()));
			buf.append(Constants.MESSAGE_TRUNCATED_SUFFIX);
			
			return buf.toString();
		}
		return message;
	}
}
