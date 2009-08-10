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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DictionaryScreenNameMapper implements ScreenNameMapper {
	private final static Log LOG = LogFactory.getLog(DictionaryScreenNameMapper.class);
	
	private final DictionaryScreenNameMapperConfig config;

	private final Map<String, String> map = new HashMap<String, String>();
	
	public DictionaryScreenNameMapper(DictionaryScreenNameMapperConfig config) {
		this.config = config;
		digest();
	}

	public Map<String, String> lookupByAuthor(Iterable<String> uniques) {
		Map<String, String> names = new HashMap<String, String>();
		
		for (String s : uniques) {
			final String sn = map.get(s.toLowerCase());
			if (StringUtils.isNotBlank(sn)) {
				names.put(s, sn);
			}
		}
		
		return names;
	}

	public void digest() {
		map.clear();
		
		for (String pair : config.getEntries()) {
			String[] parts = pair.split("=");
			if (parts.length != 2) {
				LOG.warn("Entry " + pair + " is not in format commit_author=screen_name.  Ignoring.");
				continue;
			}
			map.put(parts[0].toLowerCase().trim(), parts[1].trim());
		}
	}
}
