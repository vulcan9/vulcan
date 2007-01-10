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
package net.sourceforge.vulcan.spring.mbean;

import javax.naming.NamingException;

import net.sourceforge.vulcan.metadata.SvnRevision;

import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.jndi.JndiObjectFactoryBean;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class MbeanServerLocator extends JndiObjectFactoryBean {
	Object mbeanServer;
	
	@Override
	public void afterPropertiesSet() throws NamingException {
		try {
			mbeanServer = JmxUtils.locateMBeanServer();
		} catch (MBeanServerNotFoundException e) {
			super.afterPropertiesSet();
		}
	}
	
	@Override
	public Class<?> getObjectType() {
		if (mbeanServer != null) {
			return mbeanServer.getClass();
		}
		return super.getObjectType();
	}
	
	@Override
	public Object getObject() {
		if (mbeanServer != null) {
			return mbeanServer;
		}
		return super.getObject();
	}
}

