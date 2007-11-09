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

import java.util.Date;
import java.util.List;

import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ReportHelper {
	private final List<ProjectStatusDto> samples;
	
	private long longestTime;
	private long average = -1;
	
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

	private void calculate(long maxTime) {
		long max = -1;
		long total = 0;
		long count = 0;
		
		for (int i=0; i<samples.size(); i++) {
			long failTime = 0;
			long passTime = maxTime;
			
			for (; i<samples.size(); i++) {
				final ProjectStatusDto sample = samples.get(i);
				if (sample.getStatus() == Status.FAIL) {
					failTime = sample.getCompletionDate().getTime();
					break;
				}
			}
			
			if (i>=samples.size()) {
				break;
			}
			
			for (; i<samples.size(); i++) {
				final ProjectStatusDto sample = samples.get(i);
				if (sample.getStatus() == Status.PASS) {
					passTime = sample.getCompletionDate().getTime();
					break;
				}
			}

			long elapsed = passTime - failTime;
			
			if (elapsed > max) {
				max = elapsed;
			}
			if (elapsed > 0) {
				total += elapsed;
				count++;
			}
		}
		
		longestTime = max;
		if (count > 0) {
			average = total/count;
		}
	}
}
