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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sourceforge.vulcan.core.BuildOutcomeStore;
import net.sourceforge.vulcan.core.ConfigurationStore;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;
import net.sourceforge.vulcan.dto.PluginMetaDataDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class StoreStub implements ConfigurationStore, BuildOutcomeStore {
	StateManagerConfigDto stateManagerConfig;
	private int commitCount;
	
	public StoreStub(StateManagerConfigDto config) {
		stateManagerConfig = config;
	}
	public int getCommitCount() {
		return commitCount;
	}
	public boolean isCommitCalled() {
		return commitCount > 0;
	}
	public void commitCalled() {
		commitCount++;
	}
	public void clearCommitCount() {
		commitCount = 0;
	}
	public StateManagerConfigDto getStateManagerConfig() {
		return stateManagerConfig;
	}
	public void setStateManagerConfig(StateManagerConfigDto stateManagerConfig) {
		this.stateManagerConfig = stateManagerConfig;
	}
	public StateManagerConfigDto loadConfiguration() {
		return stateManagerConfig;
	}
	public void storeConfiguration(StateManagerConfigDto config) {
		commitCalled();
		stateManagerConfig = config;
	}
	public PluginMetaDataDto extractPlugin(InputStream is) throws StoreException {
		return null;
	}
	public String getWorkingCopyLocationPattern() {
		return null;
	}
	public void deletePlugin(String id) throws StoreException {
	}
	public PluginMetaDataDto[] getPluginConfigs() {
		return null;
	}
	public UUID storeBuildOutcome(ProjectStatusDto outcome) {
		final UUID id = UUID.randomUUID();
		outcome.setId(id);
		return id;
	}
	public ProjectStatusDto loadBuildOutcome(UUID id) {
		return null;
	}
	public List<ProjectStatusDto> loadBuildSummaries(BuildOutcomeQueryDto query) {
		return null;
	}
	public List<BuildMessageDto> loadTopBuildErrors(BuildOutcomeQueryDto query, int maxResultCount) {
		return null;
	}
	public List<TestFailureDto> loadTopTestFailures(BuildOutcomeQueryDto query,	int maxResultCount) {
		return null;
	}
	public Integer findMostRecentBuildNumberByWorkDir(String workDir) {
		return null;
	}
	public Map<String, List<UUID>> getBuildOutcomeIDs() {
		return new HashMap<String, List<UUID>>();
	}
	public File getBuildLog(String projectName, UUID diffId)
			throws StoreException {
		return null;
	}
	public File getChangeLog(String projectName, UUID diffId) throws StoreException {
		return null;
	}
	public void exportConfiguration(OutputStream os) {
	}
	public String getExportMimeType() {
		return null;
	}
	public void importConfiguration(InputStream is) throws StoreException, IOException {
	}
	public boolean isWorkingCopyLocationInvalid(String location) {
		return false;
	}
	public boolean buildLogExists(String projectName, UUID diffId) {
		return false;
	}
	public boolean diffExists(String projectName, UUID diffId) {
		return false;
	}
}