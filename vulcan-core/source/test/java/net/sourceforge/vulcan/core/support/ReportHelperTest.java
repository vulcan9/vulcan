/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2007 Chris Eldredge
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
	
	public void testGetSuccessCount() throws Exception {
		for (int i=0; i<10; i++) {
			addSample(PASS, new Date(0L));
		}
		for (int i=0; i<10; i++) {
			addSample(SKIP, new Date(0L));
		}
		for (int i=0; i<10; i++) {
			addSample(ERROR, new Date(0L));
		}
		for (int i=0; i<10; i++) {
			addSample(FAIL, new Date(0L));
		}
		
		assertEquals(10, new ReportHelper(samples, null).getSuccessCount());
	}

	public void testGetLongestTimeToFixBuildNoFails() throws Exception {
		addSample(PASS, new Date(1000L));
		
		assertEquals(-1, new ReportHelper(samples, null).getLongestTimeToFixBuild());
	}
	
	public void testGetLongestTimeToFixBuildNeverFixed() throws Exception {
		addSample(FAIL, new Date(1000L));
		addSample(FAIL, new Date(1200L));
		
		assertEquals(500L, new ReportHelper(samples, new Date(1500L)).getLongestTimeToFixBuild());
	}
	
	public void testGetLongestTimeToFixBuildNeverFixedNullMax() throws Exception {
		addSample(FAIL, new Date(1000L));
		addSample(FAIL, new Date(3400L));
		
		assertEquals(2400L, new ReportHelper(samples, null).getLongestTimeToFixBuild());
	}
	
	public void testGetLongestTimeToFixBuildSingle() throws Exception {
		addSample(FAIL, new Date(0L));
		addSample(PASS, new Date(1000L));
		
		assertEquals(1000L, new ReportHelper(samples, null).getLongestTimeToFixBuild());
	}
	
	public void testGetLongestTimeToFixBuild() throws Exception {
		addSample(FAIL, new Date(0L));
		addSample(PASS, new Date(1000L));
		addSample(FAIL, new Date(3000L));
		addSample(PASS, new Date(5000L));
		
		assertEquals(2000L, new ReportHelper(samples, null).getLongestTimeToFixBuild());
	}

	public void testGetAvgTimeToFixBuildNone() throws Exception {
		addSample(PASS, new Date(1000L));
		
		assertEquals(-1, new ReportHelper(samples, null).getAverageTimeToFixBuild());
	}
	
	public void testGetAvgTimeToFixBuildSingle() throws Exception {
		addSample(FAIL, new Date(0L));
		addSample(PASS, new Date(1000L));
		
		assertEquals(1000L, new ReportHelper(samples, null).getAverageTimeToFixBuild());
	}
	
	public void testGetAvgTimeToFixBuildTwo() throws Exception {
		addSample(FAIL, new Date(0L));
		addSample(PASS, new Date(1000L));
		addSample(FAIL, new Date(3000L));
		addSample(PASS, new Date(5000L));
		
		assertEquals(1500L, new ReportHelper(samples, null).getAverageTimeToFixBuild());
	}
	
	private void addSample(Status status, Date completionDate) {
		final ProjectStatusDto sample = new ProjectStatusDto();
		sample.setStatus(status);
		sample.setCompletionDate(completionDate);
		samples.add(sample);
	}
}
