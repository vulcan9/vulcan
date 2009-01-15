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
package net.sourceforge.vulcan.core.support;

import junit.framework.TestCase;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;

@SvnRevision(id="$Id: AbstractProjectDomBuilder.java 161 2006-12-05 22:19:21Z chris.eldredge $", url="$HeadURL: https://vulcan.googlecode.com/svn/main/trunk/source/main/java/net/sourceforge/vulcan/core/support/AbstractProjectDomBuilder.java $")
public class CommitLogParserTest extends TestCase {
	CommitLogParser parser = new CommitLogParser();
	
	public void testNoMatch() throws Exception {
		parser.parse("Fixed bug 92.");
		
		assertEquals(1, parser.getContents().size());
		assertTextNode("Fixed bug 92.", 0);
	}

	public void testSingleMatchRegex() throws Exception {
		parser.setKeywordPattern("bug (\\d+)");
		parser.parse("Fixed bug 92.");
		
		assertTextNode("Fixed ", 0);
		assertIssue("bug 92", "bug 92", 1);
		assertTextNode(".", 2);
	}

	public void testSingleMatchRegexWithIdPattern() throws Exception {
		parser.setKeywordPattern("bug (\\d+)");
		parser.setIdPattern("(\\d+)");
		parser.parse("Fixed bug 92.");
		
		assertTextNode("Fixed bug ", 0);
		assertIssue("92", "92", 1);
		assertTextNode(".", 2);
	}
	
	public void testSingleMatchRegexNoTrailingText() throws Exception {
		parser.setKeywordPattern("bug (\\d+)");
		parser.setIdPattern("(\\d+)");
		parser.parse("Fixed bug 92");
		
		assertTextNode("Fixed bug ", 0);
		assertIssue("92", "92", 1);
		assertEquals(2, parser.getContents().size());
	}
	
	public void testSingleMatchRegexNoPrecedingText() throws Exception {
		parser.setKeywordPattern("bug (\\d+)");
		parser.parse("bug 92 was fixed.");
		
		assertIssue("bug 92", "bug 92", 0);
		assertTextNode(" was fixed.", 1);
		assertEquals(2, parser.getContents().size());
	}
	
	public void testDoubleMatchRegex() throws Exception {
		parser.setKeywordPattern("[bB]ug (\\d+)");
		parser.parse("Fixed bug 92 and Bug 95.");
		
		assertTextNode("Fixed ", 0);
		assertIssue("bug 92", "bug 92", 1);
		assertTextNode(" and ", 2);
		assertIssue("Bug 95", "Bug 95", 3);
		assertTextNode(".", 4);
	}
	
	public void testDoubleMatchRegexCombined() throws Exception {
		parser.setKeywordPattern("(bug|issue) \\d+");
		parser.parse("Fixed bug 92 and issue 95.");
		
		assertTextNode("Fixed ", 0);
		assertIssue("bug 92", "bug 92", 1);
		assertTextNode(" and ", 2);
		assertIssue("issue 95", "issue 95", 3);
		assertTextNode(".", 4);
	}
	
	public void testDoubleMatchRegexComplicated() throws Exception {
		parser.setKeywordPattern("([bB]ug|[iI]ssue)#? ?#?(\\d+)");
		parser.parse("Fixed bug# 92 and\n Issue #95.");
		
		assertTextNode("Fixed ", 0);
		assertIssue("bug# 92", "bug# 92", 1);
		assertTextNode(" and\n ", 2);
		assertIssue("Issue #95", "Issue #95", 3);
		assertTextNode(".", 4);
	}
	
	public void testMatchRegexMultpleIds() throws Exception {
		parser.setKeywordPattern("bugs #?(\\d+)(,? ?#?(\\d+))+");
		parser.setIdPattern("\\d+");
		
		parser.parse("Fixed bugs #92, #95.");
		
		assertTextNode("Fixed bugs #", 0);
		assertIssue("92", "92", 1);
		assertTextNode(", #", 2);
		assertIssue("95", "95", 3);
		assertTextNode(".", 4);
	}
	
	public void testMatchRegexMultpleIdsWithConjunction() throws Exception {
		parser.setKeywordPattern("bugs #?(\\d+)(,? ?(and)? ?#?(\\d+))+");
		parser.setIdPattern("\\d+");
		
		parser.parse("Fixed bugs #92, #95, and #100.");
		
		assertTextNode("Fixed bugs #", 0);
		assertIssue("92", "92", 1);
		assertTextNode(", #", 2);
		assertIssue("95", "95", 3);
		assertTextNode(", and #", 4);
		assertIssue("100", "100", 5);
		assertTextNode(".", 6);
	}
	
	public void testMatchRegexMultpleIdsAndThenSome() throws Exception {
		parser.setKeywordPattern("bugs? #?(\\d+)(,? ?#?(\\d+))+");
		parser.setIdPattern("\\d+");
		
		parser.parse("Fixed bugs #92, #95.\r\nAlso addresses bug 24.");
		
		assertTextNode("Fixed bugs #", 0);
		assertIssue("92", "92", 1);
		assertTextNode(", #", 2);
		assertIssue("95", "95", 3);
		assertTextNode(".\r\nAlso addresses bug ", 4);
		assertIssue("24", "24", 5);
		assertTextNode(".", 6);
	}
	
	public void testMatchUrl() throws Exception {
		parser.parse("Check out the new site at http://www.example.com");
		assertTextNode("Check out the new site at ", 0);
		assertLink("http://www.example.com", 1);
	}
	
	public void testMatchUrlTrimsTrailingParenthesis() throws Exception {
		parser.parse("Finished the project (see https://www.example.com)");
		
		assertTextNode("Finished the project (see ", 0);
		assertLink("https://www.example.com", 1);
		assertTextNode(")", 2);
	}
	
	public void testMatchUrlTrimsTrailingPeriod() throws Exception {
		parser.parse("Look at ftp://example.com.");
		
		assertTextNode("Look at ", 0);
		assertLink("ftp://example.com", 1);
		assertTextNode(".", 2);
	}
	
	public void testMatchUrlAndIssue() throws Exception {
		parser.setKeywordPattern("bug (\\d+)");
		parser.parse("Check out the new site at http://www.example.com (bug 92 was fixed).");
		
		assertTextNode("Check out the new site at ", 0);
		assertLink("http://www.example.com", 1);
		assertTextNode(" (", 2);
		assertIssue("bug 92", "bug 92", 3);
		assertTextNode(" was fixed).", 4);
	}
	
	public void testChoosesRightCaptureGroupOnMany() throws Exception {
		parser.setKeywordPattern("(Bug:? |(card|story) \\d+)");
		parser.parse("Did stuff for card 3.");
		
		assertTextNode("Did stuff for ", 0);
		assertIssue("card 3", "card 3", 1);
		assertTextNode(".", 2);
	}
	
	public void testMatchUrlWhenIdPatternSet() throws Exception {
		parser.setIdPattern("\\d+");
		parser.parse("http://bugzilla.example.com/show_bug.cgi?id=1234");
		
		assertLink("http://bugzilla.example.com/show_bug.cgi?id=1234", 0);
	}
	
	private void assertLink(String expectedText, int nodeIndex) {
		assertTrue("Index out of bounds", nodeIndex < parser.getContents().size());
		
		final Content content = parser.getContents().get(nodeIndex);
		
		assertTrue("Index " + nodeIndex + " not an Issue node", content instanceof Element);
		
		final Element element = (Element) content;
		
		assertEquals("link", element.getName());
		assertEquals(expectedText, element.getText());
	}

	private void assertIssue(String expectedText, String expectedId, int nodeIndex) {
		assertTrue("Index out of bounds", nodeIndex < parser.getContents().size());
		
		final Content content = parser.getContents().get(nodeIndex);
		
		assertTrue("Index " + nodeIndex + " not an Issue node", content instanceof Element);
		
		final Element element = (Element) content;
		
		assertEquals("issue", element.getName());
		assertEquals(expectedText, element.getText());
		assertEquals(expectedId, element.getAttributeValue("issue-id"));
	}

	private void assertTextNode(String expectedText, int nodeIndex) {
		assertTrue("Index out of bounds", nodeIndex < parser.getContents().size());
		
		final Content content = parser.getContents().get(nodeIndex);
		
		assertTrue("Index " + nodeIndex + " not a Text node", content instanceof Text);
		assertEquals(expectedText, ((Text)content).getText());
	}
}
