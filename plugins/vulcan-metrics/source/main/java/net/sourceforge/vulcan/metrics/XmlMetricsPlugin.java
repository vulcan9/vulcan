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
package net.sourceforge.vulcan.metrics;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;

import net.sourceforge.vulcan.core.support.BuildOutcomeCache;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.integration.BuildManagerObserverPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.MetricsPlugin;
import net.sourceforge.vulcan.metrics.dom.DomBuilder;
import net.sourceforge.vulcan.metrics.dto.GlobalConfigDto;
import net.sourceforge.vulcan.metrics.scanner.FileScanner;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.springframework.core.io.Resource;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;

@MetricsPlugin
public class XmlMetricsPlugin implements BuildManagerObserverPlugin, ConfigurablePlugin {
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.metrics";
	public static final String PLUGIN_NAME = "XML Metrics";

	EventHandler eventHandler;
	FileScanner fileScanner;
	Resource[] transfomSources;
	TransformerFactory transformerFactory;
	BuildOutcomeCache buildOutcomeCache;
	
	List<Transformer> transformers = new ArrayList<Transformer>();
	GlobalConfigDto globalConfig = new GlobalConfigDto();

	public void init() {
		for (Resource r : transfomSources) {
			try {
				final SAXSource source = new SAXSource(
						XMLReaderFactory.createXMLReader(),
						new InputSource(r.getInputStream()));
				
				transformers.add(transformerFactory.newTransformer(source));
			} catch (Exception e) {
				eventHandler.reportEvent(new ErrorEvent(this, "metrics.errors.load.transform",
						new Object[] {e.getMessage()}, e));
			}
		}
	}
	
	public void onBuildCompleted(BuildCompletedEvent event) {
		final ProjectStatusDto status = event.getStatus();
		
		if (status.getStatus() == ProjectStatusDto.Status.ERROR
				|| status.getStatus() == ProjectStatusDto.Status.SKIP) {
			preserveTestFailures(status);
			
			return;
		}
		
		// Step 1 find xml files
		final File rootDir = new File(event.getProjectConfig().getWorkDir());
		
		final String[] matches = fileScanner.scanFiles(rootDir, globalConfig.getIncludes(),
				globalConfig.getExcludes());
		
		if (matches.length == 0) {
			return;
		}
		
		// Step 2 load matches into one big document
		final Document mergedRoot = merge(rootDir, matches);
		
		// Step 3 apply transforms
		final Document metrics = transform(mergedRoot);
		
		// Step 4 digest transformed metrics and add them to ProjectStatus.
		digest(metrics.getRootElement(), status);
		
		// Step 5 if no tests were executed in current build, carry failures forward:
		preserveTestFailures(status);
	}

	public String getId() {
		return PLUGIN_ID;
	}
	public String getName() {
		return PLUGIN_NAME;
	}
	public GlobalConfigDto getConfiguration() {
		return globalConfig;
	}
	public void setConfiguration(PluginConfigDto config) {
		this.globalConfig = (GlobalConfigDto) config;
	}
	public void setBuildOutcomeCache(BuildOutcomeCache buildOutcomeCache) {
		this.buildOutcomeCache = buildOutcomeCache;
	}
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	public void setFileScanner(FileScanner fileScanner) {
		this.fileScanner = fileScanner;
	}
	public void setTransfomSources(Resource[] transfomSources) {
		this.transfomSources = transfomSources;
	}
	public void setTransformerFactory(TransformerFactory transformerFactory) {
		this.transformerFactory = transformerFactory;
	}
	protected Document merge(final File rootDir, final String[] matches) {
		final DomBuilder builder = new DomBuilder();
		
		for (String file : matches) {
			builder.merge(new File(rootDir, file));
		}
		
		final Document mergedRoot = builder.getMergedDocument();
		return mergedRoot;
	}
	protected Document transform(Document mergedRoot) {
		final Element metricsRoot = new Element("metrics");
		final Document metrics = new Document(metricsRoot);
		
		final JDOMSource source = new JDOMSource(mergedRoot);
		for (Transformer transformer : transformers) {
			final JDOMResult result = new JDOMResult();
			
			synchronized(transformer) {
				try {
					transformer.transform(source, result);
				} catch (TransformerException e) {
					eventHandler.reportEvent(new ErrorEvent(this, "metrics.errors.transform",
							new Object[] {e.getMessage()}, e));
				}
			}
			
			final Element root = result.getDocument().getRootElement();
			metricsRoot.addContent(root.cloneContent());
		}
		
		return metrics;
	}
	
	protected void digest(Element root, ProjectStatusDto status) {
		digestMetrics(root, status);

		digestTestFailures(root, status);
	}

	protected void preserveTestFailures(final ProjectStatusDto status) {
		if (testsWereExecuted(status)) {
			return;
		}
		
		final ProjectStatusDto previousStatus = 
			buildOutcomeCache.getLatestOutcome(status.getName());

		if (previousStatus != null && previousStatus.getTestFailures() != null) {
			status.setTestFailures(new ArrayList<TestFailureDto>(previousStatus.getTestFailures()));
		}
	}

	@SuppressWarnings("unchecked")
	private void digestMetrics(Element root, ProjectStatusDto status) {
		final List<MetricDto> metricList = new ArrayList<MetricDto>();
		final List<Element> metrics = root.getChildren("metric");
		
		for (Element c : metrics) {
			final MetricDto m = new MetricDto();
			m.setMessageKey(c.getAttributeValue("key"));
			m.setValue(c.getAttributeValue("value"));
			metricList.add(m);
		}
		
		status.setMetrics(metricList);
	}
	
	@SuppressWarnings("unchecked")
	private void digestTestFailures(Element root, ProjectStatusDto status) {
		final List<Element> testFailures = root.getChildren("test-failure");
		
		if (testFailures.isEmpty()) {
			return;
		}
		
		final Integer buildNumber = status.getBuildNumber();
		final List<TestFailureDto> testFailureList = new ArrayList<TestFailureDto>();
		final Map<String, Integer> firstFailures = hashExistingTestFailures(status);
		
		for (Element c : testFailures) {
			final TestFailureDto f = new TestFailureDto();
			final String name = c.getText();
			
			f.setName(name);
			
			if (firstFailures.containsKey(name)) {
				f.setBuildNumber(firstFailures.get(name));
			} else {
				f.setBuildNumber(buildNumber);
			}
			
			testFailureList.add(f);
		}
		
		Collections.sort(testFailureList, new Comparator<TestFailureDto>() {
			public int compare(TestFailureDto o1, TestFailureDto o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		status.setTestFailures(testFailureList);
	}

	private Map<String, Integer> hashExistingTestFailures(ProjectStatusDto status) {
		final ProjectStatusDto previousStatus = 
			buildOutcomeCache.getLatestOutcome(status.getName());
		
		if (previousStatus == null) {
			return Collections.emptyMap();
		}
		
		final List<TestFailureDto> previousFailures = previousStatus.getTestFailures();
		
		if (previousFailures == null || previousFailures.isEmpty()) {
			return Collections.emptyMap();
		}
		
		final Map<String, Integer> map = new HashMap<String, Integer>();
		
		for (TestFailureDto failure : previousFailures) {
			map.put(failure.getName(), failure.getBuildNumber());
		}
		
		return map;
	}
	private boolean testsWereExecuted(ProjectStatusDto status) {
		final List<MetricDto> metrics = status.getMetrics();
		
		if (metrics == null || metrics.isEmpty()) {
			return false;
		}
		
		for (MetricDto m : metrics) {
			if ("vulcan.metrics.tests.executed".equals(m.getMessageKey())) {
				return true;
			}
		}
		
		return false;
	}
}