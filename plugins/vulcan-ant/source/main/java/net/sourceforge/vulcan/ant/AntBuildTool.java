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
package net.sourceforge.vulcan.ant;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import net.sourceforge.vulcan.ant.receiver.AntEventSource;
import net.sourceforge.vulcan.ant.receiver.EventListener;
import net.sourceforge.vulcan.ant.receiver.UdpEventSource;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;

public class AntBuildTool implements BuildTool {
	private static final Log log = LogFactory.getLog(AntBuildTool.class);
	
	static final String JAVA_EXECUTABLE_PATH = "bin" + File.separator + "java";
	static final String EXECUTABLE_EXTENSION = ".exe";
	
	static final String ANT_CORE_PATH = "lib" + File.separator + "ant.jar";
	static final String LAUNCHER_PATH = "lib" + File.separator + "ant-launcher.jar";
	
	static final String ANT_LAUNCHER_CLASS_NAME = "org.apache.tools.ant.launch.Launcher";
	
	protected final AntConfig antConfig;
	protected final AntProjectConfig antProjectConfig;
	
	protected JavaHome javaEnvironment;
	
	protected File logFile;
	
	protected AntEventSource eventSource;
	
	private final Map<String, String> antProps = new HashMap<String, String>();
	
	private DetailCallbackEventListener listener;
	private String currentTarget;

	
	public AntBuildTool(AntProjectConfig projectConfig, AntConfig antConfig) {
		this(projectConfig, antConfig, new UdpEventSource());
	}
	public AntBuildTool(AntProjectConfig projectConfig, AntConfig antConfig, AntEventSource eventSource) {
		this(projectConfig, antConfig, null, eventSource);
	}
	public AntBuildTool(AntProjectConfig projectConfig, AntConfig antConfig, JavaHome javaEnvironment) {
		this(projectConfig, antConfig, javaEnvironment, new UdpEventSource());
	}

	public AntBuildTool(AntProjectConfig projectConfig, AntConfig antConfig, JavaHome javaEnvironment, AntEventSource eventSource) {
		this.antProjectConfig = projectConfig;
		this.antConfig = antConfig;
		this.javaEnvironment = javaEnvironment;
		this.eventSource = eventSource;
	}
	public void buildProject(ProjectConfigDto projectConfig, ProjectStatusDto status, File logFile, BuildDetailCallback detailCallback) throws BuildFailedException, ConfigException {
		final Thread listenerThread;

		this.logFile = logFile;
		
		this.listener = new DetailCallbackEventListener(detailCallback);
		
		if (eventSource != null) {
			this.eventSource.addEventListener(listener);
			this.eventSource.addEventListener(new EventListener() {
				private Stack<String> targets = new Stack<String>();
				public void eventReceived(AntEventSummary event) {
					final String targetName = event.getTargetName();
					if (AntBuildEvent.TARGET_STARTED.name().equals(event.getType())) {
						targets.push(targetName);
						currentTarget = event.getTargetName();
					} else if (AntBuildEvent.TARGET_FINISHED.name().equals(event.getType())) {
						if (targets.isEmpty()) {
							currentTarget = null;
						} else {
							currentTarget = targets.pop();
						}
					}
				}
			});
		
			listenerThread = new Thread("Ant Build Listener") {
				@Override
				public void run() {
					eventSource.run();
				}
			};
			
			listenerThread.start();
			
			synchronized(eventSource) {
				if (!eventSource.isActive()) {
					try {
						eventSource.wait(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}
		} else {
			listenerThread = null;
		}
		
		recordMetrics(detailCallback);
		
		try {
			final String[] args = constructExecutableArgs(projectConfig, status, logFile);
			
			execute(args, projectConfig.getWorkDir());
		} finally {
			if (listenerThread != null) {
				eventSource.shutDown();
				try {
					listenerThread.join();
				} catch (InterruptedException e) {
					log.warn("Interrupt while waiting for listener thread to terminate.");
				}
			}
		}
	}

	protected void recordMetrics(BuildDetailCallback detailCallback) {
		if (!antConfig.isRecordMetrics())
		{
			return;
		}
		
		detailCallback.addMetric(new MetricDto("vulcan.metrics.build.script", antProjectConfig.getBuildScript(), MetricType.STRING));
		detailCallback.addMetric(new MetricDto("vulcan.metrics.build.targets", antProjectConfig.getTargets(), MetricType.STRING));
	}
	
	public final AntEventSource getEventSource() {
		return eventSource;
	}

	public final void setEventSource(AntEventSource eventSource) {
		if (this.eventSource != null) {
			this.eventSource.removeEventListener(listener);
		}
		
		this.eventSource = eventSource;
		
		if (this.eventSource != null) {
			this.eventSource.addEventListener(listener);
		}
	}
	
	protected String[] constructExecutableArgs(ProjectConfigDto projectConfig, ProjectStatusDto status, File logFile) throws ConfigException {
		final JavaCommandBuilder commandBuilder = 
			createJavaCommand(projectConfig, status, logFile);
		
		final String[] args = commandBuilder.construct();
		return args;
	}

	protected JavaCommandBuilder createJavaCommand(ProjectConfigDto projectConfig, ProjectStatusDto status, File logFile) throws ConfigException {
		final File buildScript = checkBuildScriptExists(projectConfig);
		final JavaCommandBuilder jcb = new JavaCommandBuilder();
		
		setJavaExecutablePath(jcb);
		
		final String antHome = antConfig.getAntHome();
		
		try {
			jcb.addClassPathEntry(getAntHomeResource(antHome, LAUNCHER_PATH).getCanonicalPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		jcb.addSystemProperty("ant.home", antHome);
		jcb.addSystemProperty("ant.library.dir", antHome + File.separator + "lib");
		
		jcb.setMainClassName(ANT_LAUNCHER_CLASS_NAME);
		
		addProps(jcb, antConfig.getAntProperties(), true);
		addProps(jcb, antProjectConfig.getAntProperties(), true);
		
		addVersionProperties(status);
		
		antPropsToArgs(jcb);
		
		if (eventSource != null) {
			eventSource.addSystemProperties(jcb);
			eventSource.addAntCommandLineArgs(jcb);
		}
		
		if (this.antProjectConfig.isDebug()) {
			jcb.addArgument("-debug");
		}
		
		if (this.antProjectConfig.isVerbose()) {
			jcb.addArgument("-verbose");
		}

		jcb.addArgument("-noinput");
		
		if (logFile != null) {
			jcb.addArgument("-logfile");
			try {
				jcb.addArgument(logFile.getCanonicalPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		jcb.addArgument("-buildfile");
		try {
			jcb.addArgument(buildScript.getCanonicalPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		final String allTargets = this.antProjectConfig.getTargets();
		if (!StringUtils.isBlank(allTargets)) {
			final String[] targets = allTargets.split(" ");
			for (String target : targets) {
				jcb.addArgument(target);
			}
		}
		return jcb;
	}

	protected void addVersionProperties(ProjectStatusDto status) {
		if (isNotBlank(antConfig.getBuildNumberPropertyName())) {
			antProps.put(antConfig.getBuildNumberPropertyName(), status.getBuildNumber().toString());
		}
		final RevisionTokenDto revision = status.getRevision();
		if (isNotBlank(antConfig.getRevisionPropertyName()) && revision != null) {
			antProps.put(antConfig.getRevisionPropertyName(), revision.getLabel());
		}
		if (isNotBlank(antConfig.getNumericRevisionPropertyName()) && revision != null) {
			antProps.put(antConfig.getNumericRevisionPropertyName(), revision.getRevision().toString());
		}
		final String tagName = status.getTagName();
		if (isNotBlank(antConfig.getTagNamePropertyName()) && isNotBlank(tagName)) {
			antProps.put(antConfig.getTagNamePropertyName(), tagName);
		}
		
		String requestedBy = status.getRequestedBy();
		if (requestedBy == null) {
			requestedBy = "";
		}
		
		if (!status.isScheduledBuild() && isNotBlank(antConfig.getBuildUserPropertyName())) {
			antProps.put(antConfig.getBuildUserPropertyName(), requestedBy);
		}
		
		if (status.isScheduledBuild() && isNotBlank(antConfig.getBuildSchedulerPropertyName())) {
			antProps.put(antConfig.getBuildSchedulerPropertyName(), requestedBy);
		}
	}

	protected String setJavaExecutablePath(final JavaCommandBuilder jcb) throws ConfigException {
		final String javaHomeLocation;
		
		if (javaEnvironment != null) {
			javaHomeLocation = javaEnvironment.getJavaHome();
			addJavaConfigParams(jcb, javaEnvironment);
		} else {
			javaHomeLocation = System.getProperty("java.home");
		}
		
		try {
			final File javaProgram = getVirtualMachineExecutable(javaHomeLocation);
			jcb.setJavaExecutablePath(javaProgram.getCanonicalPath());
			return javaHomeLocation;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected File checkBuildScriptExists(ProjectConfigDto projectConfig) throws ConfigException {
		final File buildScript = new File(projectConfig.getWorkDir(), this.antProjectConfig.getBuildScript());
		
		if (!buildScript.canRead()) {
			throw new ConfigException("ant.script.invalid", new Object[] {buildScript});
		}
		return buildScript;
	}

	public static File getVirtualMachineExecutable(String javaHome) throws ConfigException {
		
		File file = new File(
				javaHome,
				JAVA_EXECUTABLE_PATH);
		
		if (!file.exists()) {
			file = new File(
					javaHome,
					JAVA_EXECUTABLE_PATH + EXECUTABLE_EXTENSION);
			
			if (!file.exists()) {
				throw new ConfigException("java.home.invalid", new String[] {
						JAVA_EXECUTABLE_PATH,
						javaHome
				});
			}
		}
		return file;
	}

	public static File getAntHomeResource(String antHome, String resource) throws ConfigException {
		if (StringUtils.isBlank(antHome)) {
			throw new ConfigException("ant.home.required", null);
		}
		
		final File launcher = new File(antHome, resource);
		
		if (!launcher.canRead()) {
			throw new ConfigException("ant.home.missing.resource", new String[] {
				resource,
				antHome	
			});
		}
		return launcher;
	}
	
	public static File getLocalClassPathEntry(Class<?> cls, ClassLoader ldr) throws ConfigException {
		if (ldr == null) {
			ldr = cls.getClassLoader();
		}
		
		String name = cls.getName().replaceAll("\\.", "/") + ".class";
		
		final ClassPathResource resource = new ClassPathResource(name, ldr);
		
		final URL url;
		try {
			url = resource.getURL();
		} catch (IOException e) {
			throw new ConfigException("ant.classpath.resolution.failed",
					new String[] {cls.getName(), e.getMessage()});
		}
		
		if ("jar".equals(url.getProtocol())) {
			String path = url.getPath().substring(5);
			path = path.substring(0, path.indexOf('!'));
			
			return new File(path.replaceAll("%20", " "));
		}
		
		if (File.separatorChar == '\\') {
			name = name.replaceAll("/", "\\\\");
		}
		
		final String path;
		try {
			path = resource.getFile().getPath();
		} catch (IOException e) {
			throw new ConfigException("ant.classpath.resolution.failed",
					new String[] {cls.getName(), e.getMessage()});
		}
		return new File(path.substring(0, path.indexOf(name)));
	}

	protected final void execute(String[] args, String workDir) throws ConfigException, BuildFailedException {
		final Process process;
		
		log.debug("Executing command: " + StringUtils.join(args, ' '));
		
		try {
			process = Runtime.getRuntime().exec(args, null, new File(workDir));
			preparePipes(process);
		} catch (IOException e) {
			throw new ConfigException("ant.exec.failure", new String[] {e.getMessage()});
		}

		try {
			try {
				final int exitCode = process.waitFor();
				if (exitCode != 0) {
					final String message;
					final AntEventSummary latestEvent;
					
					if (listener != null) {
						latestEvent = listener.getLatestEvent();
					} else {
						latestEvent = null;
					}
					
					if (latestEvent != null) {
						message = latestEvent.getMessage();
					} else {
						message = "unknown";
					}
					
					throw new BuildFailedException(message, currentTarget, exitCode);
				}
			} catch (InterruptedException e) {
				process.destroy();
			}
		} finally {
			try {
				flushPipes();
			} catch (IOException e) {
				log.error("IOException closing Process streams", e);
			}
		}
	}
	/**
	 * Subclasses may override this method to perform custom handling of
	 * I/O.  The default behavior is to close all streams.
	 */
	protected void preparePipes(final Process process) throws IOException {
		process.getOutputStream().close();
		process.getInputStream().close();
		process.getErrorStream().close();
	}
	/**
	 * Subclasses should override this method when overriding <code>preparePipes</code>.
	 */
	protected void flushPipes() throws IOException {
	}
	protected void addJavaConfigParams(final JavaCommandBuilder jcb, JavaHome profile) throws ConfigException {
		final String maxMemory = profile.getMaxMemory();
		if (!StringUtils.isBlank(maxMemory)) {
			try {
				jcb.setMaxMemoryInMegabytes(Integer.valueOf(maxMemory));
			} catch (NumberFormatException e) {
				throw new ConfigException("errors.max.mem.integer", new String[] {maxMemory});
			}
		}
		
		addProps(jcb, profile.getSystemProperties(), false);
	}
	protected void addProps(JavaCommandBuilder jcb, String[] props, boolean isAntProperties) {
		if (props == null || props.length == 0) {
			return;
		}
		
		for (String kvp : props) {
			if (StringUtils.isBlank(kvp)) {
				continue;
			}
			
			if (kvp.startsWith("-D")) {
				kvp = kvp.substring(2);
			}
			
			final String[] kv = kvp.split("=");
			final String value;
			
			if (kv.length > 1) {
				value = kv[1];
			} else {
				value = "";
			}
			
			if (isAntProperties) {
				final String key = kv[0];
				if (antProps.containsKey(key) && log.isDebugEnabled()) {
					log.debug("Overriding previous ant property " + key + " with new value " + value);
				}
				antProps.put(key, value);
			} else {
				jcb.addSystemProperty(kv[0], value);
			}
		}
	}
	protected void antPropsToArgs(final JavaCommandBuilder jcb) {
		final List<String> keys = new ArrayList<String>(antProps.keySet());
		
		Collections.sort(keys);
		
		for (String key : keys) {
			final StringBuffer buf = new StringBuffer("-D");
			buf.append(key);
			buf.append("=");
			buf.append(antProps.get(key));
			
			jcb.addArgument(buf.toString());
		}
	}
	protected Map<String, String> getAntProps() {
		return antProps;
	}
}
