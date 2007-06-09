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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.ProjectDomBuilder;
import net.sourceforge.vulcan.core.Store;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.NoSuchTransformFormatException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.JstlFunctions;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.RequestUtils;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.SAXException;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class ProjectReportBaseAction extends Action {
	protected Store store;
	protected BuildManager buildManager;
	protected ProjectDomBuilder projectDomBuilder;
	protected ProjectManager projectManager;
	
	public Store getStore() {
		return store;
	}
	public void setStore(Store store) {
		this.store = store;
	}
	public BuildManager getBuildManager() {
		return buildManager;
	}
	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
	public ProjectDomBuilder getProjectDomBuilder() {
		return projectDomBuilder;
	}
	public void setProjectDomBuilder(ProjectDomBuilder projectDomBuilder) {
		this.projectDomBuilder = projectDomBuilder;
	}
	public ProjectManager getProjectManager() {
		return projectManager;
	}
	public void setProjectManager(ProjectManager projectManager) {
		this.projectManager = projectManager;
	}
	protected URL getSiteBaseURL(ActionMapping mapping, HttpServletRequest request, ProjectConfigDto projectConfig) throws MalformedURLException {
		final StringBuffer buf = new StringBuffer();
		
		final String forwardPath = mapping.findForward("site").getPath();
		
		buf.append(forwardPath);
		if (!forwardPath.endsWith("/")) {
			buf.append('/');	
		}
		
		buf.append(JstlFunctions.encode(projectConfig.getName()));
		buf.append('/');
		
		String sitePath = projectConfig.getSitePath();
		
		if (!StringUtils.isBlank(sitePath)) {
			if (sitePath.startsWith("/")) {
				sitePath = sitePath.substring(1);	
			}
		} else if (sitePath == null){
			sitePath = "";
		}
		
		buf.append(sitePath);
		
		return RequestUtils.absoluteURL(request, buf.toString());
	}
	protected URL getSelfURL(ActionMapping mapping, HttpServletRequest request, String transform) throws MalformedURLException {
		final StringBuilder buf = new StringBuilder(mapping.findForward("viewProjectStatus").getPath());
		
		buf.append("?transform=");
		buf.append(TagUtils.getInstance().encodeURL(transform));
		
		return RequestUtils.absoluteURL(request, buf.toString());
	}
	protected void sendDocument(Document document, Writer writer) throws IOException {
		final XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
		
		out.output(document, writer);
	}
	protected ActionForward sendDocument(Document doc, String transform, ProjectConfigDto projectConfig, int index, ActionMapping mapping, HttpServletRequest request, HttpServletResponse response) throws IOException, MalformedURLException, SAXException, TransformerException {
		final PrintWriter writer = response.getWriter();
	
		if (StringUtils.isBlank(transform)) {
			response.setContentType("application/xml");
			try {
				sendDocument(doc, writer);
			} finally {
				writer.close();
			}
		} else {
			response.setContentType("text/html");
			
			try {
				final URL projectStatusUrl = getSelfURL(mapping, request, transform);
				
				final URL projectSiteUrl;
				final URL issueTrackerURL;

				final String issueTrackerUrl;
				
				if (projectConfig == null) {
					projectSiteUrl = null;
					issueTrackerUrl = null;
				} else {
					projectSiteUrl = getSiteBaseURL(mapping, request, projectConfig);
					issueTrackerUrl = projectConfig.getBugtraqUrl();
				}
				
				if (isNotBlank(issueTrackerUrl)) {
					issueTrackerURL = new URL(issueTrackerUrl);
				} else {
					issueTrackerURL = null;
				}
				
				projectDomBuilder.transform(doc,
						projectSiteUrl, projectStatusUrl, issueTrackerURL,
						index, request.getLocale(), transform, new StreamResult(writer));
				writer.close();
			} catch (NoSuchTransformFormatException e) {
				BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
						new ActionMessage("errors.transform.not.found",
								new String[] { transform }));
				return mapping.findForward("failure");
			}
		}
		
		return null;
	}
}
