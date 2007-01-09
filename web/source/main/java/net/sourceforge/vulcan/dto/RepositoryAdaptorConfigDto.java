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
package net.sourceforge.vulcan.dto;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class RepositoryAdaptorConfigDto extends PluginConfigDto {
	private Map<String, Long> workingCopyByteCounts = new HashMap<String, Long>();
	
	public Map<String, Long> getWorkingCopyByteCounts() {
		return workingCopyByteCounts;
	}
	public void setWorkingCopyByteCounts(Map<String, Long> workingCopyByteCounts) {
		this.workingCopyByteCounts = workingCopyByteCounts;
		normalizeCounts();
	}
	
	// Spring will initialize byteCounters as Strings (not Longs).  Fix it.
	// This is one example of why Java Generics Type Erasure sucks.
	private void normalizeCounts() {
		for (Map.Entry<String, Long> e : workingCopyByteCounts.entrySet()) {
			final Object value = e.getValue();
			if (value instanceof String) {
				e.setValue(Long.valueOf((String)value));
			}
		}
	}

}
