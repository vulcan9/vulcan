/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto.UpdateStrategy;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class ProjectRebuildExpert {
	private final Log log = LogFactory.getLog(getClass());
	
	private final List<RebuildRule> rules = new ArrayList<RebuildRule>();
	
	private BuildManager buildManager;
	private ProjectManager projectManager;
	private WorkingCopyUpdateExpert workingCopyUpdateExpert;
	
	private String messageKey;
	private String[] messageArgs;

	ProjectRebuildExpert() {
		// Rules are in order of fastest to slowest.
		rules.add(new BuildForcedRule());
		rules.add(new FirstBuildRule());
		rules.add(new DependencyTimestampChangeRule());
		rules.add(new ProjectConfigurationChangeRule());
		rules.add(new PluginConfigurationChangeRule());
		rules.add(new FullBuildAfterIncrementalBuildRule());
		rules.add(new WorkingCopyNotPresentRule());
		rules.add(new WorkingCopyNotUsingSameTagRule());
		rules.add(new IncomingChangesRule());
	}
	
	public boolean shouldBuild(ProjectConfigDto project, ProjectStatusDto previousStatus) throws ConfigException {
		for (RebuildRule rule : rules) {
			if (rule.isSatisfiedBy(project, previousStatus)) {
				return true;
			}
		}
		
		if (log.isInfoEnabled()) {
			log.info("Not building project '" + 
				project.getName() + "' because no updates are available.  " +
				"The project is at revision " + previousStatus.getRevision() + ".");
		}
		
		return false;
	}
	
	public String getMessageKey() {
		return messageKey;
	}
	
	public String[] getMessageArgs() {
		return messageArgs;
	}

	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
	
	public void setProjectManager(ProjectManager projectManager) {
		this.projectManager = projectManager;
	}
	
	public void setWorkingCopyUpdateExpert(WorkingCopyUpdateExpert workingCopyUpdateExpert) {
		this.workingCopyUpdateExpert = workingCopyUpdateExpert;
	}
	
	abstract class RebuildRule {
		public abstract boolean isSatisfiedBy(ProjectConfigDto project, ProjectStatusDto previousStatus) throws ConfigException;
		
		protected void setBuildReason(String messageKey, String... args) {
			ProjectRebuildExpert.this.messageKey = messageKey;
			ProjectRebuildExpert.this.messageArgs = args;
		}
	}
	
	class WorkingCopyNotUsingSameTagRule extends RebuildRule {
		@Override
		public boolean isSatisfiedBy(ProjectConfigDto project, ProjectStatusDto previousStatus) throws ConfigException {
			ProjectStatusDto mostRecentBuildByWorkDir = previousStatus;
			
			if (!project.getWorkDir().equals(previousStatus.getWorkDir())) {
				mostRecentBuildByWorkDir = buildManager.getMostRecentBuildByWorkDir(project.getName(), project.getWorkDir());
			}
			
			String tagName = project.getRepositoryTagName();
			
			if (tagName == null) {
				final RepositoryAdaptor ra = projectManager.getRepositoryAdaptor(project);
				tagName = ra.getTagOrBranch();
			}
			
			if (mostRecentBuildByWorkDir == null || tagName.equals(mostRecentBuildByWorkDir.getTagName())) {
				return false;
			}
			
			if (log.isInfoEnabled()) {
				String msg = MessageFormat.format("Building project {0} because last build in directory {1} used tag {2} and this build uses tag {3}.",
						project.getName(), project.getWorkDir(), tagName, previousStatus.getTagName());
				
				log.info(msg);
			}
			
			setBuildReason("messages.build.reason.different.tag", project.getWorkDir(), tagName, mostRecentBuildByWorkDir.getTagName());
			
			return true;
		}
	}
	/**
	 * Causes the project to rebuild when the working copy is out of date with the repository
	 */
	class IncomingChangesRule extends RebuildRule {
		public boolean isSatisfiedBy(ProjectConfigDto project, ProjectStatusDto previousStatus) throws ConfigException {
			ProjectStatusDto mostRecentBuildByWorkDir = previousStatus;
			
			if (!project.getWorkDir().equals(previousStatus.getWorkDir())) {
				mostRecentBuildByWorkDir = buildManager.getMostRecentBuildByWorkDir(project.getName(), project.getWorkDir());
			}
			
			RevisionTokenDto previousRevision = mostRecentBuildByWorkDir == null ? null : mostRecentBuildByWorkDir.getRevision();
			
			final RepositoryAdaptor repository = projectManager.getRepositoryAdaptor(project);
			
			if (!repository.hasIncomingChanges(mostRecentBuildByWorkDir)) {
				return false;
			}

			if (log.isInfoEnabled()) {
				log.info("Building project '" + 
					project.getName() + "' because changes were detected since " + previousRevision + ".");
			}
			
			setBuildReason("messages.build.reason.repository.changes");

			return true;
		}
	}
	
	class WorkingCopyNotPresentRule extends RebuildRule {
		public boolean isSatisfiedBy(ProjectConfigDto project, ProjectStatusDto previousStatus) throws ConfigException {
			if (!new File(project.getWorkDir()).exists()) {
				if (log.isInfoEnabled()) {
					log.info("Building project '" + 
						project.getName() + "' because working copy " + project.getWorkDir() + " does not exist.");
				}
				
				setBuildReason("messages.build.reason.missing.working.copy");
				return true;
			}
			return false;
		}
	}

	/**
	 * Causes the project to rebuild when a dependency is newer than the last build of this project. 
	 */
	class DependencyTimestampChangeRule extends RebuildRule {
		public boolean isSatisfiedBy(ProjectConfigDto project, ProjectStatusDto previousStatus) {
			final Map<String, UUID> dependencyIds = previousStatus.getDependencyIds();
			
			for (String depName : project.getDependencies()) {
				final ProjectStatusDto currentStatus = buildManager.getLatestStatus(depName);
				
				final UUID statusId = dependencyIds.get(depName);
				
				if (currentStatus == null) {
					log.warn("Unable to determine latest build revision of dependency '" + depName + "'");
					return true;
				}
				
				if (!currentStatus.getId().equals(statusId)) {
					if (log.isInfoEnabled()) {
						log.info("Building project '" + 
								project.getName() + "' because dependency '" +
								depName + "' has been built more recently.");
					}

					setBuildReason("messages.build.reason.dependency", depName);

					return true;
				}
			}
			
			return false;
		}
	}
	
	/**
	 * Causes the project to rebuild when project configuration changed
	 */
	class ProjectConfigurationChangeRule extends RebuildRule {
		public boolean isSatisfiedBy(ProjectConfigDto project, ProjectStatusDto previousStatus) {
			final Date configDate = project.getLastModificationDate();
			
			if (configDate == null) {
				return false;
			}
			
			final Date lastBuild = previousStatus.getCompletionDate();
			
			if (configDate.after(lastBuild)) {
				if (log.isInfoEnabled()) {
					log.info("Buiding project '" + 
							project.getName() + "' because project configuration changed.");
				}

				setBuildReason("messages.build.reason.project.config");

				return true;
			}
			
			return false;
		}
	}
	
	/**
	 * Causes the project to rebuild when plugin configuration changed.
	 */
	class PluginConfigurationChangeRule extends RebuildRule {
		public boolean isSatisfiedBy(ProjectConfigDto project, ProjectStatusDto previousStatus) {
			final java.util.Date plugin1 = projectManager.getPluginModificationDate(project.getRepositoryAdaptorPluginId());
			final java.util.Date plugin2 = projectManager.getPluginModificationDate(project.getBuildToolPluginId());
			final Date lastBuild = previousStatus.getCompletionDate();
			
			final String name;
			
			if (plugin1 != null && plugin1.after(lastBuild)) {
				name = project.getRepositoryAdaptorConfig().getPluginName();
			} else if (plugin2 != null && plugin2.after(lastBuild)) {
				name = project.getBuildToolConfig().getPluginName();
			} else {
				name = null;
			}
			
			if (name != null) {
				if (log.isInfoEnabled()) {
					log.info("Buiding project '" + 
							project.getName() + "' because " +
							name + " plugin configuration changed.");
				}

				setBuildReason("messages.build.reason.plugin.config", name);

				return true;

			}
			return false;
		}
	}
	
	class BuildForcedRule extends RebuildRule {
		public boolean isSatisfiedBy(ProjectConfigDto project, ProjectStatusDto previousStatus) {
			if (project.isBuildOnNoUpdates()) {
				if (log.isInfoEnabled()) {
					log.info("Buiding project '" + 
							project.getName() + "' because 'force build' flag is set.");
				}

				setBuildReason("messages.build.reason.forced");

				return true;
			}
			
			return false;
		}
	}
	
	class FirstBuildRule extends RebuildRule {
		public boolean isSatisfiedBy(ProjectConfigDto project, ProjectStatusDto previousStatus) {
			if (previousStatus == null) {
				if (log.isInfoEnabled()) {
					log.info("Buiding project '" + 
							project.getName() + "' because no previous build exists.");
				}

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
	class FullBuildAfterIncrementalBuildRule extends RebuildRule {
		public boolean isSatisfiedBy(ProjectConfigDto project, ProjectStatusDto previousStatus) {
			final UpdateType previousUpdateType = previousStatus.getUpdateType();
			if (UpdateStrategy.CleanAlways == project.getUpdateStrategy() && UpdateType.Incremental == previousUpdateType) {
				log.info("Buiding project '" + 
						project.getName() + "' even though no updates are available because a full build was requested and the previous build was incremental.");
				
				setBuildReason("messages.build.reason.full.after.incremental");

				return true;
			} else if (previousUpdateType != UpdateType.Full &&
					workingCopyUpdateExpert.isDailyFullBuildRequired(project, previousStatus)) {
				
				log.info("Buiding project '" + 
						project.getName() + "' even though no updates are available because this is the first full build of the day.");
				
				setBuildReason("messages.build.reason.full.after.incremental");

				return true;
			}
			return false;
		}
	}

}
