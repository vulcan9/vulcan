/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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
package net.sourceforge.vulcan.jabber;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.ArrayUtils;

import net.sourceforge.vulcan.dto.BaseDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;

public class DictionaryScreenNameMapperConfig extends PluginConfigDto {

	private String[] entries = {};
	
	@Override
	public String getPluginId() {
		return JabberPlugin.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return JabberPlugin.PLUGIN_NAME;
	}

	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();

		addProperty(pds, "entries", "DictionaryScreenNameMapperConfig.entries.name", "DictionaryScreenNameMapperConfig.entries.description", locale);

		return pds;
	}
	
	@Override
	public BaseDto copy() {
		final DictionaryScreenNameMapperConfig copy = (DictionaryScreenNameMapperConfig) super.copy();
		copy.setEntries((String[]) ArrayUtils.clone(getEntries()));
		return copy;
	}
	
	@Override
	public String getHelpTopic() {
		return "JabberDictionaryScreenNameMapperConfig";
	}

	public String[] getEntries() {
		return entries;
	}
	
	public void setEntries(String[] entries) {
		this.entries = entries;
	}

}
