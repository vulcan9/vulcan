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
package net.sourceforge.vulcan.spring.mbean;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.StoreException;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;


@ManagedResource(objectName="vulcan:name=Manager")
public final class VulcanManager {
	private StateManager stateManager;

	public StateManager getStateManager() {
		return stateManager;
	}

	public void setStateManager(StateManager stateManager) {
		this.stateManager = stateManager;
	}
	
	@ManagedOperation()
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="name", description=""),
		@ManagedOperationParameter(name="interval", description="")})
	public void createScheduler(String name, long interval) throws DuplicateNameException, StoreException {
		final SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName(name);
		config.setInterval(interval);
		
		stateManager.addSchedulerConfig(config);
	}
}
