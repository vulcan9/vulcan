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

import java.util.Arrays;
import java.util.List;

import net.sourceforge.vulcan.core.support.BuildOutcomeCache;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;

import org.jdom.Element;

public class DigestTest extends TransformTestCase {
	ProjectStatusDto status = new ProjectStatusDto();
	ProjectStatusDto prevStatus = new ProjectStatusDto();
	
	BuildOutcomeCache cache = new BuildOutcomeCache() {
		@Override
		public ProjectStatusDto getLatestOutcome(String name) {
			assertEquals(status.getName(), name);
			return prevStatus;
		}
	};
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		plugin.setBuildOutcomeCache(cache);
	}
	
	public void testDigest() throws Exception {
		final Element e = new Element("metrics");
		
		final Element m = new Element("metric");
		
		m.setAttribute("key", "foo");
		m.setAttribute("value", "124");
		m.setAttribute("type", "number");
		
		e.addContent(m);
		
		plugin.digest(e, status);
		
		final List<MetricDto> digest = status.getMetrics();
		
		assertEquals(1, digest.size());
		assertEquals("foo", digest.get(0).getMessageKey());
		assertEquals("124", digest.get(0).getValue());
		assertEquals(MetricType.NUMBER, digest.get(0).getType());
	}
	
	public void testDigestTestFailure() throws Exception {
		status.setBuildNumber(465);
		
		final String name = "com.example.foo.testBar";

		final Element e = new Element("metrics");

		final Element t1 = new Element("test-failure");
		t1.setText(name);
		addElementWithText(t1, "message", "the message");
		addElementWithText(t1, "details", "the stack trace");
		
		e.addContent(t1);
		
		plugin.digest(e, status);

		final List<TestFailureDto> failures = status.getTestFailures();
		assertNotNull(failures);
		assertEquals(1, failures.size());
		assertEquals(name, failures.get(0).getName());
		assertEquals((Integer)465, failures.get(0).getBuildNumber());
		assertEquals("the message", failures.get(0).getMessage());
		assertEquals("the stack trace", failures.get(0).getDetails());
	}
	
	public void testDigestTestFailureSorts() throws Exception {
		status.setBuildNumber(465);
		
		final String name1 = "net.example.foo.test1";
		final String name2 = "com.example.foo.test2";
		
		final Element e = new Element("metrics");

		final Element t1 = new Element("test-failure");
		t1.setText(name1);
		
		e.addContent(t1);
		
		final Element t2 = new Element("test-failure");
		t2.setText(name2);
		
		e.addContent(t2);
		
		plugin.digest(e, status);

		final List<TestFailureDto> failures = status.getTestFailures();
		assertNotNull(failures);
		assertEquals(2, failures.size());
		assertEquals(name2, failures.get(0).getName());
		assertEquals(name1, failures.get(1).getName());
	}
	
	public void testDigestTestFailureConsecutive() throws Exception {
		final String name = "com.example.foo.testBar";

		status.setBuildNumber(465);
		TestFailureDto prevFailure = new TestFailureDto();
		prevFailure.setName(name);
		prevFailure.setBuildNumber(461);
		
		prevStatus.setTestFailures(Arrays.asList(prevFailure));

		final Element e = new Element("metrics");

		final Element t1 = new Element("test-failure");
		t1.setText(name);
		
		e.addContent(t1);
		
		plugin.digest(e, status);

		final List<TestFailureDto> failures = status.getTestFailures();
		assertNotNull(failures);
		assertEquals(1, failures.size());
		assertEquals(name, failures.get(0).getName());
		assertEquals((Integer)461, failures.get(0).getBuildNumber());
	}

	private void addElementWithText(final Element elem, String nodeName, String text) {
		final Element child = new Element(nodeName);
		child.setText(text);
		elem.addContent(child);
	}
}
