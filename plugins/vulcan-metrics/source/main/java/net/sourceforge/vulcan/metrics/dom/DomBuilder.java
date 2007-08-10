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
package net.sourceforge.vulcan.metrics.dom;

import java.io.File;
import java.io.StringReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class DomBuilder {
	private static Log log = LogFactory.getLog(DomBuilder.class);
	private final Document doc = new Document();
	private final Element root = new Element("merged-root");
	
	public DomBuilder() {
		doc.addContent(root);
	}
	
	public Document getMergedDocument() {
		return doc;
	}
	
	public void merge(File xmlFile) {
		if (log.isDebugEnabled()) {
			log.debug("Merging file " + xmlFile.getPath() + " into metrics document.");
		}
		
		try {
			final String xml = FileUtils.readFileToString(xmlFile, "UTF-8");
			
			final String xmlWithDoctype = fixNonStandardXml(xml);
			
			final Document xmlDoc = new SAXBuilder().build(new StringReader(xmlWithDoctype));

			merge(xmlDoc);
		} catch (Exception e) {
			warn(e); 
		}
	}
	
	void merge(Document xmlDoc) {
		root.addContent(xmlDoc.detachRootElement());
	}
	
	static void setLog(Log log)
	{
		DomBuilder.log = log;
	}
	
	private void warn(Exception e) {
		log.error("error", e);
	}

	/**
	 * @return XML with non-standard entities removed.
	 */
	private String fixNonStandardXml(String xml) {
		if (xml.contains("&nbsp;")) {
			return xml.replaceAll("&nbsp;", "");
		}
		
		return xml;
	}
}
