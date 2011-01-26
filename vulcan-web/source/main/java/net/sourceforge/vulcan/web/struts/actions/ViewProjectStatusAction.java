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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.vulcan.dto.PreferencesDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.web.Keys;
import net.sourceforge.vulcan.web.struts.forms.ProjectStatusForm;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.jdom.Document;
import org.jdom.Element;

public final class ViewProjectStatusAction extends ProjectReportBaseAction {
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		final String projectName = request.getParameter("projectName");
		
		final ProjectStatusForm statusForm = (ProjectStatusForm) form;
		
		final ProjectConfigDto projectConfig;
		
		try {
			projectConfig = projectManager.getProjectConfig(projectName);
		} catch (NoSuchProjectException e) {
			BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.no.such.project", new String[] {projectName}));
			return mapping.findForward("failure");
		}

		ProjectStatusDto status = buildManager.getProjectsBeingBuilt().get(projectName);
		boolean currentlyBuilding = status != null;
		
		final List<UUID> ids = buildManager.getAvailableStatusIds(projectName);
		int index = -1;
		
		if (currentlyBuilding && statusForm.shouldDisplayLatest()) {
			if (ids == null) {
				index = 0;
			} else {
				index = ids.size();
			}
		} else if (statusForm.isBuildNumberSpecified()) {
			final int buildNumber = Integer.valueOf(statusForm.getBuildNumber());
			
			if (currentlyBuilding && status.getBuildNumber() != null
					&& status.getBuildNumber() == buildNumber) {
				if (ids != null) {
					index = ids.size();
				} else {
					index = 0;
				}
			} else {
				status = buildManager.getStatusByBuildNumber(projectName, buildNumber);
				if (status != null) {
					index = ids.indexOf(status.getId());
				}
			}
			
			if (index < 0) {
				BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
						new ActionMessage("errors.status.not.available.by.build.number",
								new String[] {projectName, Integer.toString(buildNumber), status != null ? status.getId().toString() : ""}));
				return mapping.findForward("failure");
			}
		} else {
			if (statusForm.isIndexSpecified()) {
				index = Integer.valueOf(statusForm.getIndex());
			}
			
			if (ids != null && !ids.isEmpty()) {
				if (index < 0) {
					index += ids.size();
				}
				
				if (index < ids.size()) {
					status = buildManager.getStatus(ids.get(index));
				} else if (index > ids.size()) {
					BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
							new ActionMessage("errors.request.invalid"));
					return mapping.getInputForward();
				}
			}
		}
		
		if (status == null) {
			BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.status.not.available", new String[] {projectName}));
			return mapping.findForward("failure");
		}

		final String view = statusForm.getView();
		
		if ("diff".equals(view)) {
			sendDiff(request, response, status);
			return null;
		} else if ("log".equals(view)) {
			sendLog(request, response, status);
			return null;
		}
		
		final Document doc = createDocument(request, status, ids, index, currentlyBuilding);
		final String transform = statusForm.getTransform();
		
		return sendDocument(doc, transform, projectConfig, status.getBuildNumber(), createTransformParameters(request, status, view), mapping, request, response);
	}
	
	protected Document createDocument(HttpServletRequest request, ProjectStatusDto status, final List<UUID> ids, int index, boolean currentlyBuilding) {
		final Document doc = projectDomBuilder.createProjectDocument(status, request.getLocale());
		
		if (ids != null) {
			if (index > 0) {
				final Element e = new Element("previous-build-number");
				
				e.setText(buildManager.getStatus(ids.get(index-1)).getBuildNumber().toString());
				doc.getRootElement().addContent(e);
			}
			
			final int size = ids.size();
			final boolean inRange = index+1 < size;
			final boolean currentlyBuilindgInRange = currentlyBuilding && index + 1 == size;
			
			if (inRange || currentlyBuilindgInRange) {
				final Element e = new Element("next-build-number");
				
				if (inRange) {
					e.setText(buildManager.getStatus(ids.get(index+1)).getBuildNumber().toString());
				} else {
					e.setText(Integer.toString(status.getBuildNumber()+1));
				}
				
				doc.getRootElement().addContent(e);
			}
		}
		return doc;
	}
	
	protected void sendDiff(HttpServletRequest request, HttpServletResponse response, ProjectStatusDto status) throws IOException {
		InputStream is = null;
		
		if (status != null && status.getDiffId() != null) {
			UUID diffId = status.getDiffId();
			try {
				is = new FileInputStream(configurationStore.getChangeLog(status.getName(), diffId));
			} catch (StoreException e) {
				is = null;
			}
		}
		
		sendText(response, is);
	}
	
	protected void sendLog(HttpServletRequest request, HttpServletResponse response, ProjectStatusDto status) throws IOException {
		InputStream is = null;
		
		if (status != null && status.getBuildLogId() != null) {
			UUID logId = status.getBuildLogId();
			try {
				is = new FileInputStream(configurationStore.getBuildLog(status.getName(), logId));
			} catch (StoreException e) {
				is = null;
			}
		}
		
		sendText(response, is);
	}
	
	protected void sendText(HttpServletResponse response, InputStream is) throws IOException {
		if (is == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		response.setContentType("text/plain");
		
		final Writer os = response.getWriter();
		try {
			IOUtils.copy(is, os);
		} finally {
			is.close();
			os.close();
		}
	}
	
	private Map<String, Object> createTransformParameters(HttpServletRequest request, ProjectStatusDto status, String view) {
		final Map<String, Object> params = new HashMap<String, Object>();
		
		Integer buildNo = buildManager.getMostRecentBuildNumberByWorkDir(status.getWorkDir());
		
		if (buildNo == null) {
			buildNo = -1;
		}
		
		params.put("workingCopyBuildNumber", buildNo);
		
		final HttpSession session = request.getSession(false);
		if (session != null) {
			final PreferencesDto prefs = (PreferencesDto) session.getAttribute(Keys.PREFERENCES);
			
			if (prefs != null) {
				params.put("reloadInterval", Integer.valueOf(prefs.getReloadInterval()));
			}
		}
	
		if (StringUtils.isBlank(view)) {
			view = "summary";
		}
		
		params.put("view", view);
		
		return params;
	}
}
