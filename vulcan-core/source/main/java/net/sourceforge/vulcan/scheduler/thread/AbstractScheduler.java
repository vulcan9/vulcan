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
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.scheduler.Scheduler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class AbstractScheduler implements Scheduler {
	protected final Log log = LogFactory.getLog(getClass());
	
	protected EventHandler eventHandler;
	
	private boolean running;
	private boolean shutdown;
	private SchedulerConfigDto config;
	private Date nextExecutionDate;

	Thread thread;
	
	protected abstract void execute() throws Exception;
	
	public final void init(SchedulerConfigDto config) {
		this.config = config;
		init();
	}

	/**
	 * Subclasses can override this method to perform any initialization.
	 */
	protected void init() {
	}
	
	public final void configurationChanged(SchedulerConfigDto config) {
		this.config = config;
		if (thread != null) {
			thread.setName("Scheduler[" + config.getName() + "]");
			thread.interrupt();
		}
	}

	public final void start() {
		thread = new Thread("Scheduler[" + config.getName() + "]") {
			@Override
			public void run() {
				while (!shutdown) {
					final long interval = config.getInterval();
					if (interval == 0) {
						nextExecutionDate = null;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}
					} else {
						nextExecutionDate = new Date(new Date().getTime() + interval);
						try {
							synchronized(AbstractScheduler.this) {
								AbstractScheduler.this.wait(interval);
							}
							try {
								execute();
							} catch (InterruptedException e) {
								throw e;
							} catch (Exception e) {
								log.error("uncaught exception while executing scheduler '" + config.getName() + "'", e);
								eventHandler.reportEvent(
										new ErrorEvent(this, "Scheduler.exception",
												new Object[] {config.getName(), e.getMessage()}, e));
							}
						} catch (InterruptedException e) {
						}
					}
				}
				nextExecutionDate = null;
			}
		};
		shutdown = false;
		thread.start();
		running = true;
	}

	public final void stop() {
		shutdown();
		
		boolean interrupted = false;
		shutdown = true;
		
		while (thread != null && thread.isAlive()) {
			thread.interrupt();			
			try {
				thread.join();
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
		thread = null;
		running = false;
		if (interrupted) {
			Thread.currentThread().interrupt();
		}
	}
	
	protected void shutdown() {
	}
	
	public final String getName() {
		return config.getName();
	}
	
	public final Date getNextExecutionDate() {
		return nextExecutionDate;
	}
	
	public final boolean isRunning() {
		return running;
	}
	
	public EventHandler getEventHandler() {
		return eventHandler;
	}
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	
	protected final SchedulerConfigDto getConfig() {
		return config;
	}
	protected final Thread getThread() {
		return thread;
	}

	public synchronized void wakeUp() {
		notify();
	}
}
