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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.ProjectDomBuilder;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.Date;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.exception.NoSuchTransformFormatException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;
import org.xml.sax.SAXException;


@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class AbstractProjectDomBuilder implements ProjectDomBuilder {
	private ProjectManager projectManager;
	private BuildManager buildManager;
	private EventHandler eventHandler;
	private Map<String, String> transformMessageKeys = Collections.emptyMap();
	
	public final Document createProjectDocument(ProjectStatusDto status, Locale locale) {
		final DateFormat format = new SimpleDateFormat(formatMessage("build.timestamp.format", null, locale));
		
		final Element root = new Element("project");
		final Document doc = new Document(root);
		
		addBasicContents(root, status, locale, format);
		
		final String repositoryUrl = status.getRepositoryUrl();
		if (!StringUtils.isBlank(repositoryUrl)) {
			addChildNodeWithText(root, "repository-url", repositoryUrl);
		}
		
		addDependencies(root, status, format);
		
		addChangeLog(root, status, format);
		
		addBuildMessages(root, "error", status.getErrors());
		addBuildMessages(root, "warning", status.getWarnings());
		
		final Status buildStatus = status.getStatus();
		
		if (buildStatus == null || !Status.PASS.equals(buildStatus)) {
			if (status.getLastGoodBuildNumber() != null) {
				addChildNodeWithText(root, "last-good-build-number", status.getLastGoodBuildNumber().toString());
			}
		}
		final RevisionTokenDto lastKnownRevision = status.getLastKnownRevision();
		if (status.getRevision() == null && lastKnownRevision != null) {
			final Element rev = addChildNodeWithText(root, "last-known-revision",
					lastKnownRevision.getLabel());
			rev.setAttribute("numeric", lastKnownRevision.getRevision().toString());
		}

		addMetrics(root, status.getMetrics(), locale);
		addTestFailures(root, status.getTestFailures());
		
		return doc;
	}
	
	public Document createProjectSummaries(List<ProjectStatusDto> outcomes, Object fromLabel, Object toLabel, Locale locale) {
		final DateFormat format = new SimpleDateFormat(formatMessage("build.timestamp.format", null, locale));
		final Element root = new Element("build-history");
		final Document doc = new Document(root);

		if (fromLabel instanceof java.util.Date) {
			fromLabel = format.format(fromLabel);
		}
		if (toLabel instanceof java.util.Date) {
			toLabel = format.format(toLabel);
		}
		
		root.setAttribute("from", fromLabel.toString());
		root.setAttribute("to", toLabel.toString());
		
		for (ProjectStatusDto outcome : outcomes) {
			final Element summary = new Element("project");
			
			addBasicContents(summary, outcome, locale, format);
			
			root.addContent(summary);
		}
		return doc;
	}
	public void transform(Document document, URL projectSiteURL, URL viewProjectStatusURL, URL issueTrackerURL, int index, Locale locale, String format, Result result) throws SAXException, IOException, TransformerException, NoSuchTransformFormatException {
		final Transformer transformer = createTransformer(format);
		
		applyParameters(transformer, locale);
		if (projectSiteURL != null) {
			transformer.setParameter("projectSiteURL", projectSiteURL);
		}
		if (viewProjectStatusURL != null) {
			transformer.setParameter("viewProjectStatusURL", viewProjectStatusURL);	
		}
		if (issueTrackerURL != null) {
			transformer.setParameter("issueTrackerURL", issueTrackerURL);	
		}
		
		transformer.setParameter("index", new Integer(index));
		transformer.transform(new JDOMSource(document), result);
	}
	public void setProjectManager(ProjectManager projectManager) {
		this.projectManager = projectManager;
	}
	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	public void setTransformMessageKeys(Map<String, String> transformMessageKeys) {
		this.transformMessageKeys = transformMessageKeys;
	}
	
	protected abstract Transformer createTransformer(String format) throws NoSuchTransformFormatException;
	protected abstract String formatMessage(String key, Object[] args, Locale locale);
	
	protected String readLog(File log) throws IOException {
		final InputStream is = new FileInputStream(log);
		try {
			return IOUtils.toString(is);
		} finally {
			is.close();
		}
	}
	private void applyParameters(Transformer transformer, Locale locale) {
		for (Map.Entry<String, String> e: transformMessageKeys.entrySet()) {
			transformer.setParameter(e.getKey(),
					formatMessage(e.getValue(), null, locale));
		}
	}
	private static void addBuildMessages(Element root, String type, List<BuildMessageDto> messages) {
		if (messages != null && !messages.isEmpty()) {
			final Element listElem = new Element(type + "s");
			root.addContent(listElem);
			
			for (BuildMessageDto m : messages) {
				final Element msg = new Element(type);
				
				msg.setText(m.getMessage());
				final Integer lineNumber = m.getLineNumber();
				if (lineNumber != null) {
					msg.setAttribute("line-number", lineNumber.toString());
				}
				final String file = m.getFile();
				if (file != null) {
					msg.setAttribute("file", file);
				}
				final String code = m.getCode();
				if (code != null) {
					msg.setAttribute("code", code);
				}
				listElem.addContent(msg);
			}
		}
	}
	private void addMetrics(Element root, List<MetricDto> metrics, Locale locale) {
		if (metrics != null && !metrics.isEmpty()) {
			final Element mRoot = new Element("metrics");
			
			for (MetricDto m : metrics) {
				final Element mEl = new Element("metric");
				mEl.setAttribute("label", formatMessage(m.getMessageKey(), null, locale));
				mEl.setAttribute("value", m.getValue());
				
				mRoot.addContent(mEl);
			}
			root.addContent(mRoot);
		}
	}
	private void addTestFailures(Element root, List<TestFailureDto> failures) {
		if (failures != null && !failures.isEmpty()) {
			final Element testFailuresRoot = new Element("test-failures");
			
			for (TestFailureDto failure : failures) {
				final Element failureElement = new Element("test-failure");
				failureElement.setAttribute("name", failure.getName());
				failureElement.setAttribute("first-build", failure.getBuildNumber().toString());
				
				testFailuresRoot.addContent(failureElement);
			}
			
			root.addContent(testFailuresRoot);
		}
	}
	private static Element addChildNodeWithText(final Element root, final String name, final String text) {
		final Element child = new Element(name);
		child.setText(text);
		root.addContent(child);
		return child;
	}
	private void addDependencies(Element root, ProjectStatusDto status, DateFormat format) {
		final Map<String, UUID> dependencyIds = status.getDependencyIds();

		if (dependencyIds == null || dependencyIds.isEmpty()) {
			return;
		}
		
		final Element deps = new Element("dependencies");
		root.addContent(deps);
		
		for (Map.Entry<String, UUID> e : dependencyIds.entrySet()) {
			final Element dep = new Element("dependency");

			dep.setAttribute("name", e.getKey());
			
			final ProjectStatusDto depStatus = buildManager.getStatus(e.getValue());
			if (depStatus != null) {
				final RevisionTokenDto rev = depStatus.getRevision();
				final String revString;
				if (rev != null) {
					revString = rev.toString();
				} else {
					revString = "";
				}
				dep.setAttribute("build-number", depStatus.getBuildNumber().toString());
				dep.setAttribute("revision", revString);
				dep.setAttribute("status", depStatus.getStatus().name());
				
				final Date completionDate = depStatus.getCompletionDate();
				final Element timestampNode = addChildNodeWithText(dep, "timestamp", format.format(completionDate));
				timestampNode.setAttribute("millis", Long.toString(completionDate.getTime()));
				
				final String tagName = depStatus.getTagName();
				if (!StringUtils.isBlank(tagName)) {
					dep.setAttribute("repository-tag-name", tagName);
				}
			}
			
			deps.addContent(dep);
		}
	}
	private void addChangeLog(final Element root, ProjectStatusDto status, DateFormat dateFormat) {
		final ChangeLogDto changeLog = status.getChangeLog();
		
		if (changeLog == null) {
			return;
		}
		
		final Element changeSets = new Element("change-sets");
		
		List<ChangeSetDto> changeSetDtos = changeLog.getChangeSets();
		if (changeSetDtos == null) {
			changeSetDtos = Collections.emptyList();
		}
		
		for (ChangeSetDto changes : changeSetDtos) {
			final Element changeSet = new Element("change-set");
			
			changeSet.setAttribute("author", changes.getAuthor());
			changeSet.setAttribute("revision", changes.getRevision().toString());

			final Element timestampNode = addChildNodeWithText(changeSet, "timestamp", dateFormat.format(changes.getTimestamp()));
			timestampNode.setAttribute("millis", Long.toString(changes.getTimestamp().getTime()));

			linkifyCommitMessage(changeSet, changes, status.getName());
			
			addModifiedPaths(changeSet, changes.getModifiedPaths());
			
			changeSets.addContent(changeSet);
		}
		
		if (changeSets.getContentSize() > 0) {
			root.addContent(changeSets);
		}
	}

	private void linkifyCommitMessage(Element changeSet, ChangeSetDto changes, String projectName) {
		final CommitLogParser commitLogParser = new CommitLogParser();

		try {
			final ProjectConfigDto projectConfig = projectManager.getProjectConfig(projectName);
		
			commitLogParser.setKeywordPattern(projectConfig.getBugtraqLogRegex1());
			commitLogParser.setIdPattern(projectConfig.getBugtraqLogRegex2());
		} catch (NoSuchProjectException ignore) {
		}
		
		try {
			commitLogParser.parse(changes.getMessage());
			
			changeSet.addContent(commitLogParser.getMessageNode());
		} catch (PatternSyntaxException e) {
			eventHandler.reportEvent(new ErrorEvent(this, "errors.bugtraq.regex",
					new Object[] {e.getPattern(), e.getDescription(), e.getIndex()}, e));
			
			final Element message = new Element("message");
			message.setText(changes.getMessage());
			changeSet.addContent(message);
		}
	}
	
	private static void addModifiedPaths(Element changeSet, String[] modifiedPaths) {
		final Element mps = new Element("modified-paths");

		for (String path : modifiedPaths) {
			final Element pathElem = new Element("path");
			pathElem.setText(path);
			mps.addContent(pathElem);
		}
		changeSet.addContent(mps);
	}
	private void addBasicContents(final Element root, ProjectStatusDto status, Locale locale, final DateFormat format) {
		addChildNodeWithText(root, "name", status.getName());
		
		addChildNodeWithText(root, "build-number", status.getBuildNumber().toString());
		
		addChildNodeWithText(root, "update-type", status.getUpdateType().name());
		
		final Status buildStatus = status.getStatus();
		if (buildStatus != null) {
			addChildNodeWithText(root, "status", buildStatus.name());
		}
		
		addElapsedTime(root, status, locale);
		
		final Date completionDate = status.getCompletionDate();
		if (completionDate != null) {
			final Element timestampNode = addChildNodeWithText(root, "timestamp", format.format(completionDate));
			timestampNode.setAttribute("millis", Long.toString(completionDate.getTime()));
		}
		
		final String messageKey = status.getMessageKey();
		if (messageKey != null) {
			addChildNodeWithText(root, "message", 
					formatMessage(messageKey,
								status.getMessageArgs(),
								locale));
		}
		
		final RevisionTokenDto revision = status.getRevision();
		if (revision != null) {
			final Element rev = addChildNodeWithText(root, "revision", revision.toString());
			rev.setAttribute("numeric", revision.getRevision().toString());
		}
		
		final String repositoryTagName = status.getTagName();
		if (isNotBlank(repositoryTagName)) {
			addChildNodeWithText(root, "repository-tag-name", repositoryTagName);
		}
		
		final String requestedBy = status.getRequestedBy();
		if (isNotBlank(requestedBy)) {
			addChildNodeWithText(root, "build-requested-by", requestedBy);
		}
		
		final String buildReasonKey = status.getBuildReasonKey();
		if (isNotBlank(buildReasonKey)) {
			addChildNodeWithText(root, "build-reason",
					formatMessage(buildReasonKey, status.getBuildReasonArgs(), locale));
		}
	}
	private void addElapsedTime(Element root, ProjectStatusDto status, Locale locale) {
		final Date startDate = status.getStartDate();
		
		if (startDate == null) {
			return;
		}
		
		Date completionDate = status.getCompletionDate();
		
		if (completionDate == null) {
			completionDate = new Date();
		}
		
		final long elapsedMillis = completionDate.getTime() - startDate.getTime();
		
		final Element elapsed = new Element("elapsed-time");
		
		elapsed.setAttribute("millis", Long.toString(elapsedMillis));
		
		final DateFormat format = new SimpleDateFormat(formatMessage("build.time.elapsed.format", null, locale));
		format.setTimeZone(TimeZone.getTimeZone("Greenwich"));
		
		elapsed.setText(format.format(elapsedMillis));
		
		root.addContent(elapsed);
	}
}
