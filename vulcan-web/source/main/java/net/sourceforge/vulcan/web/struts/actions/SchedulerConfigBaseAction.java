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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.exception.DuplicateNameException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.web.struts.forms.SchedulerConfigForm;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;


public abstract class SchedulerConfigBaseAction extends BaseDispatchAction {
	private boolean allowCronExpressions;
	
	protected abstract SchedulerConfigDto getConfig(String name);
	protected abstract void addScheduler(SchedulerConfigDto config) throws DuplicateNameException, StoreException;
	protected abstract void updateSchedulerConfig(String originalName, SchedulerConfigDto config) throws DuplicateNameException, StoreException;
	protected abstract void deleteSchedulerConfig(String name) throws StoreException;
	
	public final ActionForward edit(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		final SchedulerConfigForm configForm = (SchedulerConfigForm) form;
		final SchedulerConfigDto scheduler;
		
		if (configForm.isCreateNew()) {
			scheduler = new SchedulerConfigDto();
			scheduler.setName("");
		} else {
			scheduler = getConfig(configForm.getConfig().getName());
		}
		
		configForm.populate(scheduler, allowCronExpressions);
		
		return mapping.findForward("schedulerDetails");
	}

	public final ActionForward create(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final SchedulerConfigForm configForm = (SchedulerConfigForm) form;
		final SchedulerConfigDto config = configForm.getSchedulerConfig();
		
		addScheduler(config);
		
		saveSuccessMessage(request);
		
		return mapping.findForward("schedulerList");
	}
	public final ActionForward update(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final SchedulerConfigForm configForm = (SchedulerConfigForm) form;
		final SchedulerConfigDto config = configForm.getSchedulerConfig();
		
		if (configForm.isDirty()) {
			updateSchedulerConfig(configForm.getOriginalName(), config);
			saveSuccessMessage(request);
		} else {
			addMessage(request, "warnings", ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("warnings.no.change.made"));
		}

		return mapping.findForward("schedulerList");
	}
	
	public final ActionForward delete(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final SchedulerConfigForm configForm = (SchedulerConfigForm) form;
		
		deleteSchedulerConfig(configForm.getConfig().getName());
		
		saveSuccessMessage(request);

		return mapping.findForward("schedulerList");
	}
	
	public boolean isAllowCronExpressions() {
		return allowCronExpressions;
	}
	public void setAllowCronExpressions(boolean allowCronExpressions) {
		this.allowCronExpressions = allowCronExpressions;
	}
}
