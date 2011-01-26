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
package net.sourceforge.vulcan.ant.buildlistener;

import junit.framework.TestCase;

import net.sourceforge.vulcan.ant.buildlistener.UdpBuildEventPublisher;
import net.sourceforge.vulcan.ant.receiver.EventListener;
import net.sourceforge.vulcan.ant.receiver.UdpEventSource;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;


public class UdpBuildEventPublisherTest extends TestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		System.getProperties().remove(Constants.PORT_PROPERTY);
		
		// clear any interrupts that may have happened in previous tests
		Thread.interrupted();
	}
	
	public void testDefaultCtrUsesDefaults() throws Exception {
		final UdpBuildEventPublisher l = new UdpBuildEventPublisher();
		
		assertEquals(Constants.DEFAULT_HOST, l.remoteAddress.getHostName());
		assertEquals(Integer.parseInt(Constants.DEFAULT_PORT), l.remotePort);
	}
	public void testDefaultCtrGetsParamsFromSystem() throws Exception {
		final String host = "127.0.0.2";
		System.setProperty(Constants.HOST_PROPERTY, host);
		System.setProperty(Constants.PORT_PROPERTY, "6543");
		
		final UdpBuildEventPublisher l = new UdpBuildEventPublisher();
		
		assertEquals(host, l.remoteAddress.getHostAddress());
		assertEquals(6543, l.remotePort);
	}
	AntEventSummary event; 
	public void testSendsEvents() throws Exception {
		final UdpEventSource source = new UdpEventSource();
		final EventListener receiveListener = new EventListener() {
			public void eventReceived(AntEventSummary event) {
				UdpBuildEventPublisherTest.this.event = event;
				synchronized(this) {
					notifyAll();
				}
			}
		};
		source.addEventListener(receiveListener);
		
		final Thread thread = new Thread(source, "Event Receiver");
		thread.start();
		
		synchronized(source) {
			if (!source.isActive()) {
				source.wait(250);
			}
		}
		
		final String host = "localhost";
		final String port = Integer.toString(source.getPort());

		System.setProperty(Constants.HOST_PROPERTY, host);
		System.setProperty(Constants.PORT_PROPERTY, port);
		
		final UdpBuildEventPublisher l = new UdpBuildEventPublisher();
		
		final Project project = new Project();
		project.setName("mockProject");
		
		final Target target = new Target();
		target.setProject(project);
		
		final BuildEvent buildEvent = new BuildEvent(target);
		buildEvent.setMessage("foo bar", Project.MSG_DEBUG);
		
		l.buildStarted(buildEvent);
		
		synchronized(receiveListener) {
			if (event == null) {
				receiveListener.wait(1000);
			}
		}
		
		assertNotNull("packet not received", event);
		assertEquals("foo bar", event.getMessage());
	}
}
