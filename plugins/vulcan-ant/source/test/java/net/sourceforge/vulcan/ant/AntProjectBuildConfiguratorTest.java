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
package net.sourceforge.vulcan.ant;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.ProjectConfigDto;

public class AntProjectBuildConfiguratorTest extends TestCase {
	AntProjectBuildConfigurator cfgr;
	ProjectConfigDto config = new ProjectConfigDto();
	
	public void testBasic() throws Exception {
		cfgr = new AntProjectBuildConfigurator(null, "project name", null);
		
		cfgr.applyConfiguration(config, null, null, false);
		
		assertEquals("project name", config.getName());
		assertNotNull(config.getBuildToolConfig());
	}

	public void testUsesSpecifiedFilename() throws Exception {
		cfgr = new AntProjectBuildConfigurator(null, "project name", ".");
		
		cfgr.applyConfiguration(config, "ant.xml", null, false);

		AntProjectConfig buildConfig = (AntProjectConfig) config.getBuildToolConfig();
		assertEquals("ant.xml", buildConfig.getBuildScript());
	}
}
