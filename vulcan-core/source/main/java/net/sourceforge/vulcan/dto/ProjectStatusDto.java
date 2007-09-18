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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class ProjectStatusDto extends NameDto {
	/**
	 * First line of constants are possible build outcomes.  Following constants are for internal use only.
	 */
	public enum Status { PASS, FAIL, SKIP, ERROR,
						IN_QUEUE, BUILDING, UP_TO_DATE };
	
	public enum UpdateType { Full, Incremental };
	
	private String workDir;
	private Status status;
	private String messageKey;
	private Object[] messageArgs;
	private String buildReasonKey;
	private Object[] buildReasonArgs;
	private Date startDate;
	private Date completionDate;
	private RevisionTokenDto revision;
	private ChangeLogDto changeLog;

	private UUID id;
	private UUID diffId;
	private UUID buildLogId;
	
	private Map<String, UUID> dependencyIds = new HashMap<String, UUID>();
	
	private Integer buildNumber;
	private Integer lastGoodBuildNumber;
	
	/**
	 * If build outcome results in SKIP or ERROR,
	 * the current revision will not be fetched
	 * from the repository and therefore will
	 * be null.  In those cases this property
	 * carries the revision from previous builds
	 * forward.
	 */
	private RevisionTokenDto lastKnownRevision;
	
	private String tagName;
	private String repositoryUrl;
	
	private boolean isStatusChanged;
	
	private boolean isScheduledBuild;
	private String requestedBy;
	
	private UpdateType updateType = UpdateType.Full;
	
	private List<BuildMessageDto> errors;
	private List<BuildMessageDto> warnings;
	private List<MetricDto> metrics;
	private List<TestFailureDto> testFailures;
	
	public String getWorkDir() {
		return workDir;
	}
	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}
	public Integer getBuildNumber() {
		return buildNumber;
	}
	public void setBuildNumber(Integer buildNumber) {
		this.buildNumber = buildNumber;
	}
	public Integer getLastGoodBuildNumber() {
		return lastGoodBuildNumber;
	}
	public void setLastGoodBuildNumber(Integer lastGoodBuildNumber) {
		this.lastGoodBuildNumber = lastGoodBuildNumber;
	}
	public RevisionTokenDto getLastKnownRevision() {
		return lastKnownRevision;
	}
	public void setLastKnownRevision(RevisionTokenDto lastKnownRevision) {
		this.lastKnownRevision = lastKnownRevision;
	}
	public String getMessageKey() {
		return messageKey;
	}
	public void setMessageKey(String message) {
		this.messageKey = message;
	}
	public Object[] getMessageArgs() {
		return messageArgs;
	}
	public void setMessageArgs(Object[] messageArgs) {
		this.messageArgs = messageArgs;
	}
	public Object[] getBuildReasonArgs() {
		return buildReasonArgs;
	}
	public void setBuildReasonArgs(Object[] buildReasonArgs) {
		this.buildReasonArgs = buildReasonArgs;
	}
	public String getBuildReasonKey() {
		return buildReasonKey;
	}
	public void setBuildReasonKey(String buildReasonKey) {
		this.buildReasonKey = buildReasonKey;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getCompletionDate() {
		return completionDate;
	}
	public void setCompletionDate(Date completionDate) {
		this.completionDate = completionDate;
	}
	public RevisionTokenDto getRevision() {
		return revision;
	}
	public void setRevision(RevisionTokenDto revision) {
		this.revision = revision;
	}
	public Map<String, UUID> getDependencyIds() {
		return dependencyIds;
	}
	public void setDependencyIds(Map<String, UUID> dependencyIds) {
		this.dependencyIds = dependencyIds;
	}
	/**
	 * @deprecated Use dependencyIds instead
	 */
	@Deprecated
	public Map<String, RevisionTokenDto> getDependencyRevisions() {
		return null;
	}
	/**
	 * @deprecated Use dependencyIds instead
	 */
	@Deprecated
	public void setDependencyRevisions(
			Map<String, RevisionTokenDto> dependencyRevisions) {
	}
	public ChangeLogDto getChangeLog() {
		return changeLog;
	}
	public void setChangeLog(ChangeLogDto changeLog) {
		this.changeLog = changeLog;
	}
	public boolean isInQueue() {
		return Status.IN_QUEUE == status;
	}
	public boolean isBuilding() {
		return Status.BUILDING == status;
	}
	public boolean isFail() {
		return !(Status.PASS == status);
	}
	public boolean isPass() {
		return Status.PASS == status;
	}
	public boolean isSkip() {
		return Status.SKIP == status;
	}
	@Deprecated
	public void setLastGoodBuildCompletionDate(Date lastGoodBuildCompletionDate) {
	}
	@Deprecated
	public void setLastGoodBuildRevision(RevisionTokenDto lastGoodBuildRevision) {
	}
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public UUID getDiffId() {
		return diffId;
	}
	public void setDiffId(UUID diffId) {
		this.diffId = diffId;
	}
	public UUID getBuildLogId() {
		return buildLogId;
	}
	public void setBuildLogId(UUID buildLogId) {
		this.buildLogId = buildLogId;
	}
	public String getTagName() {
		return tagName;
	}
	public void setTagName(String tagName) {
		this.tagName = tagName;
	}
	public String getRepositoryUrl() {
		return repositoryUrl;
	}
	public void setRepositoryUrl(String repositoryUrl) {
		this.repositoryUrl = repositoryUrl;
	}
	public boolean isStatusChanged() {
		return isStatusChanged;
	}
	public void setStatusChanged(boolean isStatusChanged) {
		this.isStatusChanged = isStatusChanged;
	}
	public boolean isScheduledBuild() {
		return isScheduledBuild;
	}
	public void setScheduledBuild(boolean isScheduledBuild) {
		this.isScheduledBuild = isScheduledBuild;
	}
	public String getRequestedBy() {
		return requestedBy;
	}
	public void setRequestedBy(String requestedBy) {
		this.requestedBy = requestedBy;
	}
	public List<BuildMessageDto> getErrors() {
		return errors;
	}
	public void setErrors(List<BuildMessageDto> errors) {
		this.errors = errors;
	}
	public List<BuildMessageDto> getWarnings() {
		return warnings;
	}
	public void setWarnings(List<BuildMessageDto> warnings) {
		this.warnings = warnings;
	}
	public List<MetricDto> getMetrics() {
		return metrics;
	}
	public void setMetrics(List<MetricDto> metrics) {
		this.metrics = metrics;
	}
	public List<TestFailureDto> getTestFailures() {
		return testFailures;
	}
	public void setTestFailures(List<TestFailureDto> testFailures) {
		this.testFailures = testFailures;
	}
	public UpdateType getUpdateType() {
		return updateType;
	}
	public void setUpdateType(UpdateType updateType) {
		this.updateType = updateType;
	}
}
