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
package net.sourceforge.vulcan.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventPool;
import net.sourceforge.vulcan.event.EventType;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;


@SvnRevision(id="$Id$", url="$HeadURL$")
@ManagedResource(objectName="vulcan:name=eventPool")
public final class SpringEventPool implements EventPool, ApplicationListener {
	List<Event> events = new ArrayList<Event>();
	int maxSize = 20;
	
	public List<Event> get(Object key) {
		final EventType type = EventType.valueOf(key.toString());
		return getEvents(type);
	}
	
	@ManagedOperation
	@ManagedOperationParameters({@ManagedOperationParameter(name="type", description="")})
	public List<Event> getEvents(String type) {
		return get(type);
	}
	
	public List<Event> getEvents(EventType type) {
		final List<Event> list = getEvents(new EventType[] {type});
		
		if (type.equals(EventType.BUILD)) {
			Collections.reverse(list);
		}
		
		return list;
	}
	public synchronized List<Event> getEvents(EventType[] types) {
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
			events.add(((EventBridge)event).getEvent());
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
			events = events.subList(size-maxSize, size);
		}
	}
	public Set<EventType> keySet() {
		return new HashSet<EventType>(Arrays.asList(EventType.values()));
	}
	@ManagedOperation
	public synchronized void clear() {
		events.clear();
	}
	public boolean containsKey(Object key) {
		try {
			EventType.valueOf(key.toString());
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}
	public Set<Entry<EventType, List<Event>>> entrySet() {
		throw new UnsupportedOperationException();
	}
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}
	public List<Event> put(EventType key, List<Event> value) {
		throw new UnsupportedOperationException();
	}
	public void putAll(Map<? extends EventType, ? extends List<Event>> t) {
		throw new UnsupportedOperationException();
	}
	public int size() {
		throw new UnsupportedOperationException();
	}
	public List<Event> remove(Object key) {
		throw new UnsupportedOperationException();
	}
	public Collection<List<Event>> values() {
		throw new UnsupportedOperationException();
	}
}
