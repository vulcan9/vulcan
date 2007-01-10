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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.struts.action.ActionMessages;
import org.easymock.EasyMock;
import org.jdom.Document;
import org.jdom.Element;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ViewProjectBuildHistoryActionTest extends MockApplicationContextStrutsTestCase {
	ProjectStatusDto status = new ProjectStatusDto();
	Document dom = new Document();
	List<UUID> ids = new ArrayList<UUID>();
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/viewProjectBuildHistory.do");
		
		dom.setRootElement(new Element("project"));
		status.setName("some project");
		
		ids.add(UUID.randomUUID());
		ids.add(UUID.randomUUID());
		ids.add(UUID.randomUUID());
	}
	
	public void testBlankName() throws Exception {
		addRequestParameter("startIndex", "0");
		addRequestParameter("endIndex", "2");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		
		assertPropertyHasError("projectNames", "errors.required");
	}
	public void testHighOutOfRange() throws Exception {
		addRequestParameter("projectNames", "some project");
		addRequestParameter("rangeType", "index");
		addRequestParameter("startIndex", "0");
		addRequestParameter("endIndex", "35");

		buildManager.getAvailableStatusIds("some project");
		EasyMock.expectLastCall().andReturn(ids);

		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		
		assertPropertyHasError("endIndex", "errors.out.of.range");
	}
	public void testNullIds() throws Exception {
		addRequestParameter("projectNames", "some project");
		addRequestParameter("rangeType", "index");
		addRequestParameter("startIndex", "0");
		addRequestParameter("endIndex", "2");

		buildManager.getAvailableStatusIds("some project");
		EasyMock.expectLastCall().andReturn(null);

		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.no.history");
	}
	public void testEmptyIds() throws Exception {
		addRequestParameter("projectNames", "some project");
		addRequestParameter("rangeType", "index");
		addRequestParameter("startIndex", "0");
		addRequestParameter("endIndex", "2");

		buildManager.getAvailableStatusIds("some project");
		EasyMock.expectLastCall().andReturn(Collections.emptyList());

		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.no.history");
	}
	public void testEmptyIdsByDateRange() throws Exception {
		addRequestParameter("projectNames", "Trundle");
		addRequestParameter("rangeType", "date");
		addRequestParameter("startDate", "06/6/2002");
		addRequestParameter("endDate", "06/6/2003");
		
		buildManager.getAvailableStatusIdsInRange(Collections.singleton("Trundle"), new Date(1023336000000L), new Date(1054872000000L));
		EasyMock.expectLastCall().andReturn(Collections.emptyList());
		
		replay();
		
		actionPerform();
		
		verify();

		verifyInputForward();
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.no.history");
	}
	public void testBlankByDate() throws Exception {
		addRequestParameter("rangeType", "date");
		replay();
		
		actionPerform();
		
		verify();

		verifyInputForward();
		
		assertPropertyHasError("projectNames", "errors.required");
		assertPropertyHasError("startDate", "errors.required");
		assertPropertyHasError("endDate", "errors.required");
	}
	public void testBlankByIndex() throws Exception {
		addRequestParameter("projectNames", "foo");
		addRequestParameter("rangeType", "index");
		
		replay();
		
		actionPerform();
		
		verify();

		verifyInputForward();
		
		assertPropertyHasError("startIndex", "errors.required");
		assertPropertyHasError("endIndex", "errors.required");
	}
	public void testIndicesOutOfOrder() throws Exception {
		addRequestParameter("projectNames", "some project");
		addRequestParameter("rangeType", "index");
		addRequestParameter("startIndex", "2");
		addRequestParameter("endIndex", "0");

		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		
		assertPropertyHasError("startIndex", "errors.out.of.order");
	}
	public void testGetsSummaries() throws Exception {
		addRequestParameter("projectNames", "Trundle");
		addRequestParameter("rangeType", "index");
		addRequestParameter("startIndex", "1");
		addRequestParameter("endIndex", "2");
		addRequestParameter("download", "false");
		
		buildManager.getAvailableStatusIds("Trundle");
		EasyMock.expectLastCall().andReturn(ids);
		
		buildManager.getStatus(ids.get(1));
		EasyMock.expectLastCall().andReturn(status);
		
		buildManager.getStatus(ids.get(2));
		EasyMock.expectLastCall().andReturn(status);
		
		projectDomBuilder.createProjectSummaries(Collections.nCopies(2, status),
				"1", "2", request.getLocale());
		
		EasyMock.expectLastCall().andReturn(dom);
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
		assertEquals(null, response.getHeader("Content-Disposition"));
	}		
	public void testIncludeAll() throws Exception {
		addRequestParameter("download", "true");
		addRequestParameter("projectNames", "Trundle");
		addRequestParameter("rangeType", "all");
		
		buildManager.getAvailableStatusIds("Trundle");
		expectLastCall().andReturn(ids);
		
		buildManager.getStatus(ids.get(0));
		expectLastCall().andReturn(status);
		
		buildManager.getStatus(ids.get(1));
		expectLastCall().andReturn(status);
		
		buildManager.getStatus(ids.get(2));
		expectLastCall().andReturn(status);

		projectDomBuilder.createProjectSummaries(Collections.nCopies(3, status),
				"0", "3", request.getLocale());
		
		expectLastCall().andReturn(dom);
		
		replay();
		
		actionPerform();
		
		verifyNoActionErrors();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
		assertEquals("attachment; filename=vulcan-build-history.xml", response.getHeader("Content-Disposition"));
	}		
	public void testIncludeAllMultipleProjects() throws Exception {
		addRequestParameter("projectNames", new String[] {"Trundle", "Other"});
		addRequestParameter("rangeType", "all");
		
		buildManager.getAvailableStatusIds("Trundle");
		expectLastCall().andReturn(ids.subList(0, 1));

		buildManager.getAvailableStatusIds("Other");
		expectLastCall().andReturn(ids.subList(1, 2));
		
		buildManager.getStatus(ids.get(0));
		expectLastCall().andReturn(status);
		
		buildManager.getStatus(ids.get(1));
		expectLastCall().andReturn(status);
		
		projectDomBuilder.createProjectSummaries(Collections.nCopies(2, status),
				"0", "*", request.getLocale());
		
		expectLastCall().andReturn(dom);
		
		replay();
		
		actionPerform();
		
		verifyNoActionErrors();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
	}		
	public void testGetsSummariesByDateRangeInvalid() throws Exception {
		addRequestParameter("rangeType", "date");
		addRequestParameter("projectNames", "Trundle");
		addRequestParameter("startDate", "poofy");
		addRequestParameter("endDate", "1");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		
		assertPropertyHasError("startDate", "errors.date");
		assertPropertyHasError("endDate", "errors.date");
	}
	public void testGetsSummariesByDateRange() throws Exception {
		addRequestParameter("rangeType", "date");
		addRequestParameter("projectNames", "Trundle");
		addRequestParameter("startDate", "06/6/2002");
		addRequestParameter("endDate", "06/6/2003");
		
		final Date start = new Date(1023336000000L);
		final Date end = new Date(1054872000000L);
		
		buildManager.getAvailableStatusIdsInRange(Collections.singleton("Trundle"), start, end);
		EasyMock.expectLastCall().andReturn(ids);
		
		buildManager.getStatus(ids.get(0));
		EasyMock.expectLastCall().andReturn(status);
		
		buildManager.getStatus(ids.get(1));
		EasyMock.expectLastCall().andReturn(status);
		
		buildManager.getStatus(ids.get(2));
		EasyMock.expectLastCall().andReturn(status);
		
		projectDomBuilder.createProjectSummaries(Collections.nCopies(3, status),
				start, end, request.getLocale());
		EasyMock.expectLastCall().andReturn(dom);
		
		replay();
		
		actionPerform();
		
		verifyNoActionErrors();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
	}
}

