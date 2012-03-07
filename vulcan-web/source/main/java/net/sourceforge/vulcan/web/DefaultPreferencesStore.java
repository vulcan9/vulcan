/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.sourceforge.vulcan.dto.PreferencesDto;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultPreferencesStore implements PreferencesStore {
	private static final Log LOG = LogFactory.getLog(DefaultPreferencesStore.class);
	
	private PreferencesDto defaultPreferences;

	public String convertToString(PreferencesDto prefs) {
		try {
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			final ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(prefs);
			
			return new String(Base64.encodeBase64(os.toByteArray(), false), "us-ascii");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public PreferencesDto convertFromString(String data) {
		try {
			final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(Base64.decodeBase64(data.getBytes("us-ascii"))));
		
			final PreferencesDto prefs = (PreferencesDto) ois.readObject();
			
			// if the user preferences did not have default columns, set them now.
			if (prefs.getDashboardColumns() == null) {
				prefs.setDashboardColumns(defaultPreferences.getDashboardColumns());
			}
			
			return prefs;
		} catch (Exception e) {
			LOG.error("Failed to load user preferences", e);
			return defaultPreferences;
		}
	}
	
	public PreferencesDto getDefaultPreferences() {
		return defaultPreferences;
	}

	public void setDefaultPreferences(PreferencesDto defaultPreferences) {
		this.defaultPreferences = defaultPreferences;
	}
}
