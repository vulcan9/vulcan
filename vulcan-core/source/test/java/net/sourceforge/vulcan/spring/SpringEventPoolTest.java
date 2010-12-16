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

import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventType;
import net.sourceforge.vulcan.spring.EventBridge;
import net.sourceforge.vulcan.spring.SpringEventPool;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;


public class SpringEventPoolTest extends TestCase {
	SpringEventPool pool = new SpringEventPool();
	
	ApplicationEvent event;
	
	@Override
	public void setUp() throws Exception {
		event = new EventBridge(new BuildCompletedEvent(this, null, null, null));
	}

	public void testHoldsEvents() {
		assertEquals(0, pool.events.size());
		
		pool.onApplicationEvent(event);
		
		assertEquals(1, pool.events.size());
	}

	public void testDoesNotHoldUnknownEvents() {
		assertEquals(0, pool.events.size());
		
		pool.onApplicationEvent(new ContextRefreshedEvent(new GenericApplicationContext()));
		
		assertEquals(0, pool.events.size());
	}
	
	public void testLimitsSize() {
		pool.setMaxSize(1);
		
		assertEquals(0, pool.events.size());
		
		pool.onApplicationEvent(event);
		
		assertEquals(1, pool.events.size());
		
		EventBridge e2 = new EventBridge(new BuildCompletedEvent(this, null, null, null));
		
		pool.onApplicationEvent(e2);
		
		assertEquals(1, pool.events.size());
		
		assertSame(e2.getEvent(), pool.events.get(0));
	}
	
	public void testSetMaxTruncates() {
		pool.onApplicationEvent(new EventBridge(new BuildCompletedEvent(this, null, null, null)));
		
		final EventBridge middle = new EventBridge(new BuildCompletedEvent(this, null, null, null));
		pool.onApplicationEvent(middle);
		
		final EventBridge mostRecent = new EventBridge(new BuildCompletedEvent(this, null, null, null));
		
		pool.onApplicationEvent(mostRecent);
		
		assertEquals(3, pool.getEvents(EventType.ALL).size());
		
		pool.setMaxSize(2);
		
		assertEquals(2, pool.getEvents(EventType.ALL).size());
		
		assertSame(mostRecent.getEvent(), pool.getEvents(EventType.ALL).get(0));
		assertSame(middle.getEvent(), pool.getEvents(EventType.ALL).get(1));

		pool.setMaxSize(1);
		
		assertEquals(1, pool.getEvents(EventType.ALL).size());
		
		assertSame(mostRecent.getEvent(), pool.getEvents(EventType.ALL).get(0));
	}

	public void testGetBuildEvents() {
		final BuildCompletedEvent buildCompletedEvent1 = new BuildCompletedEvent(this, null, null, null);
		final BuildCompletedEvent buildCompletedEvent2 = new BuildCompletedEvent(this, null, null, null);
		pool.onApplicationEvent(new EventBridge(buildCompletedEvent1));
		pool.onApplicationEvent(new EventBridge(new ErrorEvent(this, null, null)));
		pool.onApplicationEvent(new EventBridge(buildCompletedEvent2));
		pool.onApplicationEvent(new EventBridge(new Event() {
			public Object getSource() {
				return this;
			}
			public String getKey() {
				return null;
			}
			public Object[] getArgs() {
				return null;
			}
			public Date getDate() {
				return null;
			}
		}));
		
		List<Event> blist = pool.getEvents("BUILD");
		
		assertEquals(2, blist.size());
		
		// Build events should come back in reverse chronological order (newest first)
		assertSame(buildCompletedEvent2, blist.get(0));
		assertSame(buildCompletedEvent1, blist.get(1));
	}

	public void testGetInvalidType() {
		try {
			pool.getEvents("NONESUCH");
			fail("expected exception");
		} catch (IllegalArgumentException e) {
		}

		try {
			pool.getEvents((String)null);
			fail("expected exception");
		} catch (NullPointerException e) {
		}
	}
	public void testGetAllGetsAll() {
		pool.onApplicationEvent(new EventBridge(new BuildCompletedEvent(this, null, null, null)));
		pool.onApplicationEvent(new EventBridge(new ErrorEvent(this, null, null)));
		pool.onApplicationEvent(new EventBridge(new Event() {
			public Object getSource() {
				return this;
			}
			public String getKey() {
				return null;
			}
			public Object[] getArgs() {
				return null;
			}
			public Date getDate() {
				return null;
			}
		}));
		
		List<Event> list = pool.getEvents(EventType.ALL);
		
		assertEquals(3, list.size());
	}
	public void testSplitsTypesByComma() {
		pool.onApplicationEvent(new EventBridge(new BuildCompletedEvent(this, null, null, null)));
		pool.onApplicationEvent(new EventBridge(new ErrorEvent(this, null, null)));
		pool.onApplicationEvent(new EventBridge(new Event() {
			public Object getSource() {
				return this;
			}
			public String getKey() {
				return null;
			}
			public Object[] getArgs() {
				return null;
			}
			public Date getDate() {
				return null;
			}
		}));
		
		List<Event> list = pool.getEvents(EventType.BUILD.name() + "," + EventType.ERROR.name());
		
		assertEquals(2, list.size());
	}
}
