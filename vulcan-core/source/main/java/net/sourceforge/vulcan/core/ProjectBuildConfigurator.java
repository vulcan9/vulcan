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
package net.sourceforge.vulcan.core;

import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

/**
 * Used by ProjectImporter to configure ProjectConfigDto and related plugin configuration.
 */
@SvnRevision(id="$Id$", url="$HeadURL$")
public interface ProjectBuildConfigurator {
	/**
	 * Take settings and apply them to projectConfig.  Any settings can be applied, but
	 * at a minimum, the following should be defined:
	 * <ul>
	 * 	<li>projectConfig.name</li>
	 * 	<li>projectConfig.buildToolPluginId</li>
	 *  <li>projectConfig.buildToolConfig</li>
	 * </ul>
	 * @param projectConfig The object onto which settings should be applied.
	 */
	void applyConfiguration(ProjectConfigDto projectConfig);

	/**
	 * @return <code>true</code> if the project does not depend on nested files or folders.  This
	 * will cause the repository adaptor to be configured in non-recursive mode.  For
	 * almost all projects, the return value should be <code>false</code>.
	 */
	boolean isStandaloneProject();
}
