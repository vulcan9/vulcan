/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2010 Chris Eldredge
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
package net.sourceforge.vulcan.core;

import java.util.List;
import java.util.Map;

import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;

public interface DependencyGroup {
	List<ProjectStatusDto> getPendingTargets();

	List<ProjectConfigDto> getPendingProjects();
	
	void initializeBuildResults(Map<String, ProjectStatusDto> statusMap);

	void addTarget(ProjectConfigDto config);
	void addTarget(ProjectConfigDto config, ProjectStatusDto buildStatus);

	boolean isEmpty();

	boolean isBlocked() throws DependencyException;

	BuildTarget getNextTarget() throws DependencyException;

	void targetCompleted(ProjectConfigDto config,
			boolean success);

	String getName();
	
	void setName(String name);
	
	boolean isManualBuild();

	void setManualBuild(boolean manualBuild);
}