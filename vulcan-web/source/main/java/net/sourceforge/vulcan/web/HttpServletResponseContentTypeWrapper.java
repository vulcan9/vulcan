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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public final  class HttpServletResponseContentTypeWrapper extends HttpServletResponseWrapper {
	private final String[] suppressContentTypes;
	private final String overrideContentType;
	private final HttpServletRequest request;
	
	HttpServletResponseContentTypeWrapper(HttpServletRequest request, HttpServletResponse response, String[] suppressContentTypes, String overrideContentType) {
		super(response);
		this.request = request;
		this.suppressContentTypes = suppressContentTypes;
		this.overrideContentType = overrideContentType;
	}
	@Override
	public void setContentType(String requestType) {
		String type = requestType;
		
		if (ContentTypeFilter.isContentTypeSupressionEnabled(request)) {
			for (String s : suppressContentTypes) {
				if (type.startsWith(s)) {
					type = this.overrideContentType;
					break;
				}
			}
		}
		
		super.setContentType(type);
	}
}