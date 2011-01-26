/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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


public class LabelForm extends MultipleProjectConfigForm implements DispatchForm {
	private String name;
	private String originalName;
	private String action;

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String oldName) {
		this.originalName = oldName;
	}

	@Override
	public String getAction() {
		return action;
	}

	@Override
	public void setAction(String action) {
		this.action = action;
	}
	
	@Override
	public String getTargetType() {
		return "project label";
	}
}
