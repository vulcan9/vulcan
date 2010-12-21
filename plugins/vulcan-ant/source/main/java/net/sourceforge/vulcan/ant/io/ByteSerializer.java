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
package net.sourceforge.vulcan.ant.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;

public class ByteSerializer implements Serializer {
	private class StringReader {
		final byte[] data;
		
		int position;

		public StringReader(final byte[] data) {
			this.data = data;
			this.position = 0;
		}

		public String readString() {
			int length = 0;
			
			while (data[position + length] != 0) {
				length++;
			}

			final String string = new String(data, position, length);
			
			position += length + 1;
			
			return string;
		}
	}
	public synchronized AntEventSummary deserialize(byte[] data) {
		final StringReader r = new StringReader(data);
		
		final String type = r.readString();
		final String projectName = r.readString();
		final String targetName = r.readString();
		final String taskName = r.readString();
		final String priority = r.readString();
		final String file = r.readString();
		final String lineNumber = r.readString();
		final String code = r.readString();
		final String message = r.readString();

		final int prio;
		
		if (StringUtils.isNotBlank(priority)) {
			prio = Integer.parseInt(priority);
		} else {
			prio = Project.MSG_INFO;
		}
		
		final Integer lineNum;

		if (StringUtils.isNotBlank(lineNumber)) {
			lineNum = Integer.valueOf(lineNumber);
		} else {
			lineNum = null;
		}
		
		return new AntEventSummary(
				type,
				projectName,
				targetName,
				taskName,
				message,
				prio,
				file,
				lineNum,
				code);
	}

	public byte[] serialize(AntEventSummary eventSummary) {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		final String lineNumber;
		
		if (eventSummary.getLineNumber() != null) {
			lineNumber = eventSummary.getLineNumber().toString();
		} else {
			lineNumber = null;
		}
		
		addString(os, eventSummary.getType());
		addString(os, eventSummary.getProjectName());
		addString(os, eventSummary.getTargetName());
		addString(os, eventSummary.getTaskName());
		addString(os, Integer.toString(eventSummary.getPriority()));
		addString(os, eventSummary.getFile());
		addString(os, lineNumber);
		addString(os, eventSummary.getCode());
		addString(os, eventSummary.getMessage());
		
		return os.toByteArray();
	}

	private void addString(final ByteArrayOutputStream os, String string) {
		try {
			if (string != null) {
				os.write(string.getBytes());
			}
			os.write(0);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
