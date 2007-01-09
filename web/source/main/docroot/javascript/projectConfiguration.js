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
function onProjectNameChange(e) {
	var target = getTarget(e);
	var txtWorkDir = document.getElementById("txtWorkDir");
	
	// Set to default if it is blank
	if (txtWorkDir.value == "") {
		txtWorkDir.value = window.defaultWorkDirPattern.replace("${projectName}", target.value);
	} else {
		// Update project name with new one if applicable
		txtWorkDir.value = txtWorkDir.value.replace(target.previousValue, target.value);
	}
	
	target.previousValue = target.value;
}

function registerProjectConfigFormEventHandlers(e) {
	var txtProjectName = document.getElementById("txtProjectName");
	
	if (txtProjectName) {
		txtProjectName.previousValue = txtProjectName.value;
		customAddEventListener(txtProjectName, "change", onProjectNameChange);
	}
	
	var defaultWorkDirPattern = document.getElementById("defaultWorkDirPattern");
	
	if (defaultWorkDirPattern) {
		window.defaultWorkDirPattern = defaultWorkDirPattern.value;
	}
}

customAddEventListener(window, "load", registerProjectConfigFormEventHandlers);
