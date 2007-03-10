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

import java.util.Properties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.core.io.ClassPathResource;

class CoberturaSupport {
	static final boolean enabled;
	static final String datafileLocation;
	
	static {
		final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("cobertura.properties");
		
		if (stream != null) {
			enabled = true;
			final Properties props = new Properties();
			try {
				props.load(stream);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				try {
					stream.close();
				} catch (IOException ignore) {
				}
			}
			
			datafileLocation = props.getProperty("net.sourceforge.cobertura.datafile");
		} else {
			enabled = false;
			datafileLocation = null;
		}
	}
	
	static boolean isEnabled() {
		return enabled;
	}
	
	static String getDatafileLocation() {
		return datafileLocation;
	}

	public static String getJarLocation() {
		final ClassPathResource resource = new ClassPathResource(
				"net/sourceforge/cobertura/coveragedata/HasBeenInstrumented.class");
		
		final URL url;
		try {
			url = resource.getURL();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if ("jar".equals(url.getProtocol())) {
			String path = url.getPath().substring(5);
			path = path.substring(0, path.indexOf('!'));
			
			return new File(path.replaceAll("%20", " ")).getAbsolutePath();
		}
		
		throw new RuntimeException("Cobertura is not in a jar!");
	}
}
