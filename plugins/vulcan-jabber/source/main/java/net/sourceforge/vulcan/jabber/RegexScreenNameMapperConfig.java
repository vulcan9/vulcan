/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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
package net.sourceforge.vulcan.jabber;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.exception.ValidationException;

public class RegexScreenNameMapperConfig extends PluginConfigDto {

	private String regex = "(.*)";
	private String replacement = "$1";
	
	@Override
	public String getPluginId() {
		return JabberPlugin.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return JabberPlugin.PLUGIN_NAME;
	}

	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();

		addProperty(pds, "regex", "RegexScreenNameMapperConfig.regex.name", "RegexScreenNameMapperConfig.regex.description", locale);
		addProperty(pds, "replacement", "RegexScreenNameMapperConfig.replacement.name", "RegexScreenNameMapperConfig.replacement.description", locale);
		
		return pds;
	}
	
	@Override
	public void validate() throws ValidationException {
		super.validate();
		
		try {
			Pattern.compile(regex);
		} catch (PatternSyntaxException e) {
			throw new ValidationException("regex", "jabber.validation.regex");
		}
	}
	
	@Override
	public String getHelpTopic() {
		return "JabberRegexScreenNameMapperConfig";
	}
	
	public String getRegex() {
		return regex;
	}
	
	public void setRegex(String regex) {
		this.regex = regex;
	}
	
	public String getReplacement() {
		return replacement;
	}
	
	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}
}
