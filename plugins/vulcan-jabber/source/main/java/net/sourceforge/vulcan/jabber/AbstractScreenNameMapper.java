/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2013 Chris Eldredge
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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;

import net.sourceforge.vulcan.jabber.ScreenNameMapperConfig.CommitterField;

public abstract class AbstractScreenNameMapper implements ScreenNameMapper {
	private ScreenNameMapperConfig config;

	protected AbstractScreenNameMapper(ScreenNameMapperConfig config) {
		this.config = config;
	}
	
	@Override
	public Map<String, String> lookupByAuthor(Iterable<Committer> committers) {
		final Map<String, String> map = new HashMap<String, String>();
		
		for (Committer c : committers) {
			final String field = getCommitterField(c);
			final String result = lookupByAuthor(field);
			if (isNotBlank(result)) {
				map.put(c.getName(), result);
			}
		}
		
		return map;
	}

	protected String lookupByAuthor(String field) {
		throw new NotImplementedException();
	}

	protected String getCommitterField(Committer c) {
		return config.getField() == CommitterField.Email ? c.getEmail() : c.getName();
	}
}
