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
package net.sourceforge.vulcan.web;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sourceforge.vulcan.dto.PreferencesDto;

import org.springframework.web.context.WebApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class XslHelper {
	private static WebApplicationContext webApplicationContext;
	private final Locale locale;

	public XslHelper(String locale) {
		this.locale = new Locale(locale);
	}

	public static void setWebApplicationContext(WebApplicationContext webApplicationContext) {
		XslHelper.webApplicationContext = webApplicationContext;
	}
	
	public String getMessage(String code) {
		return webApplicationContext.getMessage(code, null, locale);
	}
	
	public String getMessage(String code, String arg1) {
		return webApplicationContext.getMessage(code, new String[] {arg1}, locale);
	}
	
	public String getMessage(String code, String arg1, String arg2) {
		return webApplicationContext.getMessage(code, new String[] {arg1, arg2}, locale);
	}
	
	public String getMessage(String code, String arg1, String arg2, String arg3) {
		return webApplicationContext.getMessage(code, new String[] {arg1, arg2, arg3}, locale);
	}
	
	public String getMessage(String code, String arg1, String arg2, String arg3, String arg4) {
		return webApplicationContext.getMessage(code, new String[] {arg1, arg2, arg3, arg4}, locale);
	}
	
	public static String mangle(String s) {
		return JstlFunctions.mangle(s);
	}
	
	public static Node getBuildHistoryVisibleColumns(Object prefsObj) throws ParserConfigurationException {
		PreferencesDto prefs = (PreferencesDto) prefsObj;
		
		return makeColumnList(Arrays.asList(prefs.getBuildHistoryColumns()));
	}

	public static Node getBuildHistoryAvailableColumns() throws ParserConfigurationException {
		final List<String> columns = JstlFunctions.getAllDashboardColumns(Keys.BUILD_HISTORY_COLUMNS);
		columns.addAll(JstlFunctions.getAvailableMetrics());
		return makeColumnList(columns);
	}
	
	private static Node makeColumnList(Iterable<String> columns) throws ParserConfigurationException {
		final Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		final Element node = doc.createElement("visible-columns");
		
		for (String s : columns) {
			addNodeWithText(doc, node, "label", s);
		}
		
		return node;
	}

	private static Node addNodeWithText(final Document doc, final Element parent, String name, String text) {
		final Element child = doc.createElement(name);
		child.setTextContent(text);
		return parent.appendChild(child);
	}
}
