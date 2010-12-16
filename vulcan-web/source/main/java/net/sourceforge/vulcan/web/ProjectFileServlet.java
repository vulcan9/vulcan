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
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.NoSuchProjectException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class ProjectFileServlet extends HttpServlet {
	static final DateFormat HTTP_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
	static {
		HTTP_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	private class PathInfo {
		String projectName;
		int buildNumber;
	}
	
	ProjectManager projectManager;
	BuildManager buildManager;
	boolean cacheEnabled = true;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		final WebApplicationContext wac = WebApplicationContextUtils
			.getRequiredWebApplicationContext(config.getServletContext());
		
		projectManager = (ProjectManager) wac.getBean(Keys.PROJECT_MANAGER);
		buildManager = (BuildManager) wac.getBean(Keys.BUILD_MANAGER);
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
		
		final PathInfo projPathInfo = getProjectNameAndBuildNumber(pathInfo);
		
		if (isBlank(projPathInfo.projectName)) {
			response.sendRedirect(request.getContextPath());
			return;
		}
		
		final ProjectConfigDto projectConfig;
		
		try {
			projectConfig = projectManager.getProjectConfig(projPathInfo.projectName);
		} catch (NoSuchProjectException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		final String requestURI = request.getRequestURI();
		
		if (projPathInfo.buildNumber < 0) {
			redirectWithBuildNumber(response, projPathInfo, requestURI);
			return;
		}
		
		final ProjectStatusDto buildOutcome = buildManager.getStatusByBuildNumber(projPathInfo.projectName, projPathInfo.buildNumber);
		
		if (buildOutcome == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "No such build " + projPathInfo.buildNumber + " for project Project.");
			return;
		}
		
		final String workDir;
		
		if (StringUtils.isNotBlank(buildOutcome.getWorkDir())) {
			workDir = buildOutcome.getWorkDir();
		} else {
			workDir = projectConfig.getWorkDir();
		}
		
		final File file = getFile(workDir, pathInfo, true);
		
		if (!file.exists()) {
			if (shouldFallback(request, workDir, file)) {
				response.sendRedirect(getFallbackParentPath(request, workDir));
				return;
			}
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		} else if (!file.canRead()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		} else if (file.isDirectory()) {
			if (!pathInfo.endsWith("/")) {
				response.sendRedirect(requestURI + "/");
				return;
			}
			
			final File[] files = getDirectoryListing(file);
			
			request.setAttribute(Keys.DIR_PATH, pathInfo);
			request.setAttribute(Keys.FILE_LIST, files);
			
			request.getRequestDispatcher(Keys.FILE_LIST_VIEW).forward(request, response);
			return;
		}
		
		setContentType(request, response, pathInfo);
		
		final Date lastModifiedDate = new Date(file.lastModified());
		
		if (!checkModifiedSinceHeader(request, lastModifiedDate)) {
			response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		
		setLastModifiedDate(response, lastModifiedDate);
		
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

	protected PathInfo getProjectNameAndBuildNumber(String pathInfo) {
		final PathInfo info = new PathInfo();
		
		int secondSlash = pathInfo.indexOf('/', 1);
		int thirdSlash = -1;
		
		if (secondSlash < 0) {
			secondSlash = pathInfo.length();
		} else {
			thirdSlash = pathInfo.indexOf('/', secondSlash + 1);
		}

		if (thirdSlash < 0) {
			thirdSlash = pathInfo.length();	
		}
		
		info.projectName = pathInfo.substring(1, secondSlash);
		info.buildNumber = -1;
		
		if (thirdSlash > secondSlash) {
			try {
				info.buildNumber = Integer.valueOf(pathInfo.substring(secondSlash + 1, thirdSlash));
			} catch (NumberFormatException e) {
			}
		}
		
		return info;
	}
	
	protected File getFile(String workDir, String pathInfo, boolean stripProjectName) {
		if (stripProjectName) {
			int secondSlash = pathInfo.indexOf('/', 1);
			
			if (secondSlash < 0) {
				return new File(workDir);
			}
			
			int thirdSlash = pathInfo.indexOf('/', secondSlash + 1);
			
			if (thirdSlash < 0) {
				return new File(workDir);
			}
			
			pathInfo = pathInfo.substring(thirdSlash);
		}
		
		return new File(workDir, pathInfo);
	}
	
	private void redirectWithBuildNumber(HttpServletResponse response, final PathInfo projPathInfo, final String requestURI) throws IOException {
		final ProjectStatusDto latestStatus = buildManager.getLatestStatus(projPathInfo.projectName);
		
		final int projectNameIndex = requestURI.indexOf(projPathInfo.projectName);
		
		final StringBuilder sb = new StringBuilder(requestURI.substring(0, projectNameIndex));
		sb.append(projPathInfo.projectName);
		sb.append("/");
		sb.append(latestStatus.getBuildNumber());
		sb.append("/");
		
		int endProjectName = projectNameIndex + projPathInfo.projectName.length() + 1;
		if (endProjectName < requestURI.length()) {
			sb.append(requestURI.substring(endProjectName));
		}
		
		response.sendRedirect(sb.toString());
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

	private String getFallbackParentPath(HttpServletRequest request, String workDir) {
		final StringBuilder pathInfo = new StringBuilder(request.getPathInfo());
		
		while (!getFile(workDir, pathInfo.toString(), true).exists()) {
			pathInfo.delete(pathInfo.lastIndexOf("/"), pathInfo.length());
		}
		
		final StringBuilder buf = new StringBuilder(request.getContextPath());
		buf.append(request.getServletPath());
		buf.append(pathInfo);
		buf.append("/");
		
		return buf.toString();
	}

	private void setContentType(HttpServletRequest request, HttpServletResponse response, final String pathInfo) {
		ContentTypeFilter.disableContentTypeSupression(request);
		
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
		
		response.addHeader("Cache-Control", "max-age=0");
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
	 * @return true if request parameter "fallback" is specified.
	 */
	private boolean shouldFallback(HttpServletRequest request, String workDir, File file) {
		return request.getParameter("fallback") != null;
	}
}
