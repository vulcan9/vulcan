/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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
import java.io.IOException;
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
import net.sourceforge.vulcan.dto.MetricDto.MetricType;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.BuildStartingEvent;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.integration.BuildManagerObserverPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.MetricsPlugin;
import net.sourceforge.vulcan.metrics.dom.DomBuilder;
import net.sourceforge.vulcan.metrics.dto.GlobalConfigDto;
import net.sourceforge.vulcan.metrics.scanner.FileScanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;

@MetricsPlugin
public class XmlMetricsPlugin implements BuildManagerObserverPlugin, ConfigurablePlugin, ApplicationContextAware {
	private static Log LOG = LogFactory.getLog(XmlMetricsPlugin.class);
	
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.metrics";
	public static final String PLUGIN_NAME = "XML Metrics";

	EventHandler eventHandler;
	FileScanner fileScanner;
	String transformSourcePath;
	TransformerFactory transformerFactory;
	BuildOutcomeCache buildOutcomeCache;
	
	Map<String, Transformer> transformers = new HashMap<String, Transformer>();
	GlobalConfigDto globalConfig = new GlobalConfigDto();
	
	long newestTransformDate = 0;
	
	private ResourcePatternResolver resourceResolver;
	
	public void init() throws IOException {
		loadTransformers();
	}

	public void onBuildStarting(BuildStartingEvent event) {
	}
	
	public void onBuildCompleted(BuildCompletedEvent event) {
		try {
			refreshTransformers();
		} catch (IOException e) {
			eventHandler.reportEvent(new ErrorEvent(this, "metrics.errors.refresh",
				new Object[] {e.getMessage()}, e));
		}
		
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
			if (LOG.isDebugEnabled()) {
				LOG.debug("No XML files found for processing.");
			}
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
	public void setTransformSourcePath(String transformSourcePath) {
		this.transformSourcePath = transformSourcePath;
	}
	public void setTransformerFactory(TransformerFactory transformerFactory) {
		this.transformerFactory = transformerFactory;
	}
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.resourceResolver = applicationContext;
	}
	public void setResourceResolver(ResourcePatternResolver resourceResolver) {
		this.resourceResolver = resourceResolver;
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
		for (Map.Entry<String,Transformer> key : transformers.entrySet()) {
			final JDOMResult result = new JDOMResult();
			
			final Transformer transformer = key.getValue();
			
			synchronized(transformer) {
				try {
					transformer.transform(source, result);
				} catch (TransformerException e) {
					eventHandler.reportEvent(new ErrorEvent(this, "metrics.errors.transform",
							new Object[] {e.getMessage()}, e));
				}
			}
			
			boolean hasContent = false;
			
			if (result.getDocument().hasRootElement()) {
				final Element root = result.getDocument().getRootElement();
				
				if (root.getContentSize() > 0) {
					hasContent = true;
					metricsRoot.addContent(root.cloneContent());
				}
			}
			
			if (!hasContent && LOG.isDebugEnabled()) {
				LOG.debug("Transform " + key.getKey() + " did not produce any metrics.");
			}
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

	private void refreshTransformers() throws IOException {
		final Resource[] resources = resourceResolver.getResources(transformSourcePath);
		
		if (transformers.size() != resources.length) {
			loadTransformers();
			return;
		}
		
		for (Resource r : resources) {
			final long lastModified = r.getFile().lastModified();
			if (lastModified > newestTransformDate) {
				loadTransformers();
				return;
			}			
		}
	}
	
	private void loadTransformers() throws IOException {
		LOG.info("Reloading XSL transformers because they have changed.");
		
		transformers.clear();
		for (Resource r : resourceResolver.getResources(transformSourcePath)) {
			try {
				final long lastModified = r.getFile().lastModified();
				if (lastModified > newestTransformDate) {
					newestTransformDate = lastModified;
				}
				
				final SAXSource source = new SAXSource(
						XMLReaderFactory.createXMLReader(),
						new InputSource(r.getInputStream()));

				transformers.put(r.getFilename(), transformerFactory.newTransformer(source));
			} catch (Exception e) {
				eventHandler.reportEvent(new ErrorEvent(this, "metrics.errors.load.transform",
						new Object[] {e.getMessage()}, e));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void digestMetrics(Element root, ProjectStatusDto status) {
		final List<MetricDto> metricList = new ArrayList<MetricDto>();
		final List<Element> metrics = root.getChildren("metric");
		
		for (Element c : metrics) {
			final MetricDto m = new MetricDto();
			final String key = c.getAttributeValue("key");
			m.setMessageKey(key);
			m.setValue(c.getAttributeValue("value"));
			final String typeString = c.getAttributeValue("type");
			if (typeString == null) {
				throw new IllegalStateException("Metric with key='" + key + "' must declare a type.");
			}
			m.setType(MetricType.valueOf(typeString.toUpperCase()));
			metricList.add(m);
		}
		
		if (status.getMetrics() != null) {
			status.getMetrics().addAll(metricList);
		} else {
			status.setMetrics(metricList);
		}
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
		
		for (Element elem : testFailures) {
			final TestFailureDto dto = new TestFailureDto();
			final String name = elem.getText();
			
			dto.setName(name);
			
			if (firstFailures.containsKey(name)) {
				dto.setBuildNumber(firstFailures.get(name));
			} else {
				dto.setBuildNumber(buildNumber);
			}
			
			dto.setMessage(elem.getChildText("message"));
			dto.setDetails(elem.getChildText("details"));
			
			testFailureList.add(dto);
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