/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2014 Chris Eldredge
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
package net.sourceforge.vulcan.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;

import net.sourceforge.vulcan.EasyMockTestCase;

public class ParameterParsingHttpServletRequestWrapperTest extends EasyMockTestCase {
	HttpServletRequest delegate;
	ParameterParsingHttpServletRequestWrapper request;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		delegate = createNiceMock(HttpServletRequest.class);
	}

	public void testResetsReader() throws Exception {
		String sample = "foo=bar";
		ParameterParsingHttpServletRequestWrapper request = setUpFormData(sample);
		
		assertEquals("bar", request.getParameter("foo"));
		assertEquals("foo=bar", IOUtils.toString(request.getReader()));
	}

	public void testParsesFormData() throws Exception {
		String sample = "foo=bar&baz=yow";
		ParameterParsingHttpServletRequestWrapper request = setUpFormData(sample);
		
		assertEquals("bar", request.getParameter("foo"));
		assertEquals("yow", request.getParameter("baz"));
	}

	public void testMultiValue() throws Exception {
		String sample = "foo=ba%20r&foo=yo+w";
		ParameterParsingHttpServletRequestWrapper request = setUpFormData(sample);
		
		assertEquals("ba r", request.getParameter("foo"));
		assertTrue(Arrays.equals(new String[] {"ba r", "yo w"}, request.getParameterValues("foo")));
	}

	protected ParameterParsingHttpServletRequestWrapper setUpFormData(String formData) throws IOException {
		expect(delegate.getParameterMap()).andReturn(new HashMap<String, String>());
		expect(delegate.getParameterNames()).andReturn(toEnumeration());
		expect(delegate.getContentType()).andReturn("application/x-www-form-urlencoded");
		expect(delegate.getReader()).andReturn(new BufferedReader(new StringReader(formData))).anyTimes();
		replay();
		return new ParameterParsingHttpServletRequestWrapper(delegate);
	}
	
	protected Enumeration<String> toEnumeration(final String... items) {
		final int[] i = new int[1];
		
		return new Enumeration<String>() {
			@Override
			public boolean hasMoreElements() {
				return items != null && i[0] < items.length;
			}
			
			@Override
			public String nextElement() {
				return items[i[0]++];
			}
		};
	}
}
