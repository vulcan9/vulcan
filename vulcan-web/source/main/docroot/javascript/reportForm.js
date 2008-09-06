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
	
	this.rangeTypeControls = this.form.find("input[name='rangeType']");
	this.dateRangeControls = this.form.find("input[name='dateRangeSelector']");
	
	this.startDateControl = this.form.find("input[name='startDate']");
	this.endDateControl = this.form.find("input[name='endDate']");
	
	this.projectControls = this.form.find("input[name='projectNames']");
}

ReportForm.prototype.initialize = function() {
	console.debug("ReportForm.initialize");
	
	this.rangeTypeControls.change(rangeTypeChanged);
	this.dateRangeControls.change(dateRangeChanged);
	this.startDateControl.focus(dateRangeFocus);
	this.endDateControl.focus(dateRangeFocus);
	
	rangeTypeChanged();
	dateRangeChanged.apply(this.dateRangeControls.filter(":checked"), null);
}

function rangeTypeChanged() {
	console.debug("rangeTypeChanged this=%o reportForm=%o", this, reportForm);
	
	reportForm.rangeTypeControls.each(function(e) {
		console.debug("%s[%d]: %s", this.name, e, this.checked ? "checked" : "unchecked");
		
		var tr = $(this).parents("tr").get(0);
		var controls = $(tr).find("input[name!='" + this.name + "']");
		
		if (this.checked) {
			controls.removeAttr("disabled");
		} else {
			controls.attr("disabled", "disabled");
		}
	});
	
	// Switch project checkboxes to radio buttons or vice-versa.
	var selectedType = reportForm.rangeTypeControls.filter(":checked").val();
	var type = selectedType == "index" ? "radio" : "checkbox";
	
	reportForm.projectControls.each(function() {
		this.setAttribute("type", type);
	});
}

function dateRangeChanged() {
	console.debug("dateRangeChanged start=%o", reportForm.startDateControl);
	
	var now = new Date();
	
	var start = reportForm.startDateControl.val();
	var end = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);

	switch ($(this).val()) {
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
	
	reportForm.startDateControl.val((start.getMonth()+1) + "/" + start.getDate() + "/" + start.getFullYear());
	reportForm.endDateControl.val((end.getMonth()+1) + "/" + end.getDate() + "/" + end.getFullYear());
}

function dateRangeFocus() {
	console.debug("dateRangeFocus");
	$("#dateRangeSpecific").attr("checked", "checked");
}

function registerReportEventHandlers(e) {
	window.reportForm = new ReportForm($("#reportForm"));

	window.reportForm.initialize();
}

$(document).ready(registerReportEventHandlers);
