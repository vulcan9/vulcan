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
package net.sourceforge.vulcan.scheduler.thread;

import java.util.Date;

import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.scheduler.thread.AbstractScheduler;

import junit.framework.TestCase;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class AbstractSchedulerTest extends TestCase {
	AbstractScheduler rs = new AbstractScheduler() {
		@Override
		protected void execute() throws Exception {
			count++;
			synchronized(this) {
				notifyAll();
			}
		}
	};
	SchedulerConfigDto config;
	
	int count = 0;
	Event event;
	
	@Override
	public void setUp() {
		config = new SchedulerConfigDto();
		config.setName("mock");
		config.setInterval(10);
		
		handler = new EventHandler() {
					public void reportEvent(Event event) {
						AbstractSchedulerTest.this.event = event;
					};
				};
		rs.setEventHandler(handler);
		rs.init(config);
	}
	
	public void testStartStop() {
		assertFalse(rs.isRunning());
		
		rs.start();
		
		assertTrue(rs.isRunning());
		
		rs.stop();
		
		assertFalse(rs.isRunning());
	}
	
	public void testRunsInBackground() throws Exception {
		rs.start();
		
		synchronized(rs) {
			if (count == 0) {
				rs.wait(100);
			}
		}
		
		assertTrue(count > 0);
		int prev = count;
		
		synchronized(rs) {
			rs.wait(100);
		}
		assertTrue(prev < count);
		
		rs.stop();
		
		prev = count;
		
		synchronized(rs) {
			rs.wait(100);
		}
		assertTrue("stop did not stop, or did not join", prev == count);
	}
	public void testChangeConfigOkWhenStopped() throws Exception {
		config = (SchedulerConfigDto) config.copy();
		config.setName("other");
		rs.configurationChanged(config);
	}
	public void testChangeConfigUpdatesName() throws Exception {
		rs.start();
		assertEquals("Scheduler[mock]", rs.getThread().getName());
		
		config = (SchedulerConfigDto) config.copy();
		config.setName("other");
		assertEquals("Scheduler[mock]", rs.getThread().getName());
		rs.configurationChanged(config);
		assertEquals("Scheduler[other]", rs.getThread().getName());
	}
	public void testSleepInterval() throws Exception {
		config.setInterval(10000);
		rs.init(config);
		
		assertEquals(0, count);
		assertEquals(null, rs.getNextExecutionDate());
		rs.start();
		
		assertEquals("Scheduler[" + config.getName() + "]", rs.getThread().getName());
		
		synchronized(rs) {
			if (count == 0) {
				rs.wait(100);
			}
		}

		assertTrue(rs.getNextExecutionDate().getTime() - new Date().getTime() < 10000);
		assertTrue(rs.getNextExecutionDate().getTime() - new Date().getTime() > 9500);
		
		config.setInterval(20000);
		rs.configurationChanged(config);
		
		synchronized(rs) {
			if (count == 0) {
				rs.wait(100);
			}
		}

		assertTrue(rs.getNextExecutionDate().getTime() - new Date().getTime() < 20000);
		assertTrue(rs.getNextExecutionDate().getTime() - new Date().getTime() > 19500);
		
		config.setInterval(0);
		rs.configurationChanged(config);
		
		synchronized(rs) {
			if (count == 0) {
				rs.wait(100);
			}
		}
		
		assertEquals(null, rs.getNextExecutionDate());
		
		rs.stop();
		
		assertEquals(0, count);
		assertNull(rs.getNextExecutionDate());
	}
	boolean running = false;
	public void testStopDoesNotSuppressInterruptedException() throws Exception {
		rs = new AbstractScheduler() {
			@Override
			public void execute() throws Exception {
				running = true;
				synchronized(AbstractSchedulerTest.this) {
					AbstractSchedulerTest.this.notifyAll();
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				running = false;
			}
		};
		rs.init(config);
		rs.start();

		synchronized(this) {
			if (!running) {
				this.wait(1000);
			}
		}
		assertTrue(running);
		Thread.currentThread().interrupt();
		
		rs.stop();

		try {
			Thread.sleep(1);
			fail("expected exception");
		} catch (InterruptedException e) {
		}
		
		assertFalse(running);
	}
	boolean caughtInterrupt = false;
	private EventHandler handler;
	public void testStopInterruptsThread() throws Exception {
		rs = new AbstractScheduler() {
			@Override
			public void execute() throws Exception {
				running = true;
				synchronized(AbstractSchedulerTest.this) {
					AbstractSchedulerTest.this.notifyAll();
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					caughtInterrupt = true;
				}
				running = false;
			}
		};
		rs.init(config);
		rs.start();

		synchronized(this) {
			if (!running) {
				this.wait(1000);
			}
		}
		assertTrue(running);
		
		rs.stop();

		assertFalse(running);
		
		assertTrue(caughtInterrupt);
	}
	public void testInterruptNotReportedAsError() throws Exception {
		rs = new AbstractScheduler() {
			@Override
			public void execute() throws Exception {
				running = true;
				synchronized(AbstractSchedulerTest.this) {
					AbstractSchedulerTest.this.notifyAll();
				}
				Thread.sleep(100);
				running = false;
			}
		};
		rs.setEventHandler(handler);
		rs.init(config);
		
		rs.start();

		synchronized(this) {
			if (!running) {
				this.wait(1000);
			}
		}
		assertTrue(running);

		assertNull(event);
		
		rs.stop();

		assertTrue(running);

		assertNull(event);
	}
	public void testExceptionReportedAsError() throws Exception {
		final Exception e = new Exception();
		
		rs = new AbstractScheduler() {
			@Override
			public void execute() throws Exception {
				if (running) {
					running = false;
				} else {
					running = true;
				}
				synchronized(AbstractSchedulerTest.this) {
					AbstractSchedulerTest.this.notifyAll();
				}
				
				throw e;
			}
		};
		rs.setEventHandler(handler);
		config.setInterval(1);
		rs.init(config);
		
		assertNull(event);		
		rs.start();

		synchronized(this) {
			if (!running) {
				this.wait(1000);
			}
		}
		assertTrue(running);

		synchronized(this) {
			if (running) {
				this.wait(1000);
			}
		}
		assertFalse(running);

		assertNotNull(event);
		assertTrue(event instanceof ErrorEvent);
		assertSame(e, ((ErrorEvent)event).getError());
	}
}
