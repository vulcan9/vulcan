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
package net.sourceforge.vulcan.core;

import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public interface ProjectBuilder {
	public static enum BuildPhase {
		CheckForUpdates("build.phase.check.updates"),
		CleanWorkingCopy("build.phase.clean"),
		CheckoutWorkingCopy("build.phase.checkout"),
		UpdateWorkingCopy("build.phase.update"),
		GetChangeLog("build.phase.changelog"),
		Build("build.phase.build"),
		Publish("build.phase.publish");
		
		private final String messageKey;

		BuildPhase(String messageKey) {
			this.messageKey = messageKey;
		}
		
		public String getMessageKey() {
			return messageKey;
		}
	}
	
	public void build(BuildDaemonInfoDto info, ProjectConfigDto projectConfig, BuildDetailCallback buildDetailCallback);

	public boolean isBuilding();
	public boolean isKilling();
	
	/**
	 * @param timeout if set, a watchdog is killing the build; otherwise a
	 * user requested it.
	 */
	public void abortCurrentBuild(boolean timeout, String requestUsername);
}
