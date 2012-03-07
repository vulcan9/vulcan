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
package net.sourceforge.vulcan.ant.buildlistener;

public interface Constants {
	public static final String DEFAULT_HOST = "localhost";
	public static final String DEFAULT_PORT = "7123";
	
	public static final String PORT_PROPERTY = Constants.class.getName() + ".PORT";
	public static final String HOST_PROPERTY = Constants.class.getName() + ".HOST";

	public static final int MAX_MESSAGE_LENGTH = 4096;
	public static final String MESSAGE_TRUNCATED_SUFFIX = "...";
	
	public static final String BUILD_STARTED = "BUILD_STARTED";
	public static final String BUILD_FINISHED = "BUILD_FINISHED";
	public static final String TARGET_STARTED = "TARGET_STARTED";
	public static final String TARGET_FINISHED = "TARGET_FINISHED";
	public static final String TASK_STARTED = "TASK_STARTED";
	public static final String TASK_FINISHED = "TASK_FINISHED";
	public static final String MESSAGE_LOGGED = "MESSAGE_LOGGED";;

}
