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
package net.sourceforge.vulcan.ant;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.Project;

import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import net.sourceforge.vulcan.ant.receiver.EventListener;
import net.sourceforge.vulcan.core.BuildDetailCallback;

final class DetailCallbackEventListener implements EventListener {
	private final static Pattern JUNIT_SUMMARY = Pattern.compile(
			".*Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+).*",
			Pattern.MULTILINE | Pattern.DOTALL);
	
	private final Stack<String> tasks = new Stack<String>();
	private final BuildDetailCallback detailCallback;
	
	private String mostRecentTarget;
	private AntEventSummary latestEvent;
	
	private int junitTotal;
	private int junitFailures;
	private int junitErrors;
	
	public DetailCallbackEventListener(BuildDetailCallback detailCallback) {
		this.detailCallback = detailCallback;
	}
	
	public void eventReceived(AntEventSummary event) {
		latestEvent = event;
	
		final AntBuildEvent type = AntBuildEvent.valueOf(event.getType());
	
		switch (type) {
			case TARGET_STARTED:
				pushTarget(event.getTargetName(), true);
				break;
			case TARGET_FINISHED:
				popTarget();
				break;
			case TASK_STARTED:
				pushTarget(event.getTaskName(), false);
				break;
			case TASK_FINISHED:
				popTask();
				break;
		}
		
		parseMessage(event);
	}
	
	public AntEventSummary getLatestEvent() {
		return latestEvent;
	}
	
	public String getMostRecentTarget() {
		return mostRecentTarget;
	}
	
	private void pushTarget(String name, boolean setMostRecentTarget) {
		if (setMostRecentTarget) {
			mostRecentTarget = name;
			junitTotal = 0;
			junitFailures = 0;
			junitErrors = 0;
		} else if (mostRecentTarget != null) {
			return;
		}
		
		tasks.push(name);
		detailCallback.setDetail(name);
	}
	private void popTarget() {
		if (tasks.isEmpty()) {
			return;
		}
		
		tasks.pop();
		
		if (!tasks.isEmpty()) {
			mostRecentTarget = tasks.peek();
		} else {
			mostRecentTarget = null;
		}
		detailCallback.setDetail(mostRecentTarget);
	}
	private void popTask() {
		if (tasks.isEmpty() || mostRecentTarget != null) {
			return;
		}
		
		tasks.pop();
		
		if (!tasks.isEmpty()) {
			detailCallback.setDetail(tasks.peek());
		} else {
			detailCallback.setDetail(null);
		}
	}
	private void parseMessage(AntEventSummary event) {
		final String message = event.getMessage();
		
		if (message == null) {
			return;
		}
		
		final int priority = event.getPriority();
		
		if (priority == Project.MSG_ERR) {
			detailCallback.reportError(message,
					event.getFile(), event.getLineNumber(), event.getCode());
		} else if (priority == Project.MSG_WARN) {
			detailCallback.reportWarning(message,
					event.getFile(), event.getLineNumber(), event.getCode());
		}
		
		final Matcher matcher = JUNIT_SUMMARY.matcher(message);
		if (matcher.matches()) {
			matcher.start();
			
			junitTotal += Integer.parseInt(matcher.group(1));
			junitFailures += Integer.parseInt(matcher.group(2));
			junitErrors += Integer.parseInt(matcher.group(3));
			
			final StringBuffer buf = new StringBuffer(mostRecentTarget == null ? "junit" : mostRecentTarget);
			
			buf.append(" (");
			buf.append(junitTotal);
			buf.append("/");
			buf.append(junitFailures);
			buf.append("/");
			buf.append(junitErrors);
			buf.append(")");
			
			detailCallback.setDetail(buf.toString());
		}
	}
}