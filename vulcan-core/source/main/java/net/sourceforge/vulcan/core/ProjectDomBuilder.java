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
package net.sourceforge.vulcan.core;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;

import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.NoSuchTransformFormatException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.jdom.Document;
import org.xml.sax.SAXException;

@SvnRevision(id="$Id", url="$HeadURL$")
public interface ProjectDomBuilder {
	Document createProjectDocument(ProjectStatusDto status, Locale locale);
	Document createProjectSummaries(List<ProjectStatusDto> outcomes, Object fromLabel, Object toLabel, Locale locale);
	
	/**
	 * @return MIME type of transformed result, if specified.
	 */
	String transform(Document document, Map<String, ?> transormParameters, Locale locale, String format, Result result) throws SAXException, IOException, TransformerException, NoSuchTransformFormatException;
}