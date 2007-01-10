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
package net.sourceforge.vulcan;

import java.io.File;

import org.apache.commons.lang.StringUtils;

import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class TestUtils {
	private static final String basedir;
	
	static {
		String tmp = System.getProperty("basedir");
		if (StringUtils.isNotEmpty(tmp)) {
			if (!tmp.endsWith("/") && !tmp.endsWith("\\")) {
				tmp += File.separator;
			}
		} else {
			tmp = null;
		}
		
		basedir = tmp;
	}
	
	/**
	 * This method works around the problem where the current working directory
	 * may not be the same depending on which environment the tests are executing
	 * in.  Therefore, relative paths to files must be adjusted based on the
	 * environment.
	 */
	public static File resolveRelativeFile(String relativePath) {
		return new File(resolveRelativePath(relativePath));
	}
	
	/**
	 * This method works around the problem where the current working directory
	 * may not be the same depending on which environment the tests are executing
	 * in.  Therefore, relative paths to files must be adjusted based on the
	 * environment.
	 */
	public static String resolveRelativePath(String relativePath) {
		if (basedir != null) {
			return basedir + relativePath;
		}
		
		return relativePath;
	}
}
