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
package net.sourceforge.vulcan.web.struts;

import java.net.URL;

import javax.xml.transform.Result;

import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.Keys;

import org.jdom.Document;
import org.jdom.Element;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class GetCachedBuildHistoryActionTest extends MockApplicationContextStrutsTestCase {
	Document doc = new Document();
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/getBuildHistory.do");
		
		doc.setRootElement(new Element("root"));
	}
	
	public void testErrorOnNoData() throws Exception {
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyActionErrors(new String[] {"errors.no.build.history"});
		
		verifyForward("error");
	}
	
	public void testSendsXmlIfNoTransformSpecified() throws Exception {
		request.getSession().setAttribute(Keys.BUILD_HISTORY, doc);
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		assertEquals("application/xml", response.getContentType());
	}
	
	public void testPerformsTransform() throws Exception {
		request.getSession().setAttribute(Keys.BUILD_HISTORY, doc);
		request.addParameter("transform", "xyz");
		
		projectDomBuilder.transform(eq(doc), (URL)anyObject(), (URL)notNull(), (URL)eq(null), eq(request.getLocale()), eq("xyz"), (Result)notNull());
		expectLastCall().andReturn("text/plain");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		assertEquals("text/plain", response.getContentType());
	}
}
