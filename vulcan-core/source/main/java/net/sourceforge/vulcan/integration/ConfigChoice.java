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


/**
 * Enumeration of categories that may be used to display available choices to
 * a user when configuring a plugin.  These values instruct the GUI component
 * on which options should be presented to a user as available choices.
 */
public enum ConfigChoice {
	/**
	 * Allow user to select from a list of currently configured projects.
	 */
	PROJECTS,
	
	/**
	 * Choices are specified by the plugin by setting
	 * PluginConfigDto.ATTR_AVAILABLE_CHOICES to a collection
	 * of choices in the property map.
	 */
	INLINE;
}
