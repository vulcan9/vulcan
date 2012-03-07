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
package net.sourceforge.vulcan.web.struts.forms;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.vulcan.core.ConfigurationStore;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.NamedObject;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto.UpdateStrategy;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

public final class ProjectConfigForm extends ConfigForm {
	private transient ConfigurationStore store;
	
	String previousRepositoryAdaptorPluginId;
	String previousBuildToolPluginId;
	String focus;
	
	boolean locked;
	
	public ProjectConfigForm() {
		super(new ProjectConfigDto());
	}
	public String getTargetType() {
		return "project";
	}
	public boolean isLocked() {
		return locked;
	}
	public void setLocked(boolean locked) {
		this.locked = locked;
		if (locked) {
			final ProjectConfigDto cur = getProjectConfig();
			final ProjectConfigDto prev = (ProjectConfigDto) getOriginal();
			
			final int lockCount = prev.getLockCount();
			
			if (lockCount != 0) {
				cur.setLocks(prev.getLocks());
			}
		}
	}
	public String getFocus() {
		return focus;
	}
	public void setFocus(String focus) {
		this.focus = focus;
	}
	public void setStore(ConfigurationStore store) {
		this.store = store;
	}
	public ProjectConfigDto getProjectConfig() {
		return (ProjectConfigDto) getConfig();
	}
	public String getPreviousRepositoryAdaptorPluginId() {
		return previousRepositoryAdaptorPluginId;
	}
	public String getPreviousBuildToolPluginId() {
		return previousBuildToolPluginId;
	}
	public boolean isRepositoryAdaptorChanged() {
		return !(new EqualsBuilder().append(
				getProjectConfig().getRepositoryAdaptorPluginId(),
				getPreviousRepositoryAdaptorPluginId()).isEquals());
	}
	public boolean isBuildToolChanged() {
		return !(new EqualsBuilder().append(
				getProjectConfig().getBuildToolPluginId(),
				getPreviousBuildToolPluginId()).isEquals());
	}
	public boolean isFocusOnBuildTool() {
		return "config.buildToolPluginId".equals(focus);
	}
	public String getUpdateStrategy() {
		return getProjectConfig().getUpdateStrategy().name();
	}
	public void setUpdateStrategy(String updateStrategy) {
		UpdateStrategy us;
		
		try {
			us = UpdateStrategy.valueOf(updateStrategy);
		} catch (IllegalArgumentException e) {
			us = UpdateStrategy.CleanAlways;
		}
		
		getProjectConfig().setUpdateStrategy(us);
	}
	
	public boolean lockWasAdded() {
		final ProjectConfigDto prev = (ProjectConfigDto) getOriginal();
		
		return isLocked() && (prev == null || (prev != null && !prev.isLocked()));
	}

	@Override
	protected void resetInternal(ActionMapping mapping,
			HttpServletRequest request) {
		super.resetInternal(mapping, request);
		locked = false;
	}
	
	@Override
	protected void initializeConfig(NamedObject config, NamedObject previousConfig) {
		if (previousConfig == null) {
			previousRepositoryAdaptorPluginId = null;
			previousBuildToolPluginId = null;
			return;
		}
		final ProjectConfigDto prev = (ProjectConfigDto) previousConfig;
		final ProjectConfigDto curr = (ProjectConfigDto) config;
		
		curr.setRepositoryAdaptorConfig(prev.getRepositoryAdaptorConfig());
		curr.setBuildToolConfig(prev.getBuildToolConfig());
		curr.setLastModificationDate(prev.getLastModificationDate());
		
		previousRepositoryAdaptorPluginId = prev.getRepositoryAdaptorPluginId();
		previousBuildToolPluginId = prev.getBuildToolPluginId();
		
		if (prev != null) {
			curr.setLabels(prev.getLabels());
		}
	}
	
	@Override
	protected void customValidate(ActionMapping mapping, HttpServletRequest request, ActionErrors errors) {
		final String action = translateAction(getAction());
		if (!"update".equals(action) && !"create".equals(action)) {
			return;
		}
		
		final ProjectConfigDto projectConfig = getProjectConfig();
		if (isBlank(projectConfig.getRepositoryAdaptorPluginId())) {
			projectConfig.setRepositoryAdaptorConfig(null);
		} else if (isRepositoryAdaptorChanged()) {
			final RepositoryAdaptorConfigDto config = projectConfig.getRepositoryAdaptorConfig();
			
			if (config == null || !config.getPluginId().equals(projectConfig.getRepositoryAdaptorPluginId())) {
				errors.add("config.repositoryAdaptorPluginId",
						new ActionMessage("errors.plugin.not.configured"));
			}
		}
		
		if (isBlank(projectConfig.getBuildToolPluginId())) {
			projectConfig.setBuildToolConfig(null);
		} else if (isBuildToolChanged()) {
			final BuildToolConfigDto config = projectConfig.getBuildToolConfig();
			
			if (config == null || !config.getPluginId().equals(projectConfig.getBuildToolPluginId())) {
				errors.add("config.buildToolPluginId",
						new ActionMessage("errors.plugin.not.configured"));
			}
		}
		
		final String url = projectConfig.getBugtraqUrl();
		
		if (isNotBlank(url)) {
			try {
				new URL(url);
			} catch (MalformedURLException e) {
				errors.add("config.bugtraqUrl", new ActionMessage("errors.url"));
			}
			
			if (url.toUpperCase().indexOf("%BUGID%") < 0) {
				errors.add("config.bugtraqUrl", new ActionMessage("errors.bugtraq.must.contain.id"));
			}
		}
		
		validateRegex(errors, projectConfig.getBugtraqLogRegex1(), "config.bugtraqLogRegex1");
		validateRegex(errors, projectConfig.getBugtraqLogRegex2(), "config.bugtraqLogRegex2");
		
		final String workDir = projectConfig.getWorkDir();
		if (isNotBlank(workDir) && store.isWorkingCopyLocationInvalid(workDir)) {
			errors.add("config.workDir", new ActionMessage("errors.work.dir.reserved"));
		}
	}
	
	private void validateRegex(ActionErrors errors, String regex, String propertyName) {
		if (isNotBlank(regex)) {
			try {
				Pattern.compile(regex);
			} catch (PatternSyntaxException e) {
				errors.add(propertyName, new ActionMessage("errors.regex", e.getDescription(), e.getIndex()));
			}
		}
		
	}
}
