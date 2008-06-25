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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.dto.BuildArtifactLocationDto;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.exception.NoSuchTransformFormatException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format.TextMode;
import org.jdom.transform.JDOMSource;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class AbstractProjectDomBuilderTest extends EasyMockTestCase {
	Map<String, String> messages = new HashMap<String, String>();
	
	ProjectStatusDto projectStatus = new ProjectStatusDto();
	AbstractProjectDomBuilder builder = new AbstractProjectDomBuilder() {
		@Override
		protected String formatMessage(String key, Object[] args, Locale locale) {
			final String string = messages.get(key);
			if (string == null) {
				return null;
			}
			return new MessageFormat(string).format(args);
		}
		@Override
		protected Transformer createTransformer(String format) throws NoSuchTransformFormatException {
			return trans;
		}
		@Override
		protected boolean artifactExists(String workDir, String path) {
			return artifactExists;
		}
	};
	
	Date date = new Date(1234567890L);
	
	ProjectManager pm;
	BuildManager bm;
	EventHandler eh;
	
	Source transSource;
	Result transResult;
	
	Transformer trans = new Transformer() {
		@Override
		public void clearParameters() {}
		@Override
		public ErrorListener getErrorListener() { return null; }
		@Override
		public Properties getOutputProperties() { return null; }
		@Override
		public String getOutputProperty(String name) throws IllegalArgumentException { return null; }
		@Override
		public Object getParameter(String name) { return null; }
		@Override
		public URIResolver getURIResolver() { return null; }
		@Override
		public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {}
		@Override
		public void setOutputProperties(Properties oformat) {}
		@Override
		public void setOutputProperty(String name, String value) throws IllegalArgumentException {}
		@Override
		public void setParameter(String name, Object value) {}
		@Override
		public void setURIResolver(URIResolver resolver) {}
		@Override
		public void transform(Source xmlSource, Result outputTarget) throws TransformerException {
			transSource = xmlSource;
			transResult = outputTarget;
		}
	};

	private boolean artifactExists;
	private List<BuildArtifactLocationDto> artifactLocations = new ArrayList<BuildArtifactLocationDto>();
	
	public AbstractProjectDomBuilderTest() {
		pm = createMock(ProjectManager.class);
		bm = createMock(BuildManager.class);
		eh = createMock(EventHandler.class);
		
		expect(pm.getArtifactLocations()).andReturn(artifactLocations).anyTimes();
	}
	
	@Override
	protected void setUp() throws Exception {
		projectStatus.setName("a name");
		projectStatus.setStatus(Status.PASS);
		projectStatus.setCompletionDate(date);
		projectStatus.setMessageKey("fake.outcome.message");
		projectStatus.setBuildLogId(UUID.randomUUID());
		projectStatus.setDiffId(UUID.randomUUID());
		projectStatus.setRevision(new RevisionTokenDto(0l, "fake revision"));
		projectStatus.setRepositoryUrl("http://localhost");
		projectStatus.setTagName("rc2");
		projectStatus.setBuildNumber(331);
		projectStatus.setWorkDir("/home/vulcan/work/a name");
		
		projectStatus.setErrors(new ArrayList<BuildMessageDto>());
		projectStatus.setWarnings(new ArrayList<BuildMessageDto>());
		
		messages.put("fake.outcome.message", "fake build outcome message");
		messages.put("build.timestamp.format", "HH:mm");
		messages.put("build.time.elapsed.format", "HH:mm:ss");
		messages.put("foo.bar.key", "{0} ... {1}");
		
		builder.setEventHandler(eh);
		builder.setProjectManager(pm);
		builder.setBuildManager(bm);
	}
	
	public void testNotNull() throws Exception {
		replay();

		assertNotNull(doCall());
		verify();
	}

	public void testRootElem() throws Exception {
		replay();

		final Document doc = doCall();
		assertEquals("project", doc.getRootElement().getName());
		verify();
	}
	
	public void testBasics() throws Exception {
		replay();

		projectStatus.setRequestedBy("Kate");
		projectStatus.setBuildReasonKey("foo.bar.key");
		projectStatus.setBuildReasonArgs(new Object[] {"bar", "baz"});
		
		final Document doc = doCall();

		final Element elem = doc.getRootElement();
		
		assertContainsChildWithText(elem, "name", projectStatus.getName());
		assertContainsChildWithText(elem, "status", projectStatus.getStatus().name());
		final Element tstamp = assertContainsChildWithText(elem, "timestamp", "01:56");
		assertEquals(Long.toString(date.getTime()), tstamp.getAttributeValue("millis"));
		
		assertContainsChildWithText(elem, "message", "fake build outcome message");
		
		assertContainsChildWithText(elem, "revision", "fake revision");
		
		assertNull(elem.getChild("diff"));
		assertNull(elem.getChild("changeSets"));
		
		assertNull(elem.getChild("last-good-build-revision"));
		assertNull(elem.getChild("last-good-build-timestamp"));
		
		assertContainsChildWithText(elem, "repository-url", "http://localhost");
		assertContainsChildWithText(elem, "repository-tag-name", "rc2");
		
		assertContainsChildWithText(elem, "build-requested-by", "Kate");
		
		assertNull(elem.getChild("warnings"));
		assertNull(elem.getChild("errors"));
		
		assertContainsChildWithText(elem, "build-reason", "bar ... baz");
		
		assertContainsChildWithText(elem, "build-number", "331");
		
		assertContainsChildWithText(elem, "update-type", "Full");
		
		assertNotNull(elem.getChild("diff-available"));
		assertNotNull(elem.getChild("build-log-available"));
		assertContainsChildWithText(elem, "work-directory", projectStatus.getWorkDir());
		
		verify();
	}
	
	public void testAddsReportsWhenPresent() throws Exception {
		artifactExists = true;
		artifactLocations.add(new BuildArtifactLocationDto("x", "report on x", "/x.html", true));
		
		replay();

		final Document doc = doCall();

		final Element elem = doc.getRootElement();
		
		final Element reportNode = elem.getChild("reports");
		
		assertNotNull(reportNode);
		
		assertEquals(1, reportNode.getContentSize());
	}
	
	public void testIgnoresReportsWhenNotPresent() throws Exception {
		artifactExists = false;
		artifactLocations.add(new BuildArtifactLocationDto("x", "report on x", "/x.html", true));
		
		replay();

		final Document doc = doCall();

		final Element elem = doc.getRootElement();
		
		final Element reportNode = elem.getChild("reports");
		
		assertEquals(null, reportNode);
	}
	
	public void testBuiltByScheduler() throws Exception {
		replay();

		projectStatus.setRequestedBy("Nightly");
		projectStatus.setScheduledBuild(true);
		
		final Document doc = doCall();

		final Element elem = doc.getRootElement();
		
		assertContainsChildWithText(elem, "build-scheduled-by", "Nightly");
	}
	public void testBasicsNull() throws Exception {
		projectStatus.setChangeLog(null);
		projectStatus.setStartDate(null);
		projectStatus.setCompletionDate(null);
		projectStatus.setDependencyIds(null);
		projectStatus.setMessageArgs(null);
		projectStatus.setMessageKey(null);
		projectStatus.setName("a name");
		projectStatus.setRevision(null);
		projectStatus.setStatus(null);
		projectStatus.setDiffId(null);
		projectStatus.setBuildLogId(null);
		projectStatus.setBuildReasonKey(null);
		projectStatus.setBuildReasonArgs(null);
		projectStatus.setWorkDir(null);
		
		reset();
		replay();

		final Document doc = doCall();

		final Element elem = doc.getRootElement();
		
		assertContainsChildWithText(elem, "name", projectStatus.getName());
		verify();
		
		assertNull(elem.getChild("diff-available"));
		assertNull(elem.getChild("build-log-available"));
	}

	public void testElapsedTime() throws Exception {
		replay();

		projectStatus.setStartDate(new Date(projectStatus.getCompletionDate().getTime() - 97000));
		
		final Document doc = doCall();

		final Element elem = doc.getRootElement();
		
		assertContainsChildWithText(elem, "name", projectStatus.getName());

		final Element tstamp = assertContainsChildWithText(elem, "timestamp", "01:56");
		assertEquals(Long.toString(date.getTime()), tstamp.getAttributeValue("millis"));
		
		final Element elapsed = assertContainsChildWithText(elem, "elapsed-time", "00:01:37");
		assertEquals("97000", elapsed.getAttributeValue("millis"));
		
		verify();
	}
	
	public void testElapsedTimeWhenCompletionDateNull() throws Exception {
		replay();

		projectStatus.setStartDate(new Date(projectStatus.getCompletionDate().getTime() - 97000));
		projectStatus.setCompletionDate(null);
		
		final Document doc = doCall();

		final Element elem = doc.getRootElement();
		
		assertContainsChildWithText(elem, "name", projectStatus.getName());

		final Element elapsed = elem.getChild("elapsed-time");
		assertNotNull(elapsed);
		
		assertNotNull(elapsed.getAttributeValue("millis"));
		
		verify();
	}

	@SuppressWarnings("unchecked")
	public void testHasDepMap() throws Exception {
		final Map<String, UUID> ids = new HashMap<String, UUID>();
		
		ids.put("one", UUID.randomUUID());
		ids.put("two", UUID.randomUUID());
		ids.put("three", UUID.randomUUID());
		
		final ProjectStatusDto one = new ProjectStatusDto();
		one.setCompletionDate(new Date());
		one.setStatus(Status.PASS);
		one.setTagName("rc3");
		one.setBuildNumber(88);
		one.setRevision(new RevisionTokenDto(0l, "first"));
		
		final ProjectStatusDto two = new ProjectStatusDto();
		two.setCompletionDate(new Date());
		two.setStatus(Status.FAIL);
		two.setBuildNumber(191);
		two.setRevision(new RevisionTokenDto(0l, "second"));
		
		projectStatus.setDependencyIds(ids);
		
		expect(bm.getStatus(ids.get("one"))).andReturn(one);
		expect(bm.getStatus(ids.get("two"))).andReturn(two);
		expect(bm.getStatus(ids.get("three"))).andReturn(null);
		
		replay();

		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		final Element deps = root.getChild("dependencies");
		assertNotNull(deps);
		
		assertEquals(3, deps.getContentSize());
		
		final List<Element> children = new ArrayList<Element>(deps.getChildren());
		Collections.sort(children, new Comparator<Element>() {
			public int compare(Element o1, Element o2) {
				final String t1 = o1.getAttributeValue("name");
				final String t2 = o2.getAttributeValue("name");
				
				return t1.compareTo(t2);
			}
		});
		
		assertEquals("dependency", children.get(0).getName());
		assertEquals("one", children.get(0).getAttributeValue("name"));
		assertEquals("88", children.get(0).getAttributeValue("build-number"));
		assertEquals("first", children.get(0).getAttributeValue("revision"));
		assertEquals("PASS", children.get(0).getAttributeValue("status"));
		assertEquals("rc3", children.get(0).getAttributeValue("repository-tag-name"));
				
		assertEquals("dependency", children.get(1).getName());
		assertEquals("three", children.get(1).getAttributeValue("name"));
		assertEquals(null, children.get(1).getAttributeValue("revision"));
		
		assertEquals("dependency", children.get(2).getName());
		assertEquals("two", children.get(2).getAttributeValue("name"));
		assertEquals("191", children.get(2).getAttributeValue("build-number"));
		assertEquals("second", children.get(2).getAttributeValue("revision"));
		assertEquals("FAIL", children.get(2).getAttributeValue("status"));
		assertEquals(null, children.get(2).getAttributeValue("repository-tag-name"));
		
		verify();
	}
	public void testEmptyChangeLog() throws Exception {
		replay();

		final ChangeLogDto changeLog = new ChangeLogDto();
		
		changeLog.setChangeSets(null);
		
		projectStatus.setChangeLog(changeLog);
		
		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		assertNull(root.getChild("diff"));
		assertNull(root.getChild("changeSets"));
		verify();
	}
	public void testChangeSets() throws Exception {
		expect(pm.getProjectConfig("a name")).andReturn(new ProjectConfigDto());
		
		replay();

		final ChangeLogDto changeLog = new ChangeLogDto();
		final ChangeSetDto changeSet = new ChangeSetDto();
		
		changeSet.setAuthor("jane");
		changeSet.setRevisionLabel(projectStatus.getRevision().getLabel());
		changeSet.setTimestamp(new Date(date.getTime() + 1000000));
		changeSet.setMessage("fixed every bug ever in the entire project");
		changeSet.setModifiedPaths(new String[] {"/a/file", "/other/stuff"});
		
		changeLog.setChangeSets(Collections.singletonList(changeSet));
		
		projectStatus.setChangeLog(changeLog);
		
		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		final Element logs = root.getChild("change-sets");
		
		assertNotNull(logs);
		
		assertEquals(1, logs.getContentSize());
		
		final Element child = logs.getChild("change-set");
		assertEquals("fake revision", child.getAttributeValue("revision"));
		assertEquals("jane", child.getAttributeValue("author"));
		assertContainsChildWithText(child, "timestamp", "02:12");
		assertContainsChildWithText(child, "message", "fixed every bug ever in the entire project");
		
		final Element modifiedPaths = child.getChild("modified-paths");
		assertNotNull(modifiedPaths);
		assertEquals(2, modifiedPaths.getContentSize());
		assertEquals("/a/file", ((Element) modifiedPaths.getChildren().get(0)).getText());
		assertEquals("/other/stuff", ((Element) modifiedPaths.getChildren().get(1)).getText());
		verify();
	}
	public void testChangeSetNullData() throws Exception {
		replay();

		final ChangeLogDto changeLog = new ChangeLogDto();
		final ChangeSetDto changeSet = new ChangeSetDto();
		
		changeSet.setAuthor(null);
		changeSet.setRevisionLabel(null);
		changeSet.setTimestamp(null);
		changeSet.setMessage(null);
		changeSet.setModifiedPaths(null);
		
		changeLog.setChangeSets(Collections.singletonList(changeSet));
		
		projectStatus.setChangeLog(changeLog);
		
		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		final Element logs = root.getChild("change-sets");
		
		assertNotNull(logs);
		
		assertEquals(1, logs.getContentSize());
		
		final Element child = logs.getChild("change-set");

		assertNotNull(child);
		
		verify();
	}
	public void testMetrics() throws Exception {
		messages.put("a.b", "a metric");
		replay();

		final MetricDto m = new MetricDto();
		m.setMessageKey("a.b");
		m.setValue("0.23");
		m.setType(MetricType.PERCENT);
		
		projectStatus.setMetrics(Collections.singletonList(m));
		
		final Document doc = doCall();

		verify();

		final Element root = doc.getRootElement();
		
		final Element metrics = root.getChild("metrics");
		
		assertNotNull(metrics);
		assertEquals(1, metrics.getContentSize());
		assertEquals("a metric", metrics.getChild("metric").getAttributeValue("label"));
		assertEquals("0.23", metrics.getChild("metric").getAttributeValue("value"));	
		assertEquals("percent", metrics.getChild("metric").getAttributeValue("type"));
	}
	public void testTestFailures() throws Exception {
		replay();

		final TestFailureDto f = new TestFailureDto();
		f.setName("a.b.c.testBroken");
		f.setBuildNumber(95843);
		
		projectStatus.setTestFailures(Arrays.asList(f));
		
		final Document doc = doCall();

		verify();

		final Element root = doc.getRootElement();
		
		final Element failures = root.getChild("test-failures");
		
		assertNotNull(failures);
		assertEquals(1, failures.getContentSize());
		assertEquals("testBroken", failures.getChild("test-failure").getAttributeValue("name"));
		assertEquals("a.b.c", failures.getChild("test-failure").getAttributeValue("namespace"));
		assertEquals("95843", failures.getChild("test-failure").getAttributeValue("first-build"));
	}
	public void testInvalidBugtraqRegex() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setBugtraqLogRegex1("(bug");
		
		expect(pm.getProjectConfig("a name")).andReturn(config);
		
		eh.reportEvent((Event) notNull());
		
		replay();

		final ChangeLogDto changeLog = new ChangeLogDto();
		final ChangeSetDto changeSet = new ChangeSetDto();
		
		changeSet.setAuthor("jane");
		changeSet.setRevisionLabel(projectStatus.getRevision().getLabel());
		changeSet.setTimestamp(new Date(date.getTime() + 1000000));
		changeSet.setMessage("fixed every bug including bug 4352, issue 12, bug #54, bug: 51 and bug:#5443.  And stuff.");
		changeSet.setModifiedPaths(new String[] {"/a/file", "/other/stuff"});
		
		changeLog.setChangeSets(Collections.singletonList(changeSet));
		
		projectStatus.setChangeLog(changeLog);
		
		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		final Element logs = root.getChild("change-sets");
		
		assertNotNull(logs);
		
		assertEquals(1, logs.getContentSize());
		
		final Element child = logs.getChild("change-set");
		final Element message = child.getChild("message");

		assertEquals(changeSet.getMessage(), message.getText());
		verify();
	}
	public void testLinkifyBugs() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setBugtraqLogRegex1("(bug|issue):?#? ?#?(\\d+)");
		
		expect(pm.getProjectConfig("a name")).andReturn(config);
		
		replay();

		final ChangeLogDto changeLog = new ChangeLogDto();
		final ChangeSetDto changeSet = new ChangeSetDto();
		
		changeSet.setAuthor("jane");
		changeSet.setRevisionLabel(projectStatus.getRevision().getLabel());
		changeSet.setTimestamp(new Date(date.getTime() + 1000000));
		changeSet.setMessage("fixed every bug including bug 4352, issue 12, bug #54, bug: 51 and bug:#5443.  And stuff.");
		changeSet.setModifiedPaths(new String[] {"/a/file", "/other/stuff"});
		
		changeLog.setChangeSets(Collections.singletonList(changeSet));
		
		projectStatus.setChangeLog(changeLog);
		
		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		final Element logs = root.getChild("change-sets");
		
		assertNotNull(logs);
		
		assertEquals(1, logs.getContentSize());
		
		final Element child = logs.getChild("change-set");
		final Element message = child.getChild("message");

		final OutputStream os = new ByteArrayOutputStream();
		final Format format = Format.getCompactFormat();
		
		format.setTextMode(TextMode.PRESERVE);
		
		XMLOutputter out = new XMLOutputter(format);
		
		out.output(message, os);

		assertEquals("<message>fixed every bug including <issue issue-id=\"4352\">bug 4352</issue>, " +
				"<issue issue-id=\"12\">issue 12</issue>, <issue issue-id=\"54\">bug #54</issue>, " +
				"<issue issue-id=\"51\">bug: 51</issue> and <issue issue-id=\"5443\">bug:#5443</issue>.  " +
				"And stuff.</message>", os.toString());
		
		verify();
	}
	public void testLinkifyBugsAndUrls() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setBugtraqLogRegex1("([Bb]ug|issue):?#? ?#?(\\d+)");
		
		expect(pm.getProjectConfig("a name")).andReturn(config);

		replay();

		final ChangeLogDto changeLog = new ChangeLogDto();
		final ChangeSetDto changeSet = new ChangeSetDto();
		
		changeSet.setAuthor("jane");
		changeSet.setRevisionLabel(projectStatus.getRevision().getLabel());
		changeSet.setTimestamp(new Date(date.getTime() + 1000000));
		changeSet.setMessage("Used new feature found at http://www.example.com Bug# 27172\nWorks great.");
		changeSet.setModifiedPaths(new String[] {"/a/file", "/other/stuff"});
		
		changeLog.setChangeSets(Collections.singletonList(changeSet));
		
		projectStatus.setChangeLog(changeLog);
		
		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		final Element logs = root.getChild("change-sets");
		
		assertNotNull(logs);
		
		assertEquals(1, logs.getContentSize());
		
		final Element child = logs.getChild("change-set");
		final Element message = child.getChild("message");

		final OutputStream os = new ByteArrayOutputStream();
		final Format format = Format.getCompactFormat();
		
		format.setTextMode(TextMode.PRESERVE);
		
		XMLOutputter out = new XMLOutputter(format);
		
		out.output(message, os);

		assertEquals("<message>Used new feature found at <link>http://www.example.com</link>" +
				" <issue issue-id=\"27172\">Bug# 27172</issue>\nWorks great.</message>",
				os.toString().replaceAll("\r", ""));
		
		verify();
	}
	public void testLinkifyContinuesOnMissingProjectConfig() throws Exception {
		expect(pm.getProjectConfig("a name")).andThrow(new NoSuchProjectException("a name"));

		replay();

		final ChangeLogDto changeLog = new ChangeLogDto();
		final ChangeSetDto changeSet = new ChangeSetDto();
		
		changeSet.setAuthor("jane");
		changeSet.setRevisionLabel(projectStatus.getRevision().getLabel());
		changeSet.setTimestamp(new Date(date.getTime() + 1000000));
		changeSet.setMessage("Used new feature found at http://www.example.com.");
		changeSet.setModifiedPaths(new String[] {"/a/file", "/other/stuff"});
		
		changeLog.setChangeSets(Collections.singletonList(changeSet));
		
		projectStatus.setChangeLog(changeLog);
		
		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		final Element logs = root.getChild("change-sets");
		
		assertNotNull(logs);
		
		assertEquals(1, logs.getContentSize());
		
		final Element child = logs.getChild("change-set");
		final Element message = child.getChild("message");

		final OutputStream os = new ByteArrayOutputStream();
		final Format format = Format.getCompactFormat();
		
		format.setTextMode(TextMode.PRESERVE);
		
		XMLOutputter out = new XMLOutputter(format);
		
		out.output(message, os);

		assertEquals("<message>Used new feature found at <link>http://www.example.com</link>.</message>",
				os.toString().replaceAll("\r", ""));
		
		verify();
	}
	public void testLinkifyUrlsTrims() throws Exception {
		ProjectConfigDto config = new ProjectConfigDto();
		config.setBugtraqLogRegex1("([Bb]ug|issue):?#? ?#?(\\d+)");
		
		expect(pm.getProjectConfig("a name")).andReturn(config);

		replay();

		final ChangeLogDto changeLog = new ChangeLogDto();
		final ChangeSetDto changeSet = new ChangeSetDto();
		
		changeSet.setAuthor("jane");
		changeSet.setRevisionLabel(projectStatus.getRevision().getLabel());
		changeSet.setTimestamp(new Date(date.getTime() + 1000000));
		changeSet.setMessage("Used new feature (see http://www.example.com)." +
				"  Also https://www.example.com...");
		changeSet.setModifiedPaths(new String[] {"/a/file", "/other/stuff"});
		
		changeLog.setChangeSets(Collections.singletonList(changeSet));
		
		projectStatus.setChangeLog(changeLog);

		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		final Element logs = root.getChild("change-sets");
		
		assertNotNull(logs);
		
		assertEquals(1, logs.getContentSize());
		
		final Element child = logs.getChild("change-set");
		final Element message = child.getChild("message");

		final OutputStream os = new ByteArrayOutputStream();
		final Format format = Format.getCompactFormat();
		
		format.setTextMode(TextMode.PRESERVE);
		
		XMLOutputter out = new XMLOutputter(format);
		
		out.output(message, os);

		assertEquals("<message>Used new feature (see <link>http://www.example.com</link>).  Also <link>https://www.example.com</link>...</message>",
				os.toString().replaceAll("\r", ""));
		
		verify();
	}
	public void testWarnings() throws Exception {
		replay();

		final BuildMessageDto message = new BuildMessageDto("Do not do this.", "theFile", 12, "J42");
		
		projectStatus.getWarnings().add(message);
				
		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		final Element child = root.getChild("warnings");
		assertNotNull(child);
		
		final Element warning = child.getChild("warning");
		
		assertEquals(message.getMessage(), warning.getText());
		assertEquals(message.getLineNumber().toString(), warning.getAttributeValue("line-number"));
		assertEquals(message.getFile(), warning.getAttributeValue("file"));
		
		verify();
	}
	public void testBuildErrors() throws Exception {
		replay();

		final BuildMessageDto message = new BuildMessageDto("Do not do this.", null, null, null);
		
		projectStatus.getErrors().add(message);
				
		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		final Element child = root.getChild("errors");
		assertNotNull(child);
		
		final Element warning = child.getChild("error");
		
		assertEquals(message.getMessage(), warning.getText());
		assertEquals(null, warning.getAttributeValue("line-number"));
		assertEquals(null, warning.getAttributeValue("file"));
		assertEquals(null, warning.getAttributeValue("code"));
		
		verify();
	}
	public void testShowsLastGoodBuildWhenStatusNotPass() throws Exception {
		replay();

		projectStatus.setStatus(Status.ERROR);
		projectStatus.setLastGoodBuildNumber(42);
		projectStatus.setLastKnownRevision(projectStatus.getRevision());
		projectStatus.setRevision(null);
		
		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		assertContainsChildWithText(root, "last-good-build-number", "42");
		assertContainsChildWithText(root, "last-known-revision", "fake revision");
		verify();
	}
	public void testSkipsLastGoodBuildWhenStatusNotPass() throws Exception {
		replay();

		projectStatus.setStatus(Status.ERROR);
		
		final Document doc = doCall();

		final Element root = doc.getRootElement();
		
		assertNull(root.getChild("last-good-build-revision"));
		assertNull(root.getChild("last-good-build-date"));
		verify();
	}
	public void testTransform() throws Exception {
		replay();
		
		messages.put("key.msg", "fake\nlog\r");
		
		builder.setTransformMessageKeys(Collections.singletonMap("paramName", "key.msg"));
		
		final Document doc = new Document();
		final Result result = new StreamResult(new StringWriter());

		builder.transform(doc, null, null, "text", result);
		
		assertTrue(transResult instanceof StreamResult);
		assertTrue(transSource instanceof JDOMSource);
		verify();
	}
	public void testCreateSummaries() throws Exception {
		replay();
		
		Document doc = builder.createProjectSummaries(Collections.singletonList(projectStatus),
				new Integer(1), new Integer(33), null);
		
		final Element root = doc.getRootElement();
		assertEquals("build-history", root.getName());
		
		assertEquals(1, root.getChildren("project").size());
		assertEquals("1", root.getAttributeValue("from"));
		assertEquals("33", root.getAttributeValue("to"));
	}
	public void testCreateSummariesIncludesXAxisLabels() throws Exception {
		replay();
		
		Document doc = builder.createProjectSummaries(Collections.singletonList(projectStatus),
				new Integer(1), new Integer(33), null);
		
		final Element root = doc.getRootElement();
		assertEquals("build-history", root.getName());
		
		final Element axis = root.getChild("x-axis");
		assertNotNull("Should contain element 'axis'", axis);
	}
	public void testCreateSummariesByDate() throws Exception {
		replay();
		
		Document doc = builder.createProjectSummaries(Collections.singletonList(projectStatus),
				new Date(2431242342341234L), new Date(23123123L), null);
		
		final Element root = doc.getRootElement();
		assertEquals("build-history", root.getName());
		
		assertEquals(1, root.getChildren("project").size());
		assertEquals("18:05", root.getAttributeValue("from"));
		assertEquals("01:25", root.getAttributeValue("to"));
	}
	private Element assertContainsChildWithText(final Element parent, String childNodeName, String text) {
		final Element child = parent.getChild(childNodeName);
		assertNotNull("No child named " + childNodeName, child);
		assertEquals(text, child.getText());
		return child;
	}
	private Document doCall() {
		return builder.createProjectDocument(projectStatus, null);
	}
}
