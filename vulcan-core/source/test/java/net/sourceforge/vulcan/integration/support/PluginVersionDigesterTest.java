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
package net.sourceforge.vulcan.integration.support;

import java.io.Reader;
import java.io.StringReader;

import junit.framework.TestCase;
import net.sourceforge.vulcan.integration.PluginVersionSpec;

public class PluginVersionDigesterTest extends TestCase {
	public void testNull() throws Exception {
		final Reader reader = null;
		
		try {
			PluginVersionDigester.digest(reader);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	public void testBlank() throws Exception {
		final Reader reader = new StringReader("");
		
		assertNull(PluginVersionDigester.digest(reader));
	}
	
	public void testWrongRoot() throws Exception {
		final Reader reader = new StringReader("<wrong/>");
		
		assertNull(PluginVersionDigester.digest(reader));
	}

	public void testNoRev() throws Exception {
		final Reader reader = new StringReader("<vulcan-version-descriptor/>");
		
		PluginVersionSpec spec = PluginVersionDigester.digest(reader);
		assertNotNull(spec);

		assertEquals(0, spec.getPluginRevision());
	}
	
	public void testDigest() throws Exception {
		final Reader reader = new StringReader("<vulcan-version-descriptor pluginRevision=\"1234\"/>");
		
		PluginVersionSpec spec = PluginVersionDigester.digest(reader);
		assertNotNull(spec);

		assertEquals(1234, spec.getPluginRevision());
	}
	public void testDigestVersion() throws Exception {
		final Reader reader = new StringReader("<vulcan-version-descriptor pluginRevision=\"4132\" version=\"x.a.4.3\"/>");
		
		PluginVersionSpec spec = PluginVersionDigester.digest(reader);
		assertNotNull(spec);

		assertEquals(4132, spec.getPluginRevision());
		assertEquals("x.a.4.3", spec.getVersion());
	}
}
