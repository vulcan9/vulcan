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
package net.sourceforge.vulcan.ant;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.dto.ProjectConfigDto;

import org.springframework.context.ApplicationContext;

public class AntProjectBuildConfigurator implements ProjectBuildConfigurator {
	private final ApplicationContext applicationContext;
	private final String projectName;
	private final String basedir;
	private final String buildScript;
	
	public AntProjectBuildConfigurator(ApplicationContext applicationContext, String projectName, String basedir, String url) {
		this.applicationContext = applicationContext;
		this.projectName = projectName;
		this.basedir = basedir;
		this.buildScript = computeBuildScriptLocation(url, basedir);
	}

	public void applyConfiguration(ProjectConfigDto projectConfig,
			List<String> existingProjectNames, boolean createSubprojects) {
		final AntProjectConfig antConfig = new AntProjectConfig();

		antConfig.setApplicationContext(applicationContext);
		antConfig.setBuildScript(buildScript);
		
		projectConfig.setName(projectName);
		projectConfig.setBuildToolConfig(antConfig);
	}

	public String getRelativePathToProjectBasedir() {
		return basedir;
	}

	public List<String> getSubprojectUrls() {
		return null;
	}

	public boolean isStandaloneProject() {
		return false;
	}

	private static String computeBuildScriptLocation(String url, String basedir) {
		final String file = url.substring(url.lastIndexOf("/") + 1);
		
		if (isBlank(basedir)) {
			return file;
		}
		
		final String prefix = url.substring(0, url.lastIndexOf("/") + 1) + basedir;
		
		try {
			final String basePrefix = new URI(prefix).normalize().getPath();
			return new URI(url).normalize().getPath().substring(basePrefix.length());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
