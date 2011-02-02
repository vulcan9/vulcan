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
package net.sourceforge.vulcan.jabber;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * See http://issues.igniterealtime.org/browse/SMACK-141?page=com.atlassian.streams.streams-jira-plugin%3Aactivity-stream-issue-tab#issue-tabs
 * 
 * As a side effect of the above issue, Smack's keep alive thread will stay alive
 * for up to 30 seconds by default after the client has been disconnected.
 * 
 * To work around this issue, we interrupt up any thread that looks like it might be a
 * Smack Keep Alive thread to get it to shutdown.  This helps Vulcan shut down cleanly.
 */
public class SmackKeepAliveThreadInterrupter {
	private static final Log LOG = LogFactory.getLog(SmackKeepAliveThreadInterrupter.class);
	
	public void interrupt() {
		final ThreadGroup group = Thread.currentThread().getThreadGroup();
		
		final Thread[] threads = new Thread[group.activeCount()];
		
		group.enumerate(threads);
		
		for (Thread thread : threads) {	
			if (!thread.getName().startsWith("Smack Keep Alive")) {
				continue;
			}

			if (!thread.getContextClassLoader().equals(getClass().getClassLoader())) {
				// only wake up threads from our own class loader
				LOG.info("Not waking up " + thread.getName() + " because it uses a different class loader.");
				continue;
			}
			
			LOG.info("Interrupting " + thread.getName());
			
			thread.interrupt();
			
			try {
				thread.join(1000);
			} catch (InterruptedException ignore) {
			}
			
			if (thread.isAlive()) {
				LOG.error("Smack Keep Alive thread still alive after interruption.");
			}
		}
	}
}
