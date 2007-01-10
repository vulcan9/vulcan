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

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.SetPropertiesRule;
import org.xml.sax.SAXException;

import net.sourceforge.vulcan.integration.PluginVersionSpec;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class PluginVersionDigester {
	public static PluginVersionSpec digest(Reader reader) throws IOException {
		if (reader == null) {
			throw new IllegalArgumentException();
		}
		
		Digester digester = new Digester();
		
		digester.addObjectCreate("vulcan-version-descriptor", PluginVersionSpecImpl.class.getName());
		
		digester.addRule("vulcan-version-descriptor", new SetPropertiesRule());
		
		try {
			return (PluginVersionSpec)digester.parse(reader);
		} catch (SAXException e) {
			return null;
		}
	}
	
	public static class PluginVersionSpecImpl implements PluginVersionSpec {
		long pluginRevision;
		String version;
		
		public long getPluginRevision() {
			return pluginRevision;
		}
		public void setPluginRevision(long pluginRevision) {
			this.pluginRevision = pluginRevision;
		}
		public String getVersion() {
			return version;
		}
		public void setVersion(String pluginVersion) {
			this.version = pluginVersion;
		}
	}
}
