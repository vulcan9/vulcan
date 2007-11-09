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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.jdom.Document;

@SvnRevision(id="$Id$", url="$HeadURL$")
public final class GetCachedBuildHistoryAction extends ProjectReportBaseAction {
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		final Document buildHistory;
		
		final HttpSession session = request.getSession(false);
		if (session != null) {
			buildHistory = (Document)session.getAttribute("buildHistory");
		} else {
			buildHistory = null;
		}
		
		if (buildHistory == null) {
			BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.no.build.history"));
			return mapping.findForward("error");
		}
		
		final Map<String, Object> params = new HashMap<String, Object>();
		
		addParameterIfPresent(request, "metricLabel1", params);
		addParameterIfPresent(request, "metricLabel2", params);
		
		return sendDocument(buildHistory, request.getParameter("transform"), null, 0, params, mapping, request, response);
	}

	private void addParameterIfPresent(HttpServletRequest request, String key, Map<String, ? super Object> params) {
		final String value = request.getParameter(key);
		if (StringUtils.isNotBlank(value)) {
			params.put(key, value);
		}
	}
}
