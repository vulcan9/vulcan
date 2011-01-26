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
package net.sourceforge.vulcan.shell;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.sourceforge.vulcan.core.BuildDetailCallback;

class ShellOutputProcessor {
	private final static Pattern ERROR_PATTERN = Pattern.compile("(.+):(\\d+): (warning|error): (.+)");
	
	private final BufferedWriter writer;
	private final BuildDetailCallback buildDetailCallback;
	
	ShellOutputProcessor(BufferedWriter writer, BuildDetailCallback buildDetailCallback) {
		this.writer = writer;
		this.buildDetailCallback = buildDetailCallback;
	}
	
	void processStream(InputStream inputStream) throws IOException {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		
		while ((line = reader.readLine()) != null) {
			synchronized (writer) {
				writer.write(line);
				writer.write('\n');
			}
			
			if (line.startsWith("make  ")) {
				buildDetailCallback.setDetail(line.substring(6));
			} else {
				final Matcher matcher = ERROR_PATTERN.matcher(line);
				if (matcher.find()) {
					final String file = matcher.group(1);
					final Integer lineNumber = Integer.parseInt(matcher.group(2));
					final boolean isError = "error".equals(matcher.group(3));
					final String message = matcher.group(4);
					
					if (isError) {
						buildDetailCallback.reportError(message, file, lineNumber, null);
					} else {
						buildDetailCallback.reportWarning(message, file, lineNumber, null);	
					}
				}
			}
		}
	}
	
}
