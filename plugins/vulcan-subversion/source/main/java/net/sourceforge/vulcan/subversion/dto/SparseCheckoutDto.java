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
package net.sourceforge.vulcan.subversion.dto;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.exception.ValidationException;

public class SparseCheckoutDto extends PluginConfigDto implements Comparable<SparseCheckoutDto> {
	private String directoryName = StringUtils.EMPTY;
	private CheckoutDepth checkoutDepth	= CheckoutDepth.Empty;
	
	public SparseCheckoutDto() {
	}
	
	public SparseCheckoutDto(String directoryName, CheckoutDepth depth) {
		setDirectoryName(directoryName);
		setCheckoutDepth(depth);
	}
	
	@Override
	public String getPluginId() {
		return SubversionConfigDto.PLUGIN_ID;
	}
	@Override
	public String getPluginName() {
		return SubversionConfigDto.PLUGIN_NAME;
	}

	@Override
	public String toString() {
		return String.format("%1s (%2s)", directoryName, checkoutDepth);
	}
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "checkoutDepth", "SparseCheckoutDto.checkoutDepth.name", "SparseCheckoutDto.checkoutDepth.description", locale);
		addProperty(pds, "directoryName", "SparseCheckoutDto.directoryName.name", "SparseCheckoutDto.directoryName.description", locale);
		
		return pds;
	}

	public String getDirectoryName() {
		return directoryName;
	}
	
	public void setDirectoryName(String directoryName) {
		this.directoryName = directoryName;
	}
	
	public CheckoutDepth getCheckoutDepth() {
		return checkoutDepth;
	}
	
	public void setCheckoutDepth(CheckoutDepth checkoutDepth) {
		this.checkoutDepth = checkoutDepth;
	}
	
	public int compareTo(SparseCheckoutDto o) {
		return directoryName.compareToIgnoreCase(o.directoryName);
	}
	
	@Override
	public void validate() throws ValidationException {
		super.validate();
		
		if (StringUtils.isBlank(directoryName)) {
			throw new ValidationException("directoryName", "errors.required");
		}
		
		directoryName = directoryName.replace('\\', '/');
		
		if (directoryName.startsWith("/")) {
			throw new ValidationException("directoryName", "svn.errors.directoryName");
		}
	}
}
