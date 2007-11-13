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

public class MetricDto extends BaseDto {
	public static enum MetricType {
		STRING('S'), PERCENT('P'), NUMBER('N');
		
		private final char id;

		MetricType(char id) {
			this.id = id;
		}
		
		public char getId() {
			return id;
		}
		
		public static MetricType fromId(char id) {
			for (MetricType type : MetricType.values()) {
				if (type.getId() == id) {
					return type;
				}
			}
			throw new IllegalArgumentException("No such MetricType '" + id + "'");
		}
	}
	
	private String messageKey;
	private String value;
	private MetricType type;
	
	public String getMessageKey() {
		return messageKey;
	}
	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public MetricType getType() {
		return type;
	}
	public void setType(MetricType type) {
		this.type = type;
	}
}
