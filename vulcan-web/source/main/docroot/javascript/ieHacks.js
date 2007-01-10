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
/** These scripts override behavior in other scripts to allow them
  * to work with Microsoft Internet Explorer.  They are kept separate
  * in order to allow the codebase to stay readable.
  */

function fixLayoutForIE() {
	addContentBefore();
	addStyleClassToLastChildren();
}

/* MSIE does not support the CSS :before { content: "..." } technique. */
function addContentBefore() {
	for (var i=0; i<document.styleSheets.length; i++) {
		for (var j=0; j<document.styleSheets[i].rules.length; j++) {
			var rule = document.styleSheets[i].rules[j];
			
			/* MSIE 6/7 converts :before to :unknown. */
			var index = rule.selectorText.indexOf(":unknown");
			
			if (index >= 0) {
				var selector = rule.selectorText.substring(0, index);

				if (!rule.style.content) {
					continue;
				}

				/* Obtain content property, and remove quotes. */
				var content = rule.style.content.substring(
					1, rule.style.content.length - 1);
					
				var tags = cssQuery(selector);
				
				for (var k=0; k<tags.length; k++) {
					tags[k].insertBefore(document.createTextNode(content),
						tags[k].firstChild);
				}
			}
		}
	}
}

/* MSIE does not support the last-child pseudo selector */
function addStyleClassToLastChildren() {
	var theads = document.getElementsByTagName("thead");

	for (var i=0; i<theads.length; i++) {
		var headers = theads[i].getElementsByTagName("th");

		if (headers && headers.length) {
			var last = headers[headers.length-1];

			var cls = last.className;

			if (cls) {
				cls += " ";
			} else {
				cls = "";
			}
			cls += " last-child";

			last.className = cls;
		}
	}
}

if (navigator && navigator.appName == "Microsoft Internet Explorer") {
	customAddEventListener(window, 'load', fixLayoutForIE);
}
