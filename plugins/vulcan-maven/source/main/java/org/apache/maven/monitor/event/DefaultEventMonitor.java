package org.apache.maven.monitor.event;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import net.sourceforge.vulcan.ant.buildlistener.Constants;
import net.sourceforge.vulcan.ant.io.ObjectSerializer;
import net.sourceforge.vulcan.ant.io.Serializer;

import org.codehaus.plexus.logging.Logger;

/**
 * Because Maven2 (as of 2.0.4) does not provide a means to attach a custom 
 * EventMonitor through the Command Line Interface, this class replaces
 * the real default.
 * 
 * By placing the vulcan-maven jar before the core maven jars, this class
 * will be found and used instead of the real default.
 * 
 * @see http://svn.apache.org/repos/asf/maven/components/trunk/maven-core/src/main/java/org/apache/maven/monitor/event/DefaultEventMonitor.java
 */
public class DefaultEventMonitor extends AbstractSelectiveEventMonitor {
	private static final String[] MOJO_EVENTS = new String[] {MavenEvents.MOJO_EXECUTION};
	
	private final Logger logger;

	private final InetAddress remoteAddress;
	private final int remotePort;
	private final DatagramSocket socket;

	private final Serializer serializer;
	
	public DefaultEventMonitor(Logger logger) {
		this(
				logger,
				toInetAddress(System.getProperty(Constants.HOST_PROPERTY, Constants.DEFAULT_HOST)),
				Integer.parseInt(System.getProperty(Constants.PORT_PROPERTY, Constants.DEFAULT_PORT)));
	}

	public DefaultEventMonitor(Logger logger, InetAddress remoteAddress,
			int remotePort) {
		super(MOJO_EVENTS, MOJO_EVENTS, MOJO_EVENTS);
		
		this.logger = logger;
		
		this.serializer = new ObjectSerializer();
		
		this.remotePort = remotePort;
		this.remoteAddress = remoteAddress;
		
		try {
			this.socket = new DatagramSocket();
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
	}
	protected void doStartEvent(String eventName, String target, long timestamp) {
		logger.info( "[" + target + "]" );
		
		transmitEvent(new AntEventSummary(
				Constants.TARGET_STARTED,
				"unspecified project",
				target,
				null,
				null));
	}
	
	protected void doEndEvent(String eventName, String target, long timestamp) {
		transmitEvent(new AntEventSummary(
				Constants.TARGET_FINISHED,
				"unspecified project",
				target,
				null,
				null));
	}

	protected void doErrorEvent(String eventName, String target, long timestamp, Throwable cause) {
		transmitEvent(new AntEventSummary(
				Constants.MESSAGE_LOGGED,
				"unspecified project",
				target,
				null,
				cause.getMessage(),
				0));
	}
	
	private void transmitEvent(final AntEventSummary buildEventSummary) {
		transmit(serializer.serialize(buildEventSummary));
	}

	private void transmit(byte[] serializedData) {
		final DatagramPacket packet = new DatagramPacket(serializedData, serializedData.length);
		packet.setAddress(remoteAddress);
		packet.setPort(remotePort);
		
		try {
			socket.send(packet);
		} catch (Exception e) {
		}
	}
	
	public static InetAddress toInetAddress(String address) {
		try {
			return InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
}
