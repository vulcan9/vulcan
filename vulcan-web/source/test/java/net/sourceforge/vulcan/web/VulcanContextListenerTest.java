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
package net.sourceforge.vulcan.web;

import javax.servlet.ServletContextEvent;

import net.sourceforge.vulcan.Keys;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.springframework.web.context.WebApplicationContext;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class VulcanContextListenerTest extends ServletTestCase {
	VulcanContextListener l = new VulcanContextListener();
	
	ServletContextEvent event = new ServletContextEvent(servletContext);
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
	}
	@TrainingMethod("trainNoCalls")
	public void testInitNoWac() {
		servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

		try {
			l.contextInitialized(event);
			fail("expected exception");
		} catch (IllegalStateException e) {
		}
	}
	public void testInit() throws Exception {
		mgr.start();
		
		replay();
		
		l.contextInitialized(event);
		
		verify();
		
		assertNotNull("Did not construct state manager instance", servletContext.getAttribute(Keys.STATE_MANAGER));
		assertNotNull("Did not construct eventPool instance", servletContext.getAttribute(Keys.EVENT_POOL));
	}
	public void testInitManagerFailure() throws Exception {
		final RuntimeException e = new RuntimeException("failure");
		
		mgr.start();
		expectLastCall().andThrow(e);
		
		eventHandler.reportEvent((Event) notNull());
		
		replay();
		
		l.contextInitialized(event);
		
		verify();
		
		assertNotNull("Did not construct state manager instance", servletContext.getAttribute(Keys.STATE_MANAGER));
		assertNotNull("Did not construct eventPool instance", servletContext.getAttribute(Keys.EVENT_POOL));
	}

	public void testDestroyContextDoesNothingOnNullInstance() {
		replay();
		l.contextDestroyed(event);
		verify();
	}
	
	public void testDestroyContextCallsShutDown() throws Exception {
		servletContext.setAttribute(Keys.STATE_MANAGER, mgr);
		servletContext.setAttribute(Keys.EVENT_POOL, new Object());
		
		l.stateManager = mgr;
		
		mgr.shutdown();
		
		replay();
		
		l.contextDestroyed(event);
		
		verify();
		assertNull("Did not remove state manager instance", servletContext.getAttribute(Keys.STATE_MANAGER));
		assertNull("Did not remove eventPool instance", servletContext.getAttribute(Keys.EVENT_POOL));
	}
	
	public void testDestroyContextCallsShutDownLogs() throws Exception {
		final StoreException e = new StoreException(new RuntimeException("failed"));
		
		servletContext.setAttribute(Keys.STATE_MANAGER, mgr);
		servletContext.setAttribute(Keys.EVENT_POOL, new Object());
		
		l.stateManager = mgr;
		
		mgr.shutdown();
		expectLastCall().andThrow(e);
		
		replay();
		
		l.contextDestroyed(event);
		
		verify();
		assertNull("Did not remove state manager instance", servletContext.getAttribute(Keys.STATE_MANAGER));
		assertNull("Did not remove eventPool instance", servletContext.getAttribute(Keys.EVENT_POOL));
		
		assertSame(e, loggedThrowable);
	}
}
