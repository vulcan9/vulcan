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
function commitPreferences(e) {
	var target = getTarget(e);
	var form = target.form;
	
	var modes = form.windowMode;
	var selected;
	
	for (var i=0; i<modes.length; i++) {
		if (modes[i].checked) {
			selected = modes[i].value;
		}
	}
	
	if (selected) {
		createCookie("VULCAN_windowMode", selected, 3650);
	}
	
	preventDefaultHandler(e);
}

function goHome(e) {
	preventDefaultHandler(e);
	
	window.location = document.getElementById("homeLink").href;
}

function registerPreferenceEventHandlers(e) {
	customAddEventListener(document.getElementById("prefOkButton"), "click", commitPreferences);
	customAddEventListener(document.getElementById("prefOkButton"), "click", goHome);
	customAddEventListener(document.getElementById("prefCancelButton"), "click", goHome);
	
	var mode = readCookie("VULCAN_windowMode");
	if (mode) {
		document.getElementById(mode).checked = true;
	}
}

customAddEventListener(window, "load", registerPreferenceEventHandlers);
