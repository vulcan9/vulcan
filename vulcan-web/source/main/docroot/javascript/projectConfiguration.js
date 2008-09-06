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
function onProjectNameChange() {
	var newName = $(this).val();
	
	if (newName == "") {
		return;
	}
	
	var txtWorkDir = $("#txtWorkDir");
	
	if (txtWorkDir.val() == "") {
		// Use default directory when current work directory is blank.
		txtWorkDir.val($("#defaultWorkDirPattern").val().replace("${projectName}", newName));
	} else {
		// Update project name with new one if applicable
		txtWorkDir.val(txtWorkDir.val().replace(this.previousValue, newName));
	}
	
	this.previousValue = newName;
}

function registerProjectConfigFormEventHandlers(e) {
	var txtProjectName = $("#txtProjectName");
	
	txtProjectName.change(onProjectNameChange);
	
	txtProjectName.get(0).previousValue = txtProjectName.val();
}

$(document).ready(registerProjectConfigFormEventHandlers);
