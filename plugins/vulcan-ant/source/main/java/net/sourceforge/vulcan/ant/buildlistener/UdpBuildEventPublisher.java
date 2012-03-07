/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public final class UdpBuildEventPublisher extends SerializingBuildEventPublisher {
	final InetAddress remoteAddress;
	final int remotePort;
	final DatagramSocket socket;

	public UdpBuildEventPublisher() throws NumberFormatException, SocketException, UnknownHostException {
		this(
			InetAddress.getByName(System.getProperty(Constants.HOST_PROPERTY, DEFAULT_HOST)),
			Integer.parseInt(System.getProperty(Constants.PORT_PROPERTY, DEFAULT_PORT)));
	}
	
	public UdpBuildEventPublisher(final InetAddress remoteAddress,
			final int remotePort) throws SocketException {
		super();
		this.remotePort = remotePort;
		this.remoteAddress = remoteAddress;
		
		this.socket = new DatagramSocket();
	}
	
	// Ignore @Override warning because this code must be Java 1.3 compatible.
	protected void transmit(byte[] serializedData) {
		final DatagramPacket packet = new DatagramPacket(serializedData, serializedData.length);
		packet.setAddress(remoteAddress);
		packet.setPort(remotePort);
		
		try {
			socket.send(packet);
		} catch (Exception e) {
		}
	}
}
