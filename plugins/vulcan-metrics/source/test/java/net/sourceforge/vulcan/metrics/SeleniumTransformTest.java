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

import net.sourceforge.vulcan.dto.MetricDto.MetricType;

import org.jdom.Document;
import org.jdom.Element;

public class SeleniumTransformTest extends TransformTestCase {
	Element html = new Element("html");
	Element titleCell = new Element("td");
	Element summaryBody;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		Element summaryTable = addElement(addElement(addElement(addElement(addElement(html, "body"), "table"), "tr"), "td"), "table");
		
		Element suiteTitleRow = addElement(addElement(summaryTable, "thead"), "tr");
		suiteTitleRow.setAttribute("class", "foo title status_something");
		suiteTitleRow.addContent(titleCell);
		titleCell.setText("SomeSampleSeleniumSuite");
		
		summaryBody = addElement(summaryTable, "tbody");
	}
	
	public void testTransformTwoTests() throws Exception {
		addSeleniumTest("FooTest", true);
		addSeleniumTest("BarTest", true);
		
		doc.getRootElement().addContent(html);
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "2", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "0", true);
	}
	
	public void testTransformTestsWithFailures() throws Exception {
		addSeleniumTest("FooTest", true);
		addSeleniumTest("BarTest", false);
		addSeleniumTest("ZipTest", false);
		
		doc.getRootElement().addContent(html);
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "3", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "2", true);
	}
	
	public void testTransformTestsWithFailuresGetsFailedTestNames() throws Exception {
		addSeleniumTest("FooTest", true);
		addSeleniumTest("BarTest", false);
		addSeleniumTest("ZipTest", false);
		
		doc.getRootElement().addContent(html);
		
		final Document t = plugin.transform(doc);

		assertContainsTestFailure(t, "SomeSampleSeleniumSuite:BarTest");
		assertContainsTestFailure(t, "SomeSampleSeleniumSuite:ZipTest");
	}
	
	private void addSeleniumTest(String name, boolean passed) {
		Element tr = addElement(summaryBody, "tr");
		if (passed) {
			tr.setAttribute("class", "status_passed");
		} else {
			tr.setAttribute("class", "status_failed");
		}
		
		Element td = addElement(tr, "td");
		Element a = addElement(td, "a");
		a.setText(name);
	}

	private Element addElement(Element parent, String name) {
		Element body = new Element(name);
		parent.addContent(body);
		return body;
	}
}
