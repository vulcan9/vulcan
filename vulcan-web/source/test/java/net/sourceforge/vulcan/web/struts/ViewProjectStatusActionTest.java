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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.io.ByteArrayInputStream;
import java.net.URL;

import javax.xml.transform.Result;

import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.exception.NoSuchTransformFormatException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionMessages;
import org.jdom.Document;
import org.jdom.Element;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ViewProjectStatusActionTest extends MockApplicationContextStrutsTestCase {
	ProjectStatusDto status = new ProjectStatusDto();
	Document dom = new Document();
	List<UUID> ids = new ArrayList<UUID>();
	ProjectConfigDto projectConfig = new ProjectConfigDto();

	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/viewProjectStatus.do");
		
		dom.setRootElement(new Element("project"));
		status.setName("some project");
		
		ids.add(UUID.randomUUID());
		ids.add(UUID.randomUUID());
		ids.add(UUID.randomUUID());
		
		getActionServlet().getServletContext().setAttribute(Globals.SERVLET_KEY, "*.foobar");
		
		projectConfig.setName("some project");
	}
	
	public void testBlankName() throws Exception {
		addRequestParameter("projectName", "");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("failure");
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.request.invalid");
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
		
		verifyForward("failure");
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.request.invalid");
	}
	public void testEmptyStatus() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(Collections.emptyList());

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());

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

		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);

		buildManager.getStatus(ids.get(ids.size()-1));
		expectLastCall().andReturn(null);

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());

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

		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(null);

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());

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

		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(null);

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.singletonMap("some project", new BuildDaemonInfoDto()));

		addRequestParameter("projectName", "some project");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.status.not.available");
		assertEquals(Boolean.TRUE, request.getAttribute("currentlyBuilding"));
	}
	public void testGetXml() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());

		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);

		buildManager.getStatus(ids.get(ids.size()-1));
		expectLastCall().andReturn(status);

		buildManager.getProjectsBeingBuilt();
		expectLastCall().andReturn(Collections.emptyMap());
		
		projectDomBuilder.createProjectDocument(status, request.getLocale());
		expectLastCall().andReturn(dom);
		
		addRequestParameter("projectName", "some project");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
		
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>  <prev-index>1</prev-index></project>",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetOldStatus() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);
		
		buildManager.getStatus(ids.get(1));
		expectLastCall().andReturn(status);

		projectDomBuilder.createProjectDocument(status, request.getLocale());
		expectLastCall().andReturn(dom);
		
		addRequestParameter("projectName", "some project");
		addRequestParameter("index", "1");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("application/xml", response.getContentType());
		
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>  <prev-index>0</prev-index>  <next-index>2</next-index></project>",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetDiff() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);
		
		status.setDiffId(UUID.randomUUID());
		
		buildManager.getStatus(ids.get(1));
		expectLastCall().andReturn(status);

		store.getChangeLogInputStream("some project", status.getDiffId());
		expectLastCall().andReturn(new ByteArrayInputStream("hello".getBytes()));
		
		addRequestParameter("projectName", "some project");
		addRequestParameter("index", "1");
		addRequestParameter("view", "diff");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("text/plain", response.getContentType());
		
		assertEquals("hello",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetBuildLog() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());
		
		buildManager.getAvailableStatusIds("some project");
		expectLastCall().andReturn(ids);
		
		status.setBuildLogId(UUID.randomUUID());
		
		buildManager.getStatus(ids.get(1));
		expectLastCall().andReturn(status);

		store.getBuildLogInputStream("some project", status.getBuildLogId());
		expectLastCall().andReturn(new ByteArrayInputStream("hello".getBytes()));
		
		addRequestParameter("projectName", "some project");
		addRequestParameter("index", "1");
		addRequestParameter("view", "log");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("text/plain", response.getContentType());
		
		assertEquals("hello",
				response.getWriterBuffer().toString().trim()
					.replaceAll("\n", "").replaceAll("\r", ""));
	}
	public void testGetOldStatusIndexOutOfBounds() throws Exception {
		manager.getProjectConfig("some project");
		expectLastCall().andReturn(new ProjectConfigDto());
		
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
		final Map<String, BuildDaemonInfoDto> empty = Collections.emptyMap();
		trainForTransform(empty, "xhtml");

		addRequestParameter("projectName", "some project");
		addRequestParameter("transform", "xhtml");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("text/html", response.getContentType());
	}

	public void testTransformCurrentlyBuilding() throws Exception {
		projectConfig.setBugtraqUrl("http://localhost");
		
		trainForTransform(Collections.singletonMap("some project", new BuildDaemonInfoDto()), "xhtml");
		
		addRequestParameter("projectName", "some project");
		addRequestParameter("transform", "xhtml");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertEquals("text/html", response.getContentType());
		assertNotNull("dom should have added flag", dom.getRootElement().getChild("currently-building"));
	}
	public void testTransformBadFormatType() throws Exception {
		final Map<String, BuildDaemonInfoDto> empty = Collections.emptyMap();
		trainForTransform(empty, "nonesuch");

		expectLastCall().andThrow(new NoSuchTransformFormatException());
		
		addRequestParameter("projectName", "some project");
		addRequestParameter("transform", "nonesuch");
		
		replay();
		
		actionPerform();
		
		verify();
		
		verifyForward("failure");
		assertPropertyHasError(ActionMessages.GLOBAL_MESSAGE, "errors.transform.not.found");
	}
	
	private void trainForTransform(Map<String, BuildDaemonInfoDto> projectsBeingBuilt, String transormType) throws Exception {
		expect(manager.getProjectConfig("some project")).andReturn(projectConfig);

		expect(buildManager.getAvailableStatusIds("some project")).andReturn(ids);

		expect(buildManager.getStatus(ids.get(ids.size()-1))).andReturn(status);
		
		expect(buildManager.getProjectsBeingBuilt()).andReturn(projectsBeingBuilt);
		
		expect(projectDomBuilder.createProjectDocument(status, request.getLocale()))
			.andReturn(dom);
		
		projectDomBuilder.transform(
				(Document) anyObject(),
				(URL)anyObject(),
				(URL)anyObject(),
				(URL)anyObject(),
				anyInt(),
				eq(request.getLocale()),
				eq(transormType), (Result)anyObject());
	}
}
