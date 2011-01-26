/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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

import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.PluginProfileDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;
import net.sourceforge.vulcan.exception.CannotCreateDirectoryException;
import net.sourceforge.vulcan.exception.DuplicatePluginIdException;
import net.sourceforge.vulcan.exception.InvalidPluginLayoutException;
import net.sourceforge.vulcan.exception.PluginLoadFailureException;
import net.sourceforge.vulcan.exception.PluginNotConfigurableException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.exception.ValidationException;
import net.sourceforge.vulcan.web.struts.forms.PluginConfigForm;
import net.sourceforge.vulcan.web.struts.forms.ProjectConfigForm;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public final class ManagePluginAction extends BaseDispatchAction {
	public ActionForward upload(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		final PluginConfigForm configForm = (PluginConfigForm) form;
		
		try {
			stateManager.getPluginManager().importPluginZip(configForm.getPluginFile().getInputStream());
			saveSuccessMessage(request);
			return mapping.findForward("pluginList");
		} catch (InvalidPluginLayoutException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.plugin.layout"));
		} catch (DuplicatePluginIdException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.plugin.duplicate", new Object[] {e.getId()}));
		} catch (CannotCreateDirectoryException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.plugin.store.failure.dir", new Object[] {e.getDirectory()}));
		} catch (StoreException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.plugin.store.failure", new Object[] {e.getMessage()}));
		} catch (PluginLoadFailureException e) {
			saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.plugin.load.failure", new Object[] {e.getMessage()}));
		}
		
		return mapping.getInputForward();
	}
	public ActionForward delete(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		final PluginConfigForm configForm = (PluginConfigForm) form;

		try {
			stateManager.removePlugin(configForm.getPluginId());
			
			saveSuccessMessage(request);
			
			return mapping.findForward("pluginList");
		} catch (StoreException e) {
			if (e.getCause() instanceof FileNotFoundException) {
				saveError(request, ActionMessages.GLOBAL_MESSAGE,
						new ActionMessage("errors.plugin.not.found"));
				return mapping.getInputForward();
			}
			return mapping.findForward("pluginLocked");
		}
	}
	
	public ActionForward update(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final PluginConfigForm configForm = (PluginConfigForm) form;
		
		if (!validatePluginConfig(request, configForm)) {
			setHelpAttributes(request, configForm);
			
			return mapping.findForward("configure");
		}
		
		if (configForm.isDirty()) {
			stateManager.updatePluginConfig(configForm.getPluginConfig(), configForm.getRenamedProfiles());
			saveSuccessMessage(request);
		} else {
			addMessage(request, "warnings", ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("warnings.no.change.made"));
		}
		
		return mapping.findForward("pluginList");
	}
	public ActionForward configure(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final PluginConfigForm configForm = (PluginConfigForm) form;
		
		if (configForm.isNested()) {
			configForm.introspect(request);
			setHelpAttributes(request, configForm);
			return mapping.findForward("configure");
		}
		try {
			final String pluginId = configForm.getPluginId();
			final PluginConfigDto config = stateManager.getPluginManager().getPluginConfigInfo(pluginId);
			configForm.setPluginConfig(request, config, false);
			setHelpAttributes(request, configForm);
			return mapping.findForward("configure");
		} catch (PluginNotConfigurableException e) {
			super.saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.plugin.not.configurable"));
			return mapping.getInputForward();
		}
	}
	public final ActionForward back(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final PluginConfigForm configForm = (PluginConfigForm) form;

		if (!validatePluginConfig(request, configForm)) {
			setHelpAttributes(request, configForm);
			
			return mapping.findForward("configure");
		}
		
		configForm.resortArrayIfNecessary();
		
		if (configForm.isProjectPlugin() && !configForm.isNested()) {
			putConfigInProjectForm(mapping, request, configForm.getPluginConfig());
			return mapping.findForward("projectDetails");
		}
		
		final Object focusObject = configForm.getFocusObject();
		if (focusObject instanceof PluginProfileDto) {
			final PluginProfileDto pluginProfileDto = (PluginProfileDto)focusObject;
			if (pluginProfileDto.isRenamed()) {
				configForm.getRenamedProfiles().add(pluginProfileDto);
			}
		}

		String focus = configForm.getFocus();
		focus = focus.substring(0, focus.lastIndexOf("."));
		
		configForm.setFocus(focus);
		
		configForm.introspect(request);
		
		setHelpAttributes(request, configForm);
		
		return mapping.findForward("configure");
	}
	public final ActionForward add(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final PluginConfigForm configForm = (PluginConfigForm) form;
		final String target = configForm.getTarget();
		
		final BeanWrapper bw = new BeanWrapperImpl(form);
		
		final Class<?> type = PropertyUtils.getPropertyType(form, target).getComponentType();
		final int i;
		
		Object[] array = (Object[]) bw.getPropertyValue(target);
		
		if (array == null) {
			array = (Object[]) Array.newInstance(type, 1);
			i = 0;
		} else {
			i = array.length;
			Object[] tmp = (Object[]) Array.newInstance(type, i+1);
			System.arraycopy(array, 0, tmp, 0, i);
			array = tmp;
		}
		
		array[i] = stateManager.getPluginManager().createObject(configForm.getPluginId(), type.getName());
		
		bw.setPropertyValue(target, array);
		
		configForm.setFocus(target + "[" + i + "]");
		configForm.introspect(request);
		
		setHelpAttributes(request, configForm);
		
		return mapping.findForward("configure");
	}
	public final ActionForward remove(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final PluginConfigForm configForm = (PluginConfigForm) form;
		final String target = configForm.getTarget();
		
		final String[] split = target.split("\\[");
		final String arrayProperty = split[0];
		final int index = Integer.parseInt(split[1].substring(0, split[1].length()-1));
		
		final BeanWrapper bw = new BeanWrapperImpl(form);
		final Class<?> type = PropertyUtils.getPropertyType(form, arrayProperty).getComponentType();
		
		Object[] array = (Object[]) bw.getPropertyValue(arrayProperty);
		
		Object[] tmp = (Object[]) Array.newInstance(type, array.length - 1);
		System.arraycopy(array, 0, tmp, 0, index);
		System.arraycopy(array, index + 1, tmp, index, array.length - index - 1);
		
		bw.setPropertyValue(arrayProperty, tmp);
		
		configForm.putBreadCrumbsInRequest(request);
		
		return mapping.findForward("configure");
	}
	private void putConfigInProjectForm(ActionMapping mapping, HttpServletRequest request, PluginConfigDto config) {
		final ProjectConfigForm form = (ProjectConfigForm) request.getSession().getAttribute("projectConfigForm");
		if (form == null) {
			throw new IllegalStateException();
		}
		
		final ProjectConfigDto projectConfig = form.getProjectConfig();
		
		if (config instanceof BuildToolConfigDto) {
			projectConfig.setBuildToolConfig((BuildToolConfigDto) config);
		} else {
			projectConfig.setRepositoryAdaptorConfig((RepositoryAdaptorConfigDto) config);
		}
	}
	private boolean validatePluginConfig(HttpServletRequest request, PluginConfigForm form) {
		final String focus = form.getFocus();
		final Object o;

		try {
			try {
				o = PropertyUtils.getProperty(form, focus);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		} catch (Throwable e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		}
		
		if (o instanceof PluginConfigDto) {
			try {
				((PluginConfigDto)o).validate();
			} catch (ValidationException e) {
				final ActionMessages errors = new ActionMessages();
				
				convertExceptionToActionMessages(e, errors, focus);
				
				saveErrors(request, errors);
				return false;
			}
		}

		return true;
	}
	private void convertExceptionToActionMessages(ValidationException validationException, final ActionMessages errors, final String focus) {
		for (ValidationException e : validationException) {
			String propertyName = e.getPropertyName();
			
			if (StringUtils.isBlank(propertyName)) {
				propertyName = ActionMessages.GLOBAL_MESSAGE;
			} else {
				propertyName = focus + "." + propertyName;
			}
	
			errors.add(propertyName, new ActionMessage(e.getKey(), e.getArgs()));
		}
	}
	private void setHelpAttributes(HttpServletRequest request, PluginConfigForm configForm) {
		final Object focusObject = configForm.getFocusObject();
		final PluginConfigDto pluginConfig;
		
		if (focusObject instanceof PluginConfigDto) {
			pluginConfig = (PluginConfigDto) focusObject;
		} else {
			pluginConfig = configForm.getPluginConfig();
		}
		
		request.setAttribute("helpUrl", pluginConfig.getHelpUrl());
		request.setAttribute("helpTopic", pluginConfig.getHelpTopic());
	}
}
