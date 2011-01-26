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

import junit.framework.TestCase;

public class JavaCommandBuilderTest extends TestCase {
	JavaCommandBuilder builder = new JavaCommandBuilder();

	@Override
	protected void setUp() throws Exception {
		builder.setJavaExecutablePath("java");
		builder.setMainClassName("com.example.Main");
	}
	
	public void testSimple() throws Exception {
		assertEquals("java com.example.Main", builder.toString());
	}
	
	public void testAddToClassPath() throws Exception {
		builder.addClassPathEntry("/my/jars/com.example.jar");
		
		assertEquals("java -classpath /my/jars/com.example.jar com.example.Main", builder.toString());
	}

	public void testAddToClassPathMaintainsOrderDropsDups() throws Exception {
		builder.addClassPathEntry("/my/jars/com.example.jar");
		builder.addClassPathEntry("/my/jars/ar.example.jar");
		builder.addClassPathEntry("/my/jars/com.example.jar");
		builder.addClassPathEntry("/my/jars/com.example.jar");
		
		assertEquals("java -classpath /my/jars/com.example.jar" + File.pathSeparator +
				"/my/jars/ar.example.jar com.example.Main", builder.toString());
	}
	
	public void testSetSystemProperties() throws Exception {
		builder.addSystemProperty("com.example.value", "34");
		builder.addSystemProperty("com.example.value", "35");
		
		assertEquals("java -Dcom.example.value=35 com.example.Main", builder.toString());
	}
	
	public void testSetMaxMemory() throws Exception {
		builder.setMaxMemoryInMegabytes(25);
		
		assertEquals("java -Xmx25M com.example.Main", builder.toString());
	}
	
	public void testAddArgument() throws Exception {
		builder.addArgument("param=some param");
		
		assertEquals("java com.example.Main param=some param", builder.toString());
	}
}
