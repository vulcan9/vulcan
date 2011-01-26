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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;

public class ReportHelper {
	private final List<ProjectStatusDto> samples;
	
	private long longestTime;
	private long average = -1;
	private Integer failingBuildNumber;
	private Integer fixedInBuildNumber;
	private String longestElapsedFailureName;
	
	public ReportHelper(List<ProjectStatusDto> samples, Date maxDate) {
		this.samples = samples;
		
		if (maxDate == null) {
			maxDate = samples.get(samples.size()-1).getCompletionDate();
		}
		
		long maxTime = maxDate.getTime();
		
		calculate(maxTime);
	}

	public int getSuccessCount() {
		int count = 0;
		
		for (ProjectStatusDto sample : samples) {
			if (Status.PASS == sample.getStatus()) {
				count++;
			}
		}
		
		return count;
	}

	public long getLongestTimeToFixBuild() {
		return longestTime;
	}
	
	public long getAverageTimeToFixBuild() {
		return average;
	}

	public Integer getFailingBuildNumber() {
		return failingBuildNumber;
	}
	
	public Integer getFixedInBuildNumber() {
		return fixedInBuildNumber;
	}

	public String getLongestElapsedFailureName() {
		return longestElapsedFailureName;
	}
	
	private void calculate(long maxTime) {
		long max = -1;
		long total = 0;
		long count = 0;
		
		final Collection<List<ProjectStatusDto>> samplesByName = keySamplesByName();
		
		for (List<ProjectStatusDto> projectSamples : samplesByName) {
			for (int i=0; i<projectSamples.size(); i++) {
				ProjectStatusDto failSample = null;
				ProjectStatusDto passSample  = null;
				
				for (; i<projectSamples.size(); i++) {
					final ProjectStatusDto sample = projectSamples.get(i);
					if (sample.getStatus() == Status.FAIL) {
						failSample = sample;
						break;
					}
				}
				
				if (i>=projectSamples.size()) {
					break;
				}
				
				for (; i<projectSamples.size(); i++) {
					final ProjectStatusDto sample = projectSamples.get(i);
					if (sample.getStatus() == Status.PASS) {
						passSample = sample;
						break;
					}
				}
	
				long elapsed = maxTime - failSample.getCompletionDate().getTime();
				
				if (passSample != null) {
					elapsed = passSample.getCompletionDate().getTime() - failSample.getCompletionDate().getTime();
				}
				
				if (elapsed > max) {
					max = elapsed;
					failingBuildNumber = failSample.getBuildNumber();
					longestElapsedFailureName = failSample.getName();
					
					if (passSample != null) {
						fixedInBuildNumber = passSample.getBuildNumber();
					} else {
						fixedInBuildNumber = null;
					}
				}
				if (elapsed > 0) {
					total += elapsed;
					count++;
				}
			}
		}
		
		longestTime = max;
		if (count > 0) {
			average = total/count;
		}
	}

	private Collection<List<ProjectStatusDto>> keySamplesByName() {
		final Map<String, List<ProjectStatusDto>> map = new HashMap<String, List<ProjectStatusDto>>();
		
		for (ProjectStatusDto sample : samples) {
			final String name = sample.getName();
			List<ProjectStatusDto> list = map.get(name);
			if (list == null) {
				list = new ArrayList<ProjectStatusDto>();
				map.put(name, list);
			}
			
			list.add(sample);
		}
		
		return map.values();
	}
}
