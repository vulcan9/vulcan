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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessages;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class JstlFunctions {
	public static String mangle(String s) {
		return s.replaceAll("[ +\\[\\]]", "_");
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

}
