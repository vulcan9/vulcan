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

import org.jdom.Document;
import org.jdom.Element;

public class FxCopTransformTest extends TransformTestCase {
	final Element report = new Element("report");
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		doc.getRootElement().addContent(report);
	}

	public void testNoStatsOnMissingFxCopReportNode() throws Exception {
		final Element issue = new Element("Issue");
		issue.setAttribute("Level", "Warning");
		
		report.addContent(issue);
		
		final Document t = plugin.transform(doc);
		
		assertEquals(0, t.getRootElement().getContentSize());
	}
	public void testStatsPresent() throws Exception {
		final Element fxCopReport = new Element("FxCopReport");
		
/*		final Element issue = new Element("Issue");
		issue.setAttribute("Level", "Warning");
		
		fxCopReport.addContent(issue);
*/		
		report.addContent(fxCopReport);
		
		final Document t = plugin.transform(doc);
		
		assertEquals(5, t.getRootElement().getContentSize());
		
		assertContainsMetric(t, "vulcan.metrics.fxcop.informational", "0", true);
		assertContainsMetric(t, "vulcan.metrics.fxcop.warnings", "0", true);
		assertContainsMetric(t, "vulcan.metrics.fxcop.critical.warnings", "0", true);
		assertContainsMetric(t, "vulcan.metrics.fxcop.errors", "0", true);
		assertContainsMetric(t, "vulcan.metrics.fxcop.critical.errors", "0", true);
	}
	public void testTotalsStatsAcrossReports() throws Exception {
		addReportWithStat("Warning");
		addReportWithStat("Warning");
		addReportWithStat("CriticalError");
		addReportWithStat("CriticalWarning");
		addReportWithStat("Informational");
		addReportWithStat("Error");
		
		final Document t = plugin.transform(doc);
		
		assertEquals(5, t.getRootElement().getContentSize());
		
		assertContainsMetric(t, "vulcan.metrics.fxcop.informational", "1", true);
		assertContainsMetric(t, "vulcan.metrics.fxcop.warnings", "2", true);
		assertContainsMetric(t, "vulcan.metrics.fxcop.critical.warnings", "1", true);
		assertContainsMetric(t, "vulcan.metrics.fxcop.errors", "1", true);
		assertContainsMetric(t, "vulcan.metrics.fxcop.critical.errors", "1", true);
	}

	private void addReportWithStat(String level) {
		final Element fxCopReport = new Element("FxCopReport");
		
		final Element issue = new Element("Issue");
		issue.setAttribute("Level", level);
		
		fxCopReport.addContent(issue);
		
		report.addContent(fxCopReport);
	}
}