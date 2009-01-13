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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jdom.Element;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import junit.framework.TestCase;
import net.sourceforge.vulcan.core.support.BuildOutcomeCache;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.event.BuildCompletedEvent;

public class XmlMetricsPluginTest extends TestCase implements ResourcePatternResolver {
	XmlMetricsPlugin plugin = new XmlMetricsPlugin();
	
	BuildOutcomeCache cache = new BuildOutcomeCache() {
		@Override
		public ProjectStatusDto getLatestOutcome(String projectName) {
			assertEquals(previousStatus.getName(), projectName);
			return previousStatus;
		}
	};
	
	ProjectConfigDto projectConfig = new ProjectConfigDto();
	ProjectStatusDto currentStatus = new ProjectStatusDto();
	ProjectStatusDto previousStatus = new ProjectStatusDto();
	
	List<TestFailureDto> previousFailures = new ArrayList<TestFailureDto>();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		plugin.setBuildOutcomeCache(cache);
		plugin.setResourceResolver(this);
		previousStatus.setName("myProject");
		previousStatus.setTestFailures(previousFailures);
		
		previousFailures.add(new TestFailureDto());
		
		projectConfig.setName("myProject");
		currentStatus.setName("myProject");
	}
	
	public void testError() throws Exception {
		previousStatus.setTestFailures(null);
		currentStatus.setStatus(Status.ERROR);
		
		plugin.onBuildCompleted(new BuildCompletedEvent(this, null, projectConfig, currentStatus));
		
		assertEquals(null, currentStatus.getTestFailures());
	}
	public void testCarriesOldTestFailuresForwardOnError() throws Exception {
		currentStatus.setStatus(Status.ERROR);
		
		plugin.onBuildCompleted(new BuildCompletedEvent(this, null, projectConfig, currentStatus));
		
		assertEquals(previousStatus.getTestFailures(), currentStatus.getTestFailures());
	}
	public void testCarriesOldTestFailuresForwardOnSkip() throws Exception {
		currentStatus.setStatus(Status.SKIP);
		
		plugin.onBuildCompleted(new BuildCompletedEvent(this, null, projectConfig, currentStatus));
		
		assertEquals(previousStatus.getTestFailures(), currentStatus.getTestFailures());
	}
	public void testCarriesOldTestFailuresForwardOnFail() throws Exception {
		currentStatus.setStatus(Status.FAIL);
		
		plugin.preserveTestFailures(currentStatus);
		
		assertEquals(previousStatus.getTestFailures(), currentStatus.getTestFailures());
	}
	public void testDoesNotCarryOldTestFailuresForwardOnFailWhenTotalTestsExecutedZero() throws Exception {
		final MetricDto metricDto = new MetricDto();
		metricDto.setMessageKey("vulcan.metrics.tests.executed");
		metricDto.setValue("1");
		
		currentStatus.setStatus(Status.FAIL);
		currentStatus.setMetrics(Collections.singletonList(metricDto));
		plugin.preserveTestFailures(currentStatus);
		
		assertEquals(null, currentStatus.getTestFailures());
	}
	public void testDoesNotCarryOldTestFailuresForwardOnFailParseError() throws Exception {
		final MetricDto metricDto = new MetricDto();
		metricDto.setMessageKey("vulcan.metrics.tests.executed");
		metricDto.setValue("fluffy");
		
		currentStatus.setStatus(Status.FAIL);
		currentStatus.setMetrics(Collections.singletonList(metricDto));
		plugin.preserveTestFailures(currentStatus);
		
		assertEquals(null, currentStatus.getTestFailures());
	}
	public void testCreatesMetricsListWhenNull() throws Exception {
		currentStatus.setMetrics(null);
		
		plugin.digest(new Element("a"), currentStatus);
		
		assertNotNull("currentStatus.getMetrics()", currentStatus.getMetrics());
	}
	public void testMergesMetricsWhenAlreadyPresent() throws Exception {
		final MetricDto metricDto = new MetricDto();
		
		final List<MetricDto> metrics = new ArrayList<MetricDto>();
		metrics.add(metricDto);
		
		currentStatus.setMetrics(metrics);
		
		plugin.digest(new Element("a"), currentStatus);
		
		assertTrue("Should not have removed existing metric", currentStatus.getMetrics().contains(metricDto));
	}
	public Resource getResource(String location) {
		return null;
	}
	public Resource[] getResources(String locationPattern) throws IOException {
		return new Resource[0];
	}
}
