/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import java.text.SimpleDateFormat;

import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;

import org.apache.struts.action.ActionMessages;
import org.easymock.EasyMock;
import org.jdom.Document;
import org.jdom.Element;

public class ViewProjectBuildHistoryActionTest extends MockApplicationContextStrutsTestCase {
	ProjectStatusDto status = new ProjectStatusDto();
	List<ProjectStatusDto> results = Collections.nCopies(2, status);
	Document dom = new Document();
	List<UUID> ids = new ArrayList<UUID>();
	BuildOutcomeQueryDto query = new BuildOutcomeQueryDto();
	
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
	public void testNoDataFound() throws Exception {
		addRequestParameter("projectNames", "some project");
		addRequestParameter("rangeType", "index");
		addRequestParameter("minBuildNumber", "0");
		addRequestParameter("maxBuildNumber", "2");

		query.setMinBuildNumber(0);
		query.setMaxBuildNumber(2);
		query.setProjectNames(Collections.singleton("some project"));
		
		buildOutcomeStore.loadBuildSummaries(query);
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
		
		assertPropertyHasError("minBuildNumber", "errors.required");
		assertPropertyHasError("maxBuildNumber", "errors.required");
	}
	public void testIndicesOutOfOrder() throws Exception {
		addRequestParameter("projectNames", "some project");
		addRequestParameter("rangeType", "index");
		addRequestParameter("minBuildNumber", "2");
		addRequestParameter("maxBuildNumber", "0");

		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		
		assertPropertyHasError("minBuildNumber", "errors.out.of.order");
	}
	public void testGetsSummaries() throws Exception {
		addRequestParameter("projectNames", "Trundle");
		addRequestParameter("rangeType", "index");
		addRequestParameter("minBuildNumber", "1");
		addRequestParameter("maxBuildNumber", "2");
		addRequestParameter("download", "false");
		
		query.setMinBuildNumber(1);
		query.setMaxBuildNumber(2);
		query.setProjectNames(Collections.singleton("Trundle"));
		
		buildOutcomeStore.loadBuildSummaries(query);
		EasyMock.expectLastCall().andReturn(results);
		
		projectDomBuilder.createProjectSummaries(results,
				"1", "2", request.getLocale());
		
		EasyMock.expectLastCall().andReturn(dom);
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
		assertEquals(null, response.getHeader("Content-Disposition"));
	}		
	public void testStoresInSessionForOpenFlashChart() throws Exception {
		addRequestParameter("transform", "OpenFlashChart");
		addRequestParameter("projectNames", "Trundle");
		addRequestParameter("rangeType", "index");
		addRequestParameter("minBuildNumber", "1");
		addRequestParameter("maxBuildNumber", "2");
		addRequestParameter("download", "false");
		
		query.setMinBuildNumber(1);
		query.setMaxBuildNumber(2);
		query.setProjectNames(Collections.singleton("Trundle"));
		
		buildOutcomeStore.loadBuildSummaries(query);
		EasyMock.expectLastCall().andReturn(results);
		
		projectDomBuilder.createProjectSummaries(results,
				"1", "2", request.getLocale());
		
		EasyMock.expectLastCall().andReturn(dom);

		buildOutcomeStore.loadTopBuildErrors(query, 5);
		final List<BuildMessageDto> buildFailures = new ArrayList<BuildMessageDto>();
		EasyMock.expectLastCall().andReturn(buildFailures);

		buildOutcomeStore.loadTopTestFailures(query, 5);
		final List<TestFailureDto> testFailures = new ArrayList<TestFailureDto>();
		EasyMock.expectLastCall().andReturn(testFailures);
		
		MetricDto m1 = new MetricDto();
		MetricDto m2 = new MetricDto();
		MetricDto m3 = new MetricDto();
		m1.setMessageKey("b.key");
		m1.setType(MetricType.NUMBER);
		m2.setMessageKey("a.key");
		m2.setType(MetricType.PERCENT);
		m3.setMessageKey("c.key");
		m3.setType(MetricType.STRING);
		status.setMetrics(Arrays.asList(m1, m2, m3));
		status.setCompletionDate(new Date(1000L));

		replay();
		
		actionPerform();
		
		verify();

		verifyForward("OpenFlashChart");
		
		assertSame(dom, request.getSession().getAttribute("buildHistory"));
		assertEquals(Arrays.asList("a.key", "b.key"), request.getAttribute("availableMetrics"));
		assertSame(buildFailures, request.getAttribute("topErrors"));
		assertSame(testFailures, request.getAttribute("topTestFailures"));
	}		
	public void testIncludeAll() throws Exception {
		addRequestParameter("download", "true");
		addRequestParameter("projectNames", "Trundle");
		addRequestParameter("rangeType", "all");
		
		query.setProjectNames(Collections.singleton("Trundle"));
		
		buildOutcomeStore.loadBuildSummaries(query);
		EasyMock.expectLastCall().andReturn(results);
		
		projectDomBuilder.createProjectSummaries(results,
				"0", "*", request.getLocale());
		
		expectLastCall().andReturn(dom);
		
		replay();
		
		actionPerform();
		
		verifyNoActionErrors();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
		assertEquals("attachment; filename=vulcan-build-history.xml", response.getHeader("Content-Disposition"));
	}		
	
	public void testOmitSkipAndError() throws Exception {
		addRequestParameter("download", "true");
		addRequestParameter("projectNames", "Trundle");
		addRequestParameter("rangeType", "all");
		addRequestParameter("statusTypes", new String[] {"PASS", "FAIL"});
		
		query.setStatuses(new HashSet<Status>(Arrays.asList(Status.PASS, Status.FAIL)));
		query.setProjectNames(Collections.singleton("Trundle"));
		
		buildOutcomeStore.loadBuildSummaries(query);
		EasyMock.expectLastCall().andReturn(results);
		
		projectDomBuilder.createProjectSummaries(results,
				"0", "*", request.getLocale());
		
		expectLastCall().andReturn(dom);
		
		replay();
		
		actionPerform();
		
		verifyNoActionErrors();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
		assertEquals("attachment; filename=vulcan-build-history.xml", response.getHeader("Content-Disposition"));
	}

	public void testQueryByUpdateType() throws Exception {
		addRequestParameter("download", "true");
		addRequestParameter("projectNames", "Trundle");
		addRequestParameter("rangeType", "all");
		addRequestParameter("updateType", "Incremental");
		
		query.setProjectNames(Collections.singleton("Trundle"));
		query.setUpdateType(UpdateType.Incremental);
		
		buildOutcomeStore.loadBuildSummaries(query);
		EasyMock.expectLastCall().andReturn(results);
		
		projectDomBuilder.createProjectSummaries(results,
				"0", "*", request.getLocale());
		
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
		
		query.setProjectNames(new HashSet<String>(Arrays.asList("Trundle", "Other")));
		
		buildOutcomeStore.loadBuildSummaries(query);
		EasyMock.expectLastCall().andReturn(results);
		
		projectDomBuilder.createProjectSummaries(results,
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
		
		final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		final Date start = format.parse("06/6/2002");
		final Date end = format.parse("06/6/2003");
		
		query.setProjectNames(Collections.singleton("Trundle"));
		query.setMinDate(start);
		query.setMaxDate(end);
		
		buildOutcomeStore.loadBuildSummaries(query);
		EasyMock.expectLastCall().andReturn(results);
		
		projectDomBuilder.createProjectSummaries(results,
				start, end, request.getLocale());
		EasyMock.expectLastCall().andReturn(dom);
		
		replay();
		
		actionPerform();
		
		verifyNoActionErrors();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
	}
}

