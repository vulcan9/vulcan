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

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.jdom.Document;
import org.jdom.Element;

@SvnRevision(id="$Id$", url="$HeadURL$")
public final class ViewProjectStatusAction extends ProjectReportBaseAction {
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		final String projectName = request.getParameter("projectName");
		boolean byBuildNumber = false;
		
		if (StringUtils.isBlank(projectName)) {
			BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.request.invalid"));
			return mapping.findForward("failure");
		}
		
		int index = -1;
		
		final String indexString = request.getParameter("index");
		if (!StringUtils.isBlank(indexString)) {
			try {
				index = Integer.parseInt(indexString);
			} catch (NumberFormatException e) {
				BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
						new ActionMessage("errors.request.invalid"));
				return mapping.findForward("failure");
			}
		}
		
		int buildNumber = -1;
		final String buildNumberString = request.getParameter("buildNumber");
		if (!StringUtils.isBlank(buildNumberString)) {
			try {
				buildNumber = Integer.parseInt(buildNumberString);
				byBuildNumber = true;
			} catch (NumberFormatException e) {
				BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
						new ActionMessage("errors.request.invalid"));
				return mapping.findForward("failure");
			}
		}
		
		final ProjectConfigDto projectConfig;
		
		try {
			projectConfig = projectManager.getProjectConfig(projectName);
		} catch (NoSuchProjectException e) {
			BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.no.such.project", new String[] {projectName}));
			return mapping.findForward("failure");
		}

		ProjectStatusDto status = null;
		final List<UUID> ids = buildManager.getAvailableStatusIds(projectName);
		
		if (byBuildNumber) {
			status = buildManager.getStatusByBuildNumber(projectName, buildNumber);
			index = ids.indexOf(status.getId());
			
			if (index < 0) {
				throw new IllegalStateException(
						"Build Number " + buildNumber +
						" is not in the list of available outcomes for project " + 
						projectName + " ID {" + status.getId() + "}.");
			}
		} else {
			if (ids != null && !ids.isEmpty()) {
				if (index < 0) {
					index += ids.size();
				}
				try {
					status = buildManager.getStatus(ids.get(index));
				} catch (IndexOutOfBoundsException e) {
					BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
							new ActionMessage("errors.request.invalid"));
					return mapping.findForward("failure");
				}
			}
		}
		
		if (status == null) {
			BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
					new ActionMessage("errors.status.not.available", new String[] {projectName}));
			if (buildManager.getProjectsBeingBuilt().containsKey(projectName)) {
				request.setAttribute("currentlyBuilding", Boolean.TRUE);
			}
			return mapping.findForward("failure");
		}

		final String transform = request.getParameter("transform");
		final String view = request.getParameter("view");
		
		if ("diff".equals(view)) {
			sendDiff(request, response, status);
			return null;
		} else if ("log".equals(view)) {
			sendLog(request, response, status);
			return null;
		}
		
		final Document doc = projectDomBuilder.createProjectDocument(status, request.getLocale());
		
		final boolean isNewest = index == ids.size() - 1;
		if (isNewest && buildManager.getProjectsBeingBuilt().containsKey(projectName)) {
			final Element e = new Element("currently-building");
			e.setText("true");
			
			doc.getRootElement().addContent(e);
		}
		
		if (index > 0) {
			final Element e = new Element("prev-index");
			e.setText(Integer.toString(index-1));
			doc.getRootElement().addContent(e);
		}
		
		if (index+1 < ids.size()) {
			final Element e = new Element("next-index");
			e.setText(Integer.toString(index+1));
			doc.getRootElement().addContent(e);
		}
		
		return sendDocument(doc, transform, projectConfig, index, mapping, request, response);
	}
	protected void sendDiff(HttpServletRequest request, HttpServletResponse response, ProjectStatusDto status) throws IOException {
		InputStream is = null;
		
		if (status != null) {
			UUID diffId = status.getDiffId();
			try {
				is = store.getChangeLogInputStream(status.getName(), diffId);
			} catch (StoreException e) {
				is = null;
			}
		}
		
		sendText(response, is);
	}
	protected void sendLog(HttpServletRequest request, HttpServletResponse response, ProjectStatusDto status) throws IOException {
		InputStream is = null;
		
		if (status != null) {
			UUID logId = status.getBuildLogId();
			try {
				is = store.getBuildLogInputStream(status.getName(), logId);
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
}
