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

import static net.sourceforge.vulcan.dto.ProjectStatusDto.Status.ERROR;
import static net.sourceforge.vulcan.dto.ProjectStatusDto.Status.FAIL;
import static net.sourceforge.vulcan.dto.ProjectStatusDto.Status.PASS;
import static net.sourceforge.vulcan.dto.ProjectStatusDto.Status.SKIP;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;

public class ReportHelperTest extends TestCase {
	private List<ProjectStatusDto> samples = new ArrayList<ProjectStatusDto>();
	private int buildNumber;
	
	public void testGetSuccessCount() throws Exception {
		for (int i=0; i<10; i++) {
			addSample(PASS, new Date(0L), "a");
		}
		for (int i=0; i<10; i++) {
			addSample(SKIP, new Date(0L), "a");
		}
		for (int i=0; i<10; i++) {
			addSample(ERROR, new Date(0L), "a");
		}
		for (int i=0; i<10; i++) {
			addSample(FAIL, new Date(0L), "a");
		}
		
		assertEquals(10, new ReportHelper(samples, null).getSuccessCount());
	}

	public void testGetLongestTimeToFixBuildNoFails() throws Exception {
		addSample(PASS, new Date(1000L), "a");
		
		assertEquals(-1, new ReportHelper(samples, null).getLongestTimeToFixBuild());
	}
	
	public void testGetLongestTimeToFixBuildNeverFixed() throws Exception {
		addSample(FAIL, new Date(1000L), "a");
		addSample(FAIL, new Date(1200L), "a");
		
		assertEquals(500L, new ReportHelper(samples, new Date(1500L)).getLongestTimeToFixBuild());
	}
	
	public void testGetLongestTimeToFixBuildNeverFixedNullMax() throws Exception {
		addSample(FAIL, new Date(1000L), "a");
		addSample(FAIL, new Date(3400L), "a");
		
		assertEquals(2400L, new ReportHelper(samples, null).getLongestTimeToFixBuild());
	}
	
	public void testGetLongestTimeToFixBuildSingle() throws Exception {
		addSample(FAIL, new Date(0L), "a");
		addSample(PASS, new Date(1000L), "a");
		
		assertEquals(1000L, new ReportHelper(samples, null).getLongestTimeToFixBuild());
	}
	
	public void testGetLongestTimeToFixBuild() throws Exception {
		addSample(FAIL, new Date(0L), "a");
		addSample(PASS, new Date(1000L), "a");
		addSample(FAIL, new Date(3000L), "a");
		addSample(PASS, new Date(5000L), "a");
		
		assertEquals(2000L, new ReportHelper(samples, null).getLongestTimeToFixBuild());
	}

	public void testGetLongestTimeToFixBuildNumbers() throws Exception {
		addSample(FAIL, new Date(0L), "a");
		addSample(PASS, new Date(1000L), "a");
		addSample(FAIL, new Date(3000L), "a");
		addSample(PASS, new Date(5000L), "a");
		
		final ReportHelper reportHelper = new ReportHelper(samples, null);
		assertEquals(Integer.valueOf(2), reportHelper.getFailingBuildNumber());
		assertEquals(Integer.valueOf(3), reportHelper.getFixedInBuildNumber());
	}

	public void testGetLongestTimeToFixBuildNumbersNotFixed() throws Exception {
		addSample(FAIL, new Date(0L), "a");
		addSample(PASS, new Date(1000L), "a");
		addSample(FAIL, new Date(3000L), "a");
		
		final ReportHelper reportHelper = new ReportHelper(samples, new Date(10000L));
		assertEquals(Integer.valueOf(2), reportHelper.getFailingBuildNumber());
		assertEquals(null, reportHelper.getFixedInBuildNumber());
	}
	
	public void testGetAvgTimeToFixBuildNone() throws Exception {
		addSample(PASS, new Date(1000L), "a");
		
		assertEquals(-1, new ReportHelper(samples, null).getAverageTimeToFixBuild());
	}
	
	public void testGetAvgTimeToFixBuildSingle() throws Exception {
		addSample(FAIL, new Date(0L), "a");
		addSample(PASS, new Date(1000L), "a");
		
		assertEquals(1000L, new ReportHelper(samples, null).getAverageTimeToFixBuild());
	}
	
	public void testGetAvgTimeToFixBuildTwo() throws Exception {
		addSample(FAIL, new Date(0L), "a");
		addSample(PASS, new Date(1000L), "a");
		addSample(FAIL, new Date(3000L), "a");
		addSample(PASS, new Date(5000L), "a");
		
		assertEquals(1500L, new ReportHelper(samples, null).getAverageTimeToFixBuild());
	}
	
	public void testGetAvgTimeToFixBuildMultiProject() throws Exception {
		addSample(FAIL, new Date(0L), "a");
		addSample(PASS, new Date(1000L), "b");
		addSample(PASS, new Date(5000L), "a");
		
		assertEquals(5000L, new ReportHelper(samples, null).getAverageTimeToFixBuild());
	}

	public void testGetAvgTimeToFixBuildMultiProjectName() throws Exception {
		addSample(FAIL, new Date(0L), "a");
		addSample(PASS, new Date(1000L), "b");
		addSample(PASS, new Date(5000L), "a");
		
		assertEquals("a", new ReportHelper(samples, null).getLongestElapsedFailureName());
	}
	
	private void addSample(Status status, Date completionDate, String projectName) {
		final ProjectStatusDto sample = new ProjectStatusDto();
		sample.setStatus(status);
		sample.setCompletionDate(completionDate);
		sample.setName(projectName);
		sample.setBuildNumber(buildNumber++);
		samples.add(sample);
	}
}
