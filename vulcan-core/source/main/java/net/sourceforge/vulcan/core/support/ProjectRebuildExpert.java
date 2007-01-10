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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.dto.Date;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto.UpdateStrategy;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SvnRevision(id = "$Id: ProjectBuilderTest.java 148 2006-12-03 21:50:35Z chris.eldredge $", url = "$HeadURL: https://vulcan.googlecode.com/svn/main/trunk/source/test/java/net/sourceforge/vulcan/core/support/ProjectBuilderTest.java $")
class ProjectRebuildExpert {
	private final Log log = LogFactory.getLog(getClass());
	private final WorkingCopyUpdateExpert workingCopyUpdateExpert;
	private final List<ChangeDetectionStrategy> strategies = new ArrayList<ChangeDetectionStrategy>();
	
	private BuildManager buildManager;
	private ProjectManager projectManager;
	
	ProjectRebuildExpert(WorkingCopyUpdateExpert workingCopyUpdateExpert) {
		this.workingCopyUpdateExpert = workingCopyUpdateExpert;
		
		strategies.add(new RevisionChangeDetectionStrategy());
		strategies.add(new DependencyTimestampChangeDetectionStrategy());
		strategies.add(new FullBuildAfterIncrementalBuildStrategy());
		strategies.add(new ProjectConfigurationChangeDetectionStrategy());
		strategies.add(new PluginConfigurationChangeDetectionStrategy());
		strategies.add(new BuildForcedDetectionStrategy());
	}
	
	boolean shouldBuild(ProjectConfigDto currentTarget, RevisionTokenDto currentRevision, ProjectStatusDto buildStatus, RevisionTokenDto previousRevision, ProjectStatusDto previousStatus) {
		for (ChangeDetectionStrategy strategy : strategies) {
			if (strategy.shouldBuild(currentTarget, currentRevision, buildStatus, previousRevision, previousStatus)) {
				return true;
			}
		}
		
		if (log.isInfoEnabled()) {
			log.info("Not building project '" + 
				currentTarget.getName() + "' because no updates are available.  " +
				"The project is at revision " + currentRevision + ".");
		}
		
		return false;
	}
	
	void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
	
	void setProjectManager(ProjectManager projectManager) {
		this.projectManager = projectManager;
	}
	
	interface ChangeDetectionStrategy {
		boolean shouldBuild(ProjectConfigDto currentTarget, RevisionTokenDto currentRevision,
				ProjectStatusDto buildStatus, RevisionTokenDto previousRevision,
				ProjectStatusDto previousStatus);
	}
	
	class RevisionChangeDetectionStrategy implements ChangeDetectionStrategy {
		public boolean shouldBuild(ProjectConfigDto currentTarget, RevisionTokenDto currentRevision, ProjectStatusDto buildStatus, RevisionTokenDto previousRevision, ProjectStatusDto previousStatus) {
			if (previousRevision != null && previousRevision.equals(currentRevision)) {
				return false;
			}

			if (log.isInfoEnabled()) {
				log.info("Building project '" + 
					currentTarget.getName() + "' because revision " + currentRevision +
					" is newer than revision " + previousRevision + ".");
			}
				
			buildStatus.setBuildReasonKey("messages.build.reason.repository.changes");
			buildStatus.setBuildReasonArgs(null);

			return true;
		}
	}
	
	class DependencyTimestampChangeDetectionStrategy implements ChangeDetectionStrategy {
		public boolean shouldBuild(ProjectConfigDto currentTarget, RevisionTokenDto currentRevision, ProjectStatusDto buildStatus, RevisionTokenDto previousRevision, ProjectStatusDto previousStatus) {
			final Map<String, UUID> dependencyIds = previousStatus.getDependencyIds();
			
			for (String depName : currentTarget.getDependencies()) {
				final ProjectStatusDto currentStatus = buildManager.getLatestStatus(depName);
				
				final UUID statusId = dependencyIds.get(depName);
				
				if (currentStatus == null) {
					log.warn("Unable to determine latest build revision of dependency '" + depName + "'");
					return true;
				}
				
				if (!currentStatus.getId().equals(statusId)) {
					if (log.isInfoEnabled()) {
						log.info("Building project '" + 
								currentTarget.getName() + "' because dependency '" +
								depName + "' has been built more recently.");
					}

					buildStatus.setBuildReasonKey("messages.build.reason.dependency");
					buildStatus.setBuildReasonArgs(new Object[] {depName});

					return true;
				}
			}
			
			return false;
		}
	}
	
	class ProjectConfigurationChangeDetectionStrategy implements ChangeDetectionStrategy {
		public boolean shouldBuild(ProjectConfigDto currentTarget, RevisionTokenDto currentRevision, ProjectStatusDto buildStatus, RevisionTokenDto previousRevision, ProjectStatusDto previousStatus) {
			final Date configDate = currentTarget.getLastModificationDate();
			
			if (configDate == null) {
				return false;
			}
			
			final Date lastBuild = previousStatus.getCompletionDate();
			
			if (configDate.after(lastBuild)) {
				if (log.isInfoEnabled()) {
					log.info("Buiding project '" + 
							currentTarget.getName() + "' because project configuration changed.");
				}

				buildStatus.setBuildReasonKey("messages.build.reason.project.config");
				buildStatus.setBuildReasonArgs(null);

				return true;
			}
			
			return false;
		}
	}
	
	class PluginConfigurationChangeDetectionStrategy implements ChangeDetectionStrategy {
		public boolean shouldBuild(ProjectConfigDto currentTarget, RevisionTokenDto currentRevision, ProjectStatusDto buildStatus, RevisionTokenDto previousRevision, ProjectStatusDto previousStatus) {
			final java.util.Date plugin1 = projectManager.getPluginModificationDate(currentTarget.getRepositoryAdaptorPluginId());
			final java.util.Date plugin2 = projectManager.getPluginModificationDate(currentTarget.getBuildToolPluginId());
			final Date lastBuild = previousStatus.getCompletionDate();
			
			final String name;
			
			if (plugin1 != null && plugin1.after(lastBuild)) {
				name = currentTarget.getRepositoryAdaptorConfig().getPluginName();
			} else if (plugin2 != null && plugin2.after(lastBuild)) {
				name = currentTarget.getBuildToolConfig().getPluginName();
			} else {
				name = null;
			}
			
			if (name != null) {
				if (log.isInfoEnabled()) {
					log.info("Buiding project '" + 
							currentTarget.getName() + "' because " +
							name + " plugin configuration changed.");
				}

				buildStatus.setBuildReasonKey("messages.build.reason.plugin.config");
				buildStatus.setBuildReasonArgs(new String[] {name});

				return true;

			}
			return false;
		}
	}
	class BuildForcedDetectionStrategy implements ChangeDetectionStrategy {
		public boolean shouldBuild(ProjectConfigDto currentTarget, RevisionTokenDto currentRevision, ProjectStatusDto buildStatus, RevisionTokenDto previousRevision, ProjectStatusDto previousStatus) {
			if (currentTarget.isBuildOnNoUpdates()) {
				if (log.isInfoEnabled()) {
					log.info("Buiding project '" + 
							currentTarget.getName() + "' even though no updates are available because it was forced by a user.");
				}

				buildStatus.setBuildReasonKey("messages.build.reason.forced");
				buildStatus.setBuildReasonArgs(null);

				return true;
			}
			
			return false;
		}
	}
	/**
	 * Strategy that will build when all of these criteria are met:
	 * <ul>
	 * 	<li>Previous build was incremental</li>
	 *  <li>Full build was requested, or this build would be the first daily build</li>
	 * </ul>  
	 */
	class FullBuildAfterIncrementalBuildStrategy implements ChangeDetectionStrategy {
		public boolean shouldBuild(ProjectConfigDto currentTarget, RevisionTokenDto currentRevision, ProjectStatusDto buildStatus, RevisionTokenDto previousRevision, ProjectStatusDto previousStatus) {
			final UpdateType previousUpdateType = previousStatus.getUpdateType();
			if (UpdateStrategy.CleanAlways == currentTarget.getUpdateStrategy() && UpdateType.Incremental == previousUpdateType) {
				log.info("Buiding project '" + 
						currentTarget.getName() + "' even though no updates are available because a full build was requested and the previous build was incremental.");
				
				buildStatus.setBuildReasonKey("messages.build.reason.full.after.incremental");
				buildStatus.setBuildReasonArgs(null);

				return true;
			} else if (previousUpdateType != UpdateType.Full &&
					workingCopyUpdateExpert.isDailyFullBuildRequired(currentTarget, previousStatus)) {
				
				log.info("Buiding project '" + 
						currentTarget.getName() + "' even though no updates are available because this is the first full build of the day.");
				
				buildStatus.setBuildReasonKey("messages.build.reason.full.after.incremental");
				buildStatus.setBuildReasonArgs(null);

				return true;
			}
			return false;
		}
	}

}
