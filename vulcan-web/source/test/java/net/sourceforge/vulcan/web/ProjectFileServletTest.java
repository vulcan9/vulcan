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
package net.sourceforge.vulcan.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import javax.servlet.ServletException;

import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.core.support.FileSystem;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.NoSuchProjectException;

import org.springframework.web.context.WebApplicationContext;

public class ProjectFileServletTest extends ServletTestCase {
	final static File TEST_DIR = TestUtils.resolveRelativeFile("source/test/servlet");

	final Date modDate = new Date(1132616904000L);
	
	File fakeFile;
	
	ProjectFileServlet servlet = new ProjectFileServlet() {
		@Override
		protected File getFile(String workDir, String pathInfo, boolean stripProjectName) {
			if (fakeFile != null) {
				return fakeFile;
			}
			return super.getFile(projectConfig.getWorkDir(), pathInfo, stripProjectName);
		}
	};

	ProjectConfigDto projectConfig = new ProjectConfigDto();
	ProjectStatusDto latestStatus = new ProjectStatusDto();
	
	boolean buggy;
	boolean inputStreamCloseBuggy;
	boolean inputStreamClosed;
	boolean outputStreamCloseBuggy;
	boolean outputStreamClosed;
	
	final IOException copyException = new IOException();
	final IOException closeException = new IOException();
	
	final InputStream buggyInputStream = new InputStream() {
		@Override
		public int read() throws IOException {
			if (buggy)
				throw copyException;
			return -1;
		}
		@Override
		public void close() throws IOException {
			inputStreamClosed = true;
			if (inputStreamCloseBuggy)
				throw new IOException();
		}
	};
	
	final OutputStream buggyOutputStream = new OutputStream() {
		@Override
		public void write(int b) throws IOException {
			if (buggy)
				throw copyException;
		}
		@Override
		public void close() throws IOException {
			outputStreamClosed = true;
			if (outputStreamCloseBuggy)
				throw new IOException();
		}
	};

	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		request.setContextPath("/myApp");
		request.setServletPath("/site");
		
		assertTrue("Test directory not found; can't run tests!", TEST_DIR.isDirectory());
		
		projectConfig.setName("myProject");
		projectConfig.setWorkDir(TEST_DIR.getCanonicalPath());
		
		latestStatus.setBuildNumber(3351);
		
		servlet.setCacheEnabled(true);
		
		servlet.fileSystem = createStrictMock(FileSystem.class);
	}
	
	public void testInit() throws Exception {
		servlet.init(servletConfig);
	}
	
	public void testInitThrowsOnNoWac() throws Exception {
		servletContext.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		try {
			servlet.init(servletConfig);
			fail("expected exception");
		} catch (IllegalStateException e) {
		}
	}
	public void testClosesStreams() throws Exception {
		buggy = true;
		inputStreamCloseBuggy = true;
		outputStreamCloseBuggy = true;
		
		assertFalse(inputStreamClosed);
		assertFalse(outputStreamClosed);
		
		try {
			servlet.sendFile(buggyInputStream, buggyOutputStream);
			fail("expected IOException");
		} catch (IOException e) {
			assertSame(copyException, e);
		}
		
		assertTrue("input stream was not closed", inputStreamClosed);
		assertTrue("output stream was not closed", outputStreamClosed);
	}
	public void testClosesStreamsOnNoError() throws Exception {
		assertFalse(inputStreamClosed);
		assertFalse(outputStreamClosed);
		
		servlet.sendFile(buggyInputStream, buggyOutputStream);
		
		assertTrue("input stream was not closed", inputStreamClosed);
		assertTrue("output stream was not closed", outputStreamClosed);
	}
	public void testThrowsOnCloseIn() throws Exception {
		inputStreamCloseBuggy = true;
		
		assertFalse(inputStreamClosed);
		assertFalse(outputStreamClosed);
		
		try {
			servlet.sendFile(buggyInputStream, buggyOutputStream);
			fail("expected exception");
		} catch (IOException e) {
		}
		
		assertTrue("input stream was not closed", inputStreamClosed);
		assertTrue("output stream was not closed", outputStreamClosed);
	}
	public void testThrowsOnCloseOut() throws Exception {
		outputStreamCloseBuggy = true;
		
		assertFalse(inputStreamClosed);
		assertFalse(outputStreamClosed);
		
		try {
			servlet.sendFile(buggyInputStream, buggyOutputStream);
			fail("expected exception");
		} catch (IOException e) {
		}
		
		assertTrue("input stream was not closed", inputStreamClosed);
		assertTrue("output stream was not closed", outputStreamClosed);
	}
	public void testRedirectsOnNullPath() throws Exception {
		servlet.init(servletConfig);
		
		assertEquals(null, redirect);
		
		servlet.doGet(request, response);
		
		assertEquals("/myApp", redirect);
	}
	public void testRedirectsOnRootPath() throws Exception {
		servlet.init(servletConfig);
		
		request.setPathInfo("/");
		
		assertEquals(null, redirect);
		
		servlet.doGet(request, response);
		
		assertEquals("/myApp", redirect);
	}
	public void testRedirectsOnMissingBuildNumber() throws Exception {
		servlet.init(servletConfig);
		
		request.setRequestURI("/myApp/site/myProject");
		request.setPathInfo("/myProject");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getLatestStatus("myProject")).andReturn(latestStatus);
		
		replay();
		
		assertEquals(null, redirect);
		
		servlet.doGet(request, response);
		
		assertEquals("/myApp/site/myProject/3351/", redirect);
		
		verify();
	}
	public void testRedirectsOnMissingBuildNumberWithTrailingPath() throws Exception {
		servlet.init(servletConfig);
		
		request.setRequestURI("/myApp/site/myProject/file.txt");
		request.setPathInfo("/myProject/file.txt");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getLatestStatus("myProject")).andReturn(latestStatus);
		
		replay();
		
		assertEquals(null, redirect);
		
		servlet.doGet(request, response);
		
		assertEquals("/myApp/site/myProject/3351/file.txt", redirect);
		
		verify();
	}
	public void test404OnMissingProject() throws Exception {
		servlet.init(servletConfig);
		
		request.setPathInfo("/noSuchProject");
		
		expect(mgr.getProjectConfig("noSuchProject")).andThrow(new NoSuchProjectException("noSuchProject"));
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();
		
		assertEquals(404, response.getStatusCode());
	}
	public void test404OnMissingProjectWithSlash() throws Exception {
		servlet.init(servletConfig);
		
		request.setPathInfo("/noSuchProject/");
		
		expect(mgr.getProjectConfig("noSuchProject")).andThrow(new NoSuchProjectException("noSuchProject"));
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();
		
		assertEquals(404, response.getStatusCode());
	}
	public void test404OnMissingProjectWithSlashMore() throws Exception {
		servlet.init(servletConfig);
		
		request.setPathInfo("/noSuchProject/index.html");
		
		expect(mgr.getProjectConfig("noSuchProject")).andThrow(new NoSuchProjectException("noSuchProject"));
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();
		
		assertEquals(404, response.getStatusCode());
	}
	public void test404OnNoSuchBuild() throws Exception {
		servlet.init(servletConfig);
		
		request.setPathInfo("/myProject/555/none");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getStatusByBuildNumber("myProject", 555)).andReturn(null);
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();

		assertEquals(404, response.getStatusCode());
	}
	public void test404OnNoSuchFile() throws Exception {
		servlet.init(servletConfig);
		
		request.setPathInfo("/myProject/5/none");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getStatusByBuildNumber("myProject", 5)).andReturn(latestStatus);
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();

		assertEquals(404, response.getStatusCode());
	}
	public void test403OnCannotRead() throws Exception {
		fakeFile = new File(TEST_DIR, "youcantreadthis") {
			@Override
			public boolean canRead() {
				return false;
			}
			@Override
			public boolean exists() {
				return true;
			}
		};
		
		servlet.init(servletConfig);
		
		request.setPathInfo("/myProject/38/youcantreadthis");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getStatusByBuildNumber("myProject", 38)).andReturn(latestStatus);
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();

		assertEquals(403, response.getStatusCode());
	}
	public void testSendsFile() throws Exception {
		final File testFile = new File(TEST_DIR, "file.txt");
		assertTrue("Cannot set last modified date for unit test", testFile.setLastModified(modDate.getTime()));

		servlet.init(servletConfig);
		
		request.setPathInfo("/myProject/42/file.txt");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getStatusByBuildNumber("myProject", 42)).andReturn(latestStatus);
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();

		assertEquals(200, response.getStatusCode());
		
		assertEquals("sometext", os.toString().trim());
		assertEquals("text/plain", response.getContentType());
		
		final String lastModifiedHeader = response.getHeader("Last-Modified");
		
		assertNotNull(lastModifiedHeader);
		assertEquals(modDate, ProjectFileServlet.HTTP_DATE_FORMAT.parse(lastModifiedHeader));
		
		assertEquals(9, response.getContentLength());
		assertTrue(closeCalled);
	}
	public void testSendsFileWithOlderModifiedSinceHeader() throws Exception {
		final Date date = new Date(modDate.getTime() - 1000);
		request.setHeader("If-Modified-Since", ProjectFileServlet.HTTP_DATE_FORMAT.format(date));
		
		testSendsFile();
	}
	public void testSendsFileWithBlankOlderModifiedSinceHeader() throws Exception {
		request.setHeader("If-Modified-Since", "");
		
		testSendsFile();
	}
	public void testSendsFileWithBadOlderModifiedSinceHeader() throws Exception {
		request.setHeader("If-Modified-Since", "Wednesday The 44th");
		
		testSendsFile();
	}
	public void testSends304NotModified() throws Exception {
		final Date modifiedSince = new Date(modDate.getTime() + 1000);
		check304(modifiedSince);
	}
	public void testSends304NotModifiedOnEqualDates() throws Exception {
		check304(modDate);
	}

	private void check304(final Date modifiedSince) throws ServletException, IOException {
		final File testFile = new File(TEST_DIR, "file.txt");
		assertTrue("Cannot set last modified date for unit test", testFile.setLastModified(modDate.getTime()));
		
		request.setHeader("If-Modified-Since", ProjectFileServlet.HTTP_DATE_FORMAT.format(modifiedSince));
		
		servlet.init(servletConfig);
		
		request.setPathInfo("/myProject/38/file.txt");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getStatusByBuildNumber("myProject", 38)).andReturn(latestStatus);
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();

		assertEquals(304, response.getStatusCode());
		
		assertEquals("", os.toString().trim());
		assertEquals("text/plain", response.getContentType());
		assertEquals(0, response.getContentLength());
	}
	public void testClosesStreamsFromService() throws Exception {
		ioe = new IOException("You can't write that!");
		
		servlet.init(servletConfig);
		
		request.setPathInfo("/myProject/38/file.txt");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getStatusByBuildNumber("myProject", 38)).andReturn(latestStatus);
		
		replay();
		
		try {
			servlet.doGet(request, response);
			fail("expected IOException");
		} catch (IOException e) {
		}
		
		assertTrue(closeCalled);
		
		verify();
	}
	public void testSetsMimeType() throws Exception {
		mimeType = "application/x-some-custom-mime-type";
		
		servlet.init(servletConfig);
		
		request.setPathInfo("/myProject/99/file.txt");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getStatusByBuildNumber("myProject", 99)).andReturn(latestStatus);
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();

		assertEquals("sometext", os.toString().trim());
		assertEquals("application/x-some-custom-mime-type", response.getContentType());
		assertFalse("contentTypeSupressionEnabled", ContentTypeFilter.isContentTypeSupressionEnabled(request));
	}
	public void testSendsRedirectOnMissingWhenFallbackSet() throws Exception {
		request.setRequestURI("/myApp/myProject");
		request.addParameter("fallback", "");
		
		servlet.init(servletConfig);
		
		request.setPathInfo("/myProject/102/subdir/nosuchfile.txt");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getStatusByBuildNumber("myProject", 102)).andReturn(latestStatus);
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();
		
		assertEquals("/myApp/site/myProject/102/subdir/", redirect);
		assertEquals(0, response.getStatusCode());
	}
	public void testRedirectsToNestedFolderOnFallback() throws Exception {
		servlet.init(servletConfig);
		
		request.setPathInfo("/myProject/77/subdir/nosuchdubdir/nosuchfile.txt");
		request.addParameter("fallback", "");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getStatusByBuildNumber("myProject", 77)).andReturn(latestStatus);
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();

		assertEquals("/myApp/site/myProject/77/subdir/", redirect);
		assertEquals(0, response.getStatusCode());
	}
	public void testSendsRedirectOnDirListingNoSlash() throws Exception {
		request.setRequestURI("/myApp/myProject/5432");
		
		servlet.init(servletConfig);
		
		request.setPathInfo("/myProject/5432");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getStatusByBuildNumber("myProject", 5432)).andReturn(latestStatus);
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();
		
		assertEquals("/myApp/myProject/5432/", redirect);
	}
	public void testSendsDirListing() throws Exception {
		servlet.init(servletConfig);
		
		request.setPathInfo("/myProject/22/subdir/");
		
		expect(mgr.getProjectConfig("myProject")).andReturn(projectConfig);
		expect(buildManager.getStatusByBuildNumber("myProject", 22)).andReturn(latestStatus);
		
		expect(servlet.fileSystem.listFiles(TestUtils.resolveRelativeFile("source/test/servlet/subdir").getAbsoluteFile())).andReturn(new File[] {new File(".svn"), new File("file")});
		
		replay();
		
		servlet.doGet(request, response);
		
		verify();
		
		assertEquals("/myProject/22/subdir/", request.getAttribute(Keys.DIR_PATH));
		File[] files = (File[]) request.getAttribute(Keys.FILE_LIST);
		assertNotNull(files);

		assertEquals(2, files.length);
		assertEquals(".svn", files[0].getName());
		assertEquals("file", files[1].getName());
		
		assertEquals("/WEB-INF/jsp/fileList.jsp", requestedPath);
		assertTrue("contentTypeSupressionEnabled", ContentTypeFilter.isContentTypeSupressionEnabled(request));
	}
}
