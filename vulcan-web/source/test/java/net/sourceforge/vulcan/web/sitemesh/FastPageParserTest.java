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
package net.sourceforge.vulcan.web.sitemesh;

import junit.framework.TestCase;
import net.sourceforge.vulcan.metadata.SvnRevision;

import com.opensymphony.module.sitemesh.Page;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class FastPageParserTest extends TestCase {
	FastPageParser p = new FastPageParser();
	
	public void testNotNull() throws Exception {
		assertNotNull(call(""));
		assertNotNull(call("<html/>"));
	}
	public void testConstructsPage() throws Exception {
		final Page page = call("<html><head><title>hello</title></head><body>my body</body></html>");
		
		assertEquals("hello", page.getTitle());
		assertEquals("my body", page.getBody());
	}
	
	public void testStripsXmlHeader() throws Exception {
		final Page page = call(" <?xml version=\"1.0\"?>\n\n\n\t<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n\n<html><head><title>hello</title></head><body>my body</body></html>");
		
		assertEquals("hello", page.getTitle());
		assertEquals("my body", page.getBody());
	}
	private Page call(String input) throws Exception {
		return p.parse(input.toCharArray());
	}
}
