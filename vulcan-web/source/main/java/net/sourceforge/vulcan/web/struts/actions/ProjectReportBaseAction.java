/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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
import java.util.regex.Pattern;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.ConfigurationStore;
import net.sourceforge.vulcan.core.ProjectDomBuilder;
import net.sourceforge.vulcan.dto.PreferencesDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.NoSuchTransformFormatException;
import net.sourceforge.vulcan.web.Keys;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.RequestUtils;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.context.MessageSource;
import org.xml.sax.SAXException;

public abstract class ProjectReportBaseAction extends Action {
	private static final Pattern bugtraqRegex = Pattern.compile("%bugid%", Pattern.CASE_INSENSITIVE);
	
	protected ConfigurationStore configurationStore;
	protected BuildManager buildManager;
	protected ProjectDomBuilder projectDomBuilder;
	protected ProjectManager projectManager;
	protected MessageSource messageSource;
	
	public ConfigurationStore getConfigurationStore() {
		return configurationStore;
	}
	public void setConfigurationStore(ConfigurationStore store) {
		this.configurationStore = store;
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
	public MessageSource getMessageSource() {
		return messageSource;
	}
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
	protected URL getSelfURL(ActionMapping mapping, HttpServletRequest request, String transform) throws MalformedURLException {
		final StringBuilder buf = new StringBuilder(mapping.findForward("viewProjectStatus").getPath());
		
		return RequestUtils.absoluteURL(request, buf.toString());
	}
	protected void sendDocument(Document document, Writer writer) throws IOException {
		final XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
		
		out.output(document, writer);
	}
	protected ActionForward sendDocument(Document doc, String transform, ProjectConfigDto projectConfig, int buildNumber, Map<String, Object> transformParams, ActionMapping mapping, HttpServletRequest request, HttpServletResponse response) throws IOException, MalformedURLException, SAXException, TransformerException {
		if (StringUtils.isBlank(transform)) {
			final PrintWriter writer = response.getWriter();
			response.setContentType("application/xml");
			try {
				sendDocument(doc, writer);
			} finally {
				writer.close();
			}
		} else {
			try {
				final Map<String, Object> params = new HashMap<String, Object>();
				if (transformParams != null) {
					params.putAll(transformParams);
				}
				
				params.put("locale", request.getLocale().toString());
				params.put("preferences", findPreferences(request));
				params.put("viewProjectStatusURL", getSelfURL(mapping, request, transform));
				params.put("contextRoot", request.getContextPath());
				
				if (projectConfig != null) {
					final String bugtraqUrl = bugtraqRegex.matcher(projectConfig.getBugtraqUrl()).replaceAll("%BUGID%");
					params.put("issueTrackerURL", bugtraqUrl);
				}
				
				final StringWriter tmpWriter = new StringWriter();
				final StreamResult result = new StreamResult(tmpWriter);
				
				final String contentType = projectDomBuilder.transform(
						doc, params, request.getLocale(), transform, result);
				
				if (StringUtils.isNotBlank(contentType)) {
					response.setContentType(contentType);
				} else {
					response.setContentType("application/xml");
				}
				
				final PrintWriter writer = response.getWriter();
				try {
					IOUtils.copy(new StringReader(tmpWriter.toString()), writer);
				} finally {
					writer.close();
				}
			} catch (NoSuchTransformFormatException e) {
				BaseDispatchAction.saveError(request, ActionMessages.GLOBAL_MESSAGE,
						new ActionMessage("errors.transform.not.found",
								new String[] { transform }));
				return mapping.findForward("failure");
			}
		}
		
		return null;
	}
	
	static PreferencesDto findPreferences(HttpServletRequest request) {
		final HttpSession session = request.getSession(false);
		
		if (session != null) {
			final PreferencesDto prefs = (PreferencesDto) session.getAttribute(Keys.PREFERENCES);
			if (prefs != null) {
				return prefs;
			}
		}
		
		return (PreferencesDto) request.getAttribute(Keys.PREFERENCES);
	}
}
