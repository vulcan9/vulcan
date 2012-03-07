/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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

function updateStatusLater() {
	if (!window.importProjectRequestComplete) {
		window.setTimeout(updateStatus, 2500);
	}
}

function updateStatus() {
	$("#status").load(contextRoot + "ajax/importProjectStatus.jsp", null, updateStatusLater);
}

function onProjectImportFormSubmit(x) {
	$(".error").hide();
	
	var params = {};
	
	$("#projectImportForm input").each(function(i) {
		if ("radio" == this.type || "checkbox" == this.type) {
			if (this.checked) {
				params[this.name] = this.value;
			}
		} else {
			params[this.name] = this.value;
		}
	});
	
	window.importProjectRequestComplete = false;
	
	jQuery.get(this.action, params, onProjectImportProcessingComplete);
	
	$("#status").removeClass("hidden");

	updateStatusLater();
	
	return false;
}

function onProjectImportProcessingComplete(data, textStatus) {
	var contents;
	
	if (data && data.getElementById) {
		contents = document.importNode(data.getElementById("content"), true);
	} else {
		contents = jQuery("<div/>").append(data.replace(/<script(.|\s)*?\/script>/g, "")).find("#content");
	}
	
	$("#content").replaceWith(contents);
	
	window.importProjectRequestComplete = true;
	
	// re-register events for newly imported elements
	onLoadProjectImportForm();
}

function onLoadProjectImportForm() {
	$("#projectImportForm").submit(onProjectImportFormSubmit);
}

$(document).ready(onLoadProjectImportForm);
