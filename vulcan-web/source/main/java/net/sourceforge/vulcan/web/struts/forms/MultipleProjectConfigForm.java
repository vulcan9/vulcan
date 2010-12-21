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


import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

public class MultipleProjectConfigForm extends ValidatorForm implements DispatchForm {
	private String[] projectNames;
	private String message;
	private String action;
	
	@Override
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		super.reset(mapping, request);
		projectNames = ArrayUtils.EMPTY_STRING_ARRAY;
	}
	
	public String[] getProjectNames() {
		return projectNames;
	}
	
	public void setProjectNames(String[] projectNames) {
		this.projectNames = projectNames;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
	
	public String getName() {
		return StringUtils.join(projectNames, ", ");
	}

	public String getOriginalName() {
		return getName();
	}

	public String getTargetType() {
		return "project";
	}
}
