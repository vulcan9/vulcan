/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2010 Chris Eldredge
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
package net.sourceforge.vulcan.mercurial;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class CommitLogParser {
	public ChangeLogDto parse(String text) throws IOException, SAXException {
		final Digester digester = new Digester();
		
		digester.addObjectCreate("log", ChangeLogDto.class);
		digester.addObjectCreate("log/logentry", ChangeSetDto.class);
		digester.addRule("log/logentry", new LogEntryRevisionCompositionRule());
		
		digester.addBeanPropertySetter("log/logentry/author", "author");
		digester.addBeanPropertySetter("log/logentry/msg", "message");
		
		digester.addRule("log/logentry/date", new LogEntryDateParseRule());
		
		digester.addCallMethod("log/logentry/paths/path", "addModifiedPath", 1);
		digester.addCallParam("log/logentry/paths/path", 0);
		
		digester.addSetNext("log/logentry", "addChangeSet");
		
		return (ChangeLogDto) digester.parse(new StringReader(text));
	}
	
	static class LogEntryRevisionCompositionRule extends Rule {
		@Override
		public void begin(String namespace, String name, Attributes attributes)	throws Exception {
			final ChangeSetDto change = (ChangeSetDto) digester.peek();
			
			final String revision = attributes.getValue("revision");
			String node = attributes.getValue("node");
			
			if (node != null) {
				node = node.substring(0, 12);
			}
			
			change.setRevisionLabel(revision + ":" + node);
		}
	}
	
	// SimpleDateFormat can't handle a time zone with a colon in it.
	// Mercurial prints dates in a format like: 2010-12-09T13:44:03-05:00
	static class LogEntryDateParseRule extends Rule {
		private final static int length = 25;
		private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT);
		
		@Override
		public void body(String namespace, String name, String text) throws Exception {
			final Date date = parse(text);

			final ChangeSetDto change = (ChangeSetDto) digester.peek();
			change.setTimestamp(date);
		}

		Date parse(String text) throws ParseException {
			if (StringUtils.isBlank(text) || text.length() != length) {
				return null;
			}

			text = removeColonInTimeZone(text);
			
			return dateFormat.parse(text);
		}

		private String removeColonInTimeZone(String text) {
			return text.substring(0, 22) + text.substring(23);
		}
	}
}
