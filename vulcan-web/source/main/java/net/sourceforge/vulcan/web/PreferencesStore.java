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
package net.sourceforge.vulcan.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.sourceforge.vulcan.dto.PreferencesDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.codec.binary.Base64;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class PreferencesStore {
	public String encodePreferences(PreferencesDto prefs) {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		try {
			final ObjectOutputStream oos = new ObjectOutputStream(os);
		
			oos.writeObject(prefs);
		
			return new String(Base64.encodeBase64(os.toByteArray()));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public PreferencesDto decodePreferences(byte[] base64Bytes) {
		final byte[] serialBytes = Base64.decodeBase64(base64Bytes);
		try {
			final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serialBytes));
			return (PreferencesDto) ois.readObject();
		} catch (Exception e) {
			throw new IncompatiblePreferenceDataException(e);
		}
	}

}
