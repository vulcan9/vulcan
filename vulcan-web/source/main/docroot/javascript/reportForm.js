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
function ReportForm(form) {
	this.form = form;
	this.visibleControls = new Object();
	
	this.radioChanged = ReportForm.prototype.radioChanged.bind(this);
}

ReportForm.prototype.initialize = function() {
	customAddEventListener(this.form["startDate"], "focus", this.dateRangeFocus);
	customAddEventListener(this.form["endDate"], "focus", this.dateRangeFocus);
	
	var inputs = this.form["rangeType"];

	for (var i=0; i<inputs.length; i++) {
		customAddEventListener(inputs[i], "change", this.radioChanged);
	}

	var inputs = this.form["dateRangeSelector"];
	var target = new Object();
	
	for (var i=0; i<inputs.length; i++) {
		customAddEventListener(inputs[i], "change", this.dateRangeChanged);
		if (inputs[i].checked) {
			target.target = inputs[i];
		}
	}
	
	this.radioChanged();
	this.dateRangeChanged(target);
}

ReportForm.prototype.radioChanged = function() {
	var radios = this.form["rangeType"];
	
	for (var i=0; i<radios.length; i++) {
		var inputs = findAncestorByTagName(radios[i], "tr").getElementsByTagName("input");
		
		for (var j=0; j<inputs.length; j++) {
			if (radios[i] != inputs[j]) {
				inputs[j].disabled = !radios[i].checked;
			}
		}
	}
}

ReportForm.prototype.dateRangeChanged = function(event) {
	var target = getTarget(event);
	var startInput = target.form["startDate"];
	var endInput = target.form["endDate"];

	var now = new Date();
	
	var start = startInput.value;
	var end = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);

	switch (target.value) {
		case "today":
			start = now;
			break;
		case "weekToDate":
			start = new Date(now.getFullYear(), now.getMonth(), now.getDate() - now.getDay());
			break;
		case "monthToDate":
			start = new Date(now.getFullYear(), now.getMonth(), 1);
			break;
		case "yearToDate":
			start = new Date(now.getFullYear(), 0, 1);
			break;
		default:
			return;
	}
	
	startInput.value = (start.getMonth()+1) + "/" + start.getDate() + "/" + start.getFullYear();
	endInput.value = (end.getMonth()+1) + "/" + end.getDate() + "/" + end.getFullYear();
}

ReportForm.prototype.dateRangeFocus = function() {
	document.getElementById("dateRangeSpecific").checked = true;
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
	window.reportForm = new ReportForm(document.getElementById("reportForm"));
	window.reportForm.defaultRadioChanged = window.reportForm.radioChanged;
	window.reportForm.radioChanged = (function() {
		this.defaultRadioChanged();
		
		var value = getSelectedValue(this.form["rangeType"]);
		
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
