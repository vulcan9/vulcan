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
package net.sourceforge.vulcan;

import java.io.File;

import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public interface BuildTool {
	/**
	 * @param projectConfig Configuration information for the project to be built.
	 * @param copy of buildStatus which will have the following properties
	 * initialized:  revision, tagName, changeLog, etc.  This is intended to
	 * expose details of which revision, tag or branch of the project has been
	 * checked out from the repository.  The information is read-only (while
	 * you may call the setters, the information will not propagate to the overall
	 * build outcome).
	 * @param logFile File where output from the build process should be captured.
	 * @param buildDetailCallback Callback mechanism which enables build tool
	 * to provide details as to what task, target, job, etc is being executed.
	 * This information is displayed to users during the build process.
	 * @throws BuildFailedException If the build tool executed but returned a
	 * non-successfull result.
	 * @throws ConfigException If the build tool could not be invoked for some reason.
	 */
	public void buildProject(ProjectConfigDto projectConfig, ProjectStatusDto buildStatus, File logFile, BuildDetailCallback buildDetailCallback) throws BuildFailedException, ConfigException;
}
