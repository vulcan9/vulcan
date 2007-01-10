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

import java.io.OutputStream;

import net.sourceforge.vulcan.metadata.SvnRevision;

import org.easymock.EasyMock;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class ViewConfigActionTest extends MockApplicationContextStrutsTestCase {
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/admin/viewConfig.do");
	}
	
	public void testView() throws Exception {
		EasyMock.expect(store.getExportMimeType()).andReturn("application/xml");
		store.exportConfiguration((OutputStream) notNull());
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		assertEquals("application/xml", response.getContentType());
		assertEquals(null, response.getHeader("Content-Disposition"));
	}
	public void testDownload() throws Exception {
		addRequestParameter("download", "true");
		
		EasyMock.expect(store.getExportMimeType()).andReturn("application/xml");
		store.exportConfiguration((OutputStream) notNull());
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyNoActionMessages();
		verifyNoActionErrors();
		
		assertEquals("application/xml", response.getContentType());
		assertEquals("attachment; filename=vulcan-config.xml", response.getHeader("Content-Disposition"));
	}
}