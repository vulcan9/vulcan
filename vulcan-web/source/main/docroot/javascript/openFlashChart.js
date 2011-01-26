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
function showBuildDetails(url) {
	launchWindowHandler(null, url, "modePopup", "buildDetails");
}

function getFlash(id) {
	var flash = document.getElementById(id);
	
	if (!flash) {
		flash = document.getElementById(id + "object");
	}
	
	return flash;
}

function reloadMetrics(e) {
	var chart = getFlash("metricsChart");
	var m1 = document.getElementById("metric1");
	var m2 = document.getElementById("metric2");
	
	var url = document.getElementById("metricsUrl").value;
	
	url += "&metricLabel1=" + escape(m1.options[m1.selectedIndex].text);
	url += "&metricLabel2=" + escape(m2.options[m2.selectedIndex].text);
	
	chart.reload(url);
	
	return preventDefaultHandler(e);
}

$(document).ready(function() {
	var refreshButton = $("#btnRefresh");
	
	if (refreshButton.length == 1) {
		refreshButton.click(reloadMetrics);
		window.setTimeout(reloadMetrics, 500);
	}
});
