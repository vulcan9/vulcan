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
package net.sourceforge.vulcan.web;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.io.IOUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ProjectFileServlet extends HttpServlet {
	static final DateFormat HTTP_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
	static {
		HTTP_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	ProjectManager projectManager;
	boolean cacheEnabled;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		final WebApplicationContext wac = WebApplicationContextUtils
			.getRequiredWebApplicationContext(config.getServletContext());
		
		projectManager = (ProjectManager) wac.getBean(Keys.PROJECT_MANAGER);
	}
	
	public boolean isCacheEnabled() {
		return cacheEnabled;
	}
	
	public void setCacheEnabled(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		final String pathInfo = request.getPathInfo();
		
		if (isBlank(pathInfo)) {
			response.sendRedirect(request.getContextPath());
			return;
		}
		
		final String projectName = getProjectName(pathInfo);

		if (isBlank(projectName)) {
			response.sendRedirect(request.getContextPath());
			return;
		}
		
		final ProjectConfigDto projectConfig;
		
		try {
			projectConfig = projectManager.getProjectConfig(projectName);
		} catch (NoSuchProjectException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		final File file = getFile(projectConfig, pathInfo, true);
		
		if (!file.exists()) {
			if (shouldFallback(request, projectConfig, file)) {
				response.sendRedirect(getFallbackParentPath(request, projectConfig));
				return;
			}
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		} else if (!file.canRead()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		} else if (file.isDirectory()) {
			if (!pathInfo.endsWith("/")) {
				response.sendRedirect(request.getRequestURI() + "/");
				return;
			}
			
			final File[] files = getDirectoryListing(file);
			
			request.setAttribute(Keys.DIR_PATH, pathInfo);
			request.setAttribute(Keys.FILE_LIST, files);
			
			request.getRequestDispatcher(Keys.FILE_LIST_VIEW).forward(request, response);
			return;
		}
		
		setContentType(response, pathInfo);
		
		final Date lastModifiedDate = new Date(file.lastModified());
		
		if (!checkModifiedSinceHeader(request, lastModifiedDate)) {
			response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		
		setLastModifiedDate(response, lastModifiedDate);
		//response.setHeader("Cache-Control", "must-revalidate");
		
		response.setContentLength((int)file.length());
		
		final FileInputStream fis = new FileInputStream(file);
		final ServletOutputStream os = response.getOutputStream();
		
		sendFile(fis, os);
	}

	protected void sendFile(final InputStream is, final OutputStream os) throws IOException {
		IOException ioe = null;
		
		try {
			IOUtils.copy(is, os);
		} catch (IOException e) {
			ioe = e;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				if (ioe == null) {
					ioe = e;
				}
			}
			try {
				os.close();
			} catch (IOException e) {
				if (ioe == null) {
					ioe = e;
				}
			}
		}
		if (ioe != null) {
			throw ioe;
		}
	}

	protected String getProjectName(String pathInfo) {
		int secondSlash = pathInfo.indexOf('/', 1);
		
		if (secondSlash < 0) {
			secondSlash = pathInfo.length();
		}
		
		return pathInfo.substring(1, secondSlash);
	}
	
	protected File getFile(ProjectConfigDto projectConfig, String pathInfo, boolean stripProjectName) {
		if (stripProjectName) {
			int secondSlash = pathInfo.indexOf('/', 1);
			
			if (secondSlash < 0) {
				return new File(projectConfig.getWorkDir());
			}
			
			pathInfo = pathInfo.substring(secondSlash);
		}
		
		return new File(projectConfig.getWorkDir(), pathInfo);
	}
	
	private File[] getDirectoryListing(final File file) throws IOException {
		final File[] files = file.listFiles();
		
		if (files == null) {
			throw new IOException();
		}
		
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File o1, File o2) {
				if (o1.isDirectory() && !o2.isDirectory()) {
					return -1;
				} else if (!o1.isDirectory() && o2.isDirectory()) {
					return 1;
				}
				
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		return files;
	}

	private String getFallbackParentPath(HttpServletRequest request, ProjectConfigDto projectConfig) {
		final StringBuilder pathInfo = new StringBuilder(request.getPathInfo());
		
		while (!getFile(projectConfig, pathInfo.toString(), true).exists()) {
			pathInfo.delete(pathInfo.lastIndexOf("/"), pathInfo.length());
		}
		
		final StringBuilder buf = new StringBuilder(request.getContextPath());
		buf.append(request.getServletPath());
		buf.append(pathInfo);
		buf.append("/");
		
		return buf.toString();
	}

	private void setContentType(HttpServletResponse response, final String pathInfo) {
		if (response instanceof HttpServletResponseContentTypeWrapper) {
			((HttpServletResponseContentTypeWrapper)response).disableContentTypeSupression();
		}
		
		final String contentType = getServletContext().getMimeType(pathInfo);
		
		if (isBlank(contentType)) {
			response.setContentType("text/plain");
		} else {
			response.setContentType(contentType);
		}
	}
	
	private void setLastModifiedDate(HttpServletResponse response, Date date) {
		if (!cacheEnabled) {
			return;
		}
		
		synchronized (HTTP_DATE_FORMAT) {
			response.setHeader("Last-Modified", HTTP_DATE_FORMAT.format(date));
		}
	}
	
	/**
	 * @return <code>true</code> if the file has been modified more recently and should be retransmitted
	 * <br> <code>false</code> if the file has not been modified.
	 */
	private boolean checkModifiedSinceHeader(HttpServletRequest request, Date lastModified) {
		if (!cacheEnabled) {
			return true;
		}
		
		lastModified = new Date((lastModified.getTime() / 1000) * 1000);
		
		final String header = request.getHeader("If-Modified-Since");
		
		if (isBlank(header)) {
			return true;
		}
		
		synchronized (HTTP_DATE_FORMAT) {
			try {
				final Date lastChecked = HTTP_DATE_FORMAT.parse(header);
				
				return lastModified.after(lastChecked);
			} catch (ParseException e) {
				return true;
			}
		}
	}
	
	/**
	 * @return true if request parameter "fallback" is specified,
	 * or the requested path matches the default ProjectConfigDto.SitePath.
	 */
	private boolean shouldFallback(HttpServletRequest request, ProjectConfigDto projectConfig, File file) {
		if (request.getParameter("fallback") != null) {
			return true;
		}
		
		if (isBlank(projectConfig.getSitePath())) {
			return false;
		}
		
		final File siteFile = getFile(projectConfig, projectConfig.getSitePath(), false);
		
		return file.equals(siteFile);
	}
}
