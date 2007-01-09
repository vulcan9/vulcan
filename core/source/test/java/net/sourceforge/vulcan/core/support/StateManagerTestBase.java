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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.BuildManagerConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.scheduler.BuildDaemon;
import net.sourceforge.vulcan.scheduler.ProjectScheduler;


@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class StateManagerTestBase extends EasyMockTestCase
{
	StateManagerConfigDto stateManagerConfig = new StateManagerConfigDto();
	StoreStub store = new StoreStub(stateManagerConfig);
	
	StateManagerImpl stateMgr = new StateManagerImpl() {
		@Override
		protected BuildDaemon createBuildDaemon() {
			return null;
		}
		@Override
		protected ProjectScheduler createProjectScheduler() {
			return null;
		}
	};
	BuildManagerConfigDto buildManagerConfig;
	int buildMgrNameChangeCallCount = 0;
	int buildMgrClearCallCount = 0;
	
	PluginManager pluginMgr = createStrictMock(PluginManager.class);
	
	@Override
	public void setUp() {
		stateManagerConfig.setBuildManagerConfig(new BuildManagerConfigDto());
		
		stateMgr.setStore(store);
		stateMgr.setBuildManager(new BuildManager() {
			public void add(DependencyGroup dg) {
			}
			public ProjectConfigDto getTarget(BuildDaemonInfoDto buildDaemonInfo) {
				return null;
			}
			public void init(BuildManagerConfigDto mgrConfig) {
				buildManagerConfig = mgrConfig;
			}
			public void targetCompleted(BuildDaemonInfoDto info, ProjectConfigDto currentTarget, ProjectStatusDto buildStatus) {
			}
			public ProjectStatusDto getLatestStatus(String project) {
				return null;
			}
			public ProjectStatusDto getStatus(UUID statusId) {
				return null;
			}
			public ProjectStatusDto[] getPendingTargets() {
				return null;
			}
			public void clear() {
				buildMgrClearCallCount++;
			}
			public List<UUID> getAvailableStatusIds(String projectName) {
				return null;
			}
			public List<UUID> getAvailableStatusIdsInRange(Set<String> projectNames, Date begin, Date end) {
				return null;
			}
			public Map<String, BuildDaemonInfoDto> getProjectsBeingBuilt() {
				return null;
			}
			public Map<String, ProjectStatusDto> getProjectStatus() {
				return null;
			}
		});
		
		stateMgr.setPluginManager(pluginMgr);
		
		try {
			stateMgr.start();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
