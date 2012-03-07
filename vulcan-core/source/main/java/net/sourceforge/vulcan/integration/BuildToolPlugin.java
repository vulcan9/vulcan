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
package net.sourceforge.vulcan.integration;

import java.io.File;

import org.jdom.Document;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;

public interface BuildToolPlugin extends Plugin {
	BuildTool createInstance(BuildToolConfigDto config) throws ConfigException;
	BuildToolConfigDto getDefaultConfig();
	
	/**
	 * @param url The scm repository url used to download <code>buildSpecFile</code>.
	 * @param buildSpecFile A temporary file containing the contents of the build specification.
	 * @param xmlDocument If the contents of buildSpecFile are well-formed XML, a Document containing
	 * the parsed contents.  <code>null</code> otherwise.
	 * @throws ConfigException If <code>buildSpecFile</code> is supported but
	 * an error occurs while processing the file.
	 * @return <code>null</code> if the buildSpecFile is not recognized/supported, or an
	 * instance of ProjectConfigurator if it is.
	 */
	ProjectBuildConfigurator createProjectConfigurator(String url, File buildSpecFile, Document xmlDocument) throws ConfigException;
}
