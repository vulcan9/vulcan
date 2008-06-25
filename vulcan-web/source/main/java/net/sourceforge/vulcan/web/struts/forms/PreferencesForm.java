/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2007 Chris Eldredge
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
package net.sourceforge.vulcan.web.struts.forms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sourceforge.vulcan.dto.PreferencesDto;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.Keys;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class PreferencesForm extends ActionForm {
	private PreferencesDto config;
	private String toggleLabel;
	private List<String> availableStylesheets;
	
	public PreferencesDto getConfig() {
		return config;
	}
	
	@Override
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		super.reset(mapping, request);
		
		final HttpSession session = request.getSession(false);
		if (request.getParameter("fullReset") == null && session != null) {
			PreferencesDto prefs = (PreferencesDto) request.getAttribute(Keys.PREFERENCES);
			if (prefs == null && session != null) {
				prefs = (PreferencesDto) session.getAttribute(Keys.PREFERENCES);	
			}
			
			if (prefs != null) {
				config = (PreferencesDto) prefs.copy();
			}
		}
		
		if (config == null) {
			config = new PreferencesDto();
		}
		
		toggleLabel = null;
		availableStylesheets = findAvailableStylesheets();
	}

	public List<String> getAvailableStylesheets() {
		return availableStylesheets;
	}
	
	public String getToggleLabel() {
		return toggleLabel;
	}
	
	public void setToggleLabel(String toggleLabel) {
		this.toggleLabel = toggleLabel;
	}
	
	@SuppressWarnings("unchecked")
	List<String> findAvailableStylesheets() {
		final Set<String> paths = getServlet().getServletContext().getResourcePaths("/css/");
		
		final List<String> list = new ArrayList<String>();
		
		for (String path : paths) {
			if (path.endsWith(".css")) {
				// drop the leading path and the extension
				// such that "/css/foo.css" becomes "foo"
				list.add(path.substring(path.lastIndexOf('/')+1, path.length()-4));
			}
		}
		
		Collections.sort(list);
		
		return list;
	}
}
