/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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
package net.sourceforge.vulcan.event;

import java.util.Date;

import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class BuildEvent implements Event {
	final transient Object source;
	final BuildDaemonInfoDto buildDaemonInfo;
	final ProjectConfigDto projectConfig;
	final ProjectStatusDto status;
	final Date date;
	
	public BuildEvent(final Object source,
			final BuildDaemonInfoDto buildDaemonInfo,
			final ProjectConfigDto projectConfig, final ProjectStatusDto status) {
		this.source = source;
		this.buildDaemonInfo = buildDaemonInfo;
		this.projectConfig = projectConfig;
		this.status = status;
		this.date = new Date();
	}
	
	public BuildDaemonInfoDto getBuildDaemonInfo() {
		return buildDaemonInfo;
	}
	public ProjectConfigDto getProjectConfig() {
		return projectConfig;
	}
	public Object getSource() {
		return source;
	}
	public ProjectStatusDto getStatus() {
		return status;
	}
	public Object[] getArgs() {
		return status.getMessageArgs();
	}
	public String getKey() {
		return status.getMessageKey();
	}
	public Date getDate() {
		return date;
	}
}
