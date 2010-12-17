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
package net.sourceforge.vulcan.spring;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;


public class DelegatingResourceBundleMessageSource extends ReloadableResourceBundleMessageSource {
	private Set<ApplicationContext> delegates = new HashSet<ApplicationContext>();
	
	private boolean recurseFlag;
	private boolean useCodeAsDefaultMessage;
	
	public void addDelegate(ApplicationContext context) {
		if (context.containsBean(AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME)) {
			MessageSource msgSrc = (MessageSource) context.getBean(AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME);
			if (msgSrc instanceof AbstractMessageSource) {
				final AbstractMessageSource abstractMessageSource = (AbstractMessageSource)msgSrc;
				abstractMessageSource.setUseCodeAsDefaultMessage(false);
			}
		}

		delegates.add(context);
	}

	public boolean removeDelegate(ApplicationContext context) {
		return delegates.remove(context);
	}

	@Override
	public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
		super.setUseCodeAsDefaultMessage(useCodeAsDefaultMessage);
		this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
	}
	@Override
	protected String getMessageInternal(String code, Object[] args, Locale locale) {
		String message = super.getMessageInternal(code, args, locale);
		
		if (message != null) {
			return message;
		}

		synchronized (this) {
			if (recurseFlag) {
				return null;
			}
			
			try {		
				recurseFlag = true;
				
				for (ApplicationContext ctx : delegates) {
					try {
						return ctx.getMessage(code, args, locale);
					} catch (NoSuchMessageException e) {
					}
				}
				if (useCodeAsDefaultMessage) {
					return code;
				}
				return null;
			} finally {
				recurseFlag = false;
			}
		}
	}
}
