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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.lang.reflect.Array;

import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class BaseDto implements Serializable, Cloneable, Copyable {
	protected transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
	}
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	public BaseDto copy() {
		try {
			final BaseDto dto = (BaseDto)super.clone();
			dto.propertyChangeSupport = new PropertyChangeSupport(dto);
			return dto;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("The world is flat.", e);
		}
	}
	@SuppressWarnings("unchecked")
	protected final <T extends BaseDto> T[] copyArray(T[] arr) {
		T[] copies = (T[]) Array.newInstance(arr.getClass().getComponentType(), arr.length);
		
		for (int i=0; i<arr.length; i++) {
			copies[i] = (T) arr[i].copy();
		}
		
		return copies;
	}
}
