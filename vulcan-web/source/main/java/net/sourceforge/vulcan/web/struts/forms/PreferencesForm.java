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
package net.sourceforge.vulcan.web.struts.forms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.vulcan.dto.PreferencesDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class PreferencesForm extends ActionForm {
	public static class NameValue {
		private final String name;
		private final String value;

		public NameValue(final String name, final String value) {
			this.name = name;
			this.value = value;
		}
		public String getName() {
			return name;
		}
		public String getValue() {
			return value;
		}
	}
	
	private NameValue[] availableStyleSheets;
	private PreferencesDto dto = new PreferencesDto();
	
	@Override
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		super.reset(mapping, request);
		
		findAvailableStyleSheets();
	}
	
	public PreferencesDto getDto() {
		return dto;
	}
	
	public NameValue[] getAvailableStyleSheets() {
		return availableStyleSheets;
	}

	@SuppressWarnings("unchecked")
	private void findAvailableStyleSheets() {
		final List<String> paths = new ArrayList<String>();
		
		paths.addAll(getServlet().getServletContext().getResourcePaths("/css/"));
		
		for (Iterator<String> itr = paths.iterator(); itr.hasNext();) {
			final String path = itr.next();
			if (!path.endsWith(".css")) {
				itr.remove();
			}
		}
		
		Collections.sort(paths);
		
		availableStyleSheets = new NameValue[paths.size()];
		
		for (int i=0; i<paths.size(); i++) {
			final String path = paths.get(i);
			
			availableStyleSheets[i] = new NameValue(toName(path), path);
		}
	}

	private static String toName(String path) {
		String[] paths = StringUtils.split(path, '/');
		
		path = paths[paths.length - 1];
		
		path = path.substring(0, path.indexOf('.'));
		
		return path;
	}
}
