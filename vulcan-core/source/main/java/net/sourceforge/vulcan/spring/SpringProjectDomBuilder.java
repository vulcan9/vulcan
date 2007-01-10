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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;

import net.sourceforge.vulcan.core.support.AbstractProjectDomBuilder;
import net.sourceforge.vulcan.exception.NoSuchTransformFormatException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;

@SvnRevision(id="$Id", url="$HeadURL$")
public class SpringProjectDomBuilder extends AbstractProjectDomBuilder implements ApplicationContextAware {
	private ApplicationContext applicationContext;
	private Map<String, String> transformResources = Collections.emptyMap();
	private TransformerFactory transformerFactory;

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	public void setTransformerFactory(TransformerFactory transformerFactory) {
		this.transformerFactory = transformerFactory;
	}
	public void setTransformResources(Map<String, String> transformResources) {
		this.transformResources = transformResources;
	}
	@Override
	protected String formatMessage(String key, Object[] args, Locale locale) {
		return applicationContext.getMessage(key, args, locale);
	}

	@Override
	protected Transformer createTransformer(String format) throws NoSuchTransformFormatException {
		if (!transformResources.containsKey(format)) {
			throw new NoSuchTransformFormatException();
		}
		
		final Resource resource = applicationContext.getResource(transformResources.get(format));
		
		try {
			final SAXSource source = new SAXSource(
					XMLReaderFactory.createXMLReader(),
					new InputSource(resource.getInputStream()));
			
			return transformerFactory.newTransformer(source);
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		}
		
		
	}
}
