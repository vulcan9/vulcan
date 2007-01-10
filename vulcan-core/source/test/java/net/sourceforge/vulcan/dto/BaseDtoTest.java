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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import net.sourceforge.vulcan.dto.BaseDto;
import net.sourceforge.vulcan.metadata.SvnRevision;


import junit.framework.TestCase;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class BaseDtoTest extends TestCase {
	static class Dto extends BaseDto {
		String value;
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
			propertyChangeSupport.firePropertyChange("value", null, value);
		}
	}
	
	boolean called;
	public void testPropertyChangeSupport() {
		Dto dto = new Dto();
		
		dto.addPropertyChangeListener("value", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				called = true;
			}
		});
		
		assertFalse(called);
		dto.setValue("a");
		assertTrue(called);
	}
	public void testPropertyChangeSupportWrongName() {
		Dto dto = new Dto();
		
		dto.addPropertyChangeListener("other", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				called = true;
			}
		});
		
		assertFalse(called);
		dto.setValue("a");
		assertFalse(called);
	}
	public void testCloneClearsListeners() {
		Dto dto = new Dto();
		
		dto.addPropertyChangeListener("value", new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				called = true;
			}
		});
		
		assertFalse(called);
		dto.setValue("a");
		assertTrue(called);
		
		Dto other = (Dto) dto.copy();
		
		called = false;
		other.setValue("b");
		assertFalse(called);
		
		assertNotNull(other.propertyChangeSupport);
		
		dto.setValue("c");
		assertTrue(called);
	}
}
