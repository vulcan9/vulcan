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
package net.sourceforge.vulcan.dto;

import static org.apache.commons.lang.ArrayUtils.EMPTY_STRING_ARRAY;

import java.util.HashSet;
import java.util.Set;

import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.StringUtils;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ProjectConfigDto extends NameDto {
	public static enum UpdateStrategy {
		CleanAlways, CleanDaily, IncrementalAlways
	}
	
	String[] schedulerNames = EMPTY_STRING_ARRAY;
	
	String[] dependencies = EMPTY_STRING_ARRAY;
	
	String repositoryAdaptorPluginId;
	RepositoryAdaptorConfigDto repositoryAdaptorConfig;
	
	String buildToolPluginId;
	BuildToolConfigDto buildToolConfig;
	
	String workDir = StringUtils.EMPTY;
	String bugtraqUrl = StringUtils.EMPTY;
	String bugtraqLogRegex1 = StringUtils.EMPTY;
	String bugtraqLogRegex2 = StringUtils.EMPTY;
	
	boolean autoIncludeDependencies;
	boolean buildOnDependencyFailure;
	boolean buildOnNoUpdates;
	
	boolean suppressErrors;
	boolean suppressWarnings;
	
	java.util.Date lastModificationDate;
	
	UpdateStrategy updateStrategy = UpdateStrategy.CleanAlways;

	/*
	 * These properties are not meant to be configured with a project.
	 * They will not be persisted.
	 */
	String repositoryTagName;
	String requestedBy;
	boolean isScheduledBuild;
	
	int lockCount;
	String lockMessage;
	
	Set<String> labels = new HashSet<String>();
	
	public String getId() {
		return getName();
	}
	public String[] getSchedulerNames() {
		return schedulerNames;
	}
	public void setSchedulerNames(String[] schedulerNames) {
		this.schedulerNames = schedulerNames;
	}
	@Deprecated
	public void setSchedulerName(String scheduleName) {
		this.schedulerNames = new String[] {scheduleName};
	}
	public String getWorkDir() {
		return workDir;
	}
	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}
	public String[] getDependencies() {
		return dependencies;
	}
	public void setDependencies(String[] dependencies) {
		this.dependencies = dependencies;
	}
	public String getRepositoryAdaptorPluginId() {
		return repositoryAdaptorPluginId;
	}
	public void setRepositoryAdaptorPluginId(String repositoryAdaptorPluginId) {
		this.repositoryAdaptorPluginId = repositoryAdaptorPluginId;
	}
	public RepositoryAdaptorConfigDto getRepositoryAdaptorConfig() {
		return repositoryAdaptorConfig;
	}
	public void setRepositoryAdaptorConfig(RepositoryAdaptorConfigDto repositoryAdaptorConfig) {
		this.repositoryAdaptorConfig = repositoryAdaptorConfig;
	}
	public BuildToolConfigDto getBuildToolConfig() {
		return buildToolConfig;
	}
	public void setBuildToolConfig(BuildToolConfigDto buildToolConfig) {
		this.buildToolConfig = buildToolConfig;
	}
	public String getBuildToolPluginId() {
		return buildToolPluginId;
	}
	public void setBuildToolPluginId(String buildToolPluginId) {
		this.buildToolPluginId = buildToolPluginId;
	}
	public boolean isAutoIncludeDependencies() {
		return autoIncludeDependencies;
	}
	public void setAutoIncludeDependencies(boolean autoIncludeDependencies) {
		this.autoIncludeDependencies = autoIncludeDependencies;
	}
	public boolean isBuildOnDependencyFailure() {
		return buildOnDependencyFailure;
	}
	public void setBuildOnDependencyFailure(boolean buildOnDependencyFailure) {
		this.buildOnDependencyFailure = buildOnDependencyFailure;
	}
	public boolean isBuildOnNoUpdates() {
		return buildOnNoUpdates;
	}
	public void setBuildOnNoUpdates(boolean buildOnNoUpdates) {
		this.buildOnNoUpdates = buildOnNoUpdates;
	}
	@Deprecated
	public void setSitePath(String sitePath) {
	}
	public String getRepositoryTagName() {
		return repositoryTagName;
	}
	public void setRepositoryTagName(String repositoryTagName) {
		this.repositoryTagName = repositoryTagName;
	}
	public String getRequestedBy() {
		return requestedBy;
	}
	public void setRequestedBy(String requestedBy) {
		this.requestedBy = requestedBy;
	}
	public String getBugtraqLogRegex1() {
		return bugtraqLogRegex1;
	}
	public void setBugtraqLogRegex1(String bugtraqLogRegex1) {
		this.bugtraqLogRegex1 = bugtraqLogRegex1;
	}
	public String getBugtraqLogRegex2() {
		return bugtraqLogRegex2;
	}
	public void setBugtraqLogRegex2(String bugtraqLogRegex2) {
		this.bugtraqLogRegex2 = bugtraqLogRegex2;
	}
	@Deprecated
	public void setIssueTrackerUrl(String issueTrackerUrl) {
		this.bugtraqUrl = issueTrackerUrl;
	}
	public String getBugtraqUrl() {
		return bugtraqUrl;
	}
	public void setBugtraqUrl(String bugtraqUrl) {
		this.bugtraqUrl = bugtraqUrl;
	}
	public boolean isSuppressErrors() {
		return suppressErrors;
	}
	public void setSuppressErrors(boolean supressErrors) {
		this.suppressErrors = supressErrors;
	}
	public boolean isSuppressWarnings() {
		return suppressWarnings;
	}
	public void setSuppressWarnings(boolean supressWarnings) {
		this.suppressWarnings = supressWarnings;
	}
	public java.util.Date getLastModificationDate() {
		return lastModificationDate;
	}
	public void setLastModificationDate(java.util.Date lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}
	public UpdateStrategy getUpdateStrategy() {
		return updateStrategy;
	}
	public void setUpdateStrategy(UpdateStrategy updateStrategy) {
		this.updateStrategy = updateStrategy;
	}
	public boolean isScheduledBuild() {
		return isScheduledBuild;
	}
	public void setScheduledBuild(boolean isScheduledBuild) {
		this.isScheduledBuild = isScheduledBuild;
	}
	public Set<String> getLabels() {
		return labels;
	}
	public void setLabels(Set<String> labels) {
		this.labels = labels;
	}
	public boolean isLocked() {
		return lockCount > 0;
	}
	public int getLockCount() {
		return lockCount;
	}
	public void setLockCount(int lockCount)
	{
		this.lockCount = lockCount;
	}
	public String getLockMessage() {
		return lockMessage;
	}
	public void setLockMessage(String lockMessage) {
		this.lockMessage = lockMessage;
	}
}
