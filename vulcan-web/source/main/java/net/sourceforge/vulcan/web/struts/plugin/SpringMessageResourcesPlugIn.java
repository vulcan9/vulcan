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
package net.sourceforge.vulcan.web.struts.plugin;

import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;


import org.apache.struts.Globals;
import org.apache.struts.util.MessageResources;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.struts.ContextLoaderPlugIn;

public class SpringMessageResourcesPlugIn extends ContextLoaderPlugIn {
	@Override
	protected void onInit() throws ServletException {
		final WebApplicationContext wac = getWacInternal();
		
		final ServletContext context = getServletContextInternal();
		
		if (context.getAttribute(Globals.MESSAGES_KEY) != null) {
			logger.warn("Overriding default MessageResources");
		}
		
		MessageResources messageResources = new MessageResources(null, null) {
			@Override
			public String getMessage(Locale locale, String key) {
				return wac.getMessage(key, null, locale);
			}
		};
		
		context.setAttribute(Globals.MESSAGES_KEY, messageResources);
	}

	protected ServletContext getServletContextInternal() {
		return getServletContext();
	}

	protected WebApplicationContext getWacInternal() {
		return getWebApplicationContext();
	}
}

