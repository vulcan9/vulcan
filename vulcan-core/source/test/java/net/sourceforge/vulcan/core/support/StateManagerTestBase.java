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
package net.sourceforge.vulcan.core.support;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.BuildManagerConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventHandler;
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
		
		stateMgr.setConfigurationStore(store);
		stateMgr.setBuildManager(new BuildManager() {
			public void add(DependencyGroup dg) {
			}
			public ProjectConfigDto getTarget(BuildDaemonInfoDto buildDaemonInfo) {
				return null;
			}
			public void registerBuildStatus(BuildDaemonInfoDto info, ProjectBuilder builder, ProjectConfigDto currentTarget, ProjectStatusDto buildStatus) {
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
			public ProjectStatusDto getStatusByBuildNumber(String projectName, int buildNumber) {
				return null;
			}
			public ProjectStatusDto[] getPendingTargets() {
				return null;
			}
			public Integer getMostRecentBuildNumberByWorkDir(String workDir) {
				return null;
			}
			public void clear() {
				buildMgrClearCallCount++;
			}
			public List<UUID> getAvailableStatusIds(String projectName) {
				return null;
			}
			public Map<String, ProjectStatusDto> getProjectsBeingBuilt() {
				return null;
			}
			public Map<String, ProjectStatusDto> getProjectStatus() {
				return null;
			}
			public boolean isBuildingOrInQueue(String... originalName) {
				return false;
			}
			public ProjectBuilder getProjectBuilder(String projectName) {
				return null;
			}
			public boolean claimBrokenBuild(String projectName,
					int buildNumber, String claimUser) {
				return false;
			}
		});
		
		stateMgr.setEventHandler(new EventHandler() {
			public void reportEvent(Event event) {
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
