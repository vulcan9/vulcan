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
	public static enum ProgressUnit {
		Bytes {
			@Override
			String getMessageKeyForScalar() {
				return "progress.bytes.scalar";
			}
			@Override
			String getMessageKeyForMoreThanPrevious() {
				return "progress.bytes.more.than.previous";
			}
			@Override
			String toUnits(long bytes) {
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
		},
		
		Files {
			@Override
			String getMessageKeyForScalar() {
				return "progress.files.scalar";
			}
			@Override
			String getMessageKeyForMoreThanPrevious() {
				return "progress.files.more.than.previous";
			}
			@Override
			String toUnits(long amount) {
				return Long.toString(amount);
			}
		};
		
		abstract String toUnits(long amount);

		abstract String getMessageKeyForScalar();

		abstract String getMessageKeyForMoreThanPrevious();
	}
	
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
	 * @param unitsCounted Bytes checked out so far.
	 * @param previousUnitsCounted Total bytes checked out during last build, or -1 if not available.
	 * @param progressUnit Units used to measure progress.
	 */
	public static void setWorkingCopyProgress(BuildDetailCallback buildDetailCallback, long unitsCounted, long previousUnitsCounted, ProgressUnit progressUnit)
	{
		if (buildDetailCallback == null) {
			return;
		}
		
		final String messageKey;
		
		final Object arg;
		
		if (previousUnitsCounted <= 0) {
			messageKey = progressUnit.getMessageKeyForScalar();
			arg = progressUnit.toUnits(unitsCounted);
		} else {
			double percent = (double)unitsCounted / previousUnitsCounted;
			
			if (percent > 1) {
				arg = progressUnit.toUnits(unitsCounted - previousUnitsCounted);
				messageKey = progressUnit.getMessageKeyForMoreThanPrevious();
			} else {
				messageKey = "progress.percent";
				arg = percent;
			}
		}
		
		buildDetailCallback.setDetailMessage(messageKey, new Object[] { arg });
	}
}
