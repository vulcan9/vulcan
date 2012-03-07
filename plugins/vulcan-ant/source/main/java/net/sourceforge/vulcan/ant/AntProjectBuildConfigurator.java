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
package net.sourceforge.vulcan.ant;

import java.util.List;

import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.dto.ProjectConfigDto;

import org.springframework.context.ApplicationContext;

public class AntProjectBuildConfigurator implements ProjectBuildConfigurator {
	private final ApplicationContext applicationContext;
	private final String projectName;
	private final String basedir;
	
	public AntProjectBuildConfigurator(ApplicationContext applicationContext, String projectName, String basedir) {
		this.applicationContext = applicationContext;
		this.projectName = projectName;
		this.basedir = basedir;
	}

	public void applyConfiguration(ProjectConfigDto projectConfig,
			String buildSpecRelativePath, List<String> existingProjectNames, boolean createSubprojects) {
		final AntProjectConfig antConfig = new AntProjectConfig();

		antConfig.setApplicationContext(applicationContext);
		antConfig.setBuildScript(buildSpecRelativePath);
		
		projectConfig.setName(projectName);
		projectConfig.setBuildToolConfig(antConfig);
	}

	public String getRelativePathToProjectBasedir() {
		return basedir;
	}

	public List<String> getSubprojectUrls() {
		return null;
	}

	public boolean shouldCreate() {
		return true;
	}
	
	public boolean isStandaloneProject() {
		return false;
	}
}
