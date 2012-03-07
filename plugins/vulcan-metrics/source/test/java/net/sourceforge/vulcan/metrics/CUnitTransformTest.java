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

import java.util.Iterator;

import junit.framework.AssertionFailedError;
import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;


public class CUnitTransformTest extends TransformTestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		SAXBuilder builder = new SAXBuilder();
		builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		Document sample = builder.build(TestUtils.resolveRelativeFile("source/test/xml/cunit-sample.xml"));
		doc.getRootElement().addContent(sample.detachRootElement());
	}
	
	public void testCounts() throws Exception {
		final Document t = plugin.transform(doc);
		
		assertContainsMetric(t, "vulcan.metrics.tests.executed", MetricType.NUMBER, "19", true);
		assertContainsMetric(t, "vulcan.metrics.tests.failed", MetricType.NUMBER, "2", true);
	}
	
	public void testFailingTestNames() throws Exception {
		final Document t = plugin.transform(doc);
		
		Element failure = getTestFailureByName(t, "Suite_2.testFixedFalse");
		assertEquals("IsValidSample(1234, &num)==true", failure.getChildText("message"));
		assertEquals("test/suite12.c:156", failure.getChildText("details"));
		
		failure = getTestFailureByName(t, "Suite_3.testShutdown");
		assertEquals("CU_FAIL(\"Not implemented.\")", failure.getChildText("message"));
		assertEquals("test/suite3.c:11", failure.getChildText("details"));
	}

	@SuppressWarnings("unchecked")
	private Element getTestFailureByName(final Document t, String name) {
		for (Iterator<Element> itr = t.getRootElement().getChildren("test-failure").iterator(); itr.hasNext(); ) {
			Element e = itr.next();
			if (name.equals(e.getText())) {
				return e;
			}
		}
		
		throw new AssertionFailedError("expected test-failre[@name='" + name + "']");
	}
	
}
