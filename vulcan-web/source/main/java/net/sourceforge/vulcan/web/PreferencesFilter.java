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
import javax.servlet.http.HttpSession;

import net.sourceforge.vulcan.dto.PreferencesDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class PreferencesFilter extends OncePerRequestFilter {
	private PreferencesStore store;
	
	@Override
	protected void initFilterBean() throws ServletException {
		super.initFilterBean();
		
		store = (PreferencesStore) WebApplicationContextUtils.getRequiredWebApplicationContext(
				getServletContext()).getBean("preferencesStore", PreferencesStore.class);
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
		setupPreferences(request);
		
		chain.doFilter(request, response);
	}

	private void setupPreferences(HttpServletRequest request) {
		final HttpSession session = request.getSession(false);
		
		if (session != null && session.getAttribute(Keys.PREFERENCES) != null) {
			return;
		}
		
		final PreferencesDto prefs;
		
		final String cookieData = getCookieData(request, Keys.PREFERENCES);
		
		if (cookieData == null) {
			prefs = store.getDefaultPreferences();
		} else {
			prefs = store.convertFromString(cookieData);
		}
		
		if (session == null) {
			request.setAttribute(Keys.PREFERENCES, prefs);	
		} else if (session.getAttribute(Keys.PREFERENCES) == null) {
			session.setAttribute(Keys.PREFERENCES, prefs);
		}
	}

	private String getCookieData(HttpServletRequest request, String cookieName) {
		final Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie c : cookies) {
				if (c.getName().equals(cookieName)) {
					return c.getValue();
				}
			}
		}
		
		return null;
	}
}