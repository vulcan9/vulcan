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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.io.File;
import java.net.URL;

import javax.xml.transform.Result;

import junit.framework.AssertionFailedError;

import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.dto.PreferencesDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.exception.NoSuchTransformFormatException;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.Keys;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessages;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.jdom.Document;
import org.jdom.Element;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ViewProjectStatusActionTest extends MockApplicationContextStrutsTestCase {
	ProjectStatusDto status = new ProjectStatusDto();
	Document dom = new Document();
	List<UUID> ids = new ArrayList<UUID>();
	ProjectConfigDto projectConfig = new ProjectConfigDto();

	File helloFile = new File("");
	
	Map<String, Object> paramMap = new HashMap<String, Object>();
	Map<UUID, ProjectStatusDto> outcomeMap = new HashMap<UUID, ProjectStatusDto>();
	
	Integer mostRecentBuildNumberByWorkDir = 42;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		request.setContextPath("/vulcan-web");
		setRequestPathInfo("/viewProjectStatus.do");
		
		dom.setRootElement(new Element("project"));
		status.setName("some project");
		status.setBuildNumber(12);
		
		ids.add(UUID.randomUUID());
		ids.add(UUID.randomUUID());
		ids.add(UUID.randomUUID());
		
		for (UUID id : ids) {
			final ProjectStatusDto outcome = new ProjectStatusDto();
			outcome.setBuildNumber(11);
			outcomeMap.put(id, outcome);	
		}
		
		getActionServlet().getServletContext().setAttribute(Globals.SERVLET_KEY, "*.foobar");
		
		projectConfig.setName("some project");
		projectConfig.setBugtraqUrl("http://something/%BUGID%");
		paramMap.put("issueTrackerURL", projectConfig.getBugtraqUrl());
		paramMap.put("view", "summary");
		
		expect(buildManager.getMostRecentBuildNumberByWorkDir((String)anyObject())).andAnswer(new IAnswer<Integer>() {
			public Integer answer() throws Throwable {
				return mostRecentBuildNumberByWorkDir;
			}
		}).anyTimes();
		
		expect(buildManager.getStatus((UUID)notNull())).andAnswer(new IAnswer<ProjectStatusDto>() {
			public ProjectStatusDto answer() throws Throwable {
				final Object id = EasyMock.getCurrentArguments()[0];
				if (outcomeMap.containsKey(id)) {
					return outcomeMap.get(id);	
				}
				throw new AssertionFailedError("Unexpected method call: buildManager.getStatus(" + id + ")");
			}
		}).anyTimes();
	}
	
	public void testBlankName() throws Exception {
		addRequestParameter("projectName", "");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		
		assertPropertyHasError("projectName", "errors.required");
	}
	public void testNoProject() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andThrow(new NoSuchProjectException("some project"));

		addRequestParameter("projectName", "some project");

		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("failure");
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.no.such.project");
	}
	public void testIndexNotNumber() throws Exception {
		addRequestParameter("projectName", "some project");
		addRequestParameter("index", "pickle");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyInputForward();
		
		assertPropertyHasError("index", "errors.integer");
	}
	public void testEmptyStatus() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(Collections.emptyList());

		addRequestParameter("projectName", "some project");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.status.not.available");
		assertNull(request.getAttribute("currentlyBuilding"));
	}
	public void testNullStatus() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());

		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);

		outcomeMap.put(ids.get(ids.size()-1), null);
		
		addRequestParameter("projectName", "some project");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.status.not.available");
		assertNull(request.getAttribute("currentlyBuilding"));
	}
	public void testNullBuildIds() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());

		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(null);

		addRequestParameter("projectName", "some project");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.status.not.available");
		assertNull(request.getAttribute("currentlyBuilding"));
	}
	public void testNullBuildIdsCurrentlyBuilding() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.singletonMap("some project", status));
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);

		projectDomBuilder.createProjectDocument(status, request.getLocale());
		expectLastCall().andReturn(dom);

		addRequestParameter("projectName", "some project");
		
		replay();
		
		actionPerform();
		
		verify();

		assertEquals("application/xml", response.getContentType());
		
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>  <previous-build-number>11</previous-build-number></project>",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testCurrentlyBuildingShowsNewest() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.singletonMap("some project", status));
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);

		projectDomBuilder.createProjectDocument(status, request.getLocale());
		expectLastCall().andReturn(dom);

		addRequestParameter("projectName", "some project");
		
		replay();
		
		actionPerform();
		
		verify();

		assertEquals("application/xml", response.getContentType());
		
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>  <previous-build-number>11</previous-build-number></project>",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetByIndexCurrentlyBuilding() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.singletonMap("some project", status));
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);

		projectDomBuilder.createProjectDocument(status, request.getLocale());
		expectLastCall().andReturn(dom);

		addRequestParameter("projectName", "some project");
		addRequestParameter("index", Integer.toString(ids.size()));
		
		replay();
		
		actionPerform();
		
		verify();

		assertEquals("application/xml", response.getContentType());
		
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>  <previous-build-number>11</previous-build-number></project>",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetByBuildNumberCurrentlyBuilding() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.singletonMap("some project", status));
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(Collections.emptyList());

		projectDomBuilder.createProjectDocument(status, request.getLocale());
		expectLastCall().andReturn(dom);

		addRequestParameter("projectName", "some project");
		addRequestParameter("buildNumber", "3343");
		
		status.setBuildNumber(3343);
		
		replay();
		
		actionPerform();
		
		verify();

		assertEquals("application/xml", response.getContentType());
		
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><project />",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetOldStatus() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);
		
		outcomeMap.put(ids.get(1), status);

		projectDomBuilder.createProjectDocument(status, request.getLocale());
		expectLastCall().andReturn(dom);
		
		addRequestParameter("projectName", "some project");
		addRequestParameter("index", "1");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
		
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>  <previous-build-number>11</previous-build-number>  <next-build-number>11</next-build-number></project>",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetOldStatusHasNextIndexWhenCurrentlyBuilding() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.singletonMap("some project", status));
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);
		
		outcomeMap.put(ids.get(2), status);

		projectDomBuilder.createProjectDocument(status, request.getLocale());
		expectLastCall().andReturn(dom);
		
		addRequestParameter("projectName", "some project");
		addRequestParameter("index", "2");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
		
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>  <previous-build-number>11</previous-build-number>  <next-build-number>13</next-build-number></project>",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetStatusByBuildNumber() throws Exception {
		status.setId(ids.get(1));
		
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);

		buildManager.getStatusByBuildNumber("some project", 1234);
		expectLastCall().andReturn(status);
		
		projectDomBuilder.createProjectDocument(status, request.getLocale());
		expectLastCall().andReturn(dom);
		
		addRequestParameter("projectName", "some project");
		addRequestParameter("buildNumber", "1234");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
		
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>  <previous-build-number>11</previous-build-number>  <next-build-number>11</next-build-number></project>",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetStatusByBuildNumberNotExist() throws Exception {
		status.setId(ids.get(1));
		
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);

		buildManager.getStatusByBuildNumber("some project", 1234);
		expectLastCall().andReturn(null);
		
		addRequestParameter("projectName", "some project");
		addRequestParameter("buildNumber", "1234");

		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("failure");
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.status.not.available.by.build.number");
	}
	public void testGetDiff() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);
		
		status.setDiffId(UUID.randomUUID());
		
		outcomeMap.put(ids.get(1), status);

		configurationStore.getChangeLog("some project", status.getDiffId());
		expectLastCall().andReturn(TestUtils.resolveRelativeFile("source/test/servlet/file.txt"));
		
		addRequestParameter("projectName", "some project");
		addRequestParameter("index", "1");
		addRequestParameter("view", "diff");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("text/plain", response.getContentType());
		
		assertEquals("sometext",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetBuildLog() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);
		
		status.setBuildLogId(UUID.randomUUID());
		
		outcomeMap.put(ids.get(1), status);

		configurationStore.getBuildLog("some project", status.getBuildLogId());
		expectLastCall().andReturn(TestUtils.resolveRelativeFile("source/test/servlet/file.txt"));
		
		addRequestParameter("projectName", "some project");
		addRequestParameter("index", "1");
		addRequestParameter("view", "log");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("text/plain", response.getContentType());
		
		assertEquals("sometext",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetOldStatusIndexOutOfBounds() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);

		addRequestParameter("projectName", "some project");
		addRequestParameter("index", "11");
		
		replay();
		
		actionPerform();
		
		verify();

		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.request.invalid");
	}
	public void testTransform() throws Exception {
		paramMap.put("locale", request.getLocale().toString());
		paramMap.put("contextRoot", "/vulcan-web");
		paramMap.put("viewProjectStatusURL", new URL("http://localhost/vulcan-web/projects/"));
		paramMap.put("workingCopyBuildNumber", 42);
		
		final Map<String, ProjectStatusDto> empty = Collections.emptyMap();
		trainForTransform(empty, "xhtml");

		addRequestParameter("projectName", "some project");
		addRequestParameter("transform", "xhtml");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("text/html", response.getContentType());
	}
	public void testTransformSetsView() throws Exception {
		paramMap.put("locale", request.getLocale().toString());
		paramMap.put("contextRoot", "/vulcan-web");
		paramMap.put("viewProjectStatusURL", new URL("http://localhost/vulcan-web/projects/"));
		paramMap.put("workingCopyBuildNumber", 42);
		paramMap.put("view", "metrics");
		final Map<String, ProjectStatusDto> empty = Collections.emptyMap();
		trainForTransform(empty, "xhtml");

		addRequestParameter("projectName", "some project");
		addRequestParameter("transform", "xhtml");
		addRequestParameter("view", "metrics");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("text/html", response.getContentType());
	}
	public void testTransformUppercaseIssueTrackerBugId() throws Exception {
		projectConfig.setBugtraqUrl("http://%bugid%/");
		
		paramMap.put("locale", request.getLocale().toString());
		paramMap.put("contextRoot", "/vulcan-web");
		paramMap.put("viewProjectStatusURL", new URL("http://localhost/vulcan-web/projects/"));
		paramMap.put("workingCopyBuildNumber", 42);
		paramMap.put("issueTrackerURL", "http://%BUGID%/");
		
		final Map<String, ProjectStatusDto> empty = Collections.emptyMap();
		trainForTransform(empty, "xhtml");

		addRequestParameter("projectName", "some project");
		addRequestParameter("transform", "xhtml");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("text/html", response.getContentType());
	}
	public void testTransformSetsRefreshIntervalIfPresent() throws Exception {
		mostRecentBuildNumberByWorkDir = null;
		
		PreferencesDto prefs = new PreferencesDto();
		prefs.setReloadInterval(3424);
		request.getSession().setAttribute(Keys.PREFERENCES, prefs);
		
		paramMap.put("locale", request.getLocale().toString());
		paramMap.put("workingCopyBuildNumber", -1);
		paramMap.put("contextRoot", "/vulcan-web");
		paramMap.put("reloadInterval", Integer.valueOf(prefs.getReloadInterval()));
		paramMap.put("viewProjectStatusURL", new URL("http://localhost/vulcan-web/projects/"));
		
		final Map<String, ProjectStatusDto> empty = Collections.emptyMap();
		trainForTransform(empty, "xhtml");

		addRequestParameter("projectName", "some project");
		addRequestParameter("transform", "xhtml");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("text/html", response.getContentType());
	}
	public void testTransformBadFormatType() throws Exception {
		paramMap.put("locale", request.getLocale().toString());
		paramMap.put("workingCopyBuildNumber", 42);
		paramMap.put("contextRoot", "/vulcan-web");
		paramMap.put("viewProjectStatusURL", new URL("http://localhost/vulcan-web/projects/"));
		final Map<String, ProjectStatusDto> empty = Collections.emptyMap();
		trainForTransform(empty, "nonesuch");

		addRequestParameter("projectName", "some project");
		addRequestParameter("transform", "nonesuch");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("failure");
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.transform.not.found");
	}
	
	private void trainForTransform(Map<String, ProjectStatusDto> projectsBeingBuilt, String transormType) throws Exception {
		expect(manager.getProjectConfig("some project")).andReturn(projectConfig);

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(projectsBeingBuilt);

		expect(buildManager.getAvailableStatusIds("some project")).andReturn(ids);

		if (!projectsBeingBuilt.containsKey("some project")) {
			outcomeMap.put(ids.get(ids.size()-1), status);
		}
		
		expect(projectDomBuilder.createProjectDocument(status, request.getLocale()))
			.andReturn(dom);
		
		projectDomBuilder.transform(
				(Document) anyObject(),
				eq(paramMap),
				eq(request.getLocale()),
				eq(transormType),
				(Result)anyObject());
		
		if (transormType.equals("nonesuch")) {
			expectLastCall().andThrow(new NoSuchTransformFormatException());	
		} else {
			expectLastCall().andReturn("text/html");
		}
	}
}
