/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.ProjectDomBuilder;
import net.sourceforge.vulcan.dto.BuildArtifactLocationDto;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ModifiedPathDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.NoSuchProjectException;
import net.sourceforge.vulcan.exception.NoSuchTransformFormatException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;
import org.xml.sax.SAXException;


public abstract class AbstractProjectDomBuilder implements ProjectDomBuilder {
	static TimeZone SYSTEM_TIMEZONE = TimeZone.getDefault();
	static TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("Greenwhich");
	
	private ProjectManager projectManager;
	private BuildManager buildManager;
	private EventHandler eventHandler;
	
	public final Document createProjectDocument(ProjectStatusDto status, Locale locale) {
		if (locale == null) {
			locale = Locale.getDefault();
		}
		final DateFormat format = new SimpleDateFormat(formatMessage("build.timestamp.format", null, locale), locale);
		format.setTimeZone(SYSTEM_TIMEZONE);
		
		final Element root = new Element("project");
		final Document doc = new Document(root);
		
		addBasicContents(root, status, locale, format);
		
		final String repositoryUrl = status.getRepositoryUrl();
		if (!StringUtils.isBlank(repositoryUrl)) {
			addChildNodeWithText(root, "repository-url", repositoryUrl);
		}
		
		if (status.getEstimatedBuildTimeMillis() != null) {
			addChildNodeWithText(root, "estimated-build-time", status.getEstimatedBuildTimeMillis().toString());
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
		final String workDir = status.getWorkDir();
		if (isNotBlank(workDir)) {
			addReports(root, workDir);
		}
		
		return doc;
	}
	
	public Document createProjectSummaries(List<ProjectStatusDto> outcomes, Object fromLabel, Object toLabel, Locale locale) {
		if (locale == null) {
			locale = Locale.getDefault();
		}
		
		final DateFormat format = new SimpleDateFormat(formatMessage("build.timestamp.format", null, locale), locale);
		format.setTimeZone(SYSTEM_TIMEZONE);
		
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
		
		addXAxis(root, outcomes, format, locale);
		
		for (ProjectStatusDto outcome : outcomes) {
			final Element summary = new Element("project");
			
			addBasicContents(summary, outcome, locale, format);
			
			addMetrics(summary, outcome.getMetrics(), locale);
			root.addContent(summary);
		}
		return doc;
	}
	public String transform(Document document, Map<String, ?> transformParameters, Locale locale, String format, Result result) throws SAXException, IOException, TransformerException, NoSuchTransformFormatException {
		final Transformer transformer = createTransformer(format);
		
		if (transformParameters != null) {
			for (String key : transformParameters.keySet()) {
				transformer.setParameter(key, transformParameters.get(key));
			}
		}
		
		transformer.transform(new JDOMSource(document), result);
		
		return transformer.getOutputProperty(OutputKeys.MEDIA_TYPE);
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
	
	protected abstract Transformer createTransformer(String format) throws NoSuchTransformFormatException;
	protected abstract String formatMessage(String key, Object[] args, Locale locale);
	
	protected boolean artifactExists(String workDir, String path) {
		return new File(workDir, path).exists();
	}
	
	protected String readLog(File log) throws IOException {
		final InputStream is = new FileInputStream(log);
		try {
			return IOUtils.toString(is);
		} finally {
			is.close();
		}
	}
	
	AxisLabelGenerator createAxisLabelGenerator(long min, long max, Locale locale) {
		return new AxisLabelGenerator(min, max, locale);
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
				mEl.setAttribute("type", m.getType().name().toLowerCase());
				mEl.setAttribute("key", m.getMessageKey());
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
				
				final String testName = failure.getName();
				final int lastDot = testName.lastIndexOf('.');
				if (lastDot > 0) {
					failureElement.setAttribute("name", testName.substring(lastDot + 1));
					failureElement.setAttribute("namespace", testName.substring(0, lastDot));
				} else {
					failureElement.setAttribute("name", testName);
				}
				
				failureElement.setAttribute("first-build", failure.getBuildNumber().toString());
				String message = failure.getMessage();
				if (message == null) {
					message = StringUtils.EMPTY;
				}
				failureElement.setAttribute("message", message);
				
				String details = failure.getDetails();
				if (details == null) {
					details = StringUtils.EMPTY;
				}
				
				failureElement.setText(details);
				
				testFailuresRoot.addContent(failureElement);
			}
			
			root.addContent(testFailuresRoot);
		}
	}
	
	private void addReports(final Element root, final String workDir) {
		final List<BuildArtifactLocationDto> specs = projectManager.getArtifactLocations();
		
		final Element reportsNode = new Element("reports");
		
		for (BuildArtifactLocationDto spec : specs) {
			if (artifactExists(workDir, spec.getPath())) {
				addArtifact(reportsNode, spec);
			}
		}
		
		if (reportsNode.getContentSize() > 0) {
			root.addContent(reportsNode);
		}
	}
	
	private void addArtifact(final Element reportsNode, BuildArtifactLocationDto spec) {
		final Element artifact = new Element("report");
		
		addChildNodeWithText(artifact, "name", spec.getName());
		addChildNodeWithText(artifact, "description", spec.getDescription());
		addChildNodeWithText(artifact, "path", spec.getPath());
		
		reportsNode.addContent(artifact);
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
			
			if (changes.getAuthorName() != null) {
				changeSet.setAttribute("author", changes.getAuthorName());
			}
			
			if (changes.getAuthorEmail() != null) {
				changeSet.setAttribute("author-email", changes.getAuthorEmail());
			}
			
			if (changes.getRevisionLabel() != null) {
				changeSet.setAttribute("revision", changes.getRevisionLabel().toString());
			}
			
			if (changes.getTimestamp() != null) {
				final Element timestampNode = addChildNodeWithText(changeSet, "timestamp", dateFormat.format(changes.getTimestamp()));
				timestampNode.setAttribute("millis", Long.toString(changes.getTimestamp().getTime()));
			}
			
			if (changes.getMessage() != null) {
				linkifyCommitMessage(changeSet, changes, status.getName());
			}
			
			if (changes.getModifiedPaths() != null) {
				addModifiedPaths(changeSet, changes.getModifiedPaths());
			}
			
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
	
	private static void addModifiedPaths(Element changeSet, Iterable<ModifiedPathDto> modifiedPaths) {
		final Element mps = new Element("modified-paths");

		for (ModifiedPathDto path : modifiedPaths) {
			final Element pathElem = new Element("path");
			pathElem.setText(path.getPath());
			if (path.getAction() != null) {
				pathElem.setAttribute("action", path.getAction().name());
			}
			mps.addContent(pathElem);
		}
		changeSet.addContent(mps);
	}
	private void addBasicContents(final Element root, ProjectStatusDto status, Locale locale, final DateFormat format) {
		addChildNodeWithText(root, "name", status.getName());
		
		final Integer buildNumber = status.getBuildNumber();
		if (buildNumber != null) {
			addChildNodeWithText(root, "build-number", buildNumber.toString());
		}
		
		addChildNodeWithText(root, "update-type", status.getUpdateType().name());
		
		final Status buildStatus = status.getStatus();
		if (buildStatus != null) {
			addChildNodeWithText(root, "status", buildStatus.name());
		}
		
		addElapsedTime(root, status, locale);
		
		final Date completionDate = status.getCompletionDate();
		addTimestampNode(root, "timestamp", completionDate, format, locale);
		
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
		
		final String workDir = status.getWorkDir();
		if (isNotBlank(workDir)) {
			final Element workDirNode = addChildNodeWithText(root, "work-directory", workDir);
			workDirNode.setAttribute("available", Boolean.toString(new File(workDir).isDirectory()));
		}
		
		final String requestedBy = status.getRequestedBy();
		if (isNotBlank(requestedBy)) {
			if (status.isScheduledBuild()) {
				addChildNodeWithText(root, "build-scheduled-by", requestedBy);
			} else {
				addChildNodeWithText(root, "build-requested-by", requestedBy);
			}
		}
		
		final String buildReasonKey = status.getBuildReasonKey();
		if (isNotBlank(buildReasonKey)) {
			addChildNodeWithText(root, "build-reason",
					formatMessage(buildReasonKey, status.getBuildReasonArgs(), locale));
		}
		
		if (isNotBlank(status.getBrokenBy())) {
			addChildNodeWithText(root, "broken-by", status.getBrokenBy());
			addTimestampNode(root, "claim-date", status.getClaimDate(), format, locale);
		}
		if (status.getBuildLogId() != null) {
			addChildNodeWithText(root, "build-log-available", null);
		}

		if (status.getDiffId() != null) {
			addChildNodeWithText(root, "diff-available", null);
		}
	}

	private void addTimestampNode(final Element parent, String nodeName, final Date date, final DateFormat format, Locale locale) {
		if (date != null) {
			final DateFormat textualDateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, locale);
			
			final Element timestampNode = addChildNodeWithText(parent, nodeName, format.format(date));
			timestampNode.setAttribute("millis", Long.toString(date.getTime()));
			timestampNode.setAttribute("text", textualDateFormat.format(date));
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
		
		final DateFormat format = new SimpleDateFormat(formatMessage("build.time.elapsed.format", null, locale), locale);
		format.setTimeZone(GMT_TIMEZONE);
		
		elapsed.setText(format.format(elapsedMillis));
		
		root.addContent(elapsed);
	}
	private void addXAxis(Element root, List<ProjectStatusDto> outcomes, DateFormat format, Locale locale) {
		final Element xAxis = new Element("x-axis");
		root.addContent(xAxis);
		
		long minSample = outcomes.get(0).getCompletionDate().getTime();
		long maxSample = outcomes.get(outcomes.size()-1).getCompletionDate().getTime();
		
		final AxisLabelGenerator axisLabelGenerator = new AxisLabelGenerator(minSample, maxSample, locale);
		
		final Element minNode = addChildNodeWithText(xAxis, "minimum", format.format(new Date(axisLabelGenerator.getMin())));
		minNode.setAttribute("millis", Long.toString(axisLabelGenerator.getMin()));
		final Element maxNode = addChildNodeWithText(xAxis, "maximum", format.format(new Date(axisLabelGenerator.getMax())));
		maxNode.setAttribute("millis", Long.toString(axisLabelGenerator.getMax()));
		
		final Element labelsNode = new Element("labels");
		xAxis.addContent(labelsNode);
		
		for (AxisLabel label : axisLabelGenerator.getLabels()) {
			final Element labelNode = new Element("label");
			labelsNode.addContent(labelNode);
			labelNode.setAttribute("millis", Long.toString(label.getScalar()));
			labelNode.setText(label.getLabel());
		}
	}

	static class AxisLabel {
		private final long scalar;
		private final String label;

		AxisLabel(long scalar, String label) {
			this.scalar = scalar;
			this.label = label;
		}

		public long getScalar() {
			return scalar;
		}

		public String getLabel() {
			return label;
		}
	}
	
	class AxisLabelGenerator {
		private final List<AxisLabel> labels;
		private final long minSample;
		private final long maxSample;
		private final Locale locale;

		private long min;
		private long max;
		
		AxisLabelGenerator(long minSample, long maxSample, Locale locale) {
			this.minSample = minSample;
			this.maxSample = maxSample;
			
			if (locale != null) {
				this.locale = locale;
			} else {
				this.locale = Locale.getDefault();
			}
			
			this.labels = new ArrayList<AxisLabel>();
			
			generateLabels();
		}

		public long getMin() {
			return min;
		}

		public long getMax() {
			return max;
		}
		
		public List<AxisLabel> getLabels() {
			return labels;
		}

		private void generateLabels() {
			final long delta = maxSample - minSample;
			
			final int roundingField;
			final int incrementField;
			final DateFormat fmt;
			if (delta > DateUtils.MILLIS_PER_DAY * 7 * 6) {
				roundingField = Calendar.MONTH;
				incrementField = Calendar.MONTH;
				
				fmt = getDateFormat("axis.by.month");
			} else if (delta > DateUtils.MILLIS_PER_DAY * 10) {
				roundingField = Calendar.DATE;
				incrementField = Calendar.WEEK_OF_MONTH;
				
				fmt = getDateFormat("axis.by.week");
			} else {
				roundingField = Calendar.DATE;
				incrementField = Calendar.DATE;
				
				fmt = getDateFormat("axis.by.day");
			}
			
			Calendar cal = new GregorianCalendar();
			cal.setTimeInMillis(maxSample);
			
			cal = DateUtils.truncate(cal, roundingField);
			cal.add(roundingField, 1);
			
			max = cal.getTimeInMillis();
			
			cal.setTimeInMillis(minSample);
			
			cal = DateUtils.truncate(cal, roundingField);
			
			min = cal.getTimeInMillis();
			
			while (cal.getTimeInMillis() < max) {
				final long timeInMillis = cal.getTimeInMillis();
				labels.add(new AxisLabel(timeInMillis, fmt.format(new Date(timeInMillis))));
				cal.add(incrementField, 1);
			}
		}

		private DateFormat getDateFormat(String key) {
			final String messageFormat = formatMessage(key, null, locale);
			
			if (messageFormat == null) {
				return DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
			}
			
			return new SimpleDateFormat(messageFormat, locale);
		}
	}
}
