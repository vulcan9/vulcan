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
package net.sourceforge.vulcan.ant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import junit.framework.TestCase;

public class JUnitTallyTest extends TestCase {
	final String[] samples = {
		"    [junit] Tests run: 5, Failures: 0, Errors: 0, Time elapsed: 0.45 sec\n",
		"    [junit] Tests run: 25, Failures: 2, Errors: 0, Time elapsed: 6.57 sec\r\n",
		"    [junit] Tests run: 41, Failures: 7, Errors: 1, Time elapsed: 1.09 sec\r",
		"    [junit] Tests run: 55, failures: 6, Errors: 1, Time elapsed: 1.09 sec\n\r",
		"    [JUnit] Tests run: 23421, Failures: 9223, Errors: 1343, Time elapsed: 1.09 sec"
	};
	
	final int totals[] = {5, 25, 41, 55, 23421};
	final int failures[] = {0, 2, 7, 6, 9223};
	final int errors[] = {0, 0, 1, 1, 1343};
	
	public static class JUnitTally {
		static final Pattern pattern = Pattern.compile("(?si).*\\[junit\\] Tests run: (\\d*), Failures: (\\d*), Errors: (\\d*).*");
		final int total;
		final int failures;
		final int errors;
		
		public JUnitTally(int total, int failures, int errors) {
			this.total = total;
			this.failures = failures;
			this.errors = errors;
		}
		public JUnitTally add(JUnitTally o) {
			return new JUnitTally(
					total + o.total,
					failures + o.failures,
					errors + o.errors);
		}
		public int getErrors() {
			return errors;
		}
		public int getFailures() {
			return failures;
		}
		public int getTotal() {
			return total;
		}
		@Override
		public boolean equals(Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
		@Override
		public int hashCode() {
			return HashCodeBuilder.reflectionHashCode(this);
		}
		@Override
		public String toString() {
			return total + "/" + failures + "/" + errors;
		}
		public static boolean isMessageJUnitTally(String message) {
			return pattern.matcher(message).matches();
		}
		public static JUnitTally parse(String message) {
			final Matcher matcher = pattern.matcher(message);
			
			if (!matcher.matches()) {
				return null;
			}
			
			matcher.start();

			return new JUnitTally(
					Integer.parseInt(matcher.group(1)),
					Integer.parseInt(matcher.group(2)),
					Integer.parseInt(matcher.group(3)));
		}
	}
	public void testMacher() {
		assertFalse(JUnitTally.isMessageJUnitTally("some message"));
		
		assertFalse(JUnitTally.isMessageJUnitTally("    [junit] some message"));
		
		for (int i=0; i<samples.length; i++) {
			assertTrue("Sample " + i + " failed check", JUnitTally.isMessageJUnitTally(samples[i]));
		}
	}
	public void testExtractsResults() {
		for (int i=0; i<samples.length; i++) {
			JUnitTally t = JUnitTally.parse(samples[i]);
			assertNotNull("got null result for sample " + i, t);
			assertEquals("total for sample " + i, totals[i], t.getTotal());
			assertEquals("failures for sample " + i, failures[i], t.getFailures());
			assertEquals("errors for sample " + i, errors[i], t.getErrors());
		}
	}
	public void testAdd() {
		assertEquals(JUnitTally.parse(samples[0]).add(JUnitTally.parse(samples[1])),
				new JUnitTally(30, 2, 0));
	}
}
