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
package net.sourceforge.vulcan.dto;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.sourceforge.vulcan.exception.ValidationException;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public abstract class PluginConfigDto extends BaseDto implements ApplicationContextAware {
	public static String ATTR_CHOICE_TYPE = "net.sourceforge.vulcan.integration.CHOICE_TYPE";
	public static String ATTR_AVAILABLE_CHOICES = "net.sourceforge.vulcan.integration.CHOICES";
	public static String ATTR_WIDGET_TYPE = "net.sourceforge.vulcan.integration.WIDGET_TYPE";

	public static enum Widget { PASSWORD, DROPDOWN, TEXTAREA };
	
	protected ApplicationContext applicationContext;
	
	private java.util.Date lastModificationDate;
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	public abstract String getPluginId();
	public abstract String getPluginName();
	
	/**
	 * Retrieves a PropertyDescriptor for each property that may be modified
	 * with a user interface.  The properties will be displayed in the order
	 * of the array.
	 * 
	 * @param locale the locale for the user interface that will display
	 * the properties.
	 */
	public abstract List<PropertyDescriptor> getPropertyDescriptors(Locale locale);
	
	/**
	 * Subclasses should override this method to provide custom validation.
	 */
	public void validate() throws ValidationException {
	}

	/**
	 * Subclasses should override this method to provide a URL to link
	 * other than the standard Vulcan help.
	 * 
	 * @return null if standard documentation should be used.
	 */
	public String getHelpUrl() {
		return null;
	}
	
	/**
	 * Subclasses should override this method to provide a relative URI
	 * which will be appended to either the url provided by getHelpUrl()
	 * or the standard documentation url.
	 * 
	 * @return null if no specific help is available.
	 */
	public String getHelpTopic() {
		return null;
	}
	
	public final java.util.Date getLastModificationDate() {
		return lastModificationDate;
	}
	
	public final void setLastModificationDate(java.util.Date lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}
	
	protected final void addProperty(List<PropertyDescriptor> pds, String name, String labelKey, String shortDescriptionKey, Locale locale) {
		addProperty(pds, name, labelKey, shortDescriptionKey, locale, null);
	}

	protected final void addProperty(List<PropertyDescriptor> pds, String name, String labelKey, String shortDescriptionKey, Locale locale, Map<String, ? extends Object> attrs) {
		try {
			final PropertyDescriptor pd = new PropertyDescriptor(name, getClass());
			pd.setDisplayName(applicationContext.getMessage(labelKey, null, locale));
			pd.setShortDescription(applicationContext.getMessage(shortDescriptionKey, null, locale));
			if (attrs != null) {
				for (String key : attrs.keySet()) {
					pd.setValue(key, attrs.get(key));
				}
			}
			pds.add(pd);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected final <T> T getPlugin(Class<T> type) {
		return (T) applicationContext.getBean("plugin", type);
	}
}
