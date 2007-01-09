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
package net.sourceforge.vulcan.core.support;

import java.io.File;

import junit.framework.TestCase;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class SpringApplicationContextTest extends TestCase {
	FileSystemXmlApplicationContext ctx;
	StateManagerImpl mgr;
	
	@Override
	protected void setUp() throws Exception {
		ctx =
			new FileSystemXmlApplicationContext("WEB-INF/beans.xml") {
				@Override
				public Resource getResource(String location) {
					return new FileSystemResource(new File("source/main/docroot", location));
				}
			};
		
		mgr = (StateManagerImpl) ctx.getBean("stateManager");
	}
	
	@Override
	protected void tearDown() throws Exception {
		StdSchedulerFactory.getDefaultScheduler().shutdown();
		super.tearDown();
	}
	public void testFactoryCreatesScheduler() {
		assertNotNull(mgr.createProjectScheduler());
		assertNotSame(mgr.createProjectScheduler(), mgr.createProjectScheduler());
	}

	public void testFactoryCreatesBuildDaemon() {
		assertNotNull(mgr.createBuildDaemon());
		assertNotSame(mgr.createBuildDaemon(), mgr.createBuildDaemon());
	}
	
	public void testPluginManagerSingleton() {
		assertSame(mgr.pluginManager, ctx.getBean("pluginManager"));
	}
}
