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

import net.sourceforge.vulcan.dto.MetricDto.MetricType;

import org.jdom.Document;
import org.jdom.Element;

public class EmmaTransformTest extends TransformTestCase {
	final Element report = new Element("report");
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		doc.getRootElement().addContent(report);
	}

	public void testStats() throws Exception {
		addStats(report, 16, 150, 1101, 109, 4272);
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.source.packages", MetricType.NUMBER, "16", true);
		assertContainsMetric(t, "vulcan.metrics.source.classes", MetricType.NUMBER, "150", true);
		assertContainsMetric(t, "vulcan.metrics.source.methods", MetricType.NUMBER, "1101", true);
		assertContainsMetric(t, "vulcan.metrics.source.files", MetricType.NUMBER, "109", true);
		assertContainsMetric(t, "vulcan.metrics.source.lines", MetricType.NUMBER, "4272", true);
	}
	
	public void testCoverage() throws Exception {
		final Element data = new Element("data");
		final Element all = new Element("all");
		
		all.setAttribute("name", "all classes");
		
		report.addContent(data);
		data.addContent(all);
		
		addCoverage(all, "class, %", "91% (142/150)"); 
		addCoverage(all, "method, %", "89% (142/150)");
		addCoverage(all, "block, %", "84% (142/150)");
		addCoverage(all, "line, %", "81% (142/150)");
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.coverage.class", MetricType.PERCENT, "0.91", true);
		assertContainsMetric(t, "vulcan.metrics.coverage.method", MetricType.PERCENT, "0.89", true);
		assertContainsMetric(t, "vulcan.metrics.coverage.block", MetricType.PERCENT, "0.84", true);
		assertContainsMetric(t, "vulcan.metrics.coverage.line", MetricType.PERCENT, "0.81", true);
	}
	
	private void addCoverage(Element all, String type, String value) {
		final Element cov = new Element("coverage");
		all.addContent(cov);
		
		cov.setAttribute("type", type);
		cov.setAttribute("value", value);
	}

	private void addStats(Element report, int packages, int classes, int methods, int files, int lines) {
		final Element stats = new Element("stats");
		
		addStat(stats, "packages", Integer.toString(packages));
		addStat(stats, "classes", Integer.toString(classes));
		addStat(stats, "methods", Integer.toString(methods));
		addStat(stats, "srcfiles", Integer.toString(files));
		addStat(stats, "srclines", Integer.toString(lines));
		
		report.addContent(stats);
	}

	private void addStat(final Element stats, String type, String value) {
		final Element stat = new Element(type);
		stat.setAttribute("value", value);
		stats.addContent(stat);
	}
}
