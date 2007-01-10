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
package net.sourceforge.vulcan.integration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.exception.ValidationException;
import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class PluginConfigStub extends RepositoryAdaptorConfigDto {
	private boolean validateCalled;
	private String value;
	private Boolean bool;
	private NestedObject obj = new NestedObject();
	private String password;
	
	public static class NestedObject {
		private String nestedValue;

		public String getNestedValue() {
			return nestedValue;
		}
		public void setNestedValue(String nestedValue) {
			this.nestedValue = nestedValue;
		}
	}
	@Override
	public String getPluginId() {
		return "mock";
	}
	@Override
	public String getPluginName() {
		return "Mock Plugin";
	}
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		try {
			final PropertyDescriptor pd1 = new PropertyDescriptor("value", PluginConfigStub.class);
			pd1.setShortDescription("the foo value");
			
			final PropertyDescriptor pd2 = new PropertyDescriptor("obj", PluginConfigStub.class);
			pd2.setDisplayName("Nested Object");
			
			final PropertyDescriptor pd3 = new PropertyDescriptor("bool", PluginConfigStub.class);
			
			final PropertyDescriptor pd4 = new PropertyDescriptor("password", PluginConfigStub.class);
			pd4.setValue(ATTR_WIDGET_TYPE, Widget.PASSWORD);
			
			return Arrays.asList(new PropertyDescriptor[] {pd1, pd2, pd3, pd4 });
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public NestedObject getObj() {
		return obj;
	}
	public void setObj(NestedObject obj) {
		this.obj = obj;
	}
	public Boolean getBool() {
		return bool;
	}
	public void setBool(Boolean bool) {
		this.bool = bool;
	}
	public boolean isValidateCalled() {
		return validateCalled;
	}
	@Override
	public void validate() throws ValidationException {
		validateCalled = true;
		final ValidationException e = new ValidationException("value", "fake.error.key", null);
		if ("bad".equals(value)) {
			throw e;
		} else if ("worse".equals(value)) {
			e.append(new ValidationException(null, "other.fake.error.key", null));
			throw e;
		}
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
}
