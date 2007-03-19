package org.apache.maven.monitor.event;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import net.sourceforge.vulcan.ant.buildlistener.Constants;
import net.sourceforge.vulcan.ant.io.ObjectSerializer;
import net.sourceforge.vulcan.ant.io.Serializer;

import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
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
		super(MOJO_EVENTS, MOJO_EVENTS, new String[] {MavenEvents.PROJECT_EXECUTION});
		
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
	// Ignore @Override warning because this code must be Java 1.3 compatible.
	protected void doStartEvent(String eventName, String target, long timestamp) {
		logger.info( "[" + target + "]" );
		
		transmitEvent(new AntEventSummary(
				Constants.TARGET_STARTED,
				"unspecified project",
				target,
				null,
				null));
	}
	
	// Ignore @Override warning because this code must be Java 1.3 compatible.
	protected void doEndEvent(String eventName, String target, long timestamp) {
		transmitEvent(new AntEventSummary(
				Constants.TARGET_FINISHED,
				"unspecified project",
				target,
				null,
				null));
	}
	
	// Ignore @Override warning because this code must be Java 1.3 compatible.
	protected void doErrorEvent(String eventName, String target, long timestamp, Throwable cause) {
		final List messages = toMessages(target, cause);
		
		for (Iterator itr = messages.iterator(); itr.hasNext();) {
			final AntEventSummary message = (AntEventSummary) itr.next();
			transmitEvent(message);
		}
	}
	
	private List toMessages(String target, Throwable cause) {
		if (cause.getCause() != null) {
			return toMessages(target, cause.getCause());
		}
		
		if (cause instanceof MultipleArtifactsNotFoundException) {
			final MultipleArtifactsNotFoundException manfe = (MultipleArtifactsNotFoundException) cause;
			return parseMissingDependenciesFromFormattedMessage(target, manfe.getMessage());
		}
		
		try {
			final String longMessage = (String) cause.getClass().getMethod("getLongMessage", null).invoke(cause, null);
			return parseCompileFailuresFromFormattedMessage(target, longMessage);
		} catch (Exception e) {
		}
		
		return Collections.singletonList(new AntEventSummary(
				Constants.MESSAGE_LOGGED,
				null,
				target,
				null,
				cause.getClass().getName() + ": " + cause.getMessage(),
				0));
	}

	private List parseCompileFailuresFromFormattedMessage(String target, String message) {
		final List errors = new ArrayList();
		
		final Pattern pattern = Pattern.compile("^(.+):\\[(\\d+),\\d+\\] ", Pattern.MULTILINE);
		
		final Matcher matcher = pattern.matcher(message);
		
		boolean found = matcher.find();
	
		while (found) {
			final String file = toRelative(matcher.group(1));
			final Integer line = Integer.valueOf(matcher.group(2));
			//final String msg = matcher.group(3);
			
			int start = matcher.end();
			
			found = matcher.find();
			int end = message.length() - 1;
			if (found) {
				end = matcher.start();
			}
			
			final String msg = message.substring(start, end).replaceAll("\r", "").trim();
			
			errors.add(new AntEventSummary(Constants.MESSAGE_LOGGED, "project", target, "task",
					msg, 0, file, line, null));
		}
		
		return errors;
	}

	/**
	 * Because MultipleArtifactsNotFoundException does not provde the actual list
	 * of missing artifacts and only provides a formatted message, here we parse
	 * the artifacts out of that formatted message.
	 */
	private List parseMissingDependenciesFromFormattedMessage(String target, String message) {
		final Pattern pattern = Pattern.compile("^\\d+\\) (.*)", Pattern.MULTILINE);
		
		final Matcher matcher = pattern.matcher(message);
		final List artifacts = new ArrayList();
		
		while (matcher.find()) {
			final String missingArtifact = matcher.group(1);
			
			artifacts.add(new AntEventSummary(
					Constants.MESSAGE_LOGGED,
					null,
					target,
					null,
					"Missing artifact " + missingArtifact,
					0, "pom.xml", null, null));
		}
		
		return artifacts;
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

	private String toRelative(String fileName) {
		final String basedir = System.getProperty("user.dir");
		if (fileName.startsWith(basedir)) {
			return fileName.substring(basedir.length()+1);
		}
		return fileName;
	}
	
	public static InetAddress toInetAddress(String address) {
		try {
			return InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
}
