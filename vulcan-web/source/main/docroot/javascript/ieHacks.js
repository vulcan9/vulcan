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
/** These scripts override behavior in other scripts to allow them
  * to work with Microsoft Internet Explorer.  They are kept separate
  * in order to allow the codebase to stay readable.
  */

/* MSIE <= 7 does not support the CSS :before { content: "..." } technique. */
function addContentBefore() {
	$("#utility-nav li").not(".username").prepend("|");
}

/* MSIE sends the inner text of a button instead of the value attribute.
 * This breaks server-side actions on buttons that have been i18nized.
 */
function submitFormHandler(e) {
	var value = $(this).attr("actual-value");
	
	if (value) {
		clearPendingChangesFlagHandler(e);
		
		$("#ieSubmit").attr("value", value);
		$("#ieSubmit").click();
	}
	
	return preventDefaultHandler(e);
}

function fixLayoutForIE() {
	if (navigator && navigator.appName == "Microsoft Internet Explorer") {
		if (/MSIE [4567]/.exec(navigator.appVersion)) {
			addContentBefore();
		}
	}
	
	$("button.ie-submit-button").click(submitFormHandler);
}

$(document).ready(fixLayoutForIE);

