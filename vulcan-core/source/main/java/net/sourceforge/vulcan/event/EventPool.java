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
package net.sourceforge.vulcan.event;

import java.util.List;
import java.util.Map;

import net.sourceforge.vulcan.metadata.SvnRevision;


/**
 * Implementation Note:  This interface extends Map in order to allow JSTL to access
 * events of various types using expressions such as ${eventPool['ERROR']}.  Only the
 * "get" and "containsKey" methods need to be implemented.  Other methods derrived
 * from Map may throw UnsupportedOperationException.
 */
@SvnRevision(id="$Id$", url="$HeadURL$")
public interface EventPool extends Map<EventType, List<Event>> {
	
	List<Event> getEvents(EventType[] types);
	
	/**
	 * Convenience method for getEvents(EventType[])
	 */
	List<Event> getEvents(EventType type);
	
	/**
	 * Convenience method for JSTL
	 */
	List<Event> get(Object type);
}
