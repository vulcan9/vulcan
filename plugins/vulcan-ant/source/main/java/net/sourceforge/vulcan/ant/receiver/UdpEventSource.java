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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import net.sourceforge.vulcan.ant.AntBuildTool;
import net.sourceforge.vulcan.ant.JavaCommandBuilder;
import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import net.sourceforge.vulcan.ant.buildlistener.Constants;
import net.sourceforge.vulcan.ant.io.ObjectSerializer;
import net.sourceforge.vulcan.ant.io.Serializer;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class UdpEventSource extends SerializedEventSource {
	private final static Log log = LogFactory.getLog(UdpEventSource.class);
	private final static String ANT_LISTENER_CLASS_NAME = "net.sourceforge.vulcan.ant.buildlistener.UdpBuildEventPublisher";
	private final InetAddress listenAddress;

	private int timeout;
	private int port;
	
	private boolean active;
	
	public UdpEventSource() {
		this(getLoopbackAddress(), 0, 100, new ObjectSerializer());
	}
	public UdpEventSource(Serializer serializer) {
		this(getLoopbackAddress(), 0, 100, serializer);
	}
	public UdpEventSource(final InetAddress listenAddress, final int port, final int timeout, Serializer serializer) {
		super(serializer);
		this.port = port;
		this.timeout = timeout;
		this.listenAddress = listenAddress;
	}
	public void addSystemProperties(JavaCommandBuilder jcb) {
		jcb.addSystemProperty(Constants.HOST_PROPERTY, listenAddress.getHostName());
		jcb.addSystemProperty(Constants.PORT_PROPERTY, Integer.toString(port));
	}		
	public void addAntCommandLineArgs(JavaCommandBuilder jcb) throws ConfigException {
		jcb.addArgument("-lib");
		try {
			jcb.addArgument(AntBuildTool.getLocalClassPathEntry(UdpEventSource.class, null).getCanonicalPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		jcb.addArgument("-listener");
		jcb.addArgument(getListenerClassName());
	}
	public String getListenerClassName() {
		return ANT_LISTENER_CLASS_NAME;
	}
	public void run() {
		final DatagramSocket socket = openSocket();
		active = true;
		
		synchronized(this) {
			notifyAll();
		}
		
		final byte[] buffer = new byte[8192];
		
		try {
			final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			while (active) {
				try {
					socket.receive(packet);
					
					final AntEventSummary summary = deserialize(packet.getData());
					
					fireEvent(summary);
				} catch (SocketTimeoutException e) {
					continue;
				}
				catch (Exception e) {
					handleException(e);
				}
			}
		} finally {
			socket.close();
		}
	}
	
	public String getHostname() {
		return listenAddress.getHostAddress();
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	public void shutDown() {
		active = false;
	}
	public boolean isActive() {
		return active;
	}
	protected DatagramSocket openSocket() {
		try {
			final SocketAddress addr = new InetSocketAddress(listenAddress, port);
			final DatagramSocket socket = new DatagramSocket(addr);
			socket.setSoTimeout(timeout);
			this.port = socket.getLocalPort();
			return socket;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	private void handleException(Throwable t) {
		String message = t.getMessage();
		if (StringUtils.isBlank(message)) {
			message = t.getClass().getName();
		}
		log.error(message, t);
	}
	private static InetAddress getLoopbackAddress() {
		try {
			return InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
}
