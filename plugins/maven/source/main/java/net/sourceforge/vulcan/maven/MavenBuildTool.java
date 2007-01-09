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
package net.sourceforge.vulcan.maven;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Collection;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import net.sourceforge.vulcan.ant.AntBuildTool;
import net.sourceforge.vulcan.ant.AntConfig;
import net.sourceforge.vulcan.ant.AntProjectConfig;
import net.sourceforge.vulcan.ant.JavaCommandBuilder;
import net.sourceforge.vulcan.ant.JavaHome;
import net.sourceforge.vulcan.ant.receiver.AntEventSource;
import net.sourceforge.vulcan.ant.receiver.UdpEventSource;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.BuildFailedException;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.io.CopyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.AntProjectBuilder;

public class MavenBuildTool extends AntBuildTool {
	static final Log log = LogFactory.getLog(MavenBuildTool.class);
	
	static final String MAVEN1_LAUNCHER_PATH_PREFIX = "forehead";
	static final String MAVEN1_LAUNCHER_MAIN_CLASS_NAME = "com.werken.forehead.Forehead";
	static final String MAVEN1_ENDORSED_DIR = "lib" + File.separator + "endorsed";
	
	static final String MAVEN2_BOOT_DIR = "core" + File.separator + "boot";
	static final String MAVEN2_LAUNCHER_PATH_PREFIX = "classworlds";
	static final String MAVEN2_LAUNCHER_MAIN_CLASS_NAME = "org.codehaus.classworlds.Launcher";

	final OutputStream stdErrStream = new ByteArrayOutputStream();
	
	final MavenHome mavenHome;
	
	BuildDetailCallback callback;
	
	File mavenLaunchJar;
	File launchConfigFile;
	Thread logThread;
	Thread stdErrThread;
	String projectName;
	String mainClass;
	
	boolean maven2;
	boolean maven1_1;
	
	public MavenBuildTool(AntProjectConfig projectConfig, AntConfig mavenConfig, JavaHome javaHome, MavenHome mavenHome) {
		super(projectConfig, mavenConfig, javaHome);
		this.mavenHome = mavenHome;
	}

	@Override
	public void buildProject(ProjectConfigDto projectConfig, ProjectStatusDto status, File logFile, BuildDetailCallback callback) throws BuildFailedException, ConfigException {
		this.callback = callback;
		
		projectName = projectConfig.getName();

		configure();
		
		try {
			buildProjectInternal(projectConfig, status, logFile, callback);
		} finally {
			launchConfigFile.delete();
			launchConfigFile = null;
		}
	}

	public String getMainClass() {
		return mainClass;
	}
	
	protected void configure() throws ConfigException {
		mavenLaunchJar = getMavenLauncherJar(mavenHome.getDirectory());
		
		if (maven2) {
			mainClass = MAVEN2_LAUNCHER_MAIN_CLASS_NAME;
		} else {
			mainClass = MAVEN1_LAUNCHER_MAIN_CLASS_NAME;
		}
		
		try {
			configureLauncher();
		} catch (IOException e) {
			throw new ConfigException("errors.cannot.create.file",
					new String[] {"tmpfile", e.getMessage()});
		}
	}

	public File getMavenLauncherJar(String directory) throws ConfigException {
		try {
			maven2 = true;
			return getMavenHomeLibrary(directory, MAVEN2_LAUNCHER_PATH_PREFIX);
		} catch (ConfigException e) {
			maven2 = false;
			return getMavenHomeLibrary(directory, MAVEN1_LAUNCHER_PATH_PREFIX);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static File getMavenHomeLibrary(String mavenHome, String resourcePath) throws ConfigException {
		if (StringUtils.isBlank(mavenHome)) {
			throw new ConfigException("maven.home.required", null);
		}
		
		final File top = new File(mavenHome);
		if (!top.isDirectory()) {
			throw new ConfigException("maven.home.invalid", new String[] { mavenHome });
		}
		
		File lib = new File(mavenHome, MAVEN2_BOOT_DIR);
		
		if (!lib.isDirectory()) {
			lib = new File(mavenHome, "lib");	
		}

		if (!lib.isDirectory()) {
			throw new ConfigException("maven.home.missing.resource",
					new String[] { resourcePath, mavenHome });
		}
		
		final Collection<File> matches = FileUtils.listFiles(lib, new PrefixFileFilter(resourcePath), null);
		
		if (matches.size() == 0) {
			throw new ConfigException("maven.home.missing.resource",
					new String[] { resourcePath, mavenHome });
		} else if (matches.size() > 1) {
			throw new IllegalStateException("Found more than one resource matching " + resourcePath);
		}
		
		return matches.iterator().next();
	}
	
	protected void buildProjectInternal(ProjectConfigDto projectConfig, ProjectStatusDto status, File logFile, BuildDetailCallback callback) throws BuildFailedException, ConfigException {
		super.buildProject(projectConfig, status, logFile, callback);
	}
	@Override
	protected JavaCommandBuilder createJavaCommand(ProjectConfigDto projectConfig, ProjectStatusDto status, File logFile) throws ConfigException {
		final JavaCommandBuilder jcb = new JavaCommandBuilder();
		
		final String javaHome = setJavaExecutablePath(jcb);
		
		try {
			jcb.addClassPathEntry(mavenLaunchJar.getCanonicalPath());
		
			jcb.addSystemProperty("vulcan-ant.jar", 
					AntBuildTool.getLocalClassPathEntry(
							UdpEventSource.class, null)
								.getCanonicalPath());
			jcb.addSystemProperty("vulcan-maven.jar", 
					AntBuildTool.getLocalClassPathEntry(
							AntProjectBuilder.class, null)
								.getCanonicalPath());

			if (!maven2 && !maven1_1) {
				jcb.addSystemProperty("javax.xml.parsers.SAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl");
				jcb.addSystemProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
				jcb.addSystemProperty("java.endorsed.dirs", getEndorsedClassPath(javaHome, mavenHome.getDirectory()));
			}
			
			if (maven2) {
				jcb.addSystemProperty("classworlds.conf", launchConfigFile.getCanonicalPath());
			} else {
				jcb.addSystemProperty("forehead.conf.file", launchConfigFile.getCanonicalPath());
				jcb.addSystemProperty("tools.jar", getJdkToolsJar(javaHome).getCanonicalPath());
			}
			
			jcb.addSystemProperty("maven.home", mavenHome.getDirectory());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if (maven2) {
			jcb.setMainClassName(MAVEN2_LAUNCHER_MAIN_CLASS_NAME);
		} else {
			jcb.setMainClassName(MAVEN1_LAUNCHER_MAIN_CLASS_NAME);
		}
		
		if (antProjectConfig.isDebug()) {
			jcb.addArgument("--debug");
		}
		
		addProps(jcb, antConfig.getAntProperties(), true);
		addProps(jcb, antProjectConfig.getAntProperties(), true);

		addVersionProperties(status);
		
		antPropsToArgs(jcb);
		
		final AntEventSource eventSource = getEventSource();
		if (eventSource != null) {
			eventSource.addSystemProperties(jcb);
		}

		final String[] goals = super.antProjectConfig.getTargets().split(" ");
		for (String goal : goals) {
			jcb.addArgument(goal);
		}
		
		return jcb;
	}

	@Override
	protected File checkBuildScriptExists(ProjectConfigDto projectConfig) throws ConfigException {
		return super.checkBuildScriptExists(projectConfig);
	}

	@Override
	protected void preparePipes(final Process process) throws IOException {
		process.getOutputStream().close();
		
		stdErrThread = new PipeThread("Maven stderr l [" + projectName + "]", process.getErrorStream(), stdErrStream);
		stdErrThread.start();
		
		
		if (logFile == null) {
			process.getInputStream().close();
			return;
		}
		
		final OutputStream logOutputStream = new FileOutputStream(logFile);
		
		logThread = new PipeThread("Maven Build Logger [" + projectName + "]", process.getInputStream(), logOutputStream);
		logThread.start();
	}
	@Override
	protected void flushPipes() throws IOException {
		try {
			waitFor(logThread);
		} finally {
			try {
				waitFor(stdErrThread);
			} finally {
				logThread = null;
				stdErrThread = null;
			}
		}
		
		final String errors = stdErrStream.toString();
		if (isNotBlank(errors)) {
			callback.reportError(errors, null, null, null);
		}
	}

	protected void configureLauncher() throws IOException, ConfigException {
		final File vanillaConfig = findLaunchConfig();
		
		if (vanillaConfig == null) {
			throw new ConfigException("maven.home.missing.resource",
					new String[] { "bin/forehead.conf or bin/m2.conf", mavenHome.getDirectory() });
		}
		
		launchConfigFile = File.createTempFile("vulcan-maven-launch", ".conf");
		
		configureLaunchFile(new FileInputStream(vanillaConfig), new FileOutputStream(launchConfigFile));
	}
	protected File findLaunchConfig() {
		final String mavenHomeDirectory = mavenHome.getDirectory();
		File config = new File(mavenHomeDirectory, "bin" + File.separator + "forehead.conf");
		
		if (config.exists()) {
			return config;
		}
		
		config = new File(mavenHomeDirectory, "bin" + File.separator + "m2.conf");

		if (config.exists()) {
			return config;
		}
		
		return null;
	}

	static String getEndorsedClassPath(String javaHome, String mavenHome) throws IOException {
		final StringBuffer buf = new StringBuffer();
		buf.append(new File(javaHome, MAVEN1_ENDORSED_DIR).getCanonicalPath());
		buf.append(File.pathSeparatorChar);
		buf.append(new File(mavenHome, MAVEN1_ENDORSED_DIR).getCanonicalPath());
		
		return buf.toString();
	}
	
	void configureLaunchFile(InputStream is, OutputStream os) throws IOException {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		final PrintWriter writer = new PrintWriter(os);
	
		maven1_1 = true;
		
		try {
			String line;
			
			while ((line = reader.readLine()) != null) {
				writer.write(line);
				writer.write("\n");
				
				if ("+tools.jar".equals(line)) {
					writer.write("+vulcan-ant.jar\n");
					writer.write("+vulcan-maven.jar\n");
				} else if ("[plexus.core]".equals(line)) {
					writer.write("load ${vulcan-ant.jar}\n");
				} else if ("[plexus.core.maven]".equals(line)) {
					writer.write("load ${vulcan-maven.jar}\n");
				} else if (line.contains("${tools.jar}")) {
					writer.write("    ${vulcan-ant.jar}\n");
				} else if ("[root.maven]".equals(line)) {
					writer.write("    ${vulcan-maven.jar}\n");
				} else if (line.contains("endorsed")) {
					maven1_1 = false;
				}
			}
		} finally {
			writer.close();
			reader.close();
		}
	}
	
	static File getJdkToolsJar(String javaHome) throws ConfigException {
		final String tools = "lib" + File.separator + "tools.jar";
		
		File toolJar = new File(javaHome, tools);
		
		if (!toolJar.exists()) {
			toolJar = new File(new File(javaHome).getParentFile(), tools);
		}
		
		if (!toolJar.exists()) {
			throw new ConfigException("maven.jdk.tools.not.found", new String[] {javaHome});
		}
		
		return toolJar;
	}
	private void waitFor(Thread thread) {
		if (thread == null) {
			return;
		}
		
		try {
			thread.join();
		} catch (InterruptedException e) {
			log.error("Unexpected interrupt while waiting for logger thread to complete", e);
		} finally {
			logThread = null;
		}
	}
	
	private final class PipeThread extends Thread {
		private final OutputStream ouputStream;
		private final InputStream inputStream;
		
		private PipeThread(String name, InputStream inputStream, OutputStream outputStream) {
			super(name);
			this.inputStream = inputStream;
			this.ouputStream = outputStream;
		}
		@Override
		public void run() {
			try {
				try {
					CopyUtils.copy(inputStream, ouputStream);
				} finally {
					try {
						ouputStream.close();
					} finally {
						inputStream.close();
					}
				}
			} catch (IOException e) {
				log.error("IOException capturing maven build log", e);
			}
		}
	}
}
