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
package net.sourceforge.vulcan;

import java.util.List;

import net.sourceforge.vulcan.dto.ProjectConfigDto;

/**
 * Used by ProjectImporter to configure ProjectConfigDto and related plugin configuration.
 */
public interface ProjectBuildConfigurator {
	/**
	 * If the build file that is being imported is nested in a subdirectory of the project root,
	 * this method is used to obtain the relative path to the base directory which should be
	 * checked out by the Repository Adaptor.
	 * @return A String in the form "(../)*"; one or more levels of parent directories,
	 * or <code>null</code> if the build file is in the base directory.
	 */
	String getRelativePathToProjectBasedir();

	/**
	 * Take settings and apply them to projectConfig.  Any settings can be applied, but
	 * at a minimum, the following should be defined:
	 * <ul>
	 * 	<li>projectConfig.name</li>
	 * 	<li>projectConfig.buildToolPluginId</li>
	 *  <li>projectConfig.buildToolConfig</li>
	 * </ul>
	 * @param projectConfig The object onto which settings should be applied.
	 * @param buildSpecRelativePath The path, relative to the top level project
	 * directory, to the build spec.
	 * @param existingProjectNames List of existing project names.  This list is
	 * provided for several reasons, including:
	 * <ul>
	 * 	<li>Configure dependencies for projecs which exist</li>
	 * 	<li>Avoid a naming conflict when assigning a name to the new project</li> 
	 * </ul>
	 * @param createSubprojects If set to <code>true</code>, the project should be
	 * configured to only build the top level project and not any subprojects
	 * which should be configured as separate projects.
	 */
	void applyConfiguration(ProjectConfigDto projectConfig, String buildSpecRelativePath, List<String> existingProjectNames, boolean createSubprojects);

	/**
	 * This method will only be called when a separate project is being created for each
	 * submodule.
	 * 
	 * @return <code>true</code> if the project does not depend on nested files or folders.  This
	 * will cause the repository adaptor to be configured in non-recursive mode.  For
	 * almost all projects, the return value should be <code>false</code>.
	 */
	boolean isStandaloneProject();

	/**
	 * This method will be called to determine if the specified configuration should not
	 * be imported as a Vulcan project.  In most cases <code>true</code> should be returned.
	 * If the imported URL should only be used to import other project URLs, <code>false</code>
	 * should be returned.
	 */
	boolean shouldCreate();
	
	/**
	 * @return List of scm urls pointing to sub-projects or modules included in this project.
	 * This method is only called if the user opts to create a project for each sub-project/module
	 * instead of building them as one large project.
	 */
	List<String> getSubprojectUrls();
}
