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

import junit.framework.TestCase;

import net.sourceforge.vulcan.metadata.SvnRevision;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class AppCtxTest extends TestCase {
	GenericApplicationContext ac = new GenericApplicationContext();
	ApplicationEvent evt = new ContextRefreshedEvent(ac);
	static ApplicationEvent received;
	
	public static class Listener implements ApplicationListener {
		public void onApplicationEvent(ApplicationEvent arg0) {
			received = arg0;
		}
	}
	
	@Override
	public void setUp() {
		BeanDefinition bd = new RootBeanDefinition(Listener.class);
		
		ac.registerBeanDefinition("listener", bd);
		ac.refresh();

		received = null;
	}
	public void testReceivesInSame() throws Exception {
		assertNotSame(evt, received);
		
		ac.publishEvent(evt);
		
		assertSame(evt, received);
	}
	public void testParentReceivesFromChild() throws Exception {
		GenericApplicationContext child = new GenericApplicationContext();
		child.setParent(this.ac);
		child.refresh();
		
		assertNotSame(evt, received);
		
		child.publishEvent(evt);
		
		assertSame(evt, received);
	}
	public void testChildDoesNotReceiveFromParent() throws Exception {
		GenericApplicationContext parent = new GenericApplicationContext();
		parent.refresh();
		ac.setParent(parent);
		
		assertNotSame(evt, received);
		
		parent.publishEvent(evt);
		
		assertNotSame(evt, received);
	}
}
