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
package net.sourceforge.vulcan.web.struts.actions;

import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public final class ManageSchedulerConfigAction extends SchedulerConfigBaseAction {
	@Override
	protected SchedulerConfigDto getConfig(String name) {
		return stateManager.getSchedulerConfig(name);
	}

	@Override
	protected void addScheduler(SchedulerConfigDto config) throws DuplicateNameException, StoreException {
		stateManager.addSchedulerConfig(config);
	}

	@Override
	protected void updateSchedulerConfig(String originalName,
			SchedulerConfigDto config) throws DuplicateNameException, StoreException {
		stateManager.updateSchedulerConfig(originalName, config, true);
	}
	
	@Override
	protected void deleteSchedulerConfig(String name) {
		stateManager.deleteSchedulerConfig(name);
	}
}
