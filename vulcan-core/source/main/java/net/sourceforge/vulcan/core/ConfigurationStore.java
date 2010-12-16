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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import net.sourceforge.vulcan.dto.PluginMetaDataDto;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public interface ConfigurationStore {
	StateManagerConfigDto loadConfiguration() throws StoreException;
	void storeConfiguration(StateManagerConfigDto config) throws StoreException;

	void exportConfiguration(OutputStream os) throws StoreException, IOException;
	void importConfiguration(InputStream is) throws StoreException, IOException;
	
	String getExportMimeType();
	
	/**
	 * @return String representing the absolute path of a directory where
	 * a project's working copy should be stored by default.  The string
	 * should contain the pattern ${projectName} where the name of the
	 * project should be substituted.
	 */
	String getWorkingCopyLocationPattern();
	
	/**
	 * @return true if supplied location conflicts with directory reserved
	 * by vulcan.
	 */
	boolean isWorkingCopyLocationInvalid(String location);
	
	PluginMetaDataDto[] getPluginConfigs();
	PluginMetaDataDto extractPlugin(InputStream is) throws StoreException;
	void deletePlugin(String id) throws StoreException;

	boolean diffExists(String projectName, UUID diffId);
	boolean buildLogExists(String projectName, UUID diffId);
	
	File getChangeLog(String projectName, UUID diffId) throws StoreException;
	File getBuildLog(String projectName, UUID diffId) throws StoreException;
}
