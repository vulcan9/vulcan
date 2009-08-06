/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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
package net.sourceforge.vulcan.jabber;

import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import junit.framework.TestCase;


public class TemplateFormatterTest extends TestCase {
	ProjectStatusDto status = new ProjectStatusDto();
	BuildMessageDto error = new BuildMessageDto();
	String url = "http://example.com/vulcan/projects/foo/LATEST/";
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		status.setName("my project");
		status.setBuildNumber(24);

		error.setMessage("expected ;");
		error.setCode("FC1024");
		error.setFile("MyModule.fcs");
		error.setLineNumber(37);

	}
	
	public void testFormatMessageSimple() throws Exception {
		assertEquals(error.getMessage(), TemplateFormatter.substituteParameters("{Message}", url, "", error, status));
	}

	public void testDoesNotEatQuotes() throws Exception {
		assertEquals("Don't remove single quote.", TemplateFormatter.substituteParameters("Don't remove single quote.", url, "", error, status));
	}

	public void testFormatNullParams() throws Exception {
		assertEquals("", TemplateFormatter.substituteParameters("{Link}", "", "", null, null));
	}
	
	public void testFormatNumber() throws Exception {
		error.setLineNumber(1024);
		assertEquals("1,024", TemplateFormatter.substituteParameters("{LineNumber,number}", url, "", error, status));
	}
	
	public void testFormatParamNameCaseInsensitive() throws Exception {
		error.setLineNumber(1024);
		assertEquals("1,024", TemplateFormatter.substituteParameters("{linenumber,number}", url, "", error, status));
	}
	
	public void testFormatChoice() throws Exception {
		final String format = "{LineNumber,choice,-1#|0<Line {LineNumber, number}}";
		
		error.setLineNumber(null);
		
		assertEquals("", TemplateFormatter.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(0);
		
		assertEquals("", TemplateFormatter.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(1);
		
		assertEquals("Line 1", TemplateFormatter.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(1024);
		
		assertEquals("Line 1,024", TemplateFormatter.substituteParameters(format, url, "", error, status));
	}
	
	public void testFormatBlankable() throws Exception {
		final String format = "{Users?,choice,0#|0<We also notified {Users}.}";
		
		assertEquals("", TemplateFormatter.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(0);
		
		assertEquals("We also notified Sam.", TemplateFormatter.substituteParameters(format, url, "Sam", error, status));
	}
	
	public void testFormatBlankableSimplified() throws Exception {
		final String format = "{Users?,We also notified {Users}.}";
		
		assertEquals("", TemplateFormatter.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(0);
		
		assertEquals("We also notified Sam.", TemplateFormatter.substituteParameters(format, url, "Sam", error, status));
	}
	
	public void testFormatAllParams() throws Exception {
		final String format =
			"You broke '{ProjectName}'{Users?, (or {Users} did)}.\n" +
			"{File?,{File}}{LineNumber?,': line '{LineNumber}}{Code?,': '{Code}}: {Message}\n" +
			"See {Link} for more info.  This was build {BuildNumber}.";
		
		final String s = TemplateFormatter.substituteParameters(format, url, "Sam", error, status);
		
		assertEquals("You broke 'my project' (or Sam did).\n" +
				"MyModule.fcs: line 37: FC1024: expected ;\n" +
				"See http://example.com/vulcan/projects/foo/LATEST/ for more info.  This was build 24.", s);
	}
	
	public void testFormatBlankableSimplifiedCompound() throws Exception {
		final String format = "{File?,In {File}}{LineNumber?,:line {LineNumber}}";
		
		error.setFile("");
		error.setLineNumber(null);
		
		assertEquals("", TemplateFormatter.substituteParameters(format, url, "", error, status));
		
		error.setLineNumber(123);
		error.setFile("Foo.java");
		assertEquals("In Foo.java:line 123", TemplateFormatter.substituteParameters(format, url, "Sam", error, status));
	}
}
