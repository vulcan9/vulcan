/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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

import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;

public class CoberturaTransformTest extends TransformTestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Document coverageDoc = new SAXBuilder().build(TestUtils.resolveRelativeFile("source/test/xml/cobertura.xml"));
		doc.getRootElement().addContent(coverageDoc.detachRootElement());
	}
	
	public void testCoverage() throws Exception {
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.coverage.line", MetricType.PERCENT, "0.8332542694497154", true);
		assertContainsMetric(t, "vulcan.metrics.coverage.branch", MetricType.PERCENT, "0.8017174082747853", true);
	}
	
	public void testCountsSource() throws Exception {
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.source.lines", MetricType.NUMBER, "4216", true);
		assertContainsMetric(t, "vulcan.metrics.source.methods", MetricType.NUMBER, "1102", true);
		assertContainsMetric(t, "vulcan.metrics.source.classes", MetricType.NUMBER, "203", true);
		assertContainsMetric(t, "vulcan.metrics.source.packages", MetricType.NUMBER, "16", true);
		assertContainsMetric(t, "vulcan.metrics.source.files", MetricType.NUMBER, "148", true);
	}
}
