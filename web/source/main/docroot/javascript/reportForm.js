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
function ReportForm(form, radioName) {
	this.form = form;
	this.radioName = radioName;
	this.visibleControls = new Object();
	
	this.radioChanged = ReportForm.prototype.radioChanged.bind(this);
}

ReportForm.prototype.initialize = function() {
	var inputs = this.form[this.radioName];
	
	for (var i=0; i<inputs.length; i++) {
		customAddEventListener(inputs[i], "change", this.radioChanged);
	}
	
	this.radioChanged();
}

ReportForm.prototype.radioChanged = function() {
	var radios = this.form[this.radioName];
	
	for (var i=0; i<radios.length; i++) {
		var inputs = findAncestorByTagName(radios[i], "tr").getElementsByTagName("input");
		
		for (var j=0; j<inputs.length; j++) {
			if (radios[i] != inputs[j]) {
				inputs[j].disabled = !radios[i].checked;
			}
		}
	}
}

function getSelectedValue(radios) {
	for (var i=0; i<radios.length; i++) {
		if (radios[i].checked) {
			return radios[i].value;
		}
	}
	return null;
}

function registerReportEventHandlers(e) {
	window.reportForm = new ReportForm(document.getElementById("reportForm"), "rangeType");
	window.reportForm.defaultRadioChanged = window.reportForm.radioChanged;
	window.reportForm.radioChanged = (function() {
		this.defaultRadioChanged();
		
		var value = getSelectedValue(this.form[this.radioName]);
		
		if (value == "index") {
			type = "radio";
		} else {
			type = "checkbox";
		}
		
		var clickers = this.form["projectNames"];
		
		for (var i=clickers.length-1; i>=0; i--) {
			clickers[i].setAttribute("type", type);
		}
	}).bind(window.reportForm);
	window.reportForm.initialize();
}

customAddEventListener(window, "load", registerReportEventHandlers);
