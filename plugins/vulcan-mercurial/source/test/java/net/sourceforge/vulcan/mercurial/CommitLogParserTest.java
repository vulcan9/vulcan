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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.mercurial.CommitLogParser.LogEntryDateParseRule;

import org.apache.commons.lang.time.DateUtils;

public class CommitLogParserTest extends TestCase {

	static final String changeLogXml =
		"<?xml version='1.0'?>" +
		"<log>" +
			"<logentry revision='473' node='f672e139e966eaee45aca866c53560d52c6ee6ee'>" +
				"<parent revision='471' node='46d6f037054d4417f63d0e104ada37dcb9dc6a14' />" +
				"<author email='one@localhost'>Example One</author>" +
				"<date>2010-12-09T12:44:03-05:00</date>" +
				"<msg xml:space='preserve'>sample message 1</msg>" +
				"<paths>" +
					"<path action='A'>plugins/MercurialConfig.java</path>" +
					"<path action='A'>plugins/ProcessInvokerTest.java</path>" +
				"</paths>" +
			"</logentry>" +
			"<logentry revision='474' node='f0bb3a6e49c3f26797df9ad6d187ba7e6e552a17'>" +
				"<author email='user-2@example.com'>Sample User Two</author>" +
				"<date>2010-12-14T19:31:44-05:00</date>" +
				"<msg xml:space='preserve'>sample message 2</msg>" +
				"<paths>" +
					"<path action='A'>file three</path>" +
					"<path action='A'>4th.txt</path>" +
				"</paths>" +
			"</logentry>" +
		"</log>";
	
	public void testParseRoot() throws Exception {
		final ChangeLogDto result = new CommitLogParser().parse("<log/>");
		
		assertNotNull("return value", result);
	}
	
	public void testAddsChildren() throws Exception {
		final ChangeLogDto result = new CommitLogParser().parse("<log><logentry/><logentry/></log>");
		
		assertNotNull("return value", result);
		assertNotNull("list", result.getChangeSets());
		assertEquals(2, result.getChangeSets().size());
	}
	
	public void testParseAuthor() throws Exception {
		final List<ChangeSetDto> actual = new CommitLogParser().parse(changeLogXml).getChangeSets();
		
		assertEquals("Example One", actual.get(0).getAuthor());
		assertEquals("Sample User Two", actual.get(1).getAuthor());
	}
	
	public void testParseMessage() throws Exception {
		final List<ChangeSetDto> actual = new CommitLogParser().parse(changeLogXml).getChangeSets();
		
		assertEquals("sample message 1", actual.get(0).getMessage());
		assertEquals("sample message 2", actual.get(1).getMessage());
	}
	
	public void testParseModifiedPaths() throws Exception {
		final List<ChangeSetDto> actual = new CommitLogParser().parse(changeLogXml).getChangeSets();
		
		assertEquals(Arrays.asList("plugins/MercurialConfig.java", "plugins/ProcessInvokerTest.java"), actual.get(0).getModifiedPaths());
		assertEquals(Arrays.asList("file three", "4th.txt"), actual.get(1).getModifiedPaths());
	}
	
	public void testParseRevision() throws Exception {
		final List<ChangeSetDto> actual = new CommitLogParser().parse(changeLogXml).getChangeSets();
		
		assertEquals("473:f672e139e966", actual.get(0).getRevisionLabel());
		assertEquals("474:f0bb3a6e49c3", actual.get(1).getRevisionLabel());
	}
	
	public void testParseTimestamp() throws Exception {
		final List<ChangeSetDto> actual = new CommitLogParser().parse(changeLogXml).getChangeSets();
		
		final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		
		assertEquals(fmt.parse("2010-12-09 12:44:03-0500"), actual.get(0).getTimestamp());
		assertEquals(fmt.parse("2010-12-14 19:31:44-0500"), actual.get(1).getTimestamp());
	}
	
	public void testParseTimestampInDifferentTimeZone() throws Exception {
		final LogEntryDateParseRule rule = new LogEntryDateParseRule();
		final Date d1 = rule.parse("2012-01-31T23:59:42-08:30");
		final Date d2 = rule.parse("2012-01-31T23:59:42-05:00");
		
		final long timeZoneDelta = (DateUtils.MILLIS_PER_HOUR * 3) + DateUtils.MILLIS_PER_MINUTE * 30;
		
		assertEquals(Long.toString(timeZoneDelta), Long.toString(d1.getTime() - d2.getTime()));
		
		assertFalse(d1.equals(d2));
	}
}
