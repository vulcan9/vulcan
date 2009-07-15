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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class PluginFactoryExpert implements SpringBeanXmlEncoder.FactoryExpert {
	private static final String CREATE_OBJECT = "createObject";
	private static final String CREATE_ENUM = "createEnum";
	
	private String pluginManagerBeanName;
	
	final Map<ClassLoader, String> classLoaders = new HashMap<ClassLoader, String>();
	
	public boolean needsFactory(Object bean) {
		if (classLoaders.containsKey(bean.getClass().getClassLoader())) {
			return true;
		}
		return false;
	}
	public String getFactoryBeanName(Object bean) {
		return pluginManagerBeanName;
	}
	public String getFactoryMethod(Object bean) {
		if (bean instanceof Enum<?>) {
			return CREATE_ENUM;
		}
		return CREATE_OBJECT;
	}
	public List<String> getConstructorArgs(Object bean) {
		final String id = classLoaders.get(bean.getClass().getClassLoader());

		if (id == null) {
			throw new IllegalStateException();
		}
		
		final List<String> args = new ArrayList<String>();
		
		args.add(id);
		args.add(bean.getClass().getName());
		
		if (bean instanceof Enum<?>) {
			args.add(((Enum<?>)bean).name());
		}
		return args;
	}
	public void registerPlugin(ClassLoader classLoader, String id) {
		classLoaders.put(classLoader, id);
	}
	public String getPluginManagerBeanName() {
		return pluginManagerBeanName;
	}
	public void setPluginManagerBeanName(String pluginManagerBeanName) {
		this.pluginManagerBeanName = pluginManagerBeanName;
	}
}
