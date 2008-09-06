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
	
	customAddEventListener(input, 'click', checkRows);
	
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
			all[c].checked = on;
		}
	}
	
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

function confirmHandler(event) {
	if (!confirm(window.confirmMessage)) {
		return preventDefaultHandler(event);
	}
}

function preventDefaultHandler(event) {
	if (event && event.preventDefault) {
		event.preventDefault();
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

	var width = screen.width * 4 / 5;
	var height = screen.height * 4 / 5;
	
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
	console.debug("form data has been changed");
	window.hasPendingChanges = true;
}

/**
 * Prevent warnPendingChangesHandler from showing warning
 * since the user is submitting the pending changes.
 */
function clearPendingChangesFlagHandler(event) {
	console.debug("clearPendingChange");
	window.hasPendingChanges = false;
}

function warnPendingChangesHandler(event) {
	console.debug("warnPendingChanges");
	if (window.hasPendingChanges) {
		event.returnValue = window.confirmUnsavedChangesMessage;
	}
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

function registerHandlers() {
	registerHandler('submit', 'Delete', confirmHandler);
	registerHandler('button', 'Add', addRow);
	registerHandler('button', 'Remove', removeRow);
	registerHandler('button', 'Move Up', moveUpRows);
	registerHandler('button', 'Move Down', moveDownRows);
	registerHandler('checkbox', null, checkRows);
	registerHandler('radio', null, checkRows);
	
	var pendingChanges = document.getElementById('pendingChanges');
	if (pendingChanges != null) {
		$("input").change(setDirtyHandler);
		$("form").submit(clearPendingChangesFlagHandler);
		
		window.hasPendingChanges = (pendingChanges.value == "true");
	}
	
	registerHandlerByTagNameAndClass('a', 'confirm', 'click', confirmHandler);
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
		cssAsc: "sorted-ascending",
		cssDesc: "sorted-descending",
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
		
		return false;
	});
	
	$(".report-link").click(function() {
		setTimeout(function() { showBuildReportPanel("build-directory"); }, 500);
	});
	
	window.iframe = document.getElementById("iframe");
	if (iframe) {
		$(window).resize(resizeIframe);
		
		window.rootLocation = iframe.contentWindow.location.href;
		
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
		
		updateBreadcrumbs();
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

	height = (height - iframe.offsetTop - 20);
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
	
	resizeIframe();
}

function updateBreadcrumbs() {
	var location;
	
	try {
		location = iframe.contentWindow.location.href;
	} catch (e) {
		// if iframe navigated to another domain, ignore error
		location = "";
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
$(document).ready(getConfirmMessages);
customAddEventListener(window, "beforeunload", warnPendingChangesHandler);

