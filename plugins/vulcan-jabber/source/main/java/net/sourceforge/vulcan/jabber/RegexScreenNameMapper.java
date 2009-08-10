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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexScreenNameMapper implements ScreenNameMapper {
	private final RegexScreenNameMapperConfig config;

	public RegexScreenNameMapper(RegexScreenNameMapperConfig config) {
		this.config = config;
	}

	public Map<String, String> lookupByAuthor(Iterable<String> uniques) {
		final Pattern pattern = Pattern.compile(config.getRegex(), Pattern.CASE_INSENSITIVE);
		
		final Map<String, String> map = new HashMap<String, String>();
		
		for (String s : uniques) {
			final Matcher matcher = pattern.matcher(s);
			if (matcher.matches()) {
				map.put(s, matcher.replaceFirst(config.getReplacement()));
			}
		}
		
		return map;
	}

}
