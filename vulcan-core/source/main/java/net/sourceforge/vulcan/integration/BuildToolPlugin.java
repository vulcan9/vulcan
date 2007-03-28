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
package net.sourceforge.vulcan.integration;

import java.io.File;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public interface BuildToolPlugin extends Plugin {
	BuildTool createInstance(BuildToolConfigDto config) throws ConfigException;
	BuildToolConfigDto getDefaultConfig();
	
	/**
	 * @param buildSpecFile
	 * @throws ConfigException If <code>buildSpecFile</code> is supported but
	 * an error occurs while processing the file.
	 * @return Null if the buildSpecFile is not recognized/supported, or an
	 * instance of ProjectConfigurator if it is.
	 */
	ProjectBuildConfigurator createProjectConfigurator(File buildSpecFile) throws ConfigException;
}
