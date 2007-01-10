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
/* $Id$
 * $HeadURL$
 */

function PrunableTable(table, cookieName) {
	this.table = table;
	this.rows = table.getElementsByTagName("tr");
	this.editMode = false;
	this.cookieName = cookieName;
}

PrunableTable.prototype.init = function() {
	var headCell = document.createElement("th");
	headCell.setAttribute("title", "Display checked project in compact mode");
	headCell.style.display = "none";
	headCell.appendChild(document.createTextNode("Show"));
	
	this.rows[0].insertBefore(headCell, this.rows[0].firstChild);
	
	var cookie = readCookie(this.cookieName);
	var selectedProjects = null;
	
	if (cookie) {
		selectedProjects = cookie.split(",");
	}
	
	for (var i=1; i<this.rows.length; i++) {
		var projectName = this.rows[i].getElementsByTagName("a")[0].textContent;
		var cell = document.createElement("td");
		
		var checkbox = document.createElement("input");
		checkbox.setAttribute("type", "checkbox");
		checkbox.setAttribute("name", "projects");
		checkbox.setAttribute("value", projectName);
		
		if (checkRows) {
			customAddEventListener(checkbox, "click", checkRows);
		}
		
		cell.style.display = "none";
		cell.appendChild(checkbox);
		
		this.rows[i].insertBefore(cell, this.rows[i].firstChild);
		
		if (selectedProjects) {
			if (selectedProjects.contains(projectName)) {
				checkbox.checked = true;
			} else {
				checkbox.checked = false;
				this.rows[i].style.display = "none";
			}
		} else {
			checkbox.checked = true;
		}
	}
	
	this.enableRefresh();
}

PrunableTable.prototype.enableRefresh = function() {
	this.refreshJobId = window.setTimeout(function() { window.location.reload(); }, 30000);
}


PrunableTable.prototype.toggleEditMode = function() {
	var cellStyle = "none";

	this.editMode = !this.editMode;
		
	if (this.editMode) {
		cellStyle = "table-cell";
		window.clearTimeout(this.refreshJobId);
	} else {
		this.enableRefresh();
	}
	
	var selectedProjects = [];
	
	for (var i=0; i<this.rows.length; i++) {
		var checkbox = this.rows[i].getElementsByTagName("input")[0];
		
		if (this.editMode) {
			this.rows[i].style.display = "table-row";
		} else if (checkbox && !checkbox.checked) {
			this.rows[i].style.display = "none";
		} else if (checkbox && checkbox.checked) {

			selectedProjects[selectedProjects.length] = checkbox.value;
		}
		
		this.rows[i].firstChild.style.display = cellStyle;
	}
	
	if (!this.editMode) {
		createCookie(this.cookieName, selectedProjects.join(","), 3650);
	}
}

function toggleEditMode(e) {
	if (e.preventDefault) {
		e.preventDefault();
	}
	
	window.projectTable.toggleEditMode();
	
	var cls = "edit";
	
	if (window.projectTable.editMode) {
		cls = "editing";
	}
		
	this.setAttribute("class", cls);
	
	return false;
}

function initProjectsTable() {
	var link = document.getElementById("chooseProjectsLink");
	
	if (link) {
		window.projectTable = new PrunableTable(document.getElementById("projectTable"), "VULCAN_visibleProjects");
		window.projectTable.init();
		customAddEventListener(link, "click", toggleEditMode);
	}
}

customAddEventListener(window, 'load', initProjectsTable);