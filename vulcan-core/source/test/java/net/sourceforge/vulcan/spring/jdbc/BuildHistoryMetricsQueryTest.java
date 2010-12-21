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
package net.sourceforge.vulcan.spring.jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class BuildHistoryMetricsQueryTest extends TestCase {
	List<JdbcBuildOutcomeDto> builds = new ArrayList<JdbcBuildOutcomeDto>();
	List<JdbcMetricDto> metrics = new ArrayList<JdbcMetricDto>();
	
	public void testSplitSingle() throws Exception {
		final JdbcBuildOutcomeDto b1 = addBuild(0);
		
		final JdbcMetricDto m1 = addMetric("m1", 0);
		
		BuildHistoryMetricsQuery.splitMetricsByBuildId(builds, metrics);
		
		assertEquals(Arrays.asList(m1), b1.getMetrics());
	}

	public void testSplitTwoMetricsOneBuild() throws Exception {
		final JdbcBuildOutcomeDto b1 = addBuild(0);
		
		final JdbcMetricDto m1 = addMetric("m1", 0);
		final JdbcMetricDto m2 = addMetric("m2", 0);
		
		BuildHistoryMetricsQuery.splitMetricsByBuildId(builds, metrics);
		
		assertEquals(Arrays.asList(m1, m2), b1.getMetrics());
	}
	
	public void testSplitTwoMetricsTwoBuilds() throws Exception {
		final JdbcBuildOutcomeDto b1 = addBuild(0);
		final JdbcBuildOutcomeDto b2 = addBuild(1);
		
		final JdbcMetricDto m1 = addMetric("m1", 0);
		final JdbcMetricDto m2 = addMetric("m2", 1);
		
		BuildHistoryMetricsQuery.splitMetricsByBuildId(builds, metrics);
		
		assertEquals(Arrays.asList(m1), b1.getMetrics());
		assertEquals(Arrays.asList(m2), b2.getMetrics());
	}
	
	public void testSplitFourMetricsTwoBuilds() throws Exception {
		final JdbcBuildOutcomeDto b1 = addBuild(0);
		final JdbcBuildOutcomeDto b2 = addBuild(1);
		
		final JdbcMetricDto m1 = addMetric("m1", 0);
		final JdbcMetricDto m2 = addMetric("m2", 0);
		final JdbcMetricDto m3 = addMetric("m3", 1);
		final JdbcMetricDto m4 = addMetric("m4", 1);
		
		BuildHistoryMetricsQuery.splitMetricsByBuildId(builds, metrics);
		
		assertEquals(Arrays.asList(m1, m2), b1.getMetrics());
		assertEquals(Arrays.asList(m3, m4), b2.getMetrics());
	}
	
	private JdbcMetricDto addMetric(final String name, int buildId) {
		final JdbcMetricDto dto = new JdbcMetricDto() {
			@Override
			public String toString() {
				return name;
			}
		};
		
		dto.setBuildId(new Integer(buildId));
		metrics.add(dto);
		
		return dto;
	}

	private JdbcBuildOutcomeDto addBuild(int buildId) {
		final JdbcBuildOutcomeDto build = new JdbcBuildOutcomeDto();
		build.setPrimaryKey(new Integer(buildId));
		builds.add(build);
		
		return build;
	}
}
