/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2010 Chris Eldredge
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
package net.sourceforge.vulcan.core;

import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public interface ProjectBuilder {
	public void build(BuildDaemonInfoDto info, BuildTarget currentTarget, BuildDetailCallback buildDetailCallback);

	public boolean isBuilding();
	public boolean isKilling();
	
	/**
	 * @param timeout if set, a watchdog is killing the build; otherwise a
	 * user requested it.
	 */
	public void abortCurrentBuild(boolean timeout, String requestUsername);
	
	public void addBuildStatusListener(BuildStatusListener listener);
	public boolean removeBuildStatusListener(BuildStatusListener listener);
}
