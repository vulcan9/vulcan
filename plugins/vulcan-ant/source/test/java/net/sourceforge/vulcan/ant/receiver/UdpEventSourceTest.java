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
package net.sourceforge.vulcan.ant.receiver;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import junit.framework.TestCase;
import net.sourceforge.vulcan.ant.AntBuildEvent;
import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class UdpEventSourceTest extends TestCase {

	UdpEventSource source = new UdpEventSource();

	AntEventSummary event;
	
	final EventListener listener = new EventListener() {
		public void eventReceived(AntEventSummary event) {
			UdpEventSourceTest.this.event = event; 
			synchronized(this) {
				notifyAll();
			}
		}
	};
	
	public void testFiresListeners() {
		source.addEventListener(listener);
		
		final AntEventSummary sent = new AntEventSummary(AntBuildEvent.BUILD_STARTED.name(), "proj", null, null, null);
		source.fireEvent(sent);
		
		assertSame(sent, event);
	}
	
	public void testBindChoosesPort() {
		assertEquals(0, source.getPort());
		final DatagramSocket socket = source.openSocket();
		assertTrue(source.getPort() > 0);
		socket.close();
	}
	
	public void testRunOpensSocket() throws InterruptedException {
		final Thread t = new Thread(source);
		t.start();
		
		synchronized(source) {
			if (source.getPort() == 0) {
				source.wait(250);
			}
		}
		
		assertTrue(source.getPort() > 0);
		
		source.shutDown();
		
		t.join();
	}

	public void testReadsPackets() throws Exception {
		source.addEventListener(listener);
		final Thread t = new Thread(source, "Event Receiver");
		t.start();
		
		synchronized(source) {
			if (source.getPort() == 0) {
				source.wait(250);
			}
		}
		
		assertTrue(source.getPort() > 0);
		
		final AntEventSummary sent = new AntEventSummary(AntBuildEvent.BUILD_FINISHED.name(), null, null, null, null);
		
		sendData(sent, source.getPort());
		
		synchronized(listener) {
			if (event == null) {
				listener.wait(250);
			}
		}
		
		assertNotNull(event);
		assertNotSame(sent, event);
		
		source.shutDown();
		
		t.join();
	}

	private void sendData(Serializable sent, int port) throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(sent);
		
		sendData(os.toByteArray(), port);
	}
	private void sendData(byte[] data, int port) throws Exception {
		final SocketAddress addr = new InetSocketAddress(InetAddress.getByName("localhost"), 0);
		final DatagramSocket socket = new DatagramSocket(addr);

		socket.connect(InetAddress.getByName("localhost"), port);
		final DatagramPacket p = new DatagramPacket(data, data.length);
		socket.send(p);
		
		socket.close();
	}
}
