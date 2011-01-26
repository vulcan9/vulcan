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
package net.sourceforge.vulcan.core.conversion;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.dto.MetricDto;

public class BuildOutcomeConverterTest extends EasyMockTestCase {
	MetricDto metric = new MetricDto();
	
	public void testConvertCoverageMetricWithPercentSign() throws Exception {
		metric.setValue("89.12%");
		
		BuildOutcomeConverter.fixMetric(metric);
		
		assertEquals(MetricDto.MetricType.PERCENT, metric.getType());
		assertEquals("0.8912", metric.getValue());
	}
	
	public void testConvertNumber() throws Exception {
		metric.setValue("89.92");
		
		BuildOutcomeConverter.fixMetric(metric);
		
		assertEquals("89.92", metric.getValue());
		assertEquals(MetricDto.MetricType.NUMBER, metric.getType());
	}
	
	public void testConvertNonNumber() throws Exception {
		metric.setValue("foo");
		
		BuildOutcomeConverter.fixMetric(metric);
		
		assertEquals("foo", metric.getValue());
		assertEquals(MetricDto.MetricType.STRING, metric.getType());
	}
}
