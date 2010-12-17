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

import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
//TODO: move into web
public interface Keys {
	String STATE_MANAGER = "stateManager";
	String PROJECT_MANAGER = "stateManager";
	String EVENT_POOL = "eventPool";
	String EVENT_HANDLER = "eventHandler";
	
	String FILE_LIST = "fileList";
	String DIR_PATH = "fileListPath";
	String FILE_LIST_VIEW = "/WEB-INF/jsp/fileList.jsp";
	String LOG_CONTENTS = "logContents";
}