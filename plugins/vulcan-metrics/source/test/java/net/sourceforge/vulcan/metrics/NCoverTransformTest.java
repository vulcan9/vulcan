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
package net.sourceforge.vulcan.metrics;

import net.sourceforge.vulcan.dto.MetricDto.MetricType;

import org.jdom.Document;
import org.jdom.Element;

public class NCoverTransformTest extends TransformTestCase {
	final Element coverageReport = new Element("coverageReport2");
	final Element projectNode = new Element("project");
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		doc.getRootElement().addContent(coverageReport);
		coverageReport.addContent(projectNode);
	}

	public void testStats() throws Exception {
		addStats(150, 1101, 109, 4272, 91.12223, 92.232);
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.source.classes", MetricType.NUMBER, "150", true);
		assertContainsMetric(t, "vulcan.metrics.source.methods", MetricType.NUMBER, "1101", true);
		assertContainsMetric(t, "vulcan.metrics.source.files", MetricType.NUMBER, "109", true);
		assertContainsMetric(t, "vulcan.metrics.source.lines", MetricType.NUMBER, "4272", true);
		assertContainsMetric(t, "vulcan.metrics.coverage.block", MetricType.PERCENT, "0.9112223", true);
		assertContainsMetric(t, "vulcan.metrics.coverage.method", MetricType.PERCENT, "0.92232", true);
	}
	
	private void addStats(int classes, int methods, int files, int lines, double coverageBySeq, double coverageByFunc) {
		projectNode.setAttribute("classes", Integer.toString(classes));
		projectNode.setAttribute("totalFunctions", Integer.toString(methods));
		projectNode.setAttribute("files", Integer.toString(files));
		projectNode.setAttribute("nonCommentLines", Integer.toString(lines));
		projectNode.setAttribute("coverage", Double.toString(coverageBySeq));
		projectNode.setAttribute("functionCoverage", Double.toString(coverageByFunc));
	}
}
