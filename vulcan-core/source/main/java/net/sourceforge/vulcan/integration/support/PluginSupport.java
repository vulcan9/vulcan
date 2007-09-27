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

import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.PluginProfileDto;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.lang.StringUtils;

public abstract class PluginSupport {
	protected static interface Visitor<T> {
		boolean isMatch(T node);
	}

	protected static <T extends PluginProfileDto> T getSelectedEnvironment(T[] environments, String name,
			String missingMessageKey) throws ConfigException {
		return getSelectedEnvironment(environments, name, missingMessageKey, false);
	}
	
	protected static <T extends PluginProfileDto> T getSelectedEnvironment(T[] environments, final String name,
			String missingMessageKey, final boolean matchPartial) throws ConfigException {
		
		if (StringUtils.isBlank(name)) {
			return null;
		}
		
		final T t = getSelectedEnvironment(environments, new Visitor<T>() {
			public boolean isMatch(T node) {
				if ((matchPartial && node.getName().startsWith(name))
						||name.equals(node.getName())) {
						return true;
					}
				return false;
			}
		});
		
		if (t == null && missingMessageKey != null) {
			throw new ConfigException(missingMessageKey, new String[] {name});
		}
		
		return t;
	}
	
	protected static <T extends PluginProfileDto> T getSelectedEnvironment(T[] environments, Visitor<T> visitor) {
		for (T t : environments) {
			if (visitor.isMatch(t)) {
				return t;
			}
		}
		
		return null;
	}
	
	/**
	 * Set the details message during RepositoryAdaptor.createWorkingCopy()
	 * @param buildDetailCallback
	 * @param bytesCounted Bytes checked out so far.
	 * @param previousBytesCounted Total bytes checked out during last build, or -1 if not available.
	 */
	public static void setWorkingCopyProgress(BuildDetailCallback buildDetailCallback, long bytesCounted, long previousBytesCounted)
	{
		if (buildDetailCallback == null) {
			return;
		}
		
		final String detail;
		
		if (previousBytesCounted <= 0) {
			detail = toUnits(bytesCounted) + " so far";
		} else {
			long percent = bytesCounted * 100 / previousBytesCounted;
			
			if (percent > 100) {
				detail = "100%+ (" + toUnits(bytesCounted - previousBytesCounted) + " more than last time)";
			} else {
				detail = percent + "% (est.)";
			}
		}
		
		buildDetailCallback.setDetail(detail);
	}
	private static String toUnits(long bytes) {
		long amount;
		String unit;
		
		if (bytes > 1024 * 1024) {
			amount = bytes / (1024 * 1024);
			unit = "mb";
		} else if (bytes > 1024) {
			amount = bytes / 1024;
			unit = "kb";
		} else {
			amount = bytes;
			unit = "bytes";
		}
		
		return amount + " " + unit;
	}
}
