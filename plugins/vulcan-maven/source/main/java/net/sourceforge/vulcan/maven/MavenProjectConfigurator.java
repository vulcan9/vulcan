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

import net.sourceforge.vulcan.core.ProjectBuildConfigurator;
import net.sourceforge.vulcan.dto.ProjectConfigDto;

import org.apache.maven.project.MavenProject;

public class MavenProjectConfigurator implements ProjectBuildConfigurator {
	private final MavenProject project;

	public MavenProjectConfigurator(MavenProject project) {
		this.project = project;
	}

	public void applyConfiguration(ProjectConfigDto projectConfig) {
		projectConfig.setName(project.getArtifactId());
	}

	public boolean isStandaloneProject() {
		return false;
	}

}
