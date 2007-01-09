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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.dto.PreferencesDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class PreferencesFilter extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
		paramToCookie(request, response, "sortColumn");
		paramToCookie(request, response, "sortOrder");
		
		createPreferences(request);
		
		chain.doFilter(request, response);
	}

	private void createPreferences(HttpServletRequest request) {
		final PreferencesDto prefs = new PreferencesDto();
		
		prefs.setSortColumn(getFromParamOrCookie(request, "sortColumn"));
		prefs.setSortOrder(getFromParamOrCookie(request, "sortOrder"));
		
		request.setAttribute("preferences", prefs);
	}

	private String getFromParamOrCookie(HttpServletRequest request, String paramName) {
		String value = request.getParameter(paramName);
		
		if (value != null) {
			return value;
		}
		
		final Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			final String cookieName = "VULCAN_" + paramName;
			
			for (Cookie c : cookies) {
				if (c.getName().equals(cookieName)) {
					return c.getValue();
				}
			}
		}
		
		return null;
	}

	private boolean paramToCookie(HttpServletRequest request, HttpServletResponse response, String paramName) {
		final String value = request.getParameter(paramName);
		if (StringUtils.isNotBlank(value)) {
			response.addCookie(new Cookie("VULCAN_" + paramName, value));
			return true;
		}
		return false;
	}
}