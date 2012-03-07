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
package net.sourceforge.vulcan.spring;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.integration.PluginStub;


public class PluginFactoryExpertTest extends TestCase {
	PluginFactoryExpert expert = new PluginFactoryExpert();
	
	String id;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		expert.setPluginManagerBeanName("sprock");
	}
	
	public void testNotNeedsFactory() throws Exception {
		assertFalse(expert.needsFactory(new Object()));
	}
	
	public void testNeedsFactory() throws Exception {
		id = "a";
		
		assertFalse(expert.needsFactory(Object.class));
		
		expert.registerPlugin(Object.class.getClassLoader(), "a");
		assertTrue(expert.needsFactory(Object.class));
	}
	
	public void testGetPluginManagerBeanName() throws Exception {
		assertEquals("sprock", expert.getFactoryBeanName(null));
	}
	
	public void testGetCtrArgsThrowsOnNoId() throws Exception {
		try {
			expert.getConstructorArgs(new Object());
			fail("expected exception");
		} catch (IllegalStateException e) {
		}
	}
	
	public void testGetCtrArgs() throws Exception {
		id = "mock";
		
		expert.registerPlugin(PluginStub.class.getClassLoader(), "mock");
		final PluginConfigDto cfg = new PluginStub();
		
		assertEquals("createObject", expert.getFactoryMethod(cfg));
		
		final List<String> elems = expert.getConstructorArgs(cfg);
		assertEquals(2, elems.size());
		
		assertEquals("mock", elems.get(0));
		assertEquals(PluginStub.class.getName(), elems.get(1));
	}
	
	public static enum Foo { A, B {} };
	
	public void testGetFactoryMethod() throws Exception {
		id = "clock";
		expert.registerPlugin(PluginStub.class.getClassLoader(), id);
		final PluginConfigDto cfg = new PluginStub();
		expert.getConstructorArgs(cfg);
		
		assertEquals("createEnum", expert.getFactoryMethod(Foo.A));

		final List<String> elems = expert.getConstructorArgs(Foo.A);
		assertEquals(3, elems.size());
		
		assertEquals("clock", elems.get(0));
		assertEquals(Foo.class.getName(), elems.get(1));
		assertEquals("A", elems.get(2));
	}
	
	
	public void testUsesDeclaringClassForEnum() throws Exception {
		id = "b";
		
		expert.registerPlugin(Foo.class.getClassLoader(), id);
		
		assertEquals(Arrays.asList("b", Foo.class.getName(), Foo.B.name()), expert.getConstructorArgs(Foo.B));
	}

}
