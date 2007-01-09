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

import java.io.File;

import net.sourceforge.vulcan.metrics.scanner.FileScanner;

import org.apache.tools.ant.DirectoryScanner;

public class AntFileScanner implements FileScanner {
	public String[] scanFiles(File rootDir, String[] includes, String[] excludes) {
		if (includes.length == 0) {
			return new String[0];
		}
		
		DirectoryScanner ds = new DirectoryScanner();
		
		ds.setBasedir(rootDir);
		ds.setIncludes(includes);
		ds.setExcludes(excludes);
		
		ds.scan();
		
		return ds.getIncludedFiles();
	}
}
