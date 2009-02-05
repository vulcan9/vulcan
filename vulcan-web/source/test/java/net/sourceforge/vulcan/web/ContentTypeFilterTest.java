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
package net.sourceforge.vulcan.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.sourceforge.vulcan.metadata.SvnRevision;
import servletunit.FilterChainSimulator;
import servletunit.HttpServletRequestSimulator;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ContentTypeFilterTest extends ServletFilterTestCase {
	private static final String DEFAULT_TYPE = "application/x-mock-type";
	private static final String LEGACY_TYPE = "text/plain";

	final ContentTypeFilter filter = new ContentTypeFilter();
	
	public ContentTypeFilterTest() {
		request = new HttpServletRequestSimulator(context) {
			@Override
			public String getHeader(String name) {
				if (name.equals(ContentTypeFilter.HEADER_USER_AGENT)) {
					return userAgent;
				}
				return null;
			}
		};
		
		chain = new FilterChainSimulator() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response) throws ServletException, IOException {
				if (disableInRepsonse) {
					ContentTypeFilter.disableContentTypeSupression(request);
				}
				if (chainContentType != null) {
					response.setContentType(chainContentType);
				}
				super.doFilter(request, response);
			}
		};
	}

	boolean disableInRepsonse;
	
	String userAgent = "Mozilla 72.2";
	String chainContentType;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		initParams.put(ContentTypeFilter.PARAM_DEFAULT_CONTENT_TYPE, DEFAULT_TYPE);
		initParams.put(ContentTypeFilter.PARAM_LEGACY_CONTENT_TYPE, LEGACY_TYPE);
		initParams.put(ContentTypeFilter.PARAM_DETECT_BROWSER, "true");
		initParams.put(ContentTypeFilter.PARAM_SUPPRESS_CONTENT_TYPES, "text/supressable");
		
		chainContentType = "text/supressable";
		
		filter.init(filterConfig);
	}
	
	public void testNoInitParam() throws Exception {
		initParams.clear();
		
		try {
			filter.init(filterConfig);
			fail("expected exception");
		} catch (ServletException e) {
		}
	}
	
	public void testDefault() throws Exception {
		check(DEFAULT_TYPE);
	}

	public void testSetsParamDetectsIE() throws Exception {
		userAgent = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.1.4322)";
		
		check(LEGACY_TYPE);
		
		assertEquals(Boolean.TRUE, request.getAttribute(Keys.BROWSER_IE));
	}

	public void testDetectsIE() throws Exception {
		userAgent = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.1.4322)";
		
		check(LEGACY_TYPE);
	}
	
	/* (Previous comment: Presumably, Microsoft will fix this problem in versions 7 and later of IE.) 
	 * Nope, they didn't.
	 */
	public void testDetectsIEVersion() throws Exception {
		userAgent = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; .NET CLR 1.1.4322)";
		
		check(LEGACY_TYPE);
	}

	public void testChainCanOverrideContentType() throws Exception {
		chainContentType = "application/x-custom";
		
		check(chainContentType);
	}

	public void testSuppressContentType() throws Exception {
		initParams.put(ContentTypeFilter.PARAM_SUPPRESS_CONTENT_TYPES, "text/goofy\n , \r\n 	text/other");
		filter.init(filterConfig);

		chainContentType = "text/goofy";
		
		check(DEFAULT_TYPE);
	}

	public void testSuppressContentType2() throws Exception {
		initParams.put(ContentTypeFilter.PARAM_SUPPRESS_CONTENT_TYPES, "text/goofy,text/other,");
		filter.init(filterConfig);

		chainContentType = "text/other";
		
		check(DEFAULT_TYPE);
	}
	
	public void testNotSuppressContentType() throws Exception {
		initParams.put(ContentTypeFilter.PARAM_SUPPRESS_CONTENT_TYPES, "text/goofy,text/other,,");
		filter.init(filterConfig);

		chainContentType = "text/more";
		
		check(chainContentType);
	}
	
	public void testSuppressContentTypeOverrideInResponse() throws Exception {
		disableInRepsonse = true;
		initParams.put(ContentTypeFilter.PARAM_SUPPRESS_CONTENT_TYPES, "text/goofy,text/other,");
		filter.init(filterConfig);

		chainContentType = "text/other";
		
		check("text/other");
	}

	private void check(String expectedType) throws ServletException, IOException {
		assertFalse(chain.doFilterCalled());
		
		filter.doFilter(request, response, chain);
		
		assertTrue(chain.doFilterCalled());
		
		assertEquals(expectedType, response.getContentType());
	}
}
