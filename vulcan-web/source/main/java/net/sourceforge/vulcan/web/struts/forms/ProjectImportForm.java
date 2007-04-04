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

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.vulcan.core.NameCollisionResolutionMode;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.ArrayUtils;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ProjectImportForm extends ValidatorForm {
	private String url;
	private String nameCollisionResolutionMode;
	private String[] schedulerNames;
	private boolean createSubprojects;
	
	@Override
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		super.reset(mapping, request);
		schedulerNames = ArrayUtils.EMPTY_STRING_ARRAY;
		createSubprojects = false;
	}

	public NameCollisionResolutionMode parseNameCollisionResolutionMode() {
		return NameCollisionResolutionMode.valueOf(nameCollisionResolutionMode);
	}

	public String getNameCollisionResolutionMode() {
		return nameCollisionResolutionMode;
	}
	
	public void setNameCollisionResolutionMode(
			String nameCollisionResolutionMode) {
		this.nameCollisionResolutionMode = nameCollisionResolutionMode;
	}
	
	public boolean isCreateSubprojects() {
		return createSubprojects;
	}

	public void setCreateSubprojects(boolean createSubprojects) {
		this.createSubprojects = createSubprojects;
	}

	public String[] getSchedulerNames() {
		return schedulerNames;
	}
	
	public void setSchedulerNames(String[] schedulerNames) {
		this.schedulerNames = schedulerNames;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
