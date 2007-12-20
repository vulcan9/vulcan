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

import static org.apache.commons.lang.time.DateUtils.MILLIS_PER_SECOND;
import static org.apache.commons.lang.time.DateUtils.MILLIS_PER_MINUTE;
import static org.apache.commons.lang.time.DateUtils.MILLIS_PER_HOUR;
import static org.apache.commons.lang.time.DateUtils.MILLIS_PER_DAY;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessages;
import org.springframework.context.MessageSource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class JstlFunctions {
	private static MessageSource messageSource;
	
	public static String mangle(String s) {
		return s.replaceAll("[ +\\[\\]]", "_");
	}
	
	public static String encode(String s) {
		try {
			// replace + with %20 because + is only appropriate for query strings, not for paths.
			return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static void setStatus(HttpServletResponse response, int code) {
		response.setStatus(code);
	}
	
	@SuppressWarnings("unchecked")
	public static List<String> getActionErrorPropertyList(HttpServletRequest request) {
		final List<String> errorList = new ArrayList<String>();
		
		final ActionMessages errors = (ActionMessages) request.getAttribute(Globals.ERROR_KEY);
		if (errors != null) {
			Iterator<String> itr = errors.properties();
			while (itr.hasNext()) {
				errorList.add(itr.next());
			}
		}
		
		return errorList;
	}

	public static ProjectStatusDto getOutcomeByBuildNumber(PageContext pageContext, String projectName, int buildNumber) {
		final WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(pageContext.getServletContext());
		
		final BuildManager mgr = (BuildManager) ctx.getBean("buildManager", BuildManager.class);
		
		return mgr.getStatusByBuildNumber(projectName, buildNumber);
	}
	
	public static String formatElapsedTime(PageContext pageContext, long elapsedTime, int verbosity) {
		final StringBuilder sb = new StringBuilder();

		int count = 0;
		
		for (long millisPerUnit : Arrays.asList(MILLIS_PER_DAY, MILLIS_PER_HOUR, MILLIS_PER_MINUTE, MILLIS_PER_SECOND)) {
			final String messageKey;
			
			if (elapsedTime < millisPerUnit && sb.length() > 0) {
				//break;
			}

			if (millisPerUnit == MILLIS_PER_DAY) {
				messageKey = "time.day";
			} else if (millisPerUnit == MILLIS_PER_HOUR) {
				messageKey = "time.hour";
			} else if (millisPerUnit == MILLIS_PER_MINUTE) {
				messageKey = "time.minute";
			} else {
				messageKey = "time.second";
			}
			
			final long units = elapsedTime / millisPerUnit;

			if (units == 0) {
				continue;
			}
			
			elapsedTime -= units * millisPerUnit;

			if (sb.length() > 0) {
				sb.append(", ");
			}

			sb.append(units);
			sb.append(" ");
			sb.append(formatTimeUnit(pageContext, messageKey,  units > 1));
			
			if (++count == verbosity) {
				break;
			}
		}
		
		return sb.toString();
	}
	
	static void setMessageSource(MessageSource messageSource) {
		JstlFunctions.messageSource = messageSource;
	}
	
	private static String formatTimeUnit(PageContext pageContext, String key, boolean plural) {
		if (messageSource == null) {
			final WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(pageContext.getServletContext());
			JstlFunctions.messageSource = ctx;
		}
		
		if (plural) {
			key += "s";
		}
		
		return messageSource.getMessage(key, null, pageContext.getRequest().getLocale());
	}
}
