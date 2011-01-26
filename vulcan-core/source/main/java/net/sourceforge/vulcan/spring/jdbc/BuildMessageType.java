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
package net.sourceforge.vulcan.spring.jdbc;

enum BuildMessageType {
	Error,
	Warning;

	public String getRdbmsValue() {
		return name().substring(0, 1);
	}

	public static BuildMessageType fromRdbmsValue(String v) {
		if ("W".equals(v)) {
			return Warning;
		}
		
		if ("E".equals(v)) {
			return Error;
		}
		
		throw new IllegalArgumentException(v + " is not a valid BuildMessageType");
	}
}
