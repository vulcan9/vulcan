/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2008 Chris Eldredge
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

function log(message) {
	if (!window.console) {
		return;
	}
	
	if (window.console.debug) {
		window.console.debug(message);
	} else if (window.console.log) {
		window.console.log(message);
	}
}

function getMetaContent(name, defaultValue) {
	var meta = $("meta[name='" + name + "']");
	
	if (meta && meta.attr("content")) {
		return meta.attr("content");
	}
	
	return defaultValue;
}

function drillDown(input, name) {
	input.form.focus.value = name;
}

function targetObject(input, name) {
	input.form.target.value = name;
}

function getTarget(event) {
	if (!event) {
		var event = window.event;
	}
	
	if (event && event.target) {
		return event.target;
	} else if (event && event.srcElement) {
		return event.srcElement;
	}
	
	return this;
}

function findAncestorByTagName(node, tagName) {
	if (node.tagName.toLowerCase() == tagName) {
		return node;
	} else if (node.parentNode == null) {
		return null;
	}
	return findAncestorByTagName(node.parentNode, tagName);
}

function findPreviousSiblingByTagName(node, tagName) {
	if (node.tagName && node.tagName.toLowerCase() == tagName) {
		return node;
	} else if (node.previousSibling == null) {
		return null;
	}
	
	return findPreviousSiblingByTagName(node.previousSibling, tagName);
}

function findNextSiblingByTagName(node, tagName) {
	if (node.tagName && node.tagName.toLowerCase() == tagName) {
		return node;
	} else if (node.nextSibling == null) {
		return null;
	}
	return findNextSiblingByTagName(node.nextSibling, tagName);
}

function getName(buttonName) {
	return buttonName.split('-')[1];
}

function swap(inputs, type, i, j) {
	var tmp;
	
	if (type == 'checkbox') {
		tmp = inputs[i].checked;
		inputs[i].checked = inputs[j].checked;
		inputs[j].checked = tmp;
	} else {
		tmp = inputs[i].value;
		inputs[i].value = inputs[j].value;
		inputs[j].value = tmp;
	}
}
	
function moveUpRows(o) {
	var target = getTarget(o);
	var name = getName(target.name);
	var inputs = target.form[name];
	var selections = target.form['select-' + name];
	
	for (var i=1; i<selections.length; i++) {
		if (selections[i].checked && !selections[i-1].checked) {
			swap(selections, 'checkbox', i-1, i);
			swap(inputs, 'text', i-1, i);
		}
	}
	window.previousIndex = -1;
	window.hasPendingChanges = true;
}

function moveDownRows(o) {
	var target = getTarget(o);
	var name = getName(target.name);
	var inputs = target.form[name];
	var selections = target.form['select-' + name];
	
	for (var i=selections.length-2; i>=0; i--) {
		if (selections[i].checked && !selections[i+1].checked) {
			swap(selections, 'checkbox', i, i+1);
			swap(inputs, 'text', i, i+1);
		}
	}
	window.previousIndex = -1;
	window.hasPendingChanges = true;
}

function addRow(o) {
	var button = getTarget(o);
	var name = getName(button.name);
	
	var table = findPreviousSiblingByTagName(button, 'table');
	var tbody = findNextSiblingByTagName(table.firstChild, 'tbody');
	
	var tr = document.createElement('tr');
	tbody.appendChild(tr);
	
	var td = document.createElement('td');
	
	var input = document.createElement('input');
	input.type = 'checkbox';
	input.name = 'select-' + name;
	
	$(input).click(checkRows);
	
	td.appendChild(input);
	tr.appendChild(td);

	input = document.createElement('input');
	input.type = 'text';
	input.name = name;

	td = document.createElement('td');	
	td.appendChild(input);
	tr.appendChild(td);
	
	window.hasPendingChanges = true;
}

function removeRow(o) {
	var target = getTarget(o);
	
	var name = getName(target.name);
	var inputs = target.form[name];
	var selections = target.form['select-' + name];
	var tr;

	if (!selections) {
		return;
	}
	
	if (!selections.length) {
		if (selections.checked) {
			tr = findAncestorByTagName(selections, 'tr');
			tr.parentNode.removeChild(tr);
		}
		return;
	}
	
	for (var i=selections.length-1; i>=0; i--) {
		if (selections[i].checked) {
			tr = findAncestorByTagName(selections[i], 'tr');
			tr.parentNode.removeChild(tr);
		}
	}
	
	window.hasPendingChanges = true;
}

function checkRows(e) {
	var target = getTarget(e);
	var all = target.form[this.name];

	if (!all || !all.length) {
		all = getCheckboxesInForm(target);
	}
	
	for (var i=0; i<all.length; i++) {
		if (target == all[i]) {
			break;
		}
	}

	if (e && e.shiftKey
			&& window.previousIndex >= 0
			&& window.previousName == this.name) {
		
		var j = window.previousIndex;
		if (j < i) {
			j = i;
			i = window.previousIndex;
		}
		
		var on = all[window.previousIndex].checked;

		for (var c=i+1; c<=j; c++) {
			if (all[c].checked != on) {
				all[c].checked = on;
				
				// fire event handler for each checkbox
				$(all[c]).trigger("toggle");
			}
		}
	}
	
	$(target).trigger("toggle");
	
	window.previousIndex = i;
	window.previousName = this.name;
}

/* Alternate method of obtaining all inputs in a form
	with the same name; only used in IE. */
function getCheckboxesInForm(target) {
	var form = target.form;
	var all = form.elements;
	var checkboxes = [];

	for (var i=0; i<all.length; i++) {
		if (all[i].type == "checkbox" && all[i].name == target.name) {
			checkboxes.push(all[i]);
		}
	}
	
	return checkboxes;
}

function preventShiftClickTextSelectionHandler(event) {
	if (event && event.shiftKey) {
		return preventDefaultHandler(event);
	}
	return true;
}


function setPreference(event) {
	event.preventDefault();
	
	if (event.canceled) return false;
	
	var url = $(this).attr("href");
	$("#content").loadCustom(url, null, "#content");
	
	return false;
}

function confirmHandler(event) {
	if (!confirm(window.confirmMessage)) {
		event.canceled = true;
		return preventDefaultHandler(event);
	}
	return true;
}

function preventDefaultHandler(event) {
	if (event && event.preventDefault) {
		event.preventDefault();
	}
	if (event && event.stopPropagation) {
		event.stopPropagation();
	}
	
	return false;
}

function launchWindowHandler(event, href, launchMode, windowName) {
	var target = getTarget(event);
	var name;
	
	if (href == null) {
		href = target.href;
	}
	
	if (launchMode == null) {
		launchMode = window.launchMode;
	}
	
	if (windowName == null) {
		windowName = "vulcanExternal";
	}
	
	if (launchMode == "modeSame") {
		return true;
	} else if (launchMode == "modeNew") {
		name = windowName + new Date();
	} else {
		name = windowName;
	}

	var width = parseInt(screen.width * 4 / 5);
	var height = parseInt(screen.height * 4 / 5);
	
	var childWindow = window.open(href, name, "width=" + width + ",height=" + height
		+ ",resizable=yes,scrollbars=yes,status=yes,location=yes,menubar=yes,titlebar=yes,toolbar=yes");
	
	// if the window was already open, bring it to the front.
	childWindow.focus();
	
	if (event != null || (target != null && target != window)) {
		return preventDefaultHandler(event);
	}
}

function launchHelpHandler(event) {
	var target = getTarget(event);	
	
	return launchWindowHandler(event, target.href, "modePopup", "vulcanHelp");
}

function setDirtyHandler() {
	log("form data has been changed");
	window.hasPendingChanges = true;
}

/**
 * Prevent warnPendingChangesHandler from showing warning
 * since the user is submitting the pending changes.
 */
function clearPendingChangesFlagHandler(event) {
	log("clearPendingChange");
	window.hasPendingChanges = false;
}

function warnPendingChangesHandler(event) {
	log("warnPendingChanges");
	if (window.hasPendingChanges) {
		event.returnValue = window.confirmUnsavedChangesMessage;
		
		return window.confirmUnsavedChangesMessage;
	}
}

jQuery.fn.loadCustom = function (url, params, selector) {
	var target = this;
	
	$("#loading-message").show();
	
	jQuery.ajax({
		type: "GET",
		url: url,
		params: params,
		success: function(data, textStatus) {
			var contents;
	
			if (data && data.getElementById) {
				contents = $(document.importNode(data.getElementsByTagName("body")[0], true)).find(selector);
			} else {
				contents = jQuery("<div/>").append(data.replace(/<script(.|\s)*?\/script>/g, "")).find(selector);
			}
	
			// show the loading message in the new contents
			contents.find("#loading-message").show();
	
			target.replaceWith(contents);

			registerAjaxHandlers();
			
			// so we can fade it out after replacing.
			$("#loading-message").fadeOut("slow");
		},
		error: function(data, textStatus) {
			if (data && (data.status == 302 || data.status == 0)) {
				// Firefox shows a 302 Moved Permanently.  IE shows a 0.
				// Fall back to non-ajax GET.
				window.location = url;
				return;
			} else {
				$("#loading-message").html("error: " + data.statusText);
			}
		}
	});
}

function toggleProjectLabel(event) {
	event.preventDefault();
	$(this).toggleClass("project-label-active");
}

function refreshDashboard(e, interval, url) {
	log("refresh " + new Date());
	
	// if interval is undefined, an event fired
	if (!interval) {
		$("#content").loadCustom(window.refreshUrl, null, "#content");
	} else {
		// otherwise we're being initialized
		window.refreshInterval = interval;
		window.refreshUrl = url;
	}
	
	//TODO: probably should cancel timeout if an error happens during loadCustom
	window.setTimeout(refreshDashboard, window.refreshInterval);
}

function toggleMetricColumnVisibility(e) {
	var id = $(this).attr("id");
	
	var header = $("#col_" + id);
	
	var table = $(header.parents("table").get(0));
	
	var index = 1 + table.find("thead th").index(header);
	
	header.toggleClass("hidden");
	table.find("tbody tr td:nth-child(" + index + ")").toggleClass("hidden");
}

function registerHandler(type, value, handler) {
	var inputs = document.getElementsByTagName('input');
	for (var i=0; i<inputs.length; i++) {
		if (inputs[i].type == type && (value == null || inputs[i].value == value)) {
			customAddEventListener(inputs[i], 'click', handler);
		}
	}
}

function registerHandlerByTagNameAndClass(tagName, styleClass, eventType, handler) {
	var elems = document.getElementsByTagName(tagName);
	
	for (var i=0; i<elems.length; i++) {
		var cls;
		
		if (elems[i].className) {
			cls = elems[i].className;
		} else {
			cls = elems[i].getAttribute('class');
		}
		
		if (cls == null) {
			cls = "";
		}

		if (cls.match(new RegExp(styleClass))) {
			customAddEventListener(elems[i], eventType, handler);
		}
	}
}

function registerAjaxHandlers() {
	$("a.confirm").click(confirmHandler);
	$(".dashboard a.async").click(setPreference);
	$(".dashboard thead a").click(setPreference);
}

function registerHandlers() {
	registerHandler('submit', 'Delete', confirmHandler);
	registerHandler('button', 'Add', addRow);
	registerHandler('button', 'Remove', removeRow);
	registerHandler('button', 'Move Up', moveUpRows);
	registerHandler('button', 'Move Down', moveDownRows);
	
	$("#metrics-checkboxes input[type='checkbox']").bind("toggle", toggleMetricColumnVisibility);
	
	$("input[type='checkbox']").click(checkRows);
	$("input[type='radio']").click(checkRows);

	$("#txtNewLabel").focus(function() {
		$("#chkNewLabel").attr("checked", "checked");
	});
	$("#txtNewLabel").blur(function() {
		if ($(this).val() == "") {
			$("#chkNewLabel").removeAttr("checked");
		}
	});
	
	var pendingChanges = document.getElementById('pendingChanges');
	if (pendingChanges != null) {
		$("input").change(setDirtyHandler);
		$("form").submit(clearPendingChangesFlagHandler);
		
		window.hasPendingChanges = (pendingChanges.value == "true");
	}
	
	registerHandlerByTagNameAndClass('label', '.*', 'mousedown', preventShiftClickTextSelectionHandler);
	
	var windowLaunchMode = getMetaContent("popupMode");
	
	if (windowLaunchMode) {
		window.launchMode = windowLaunchMode;
		registerHandlerByTagNameAndClass('a', 'external', 'click', launchWindowHandler);
	}
	
	var helpLink = document.getElementById("helpLink");
	
	if (helpLink) {
		var helpTopic = getMetaContent("helpTopic", "GeneralHelp");

		helpLink.href = helpLink.href + helpTopic;
	
		customAddEventListener(helpLink, 'click', launchHelpHandler);
	}
	
	$("table.sortable").tablesorter({
		cssAsc: "sorted-descending", // tablesort plugin seems to have these backwards...
		cssDesc: "sorted-ascending",
		cssHeader: "",
		textExtraction: function(node) {
			var text = $(node).text();
			var number = parseInt(text);
			
			if (isNaN(number)) {
				return text;
			}
			
			while (text.length < 8) {
				text = "0" + text;
			}
			
			return text;
		}
	});
	
	$("ul.tabs a").click(function(e) {
		e.preventDefault();
		
		var id = $(this).attr("id");
		
		var panelName = id.replace("-tab", "");
		
		showBuildReportPanel(panelName);
		
		$(this).blur();
		
		return false;
	});
	
	$(".report-link").click(function() {
		setTimeout(function() { showBuildReportPanel("browse"); }, 500);
	});
	
	window.iframe = document.getElementById("iframe");
	if (iframe) {
		$(window).resize(resizeIframe);
		
		var buildDir = $("#build-directory-root").text();
		
		var index = buildDir.lastIndexOf("/");
		window.pathSeparator = "/";
		if (index < 0) {
			index = buildDir.lastIndexOf("\\");
			window.pathSeparator = "\\";
		}
		
		if (buildDir[buildDir.length-1] == window.pathSeparator) {
			buildDir = buildDir.substring(0, buildDir.length - 1);
			index = buildDir.lastIndexOf(window.pathSeparator);
		}
		
		window.rootDirName = buildDir.substring(index+1);
		
		$("#build-directory-root").text(buildDir.substring(0, index));
		
		window.setTimeout(updateBreadcrumbs, 250);
	}
}

function resizeIframe() {
	window.iframe = document.getElementById("iframe");
	var height = 0;
	
	if (document.documentElement && document.documentElement.clientHeight) {
		height = document.documentElement.clientHeight;
	} else if (document.body && document.body.clientHeight) {
		// IE 6
		height = document.body.clientHeight;
	}

	height = (height - iframe.offsetTop - 200);
	if (height < 300) {
		height = 300;
	}
	
	iframe.style.height = height + "px";
}

function showBuildReportPanel(panelName) {
	// hide all panels
	$(".tab-panel").hide();
	
	// show selected panel
	$("#" + panelName + "-panel").show();
	
	// make all tabs inactive
	$("#build-report-tabs li").removeClass("active");
	
	$("#" + panelName + "-tab").parent("li").addClass("active");
	
	$("a.build-link").each(function(i, a) {
		a = $(a);
		
		var href = a.attr("href");
		
		a.attr("href", href.substring(0, href.lastIndexOf("/")+1) + panelName);
	});
	
	resizeIframe();
}

function updateBreadcrumbs() {
	var location;
	
	try {
		location = iframe.contentWindow.location.href;
	} catch (e) {
		log("unable to retrieve location.href from iframe");
		// if iframe navigated to another domain, ignore error
		location = "";
	}

	if (!window.rootLocation) {
		window.rootLocation = location;
	}

	if (location.length >= rootLocation.length && (window.lastLocation != location)
			&& (location.substring(0, rootLocation.length) == rootLocation)) {
		
		window.lastLocation = location;
		
		var relativePath = location.substring(rootLocation.length);
		var paths = [rootDirName];
		paths = paths.concat(relativePath.split("/"));
		
		var crumbs = $("#build-directory-bread-crumbs");
		crumbs.empty();
		
		var path = rootLocation;
		
		for (var i in paths) {
			if (paths[i].length == 0) {
				continue;
			}
			
			if (i>0) {
				path += "/";
				path += paths[i];
			}
			
			var a = document.createElement("a");
			a.href = path;
			a.target = "iframe";
			a.appendChild(document.createTextNode(paths[i]));
			crumbs.append(document.createTextNode(window.pathSeparator));
			crumbs.append(a);
		}
	}
	
	window.setTimeout(updateBreadcrumbs, 250);
}

function getConfirmMessages() {
	window.confirmMessage = getMetaContent("confirmMessage");
	window.confirmUnsavedChangesMessage = getMetaContent("confirmUnsavedChangesMessage");
}

function customAddEventListener(target, eventType, callback) {
	if (target.addEventListener) {
		target.addEventListener(eventType, callback, false);
	} else if (target.attachEvent) {
		target.attachEvent('on' + eventType, callback);
    }
}

$(document).ready(registerHandlers);
$(document).ready(registerAjaxHandlers);
$(document).ready(getConfirmMessages);
customAddEventListener(window, "beforeunload", warnPendingChangesHandler);
