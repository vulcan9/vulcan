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
package net.sourceforge.vulcan.metrics.scanner.ant;

import junit.framework.TestCase;
import net.sourceforge.vulcan.TestUtils;

public class AntFileScannerTest extends TestCase {
	AntFileScanner scanner = new AntFileScanner();
	
	public void testScanNoIncludes() throws Exception {
		final String[] matched = scanner.scanFiles(
				TestUtils.resolveRelativeFile("source/test/scan-root"),
				new String[0],
				new String[0]);
		
		assertEquals(0, matched.length);
	}

	public void testScanIncludesAll() throws Exception {
		final String[] matched = scanner.scanFiles(
				TestUtils.resolveRelativeFile("source/test/scan-root"),
				new String[] {"**/*"},
				new String[0]);
		
		assertMatchSize(matched, 9);
	}

	public void testScanIncludesSingleLevel() throws Exception {
		final String[] matched = scanner.scanFiles(
				TestUtils.resolveRelativeFile("source/test/scan-root"),
				new String[] {"*/*"},
				new String[0]);
		
		assertMatchSize(matched, 6);
	}

	public void testScanExcludes() throws Exception {
		final String[] matched = scanner.scanFiles(
				TestUtils.resolveRelativeFile("source/test/scan-root"),
				new String[] {"**/*"},
				new String[] {"*/2"});
		
		assertMatchSize(matched, 7);
	}

	public void testScanExcludesWild() throws Exception {
		final String[] matched = scanner.scanFiles(
				TestUtils.resolveRelativeFile("source/test/scan-root"),
				new String[] {"**/*"},
				new String[] {"**/2"});
		
		assertMatchSize(matched, 6);
	}

	private void assertMatchSize(String[] matched, int expectedSize) {
		int size = matched.length;
		
		for (int i=0; i<matched.length; i++) {
			if (matched[i].indexOf("svn")>=0) {
				size--;
			}
		}
		
		assertEquals(expectedSize, size);
	}
}
