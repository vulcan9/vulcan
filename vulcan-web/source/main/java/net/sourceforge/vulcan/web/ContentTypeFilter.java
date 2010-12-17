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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * J2EE Filter which uses decorator pattern and some custom rules to decide which
 * Content-type header should be sent to a User-Agent (browser).  Specifically, this
 * filter is intended to set the Content-type to "application/xhtml+xml" in most cases.
 * However, if the User-Agent is known to misbehave when such a Content-type is set,
 * the filter will fall back to "text/html".
 * <br><br>
 * Implementation details turn out to complicate things.  In Tomcat 5.5, the default
 * Content-type for a JSP Document is "text/xml", but in WebLogic 9 the default is
 * "text/html".  Therefore, a "suppressContentTypes" init-param is provided to 
 * configure a comma-delimited list of Content-types which should not be allowed
 * to override the Content-type chosen by this filter.
 * <br><br>
 * If a Servlet (JSP or other) sets the Content-type to something that is not found in
 * the "supressContentTypes" list, the Content-type will be overridden to use that value.
 * This allows special cases (such as "application/xml+rss") to override this filter.
 */
public class ContentTypeFilter extends OncePerRequestFilter {
	public static final String HEADER_USER_AGENT = "User-Agent";
	
	public static final String PARAM_DEFAULT_CONTENT_TYPE = "defaultContentType";
	public static final String PARAM_LEGACY_CONTENT_TYPE = "legacyContentType";
	public static final String PARAM_SUPPRESS_CONTENT_TYPES = "suppressContentTypes";
	public static final String PARAM_DETECT_BROWSER = "detectBrowser";
	
	static final String ATTR_DISABLE_SUPPRESSION = ContentTypeFilter.class.getName() + ":DisableSuppression";
	
	private static final Pattern userAgentRegex = Pattern.compile(".*MSIE ([\\d]*).*");
	
	private String defaultContentType;
	private String legacyContentType;
	private String[] suppressContentTypes;
	private boolean detectBrowser;
	
	public ContentTypeFilter() {
		addRequiredProperty(PARAM_DEFAULT_CONTENT_TYPE);
	}
	public void setDefaultContentType(String defaultContentType) {
		this.defaultContentType = defaultContentType;
	}
	public void setLegacyContentType(String legacyContentType) {
		this.legacyContentType = legacyContentType;
	}
	public void setSuppressContentTypes(String suppressContentType) {
		this.suppressContentTypes = suppressContentType.split(",");
	}
	public boolean isDetectBrowser() {
		return detectBrowser;
	}
	public void setDetectBrowser(boolean detectBrowser) {
		this.detectBrowser = detectBrowser;
	}

	public static void disableContentTypeSupression(ServletRequest request) {
		request.setAttribute(ATTR_DISABLE_SUPPRESSION, Boolean.TRUE);
	}

	public static boolean isContentTypeSupressionEnabled(ServletRequest request) {
		Boolean value = (Boolean) request.getAttribute(ATTR_DISABLE_SUPPRESSION);
		return value == null || Boolean.FALSE.equals(value);
	}

	@Override
	protected void initFilterBean() throws ServletException {
		if (detectBrowser && legacyContentType == null) {
			throw new ServletException("You must specify " + PARAM_LEGACY_CONTENT_TYPE +
					" in web.xml when using " + PARAM_DETECT_BROWSER);
		}
		if (suppressContentTypes != null) {
			for (int i=0; i<suppressContentTypes.length; i++) {
				suppressContentTypes[i] = suppressContentTypes[i].trim();
			}
		} else {
			suppressContentTypes = new String[0];
		}
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

		final String contentType = detectContentType(request);
		
		final HttpServletResponseContentTypeWrapper responseWrapper =
			new HttpServletResponseContentTypeWrapper(request, response, suppressContentTypes, contentType);
		
		response.addHeader("Cache-Control", "max-age=0");
		
		chain.doFilter(request, responseWrapper);
	}
	
	private String detectContentType(HttpServletRequest request) {
		final String userAgent = request.getHeader(HEADER_USER_AGENT);
		
		if (!StringUtils.isBlank(userAgent)) {
			final Matcher matcher = userAgentRegex.matcher(userAgent);
			
			if (matcher.matches()) {
				request.setAttribute(Keys.BROWSER_IE, Boolean.TRUE);
				return legacyContentType;	
			}
		}
		
		return defaultContentType;
	}
}
