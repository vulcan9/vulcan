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

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.event.AuditEvent;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

@SvnRevision(id="$Id$", url="$HeadURL$")
public final class ToggleSchedulerAction extends BaseAuditAction {
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final String schedulerName = request.getParameter("schedulerName");
		
		if (StringUtils.isBlank(schedulerName)) {
			throw new IllegalArgumentException("Missing request paraemeter 'schedulerName'");
		}
		
		final SchedulerConfigDto schedulerConfig = 
			(SchedulerConfigDto) stateManager.getSchedulerConfig(schedulerName).copy();

		final String action = schedulerConfig.isPaused() ? "unpause" : "pause";
		
		final AuditEvent event = new AuditEvent(this,
				"audit.scheduler.toggle",
				BaseDispatchAction.getUsername(request),
				request.getRemoteHost(),
				action,
				"scheduler",
				schedulerName,
				null);

		eventHandler.reportEvent(event);
		auditLog.info(messageSource.getMessage(event.getKey(), event.getArgs(), Locale.getDefault()));
		
		schedulerConfig.setPaused(!schedulerConfig.isPaused());
		
		stateManager.updateSchedulerConfig(schedulerName, schedulerConfig, false);
		
		return mapping.findForward("dashboard");
	}
}
