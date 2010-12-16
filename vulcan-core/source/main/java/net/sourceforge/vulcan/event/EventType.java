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


public enum EventType {
	ALL(Event.class),
	AUDIT(AuditEvent.class),
	BUILD(BuildCompletedEvent.class),
	MESSAGE(MessageEvent.class),
	ERROR(ErrorEvent.class),
	WARNING(WarningEvent.class),
	INFO(InfoEvent.class);
	
	private final Class<? extends Event> eventClass;

	private EventType(Class<? extends Event> eventClass) {
		this.eventClass = eventClass;
	}

	public Class<? extends Event> getEventClass() {
		return eventClass;
	}

	public boolean matches(Event e) {
		if (getEventClass().isAssignableFrom(e.getClass())) {
			return true;
		}
		return false;
	}
}
