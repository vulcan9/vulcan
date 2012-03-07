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

import java.io.Serializable;

import org.apache.tools.ant.Project;

public final class AntEventSummary implements Serializable {
	private final String type;
	
	private final String projectName;
	private final String targetName;
	private final String taskName;
	
	private final String message;
	private final int priority;
	private String file;
	private final Integer lineNumber;
	private final String code;

	public AntEventSummary(String type, String projectName, String targetName,
			String taskName, String message) {
		this(type, projectName, targetName, taskName, message, Project.MSG_INFO);
	}
	public AntEventSummary(String type, String projectName, String targetName,
			String taskName, String message, int priority) {
		
		this(type, projectName, targetName, taskName, message, priority, null, null, null);
	}

	public AntEventSummary(String type, String projectName, String targetName,
			String taskName, String message, int priority, String file,
			Integer lineNumber, String code) {
		
		this.type = type;
		this.projectName = projectName;
		this.targetName = targetName;
		this.taskName = taskName;
		this.message = message;
		this.priority = priority;
		this.file = file;
		this.lineNumber = lineNumber;
		this.code = code;
	}
	public String getMessage() {
		return message;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getTargetName() {
		return targetName;
	}

	public String getTaskName() {
		return taskName;
	}

	public String getType() {
		return type;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public String getFile() {
		return file;
	}
	
	public Integer getLineNumber() {
		return lineNumber;
	}
	
	public String getCode() {
		return code;
	}
}
