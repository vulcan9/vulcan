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

public class JUnitTransformTest extends TransformTestCase {
	Element suite = new Element("testsuite");
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		suite.setAttribute("name", "net.sourceforge.vulcan.metrics.FakeSuite");
	}
	public void testTransformNothing() throws Exception {
		final Document doc = new Document(new Element("merged-root"));
		
		doc.getRootElement().addContent(new Element("unrecognized-one"));
		doc.getRootElement().addContent(new Element("unrecognized-two"));
		
		final Document t = plugin.transform(doc);
		
		assertNotNull(t);
		assertEquals("metrics", t.getRootElement().getName());
		assertEquals(0, t.getRootElement().getContentSize());
	}
	
	public void testTransformJUnit() throws Exception {
		doc.getRootElement().addContent(suite);
		addJUnitTestCase(suite, "Foo", false, false);
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "1", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "0", true);
	}
	public void testTransformJUnitFail() throws Exception {
		doc.getRootElement().addContent(suite);
		addJUnitTestCase(suite, "Foo", true, false);
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "1", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "1", true);
		assertContainsTestFailure(t, "net.sourceforge.vulcan.metrics.FakeSuite.Foo");
	}
	public void testTransformJUnitError() throws Exception {
		doc.getRootElement().addContent(suite);
		addJUnitTestCase(suite, "Foo", false, true);
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "1", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "1", true);
		assertContainsTestFailure(t, "net.sourceforge.vulcan.metrics.FakeSuite.Foo");
	}
	public void testTransformJUnitMultipleSuites() throws Exception {
		doc.getRootElement().addContent(suite);
		
		addJUnitTestCase(suite, "Foo", true, false);
		
		suite = new Element("testsuite");
		doc.getRootElement().addContent(suite);
		
		addJUnitTestCase(suite, "Foo", false, true);
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "2", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "2", true);
	}

	public void testTransformNUnit() throws Exception {
		Element target = addNUnitTestSuite();

		addNunitTestCase(target, "SourceForge.Vulcan.DotNet.FakeTest.Sample", "True", "True");

		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "1", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "0", true);
	}

	public void testTransformNUnitIgnored() throws Exception {
		Element target = addNUnitTestSuite();

		addNunitTestCase(target, "SourceForge.Vulcan.DotNet.FakeTest.Sample", "False", null);

		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.ignored", MetricType.NUMBER, "1", true);
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "0", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "0", true);
	}
	
	public void testTransformNUnitFailures() throws Exception {
		Element target1 = addNUnitTestSuite();

		addNunitTestCase(target1, "SourceForge.Vulcan.DotNet.FakeTest.Sample1", "True", "True");
		addNunitTestCase(target1, "SourceForge.Vulcan.DotNet.FakeTest.Sample2", "True", "False");
		
		Element target2 = addNUnitTestSuite();
		addNunitTestCase(target2, "SourceForge.Vulcan.DotNet.FakeTest.Sample3", "True", "True");
		addNunitTestCase(target2, "SourceForge.Vulcan.DotNet.FakeTest.Sample4", "True", "False");
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "4", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "2", true);
		assertContainsTestFailure(t, "SourceForge.Vulcan.DotNet.FakeTest.Sample2");
		assertContainsTestFailure(t, "SourceForge.Vulcan.DotNet.FakeTest.Sample4");
	}
	
	public void testTransformTotalsAcrossJunitAndNunit() throws Exception {
		Element target1 = addNUnitTestSuite();

		addNunitTestCase(target1, "SourceForge.Vulcan.DotNet.FakeTest.Sample1", "True", "True");
		addNunitTestCase(target1, "SourceForge.Vulcan.DotNet.FakeTest.Sample2", "True", "False");
		
		Element target2 = addNUnitTestSuite();
		addNunitTestCase(target2, "SourceForge.Vulcan.DotNet.FakeTest.Sample3", "True", "True");
		addNunitTestCase(target2, "SourceForge.Vulcan.DotNet.FakeTest.Sample4", "True", "False");
		
		doc.getRootElement().addContent(suite);
		addJUnitTestCase(suite, "Foo", false, true);
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "5", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "3", true);
	}
	
	private void addNunitTestCase(Element suite, String name, String executed, String success) {
		final Element testCase = new Element("test-case");
		testCase.setAttribute("name", name);
		testCase.setAttribute("executed", executed);
		if (success != null) {
			testCase.setAttribute("success", success);
		}
		
		suite.addContent(testCase);
	}

	private Element addNUnitTestSuite() {
		final Element testResults = new Element("test-results");
		doc.getRootElement().addContent(testResults);
		
		Element target = testResults;
		for (int i=0; i<10; i++) {
			target = addNestedSuite(target);
		}
		return target;
	}

	private Element addNestedSuite(final Element testResults) {
		final Element suite = new Element("test-suite");
		testResults.addContent(suite);

		final Element results = new Element("results");
		suite.addContent(results);
		
		return results;
	}

	private void addJUnitTestCase(Element suite, String name, boolean fail, boolean error) {
		final Element testCase = new Element("testcase");
		testCase.setAttribute("name", name);
		
		if (fail) {
			testCase.addContent(new Element("failure"));
		} else if (error) {
			testCase.addContent(new Element("error"));
		}
		
		suite.addContent(testCase);
	}
}
