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
package net.sourceforge.vulcan.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JavaCommandBuilder {
	private final static Log log = LogFactory.getLog(JavaCommandBuilder.class);
	
	private String javaExecutablePath;
	private String mainClassName;
	private int maxMemoryInMegabytes;

	private Map<String, String> systemProperties = new HashMap<String, String>();
	private List<String> classPath = new ArrayList<String>();
	private List<String> arguments = new ArrayList<String>();
	
	public String[] construct() {
		final List<String> args = new ArrayList<String>();
		
		args.add(javaExecutablePath);
		
		if (maxMemoryInMegabytes > 0) {
			args.add("-Xmx" + maxMemoryInMegabytes + "M");
		}
		
		if (!classPath.isEmpty()) {
			args.add("-classpath");
			args.add(StringUtils.join(classPath.iterator(), File.pathSeparatorChar));
		}
		
		for (String property : systemProperties.keySet()) {
			final StringBuffer arg = new StringBuffer("-D");
			arg.append(property);
			arg.append("=");
			arg.append(systemProperties.get(property));
			
			args.add(arg.toString());
		}
		
		args.add(mainClassName);
		
		for (String arg : arguments) {
			args.add(arg);
		}
		
		return args.toArray(new String[args.size()]);
	}
	public void addClassPathEntry(String path) {
		if (!classPath.contains(path)) {
			classPath.add(path);
		}
	}
	public void addSystemProperty(String key, String value) {
		if (systemProperties.containsKey(key)) {
			log.debug("Overriding system property " + key);
		}
		systemProperties.put(key, value);
	}
	public void addArgument(String arg) {
		arguments.add(arg);
	}
	@Override
	public String toString() {
		return StringUtils.join(construct(), " ");
	}
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	public String getJavaExecutablePath() {
		return javaExecutablePath;
	}
	public void setJavaExecutablePath(String javaExecutablePath) {
		this.javaExecutablePath = javaExecutablePath;
	}
	public String getMainClassName() {
		return mainClassName;
	}
	public void setMainClassName(String mainClassName) {
		this.mainClassName = mainClassName;
	}
	public int getMaxMemoryInMegabytes() {
		return maxMemoryInMegabytes;
	}
	public void setMaxMemoryInMegabytes(int maxMemoryInMegabytes) {
		this.maxMemoryInMegabytes = maxMemoryInMegabytes;
	}
	public List<String> getClassPath() {
		return Collections.unmodifiableList(classPath);
	}
}
