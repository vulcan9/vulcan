/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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
package net.sourceforge.vulcan.exception;

import java.util.Iterator;


public class ValidationException extends MessageFormatException implements Iterable<ValidationException> {
	private final String propertyName;
	
	private ValidationException next;
	
	public ValidationException(String propertyName, String key, Object... args) {
		super(key, args);
		this.propertyName = propertyName;
	}
	
	public void append(ValidationException ve) {
		if (next == null) {
			next = ve;
			return;
		}
		
		next.append(ve);
	}
	
	public Iterator<ValidationException> iterator() {
		return new Iterator<ValidationException>() {
			ValidationException cur = ValidationException.this;
			public boolean hasNext() {
				return (cur != null);
			}
			public ValidationException next() {
				final ValidationException tmp = cur;
				if (cur != null) {
					cur = cur.next;
				}
				
				return tmp;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public String getPropertyName() {
		return propertyName;
	}
}
