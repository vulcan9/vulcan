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
package net.sourceforge.vulcan.web.struts.forms;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.vulcan.core.DependencyBuildPolicy;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.core.WorkingCopyUpdateStrategy;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;


public class ManualBuildForm extends ValidatorForm {
	private String[] targets;
	private String dependencies;
	private boolean buildOnDependencyFailure;
	private boolean buildOnNoUpdates;
	
	private String updateStrategy = WorkingCopyUpdateStrategy.Default.name();
	private WorkingCopyUpdateStrategy workingCopyUpdateStrategy;
	
	private boolean chooseTags;
	private List<String> projectNames = Collections.emptyList();
	private List<List<RepositoryTagDto>> availableTags = Collections.emptyList();
	private String[] selectedTags;
	private String[] workDirOverrides;
	private transient DependencyGroup dependencyGroup;
	
	public ManualBuildForm() {
		reset(null, null);
	}
	public void populateTagChoices(List<String> projectNames, List<List<RepositoryTagDto>> availableTags, List<String> workDirOverrides, DependencyGroup dependencyGroup) {
		this.projectNames = projectNames;
		this.availableTags = availableTags;
		this.dependencyGroup = dependencyGroup;
		if (workDirOverrides != null) {
			this.workDirOverrides = workDirOverrides.toArray(new String[workDirOverrides.size()]);
		} else {
			this.workDirOverrides = ArrayUtils.EMPTY_STRING_ARRAY;
		}
	}
	public void applyTagNamesOnTargets() {
		final Map<String, String> projectNamesToTagNames = new HashMap<String, String>();
		final Map<String, String> projectNamesToWorkDirs = new HashMap<String, String>();
		
		if (projectNames.size() != selectedTags.length || projectNames.size() != workDirOverrides.length) {
			throw new IllegalStateException();
		}
		
		for (int i=0; i<projectNames.size(); i++) {
			final String tagName;
			
			if (StringUtils.isBlank(selectedTags[i])) {
				tagName = null;
			} else {
				tagName = selectedTags[i];
			}
			
			projectNamesToTagNames.put(projectNames.get(i), tagName);
			projectNamesToWorkDirs.put(projectNames.get(i), workDirOverrides[i]);
		}
		
		final List<ProjectConfigDto> pendingProjects = dependencyGroup.getPendingProjects();
		for (ProjectConfigDto config : pendingProjects) {
			config.setRepositoryTagName(projectNamesToTagNames.get(config.getName()));
			final String workDirOverride = projectNamesToWorkDirs.get(config.getName());
			if (StringUtils.isNotBlank(workDirOverride)) {
				config.setWorkDir(workDirOverride);
			}
		}
	}

	@Override
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		targets = null;
		selectedTags = null;
		updateStrategy = WorkingCopyUpdateStrategy.Default.name();
		workingCopyUpdateStrategy = null;
		dependencies = DependencyBuildPolicy.NONE.name();
		buildOnDependencyFailure = false;
		buildOnNoUpdates = false;
		chooseTags = false;
	}
	@Override
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		final ActionErrors errors = super.validate(mapping, request);
		
		if (errors.isEmpty()) {
			try {
				workingCopyUpdateStrategy = WorkingCopyUpdateStrategy.valueOf(updateStrategy);
			} catch (IllegalArgumentException e) {
				errors.add("updateStrategy", new ActionMessage("errors.enum.invalid"));
			}
		}
		
		return errors;
	}
	public String getDependencies() {
		return dependencies;
	}
	public void setDependencies(String dependencies) {
		this.dependencies = dependencies;
	}
	public String[] getTargets() {
		return targets;
	}
	public void setTargets(String[] targets) {
		this.targets = targets;
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
	public void setBuildOnNoUpdates(boolean buildWhenNoUpdates) {
		this.buildOnNoUpdates = buildWhenNoUpdates;
	}
	public boolean isChooseTags() {
		return chooseTags;
	}
	public void setChooseTags(boolean chooseTag) {
		this.chooseTags = chooseTag;
	}
	public List<String> getProjectNames() {
		return projectNames;
	}
	public List<List<RepositoryTagDto>> getAvailableTags() {
		return availableTags;
	}
	public DependencyGroup getDependencyGroup() {
		return dependencyGroup;
	}
	public String[] getSelectedTags() {
		return selectedTags;
	}
	public void setSelectedTags(String[] selectedTags) {
		this.selectedTags = selectedTags;
	}
	public String[] getWorkDirOverrides() {
		return workDirOverrides;
	}
	public void setWorkDirOverrides(String[] workDirOverrides) {
		this.workDirOverrides = workDirOverrides;
	}
	public String getUpdateStrategy() {
		return updateStrategy;
	}
	public void setUpdateStrategy(String updateStrategy) {
		this.updateStrategy = updateStrategy;
	}
	public WorkingCopyUpdateStrategy getWorkingCopyUpdateStrategy() {
		return workingCopyUpdateStrategy;
	}
}
