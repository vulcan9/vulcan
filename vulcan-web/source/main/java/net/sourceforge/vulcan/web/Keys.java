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
package net.sourceforge.vulcan.web;


public interface Keys {
	String STATE_MANAGER = "stateManager";
	String PROJECT_MANAGER = "stateManager";
	String BUILD_MANAGER = "buildManager";
	String EVENT_POOL = "eventPool";
	String BUILD_OUTCOME_STORE = "buildOutcomeStore";
	String EVENT_HANDLER = "eventHandler";
	String DASHBOARD_COLUMNS = "dashboardColumns";
	String BUILD_HISTORY_COLUMNS = "buildHistoryColumns";
	
	String FILE_LIST = "fileList";
	String DIR_PATH = "fileListPath";
	String FILE_LIST_VIEW = "/WEB-INF/jsp/fileList.jsp";
	String LOG_CONTENTS = "logContents";
	
	String BUILD_HISTORY = "buildHistory";
	String PREFERENCES = "preferences";
	String BROWSER_IE = "browserIE";
}
