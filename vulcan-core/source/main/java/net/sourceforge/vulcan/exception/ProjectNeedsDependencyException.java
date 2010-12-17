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
package net.sourceforge.vulcan.exception;

import static org.apache.commons.lang.StringUtils.join;

public final class ProjectNeedsDependencyException extends Exception {
	final String[] projectsToDelete;
	final String[] dependantProjects;

	public ProjectNeedsDependencyException(
			final String[] projectsToDelete,
			final String[] dependantProjects) {
		this.projectsToDelete = projectsToDelete;
		this.dependantProjects = dependantProjects;
	}
	public String[] getDependantProjects() {
		return dependantProjects;
	}
	public String[] getProjectsToDelete() {
		return projectsToDelete;
	}
	public String getKey() {
		return "messages.dependency.required";
	}
	public Object[] getArgs() {
		return new Object[] {
				join(dependantProjects, ", "),
				join(projectsToDelete, ", ")};
	}
}
