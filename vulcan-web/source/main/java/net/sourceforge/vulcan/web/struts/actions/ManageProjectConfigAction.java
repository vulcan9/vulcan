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
package net.sourceforge.vulcan.web.struts.actions;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.ConfigurationStore;
import net.sourceforge.vulcan.dto.LockDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.ProjectNeedsDependencyException;
import net.sourceforge.vulcan.web.struts.forms.PluginConfigForm;
import net.sourceforge.vulcan.web.struts.forms.ProjectConfigForm;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

public final class ManageProjectConfigAction extends BaseDispatchAction {
	private ConfigurationStore configurationStore;
	private BuildManager buildManager;
	
	public ActionForward edit(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		final ProjectConfigForm configForm = (ProjectConfigForm) form;
		final ProjectConfigDto project;
		
		if (configForm.isCreateNew()) {
			project = new ProjectConfigDto();
			project.setName("");
		} else {
			project = stateManager.getProjectConfig(configForm.getConfig().getName());
		}
		
		configForm.setStore(configurationStore);
		configForm.populate(project, false);
		
		return mapping.findForward("projectDetails");
	}

	public ActionForward copy(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		final ProjectConfigForm configForm = (ProjectConfigForm) form;
		final ProjectConfigDto project = configForm.getProjectConfig();
		
		final String name = project.getName() + "-copy";
		final String workDir = project.getWorkDir() + "-copy";
		
		project.setName(name);
		project.setWorkDir(workDir);
		
		configForm.setStore(configurationStore);
		configForm.populate(project, false);
		configForm.setCreateNew(true);
		
		return mapping.findForward("projectDetails");
	}
	
	public ActionForward create(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final ProjectConfigForm configForm = (ProjectConfigForm) form;
		final ProjectConfigDto config = configForm.getProjectConfig();
		final StateManager mgr = stateManager;
		
		mgr.addProjectConfig(config);
		
		saveSuccessMessage(request);
		
		return mapping.findForward("projectList");
	}
	
	public ActionForward update(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final ProjectConfigForm configForm = (ProjectConfigForm) form;
		final ProjectConfigDto config = configForm.getProjectConfig();
		final StateManager mgr = stateManager;
		
		if (configForm.isDirty() || configForm.lockWasAdded()) {
			if (configForm.lockWasAdded()) {
				if (buildManager.isBuildingOrInQueue(configForm.getOriginalName())) {
					saveError(request, ActionMessages.GLOBAL_MESSAGE,
							new ActionMessage("errors.cannot.lock.project"));
					return mapping.getInputForward();
				}
				final LockDto lock = new LockDto(formatMessage("messages.project.locked.by.user", getUsername(request), new Date()), 0);
				config.addLock(lock);
			}

			mgr.updateProjectConfig(configForm.getOriginalName(), config, true);
			saveSuccessMessage(request);
		} else {
			addMessage(request, "warnings", ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("warnings.no.change.made"));
		}

		return mapping.findForward("projectList");
	}

	public ActionForward delete(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final ProjectConfigForm configForm = (ProjectConfigForm) form;
		final StateManager mgr = stateManager;
		
		try {
			mgr.deleteProjectConfig(configForm.getConfig().getName());
			saveSuccessMessage(request);
		} catch (ProjectNeedsDependencyException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage(e.getKey(), e.getArgs()));
		}

		return mapping.findForward("projectList");
	}
	
	public ActionForward configure(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		final ProjectConfigForm projectForm = (ProjectConfigForm) form;
		final PluginConfigForm pluginForm = getOrCreatePluginForm(mapping, request);
		
		final String pluginId;
		final String property = projectForm.getFocus();
		
		if (projectForm.isFocusOnBuildTool()) {
			pluginId = projectForm.getProjectConfig().getBuildToolPluginId();
		} else {
			pluginId = projectForm.getProjectConfig().getRepositoryAdaptorPluginId();
		}
		
		if (StringUtils.isBlank(pluginId)) {
			final ActionMessages errors = new ActionMessages();
			errors.add(property,
					new ActionMessage("errors.no.plugin.selected"));
			saveErrors(request, errors);
			return mapping.getInputForward();
		}
		
		PluginConfigDto config = null;
		
		if (projectForm.isFocusOnBuildTool()) {
			if (!projectForm.isBuildToolChanged()) {
				config = projectForm.getProjectConfig().getBuildToolConfig();
			}
			
			if (config == null) {
				config = stateManager.getPluginManager().getBuildToolDefaultConfig(pluginId);
			}
		} else {
			if (!projectForm.isRepositoryAdaptorChanged()) {
				config = projectForm.getProjectConfig().getRepositoryAdaptorConfig();
			}
			
			if (config == null) {
				config = stateManager.getPluginManager().getRepositoryAdaptorDefaultConfig(pluginId);
			}
		}
		
		pluginForm.setProjectPlugin(true);
		pluginForm.setProjectName(projectForm.getProjectConfig().getName());
		pluginForm.setPluginConfig(request, config, projectForm.isDirty());
		
		request.setAttribute("helpUrl", config.getHelpUrl());
		request.setAttribute("helpTopic", config.getHelpTopic());
		
		return mapping.findForward("configure");
	}
	
	public void setConfigurationStore(ConfigurationStore store) {
		this.configurationStore = store;
	}
	
	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
	
	private PluginConfigForm getOrCreatePluginForm(ActionMapping mapping, HttpServletRequest request) {
		final HttpSession session = request.getSession();
		
		PluginConfigForm form = (PluginConfigForm) session.getAttribute("pluginConfigForm");
		
		if (form == null) {
			form = new PluginConfigForm();
			session.setAttribute("pluginConfigForm", form);
		}

		form.setServlet(getServlet());
		form.reset(mapping, request);

		return form;
	}
}
