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
import net.sourceforge.vulcan.dto.NamedObject;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.lang.StringUtils;

public abstract class PluginSupport {

	protected static <T extends NamedObject> T getSelectedEnvironment(T[] environments, String name,
			String missingMessageKey) throws ConfigException {
		return getSelectedEnvironment(environments, name, missingMessageKey, false);
	}
	
	protected static <T extends NamedObject> T getSelectedEnvironment(T[] environments, String name,
			String missingMessageKey, boolean matchPartial) throws ConfigException {
		
		if (StringUtils.isBlank(name)) {
			return null;
		}
		
		for (T dto : environments) {
			if ((matchPartial && dto.getName().startsWith(name))
				||name.equals(dto.getName())) {
				return dto;
			}
		}
		
		if (missingMessageKey != null) {
			throw new ConfigException(missingMessageKey, new String[] {name});
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
		
		if (previousBytesCounted < 0) {
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
