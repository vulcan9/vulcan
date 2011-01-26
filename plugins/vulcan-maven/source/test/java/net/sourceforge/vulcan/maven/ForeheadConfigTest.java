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
package net.sourceforge.vulcan.maven;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

public class ForeheadConfigTest extends MavenBuildToolTestBase {
	String expectedMaven102Config;
	String expectedMaven1_1beta2Config;
	String expectedMaven2Config;
	String expectedMaven2_0_6_Config;
	String expectedMaven2_2_1_Config;
	
	OutputStream os = new ByteArrayOutputStream();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		expectedMaven102Config = IOUtils.toString(getClass().getResourceAsStream("resources/forehead-1.0.2-configured.conf"));
		expectedMaven1_1beta2Config = IOUtils.toString(getClass().getResourceAsStream("resources/forehead-1.1beta2-configured.conf"));
		expectedMaven2Config = IOUtils.toString(getClass().getResourceAsStream("resources/m2-configured.conf"));
		expectedMaven2_0_6_Config = IOUtils.toString(getClass().getResourceAsStream("resources/m2.0.6-configured.conf"));
		expectedMaven2_2_1_Config = IOUtils.toString(getClass().getResourceAsStream("resources/m2.2.1-configured.conf"));
	}
	
	public void testConfigureMaven102() throws Exception {
		tool.configureLaunchFile(getClass().getResourceAsStream("resources/forehead-1.0.2-vanilla.conf"), os);
		
		assertEquals(expectedMaven102Config, os.toString());
	}

	public void testConfigureMaven1_1beta2() throws Exception {
		tool.configureLaunchFile(getClass().getResourceAsStream("resources/forehead-1.1beta2-vanilla.conf"), os);
		
		assertEquals(expectedMaven1_1beta2Config, os.toString());
	}

	public void testConfigureMaven2() throws Exception {
		tool.configureLaunchFile(getClass().getResourceAsStream("resources/m2-vanilla.conf"), os);
		
		assertEquals(expectedMaven2Config, os.toString());
	}

	public void testConfigureMaven2_0_6() throws Exception {
		tool.configureLaunchFile(getClass().getResourceAsStream("resources/m2.0.6-vanilla.conf"), os);
		
		assertEquals(clean(expectedMaven2_0_6_Config), clean(os.toString()));
	}

	public void testConfigureMaven2_2_1() throws Exception {
		tool.configureLaunchFile(getClass().getResourceAsStream("resources/m2.2.1-vanilla.conf"), os);
		
		assertEquals(clean(expectedMaven2_2_1_Config), clean(os.toString()));
	}

	private String clean(String string) {
		return string.replaceAll("\\r", "").trim();
	}
}
