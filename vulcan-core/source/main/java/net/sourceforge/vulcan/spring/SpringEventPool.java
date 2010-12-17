/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2010 Chris Eldredge
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
package net.sourceforge.vulcan.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventPool;
import net.sourceforge.vulcan.event.EventType;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource(objectName="vulcan:name=eventPool")
public final class SpringEventPool implements EventPool, ApplicationListener {
	List<Event> events = new ArrayList<Event>();
	int maxSize = 20;
	
	@ManagedOperation
	@ManagedOperationParameters({@ManagedOperationParameter(name="type", description="")})
	public List<Event> getEvents(String types) {
		final String[] typeStrings = types.split(",");
		final EventType[] eventTypes = new EventType[typeStrings.length];
		
		for (int i = 0; i < eventTypes.length; i++) {
			eventTypes[i] = EventType.valueOf(typeStrings[i]);
		}
		
		return getEvents(eventTypes);
	}
	
	public List<Event> getEvents(EventType type) {
		final List<Event> list = getEvents(new EventType[] {type});
		
		if (type.equals(EventType.BUILD)) {
			Collections.reverse(list);
		}
		
		return list;
	}
	
	public synchronized List<Event> getEvents(EventType... types) {
		List<Event> list = new ArrayList<Event>();
		
		for (Event e : events) {
			for (EventType t : types) {
				if (t.matches(e)) {
					list.add(e);
				}
			}
		}
		
		return list;
	}
	
	public synchronized void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof EventBridge) {
			if (events.size() == maxSize) {
				events.remove(0);
			}
			events.add(0, ((EventBridge)event).getEvent());
		}
	}
	
	@ManagedAttribute
	public int getMaxSize() {
		return maxSize;
	}
	
	@ManagedAttribute(persistPolicy="onUpdate")
	public synchronized void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
		
		final int size = events.size();
		if (size > maxSize) {
			events = new ArrayList<Event>(events.subList(0, maxSize));
		}
	}
	
	@ManagedOperation
	public synchronized void clear() {
		events.clear();
	}
}
