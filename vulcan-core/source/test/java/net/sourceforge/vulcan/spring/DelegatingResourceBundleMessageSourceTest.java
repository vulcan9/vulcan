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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.vulcan.MockApplicationContext;
import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.springframework.beans.BeansException;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.io.FileSystemResourceLoader;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class DelegatingResourceBundleMessageSourceTest extends TestCase {
	private final class WacStub extends MockApplicationContext {
		@Override
		public boolean containsBeanDefinition(String name) {
			if (AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME.equals(name)) {
				return true;
			}
			return super.containsBean(name);
		}
		@Override
		public Object getBean(String name) throws BeansException {
			return this.getBean(name, null);
		}
		@Override
		@SuppressWarnings("unchecked")
		public Object getBean(String name, Class cls) throws BeansException {
			if (AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME.equals(name)) {
				return source;
			}
			return super.getBean(name);
		}
	}
	
	MockApplicationContext ctx = new WacStub();
	
	DelegatingResourceBundleMessageSource source = new DelegatingResourceBundleMessageSource();
	
	Map<String, String> msgs = new HashMap<String, String>();
	
	@Override
	protected void setUp() throws Exception {
		source.setResourceLoader(new FileSystemResourceLoader());
		source.setBasename(TestUtils.resolveRelativePath("source/test/pluginTests/goodPlugin2/goodPlugin2"));
		
		ctx.refresh();
	}
	public void testDefaultResolves() throws Exception {
		assertEquals("good afternoon.", source.getMessage("plugin.message", null, null));
	}
	public void testDefaultThrowsOnMissingKey() throws Exception {
		final String code = "no.such.message.code";
		try {
			source.getMessage(code, null, null);
			fail("expected exception");
		} catch (NoSuchMessageException e) {
		}
	}
	public void testParentFindsChildMessage() throws Exception {
		final MockApplicationContext child = new MockApplicationContext() {
			@Override
			public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
				if (msgs.containsKey(code)) {
					return msgs.get(code);
				}
				throw new NoSuchMessageException(code);
			};
		};
		final String badCode = "no.such.message.key";
		final String code = "child.message.key";
		try {
			ctx.getMessage(code, null, null);
			fail("expected exception");
		} catch (NoSuchMessageException e) {
		}

		msgs.put(code, "mock");
		
		source.addDelegate(child);
		
		assertEquals("mock", ctx.getMessage(code, null, null));
		
		try {
			ctx.getMessage(badCode, null, null);
			fail("expected exception");
		} catch (NoSuchMessageException e) {
		}
	}
	public void testChildResolvesParentMessage() throws Exception {
		source = new DelegatingResourceBundleMessageSource() {
			@Override
			protected String getMessageInternal(String code, Object[] args, Locale locale) {
				return "happy";
			}
		};
		final MockApplicationContext parent = new WacStub();
		final MockApplicationContext child = new MockApplicationContext();
		
		parent.refresh();
		
		child.setParent(parent);
		child.refresh();
		
		assertEquals("happy", parent.getMessage("happy", null, null));
		
		source.addDelegate(child);
		
		assertEquals("happy", child.getMessage("happy", null, null));
	}
	public void testNotFoundDoesNotRecurseIndefinately() throws Exception {
		final MockApplicationContext parent = new WacStub();
		final MockApplicationContext child = new MockApplicationContext();
		
		parent.refresh();
		
		child.setParent(parent);
		child.refresh();
		
		source.addDelegate(child);
		
		try {
			parent.getMessage("no.such.message", null, null);
			fail("expected NoSuchMessageException");
		} catch (NoSuchMessageException e) {
		} catch (StackOverflowError e) {
			fail("should not have thrown StackOverflowException");
		}
	}
	boolean useCodeAsDefaultMessage;
	public void testDisablesUseCodeAsMessageOptionInChildren() throws Exception {
		final AbstractMessageSource src = new AbstractMessageSource() {
			@Override
			public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
				super.setUseCodeAsDefaultMessage(useCodeAsDefaultMessage);
				DelegatingResourceBundleMessageSourceTest.this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
			}
			@Override
			protected String getMessageInternal(String code, Object[] args, Locale locale) {
				return null;
			}
			@Override
			protected MessageFormat resolveCode(String code, Locale locale) {
				return null;
			}
		};
		
		MockApplicationContext child = new MockApplicationContext() {
			@Override
			public boolean containsBeanDefinition(String name) {
				if (AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME.equals(name)) {
					return true;
				}
				return super.containsBean(name);
			}
			@Override
			public Object getBean(String name) throws BeansException {
				return this.getBean(name, null);
			}
			@Override
			@SuppressWarnings("unchecked")
			public Object getBean(String name, Class cls) throws BeansException {
				if (AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME.equals(name)) {
					return src;
				}
				return super.getBean(name);
			}
		};

		src.setUseCodeAsDefaultMessage(true);
		
		child.refresh();
		
		assertTrue(useCodeAsDefaultMessage);
		source.setUseCodeAsDefaultMessage(false);
		source.addDelegate(child);
		
		try {
			source.getMessage("no.such.message", null, null);
			fail("should have thrown exception");
		} catch (NoSuchMessageException e) {
		}
		
		assertFalse(useCodeAsDefaultMessage);
		
		source.setUseCodeAsDefaultMessage(true);
		assertEquals("no.such.message", source.getMessage("no.such.message", null, null));
		
		assertEquals("no.such.message", source.getMessageInternal("no.such.message", null, null));
	}
}
