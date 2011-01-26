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
package net.sourceforge.vulcan.ant.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;

public class ObjectSerializer implements Serializer {

	public AntEventSummary deserialize(byte[] data) {
		try {
			final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
		
			return (AntEventSummary) ois.readObject();
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			
			throw new RuntimeException(e);
		}
	}

	public byte[] serialize(AntEventSummary eventSummary) {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final ObjectOutputStream oos;

		try {
			oos = new ObjectOutputStream(os);
			oos.writeObject(eventSummary);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return os.toByteArray();
	}

}
