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
import org.jdom.Text;

public class JUnitTransformTest extends TransformTestCase {
	private static final String ERROR_DETAILS = "java.lang.Exception: unanticipated exception text\n	at net.sourceforge.vulcan.Example.method";
	private static final String ERROR_MESSAGE = "unanticipated exception text";
	private static final String ASSERTION_FAILURE_DETAILS = "junit.framework.AssertionFailedError: message again\n	at junit.framework.Assert.fail(Assert.java:47)";
	private static final String ASSERTION_FAILURE_MESSAGE = "expected:<22> but was:<44>";
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
		assertContainsTestFailure(t, "net.sourceforge.vulcan.metrics.FakeSuite.Foo", ASSERTION_FAILURE_MESSAGE, ASSERTION_FAILURE_DETAILS);
	}
	public void testTransformJUnitError() throws Exception {
		doc.getRootElement().addContent(suite);
		addJUnitTestCase(suite, "Foo", false, true);
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "1", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "1", true);
		assertContainsTestFailure(t, "net.sourceforge.vulcan.metrics.FakeSuite.Foo", ERROR_MESSAGE, ERROR_DETAILS);
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
		assertContainsTestFailure(t, "SourceForge.Vulcan.DotNet.FakeTest.Sample2", null, null);
		assertContainsTestFailure(t, "SourceForge.Vulcan.DotNet.FakeTest.Sample4", null, null);
	}
	
	public void testTransformTotalsAcrossJunitAndNunit() throws Exception {
		Element target1 = addNUnitTestSuite();

		addNunitTestCase(target1, "SourceForge.Vulcan.DotNet.FakeTest.Sample1", "True", "True");
		Element failure = addNunitTestCase(target1, "SourceForge.Vulcan.DotNet.FakeTest.Sample2", "True", "False");
		addNunitFailureMessage(failure, "the message", "the stack trace");
		
		Element target2 = addNUnitTestSuite();
		addNunitTestCase(target2, "SourceForge.Vulcan.DotNet.FakeTest.Sample3", "True", "True");
		addNunitTestCase(target2, "SourceForge.Vulcan.DotNet.FakeTest.Sample4", "True", "False");
		
		doc.getRootElement().addContent(suite);
		addJUnitTestCase(suite, "Foo", false, true);
		
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "5", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "3", true);
		assertContainsTestFailure(t, "SourceForge.Vulcan.DotNet.FakeTest.Sample2", "the message", "the stack trace");
	}
	
	private Element addNunitTestCase(Element suite, String name, String executed, String success) {
		final Element testCase = new Element("test-case");
		testCase.setAttribute("name", name);
		testCase.setAttribute("executed", executed);
		if (success != null) {
			testCase.setAttribute("success", success);
		}
		
		suite.addContent(testCase);
		
		return testCase;
	}

	private void addNunitFailureMessage(Element testCase, String message, String stackTrace) {
		final Element failure = new Element("failure");
		final Element messageNode = new Element("message");
		messageNode.addContent(new Text(message));
		failure.addContent(messageNode);
		final Element stackTraceNode = new Element("stack-trace");
		stackTraceNode.addContent(new Text(stackTrace));
		failure.addContent(stackTraceNode);
		testCase.addContent(failure);
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
			final Element node = new Element("failure");
			node.setAttribute("message", ASSERTION_FAILURE_MESSAGE);
			node.addContent(new Text(ASSERTION_FAILURE_DETAILS));
			testCase.addContent(node);
		} else if (error) {
			final Element node = new Element("error");
			node.setAttribute("message", ERROR_MESSAGE);
			node.addContent(new Text(ERROR_DETAILS));
			testCase.addContent(node);
		}
		
		suite.addContent(testCase);
	}
}
