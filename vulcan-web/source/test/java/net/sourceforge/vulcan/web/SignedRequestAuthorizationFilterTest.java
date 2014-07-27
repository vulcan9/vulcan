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
import java.security.Principal;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.SimplePrincipal;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;


public class SignedRequestAuthorizationFilterTest extends EasyMockTestCase {
	HttpServletRequest request = createStrictMock(HttpServletRequest.class);
	HttpServletResponse response = createStrictMock(HttpServletResponse.class);
	FilterChain chain = createStrictMock(FilterChain.class);
	
	SecretKey secretKey;
	
	boolean validateFlag;
	
	SignedRequestAuthorizationFilter fakeFilter = new SignedRequestAuthorizationFilter() {
		@Override
		protected boolean validate(HttpServletRequest request, SecretKey secretKey) {
			return validateFlag;
		}
		
		protected HttpServletRequest decorateRequestWithFormData(HttpServletRequest request) {
			return request;
		};
	};
	
	SignedRequestAuthorizationFilter filter = new SignedRequestAuthorizationFilter();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		secretKey = new SecretKeySpec("my secret".getBytes(), "HmacMD5");
		
		checkOrder(false);
		
		expect(request.getContextPath()).andReturn("/example").anyTimes();
	}
	
	public void testInit() throws Exception {
		filter.setSharedSecret("secret");
		
		filter.initFilterBean();
		
		assertNotNull("secret key", filter.getSecretKey());
	}
	
	public void testInitBlankSecret() throws Exception {
		filter.setSharedSecret("");
		
		filter.initFilterBean();
		
		assertEquals("secrey key", null, filter.getSecretKey());
	}
	
	public void testInitThrowsOnInvalidAlgorith() throws Exception {
		filter.setSharedSecret("my secret");
		filter.setAlgorithm("nonesuch");
		
		try {
			filter.initFilterBean();
			fail("expected exception");
		} catch (ServletException e) {
		}
	}
	
	public void testDelegatesWhenValid() throws Exception {
		validateFlag = true;
		fakeFilter.setPrincipalParameterName("user");
		
		setUpRequestBody("");
		expect(request.getUserPrincipal()).andReturn(new SimplePrincipal()).atLeastOnce();
		
		chain.doFilter(matchRequestWithPrincipal(request, "testuser"), eq(response));
		
		replay();
		
		fakeFilter.doFilterInternal(request, response, chain);
		
		verify();
	}

	public void testDelegateSetsPrincipal() throws Exception {
		validateFlag = true;
		fakeFilter.setPrincipalParameterName("requestBy");
		
		setUpRequestBody("");
		expect(request.getUserPrincipal()).andReturn(null);
		expect(request.getParameter("requestBy")).andReturn("someone");
		
		chain.doFilter(matchRequestWithPrincipal(request, "someone"), eq(response));
		
		replay();
		
		fakeFilter.doFilterInternal(request, response, chain);
		
		verify();
	}
	
	public void testDelegateDoesNotSetPrincipalWhenNotConfigured() throws Exception {
		validateFlag = true;
		fakeFilter.setPrincipalParameterName("");
		
		setUpRequestBody("");
		expect(request.getUserPrincipal()).andReturn(null).atLeastOnce();
		
		chain.doFilter(matchRequestWithPrincipal(request, null), eq(response));
		
		replay();
		
		fakeFilter.doFilterInternal(request, response, chain);
		
		verify();
	}
	
	public void testDelegateDoesNotSetPrincipalWhenNotSubmitted() throws Exception {
		validateFlag = true;
		fakeFilter.setPrincipalParameterName("user");
		
		setUpRequestBody("");
		expect(request.getUserPrincipal()).andReturn(null).atLeastOnce();
		expect(request.getParameter("user")).andReturn(null);
		
		chain.doFilter(matchRequestWithPrincipal(request, null), eq(response));
		
		replay();
		
		fakeFilter.doFilterInternal(request, response, chain);
		
		verify();
	}
	
	public void testSendsForbiddenWhenInvalid() throws Exception {
		validateFlag = false;
		
		setUpRequestBody("does not validate");
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
		
		replay();
		
		fakeFilter.doFilterInternal(request, response, chain);
		
		verify();
	}
	
	public void testInvalidWhenNoSharedSecret() throws Exception {
		replay();
		
		assertEquals("isValid", false, filter.validate(request, null));
		
		verify();
	}
	
	public void testInvalidWhenHeaderNotSet() throws Exception {
		filter.setSignatureHeaderName("X-hmac");
		
		expect(request.getHeader("X-hmac")).andReturn(null);
		
		replay();
		
		assertEquals("isValid", false, filter.validate(request, secretKey));
		
		verify();
	}
	
	public void testInvalidWhenSignatureIsNotMatch() throws Exception {
		filter.setSignatureHeaderName("X-hmac");
		
		expect(request.getHeader("X-hmac")).andReturn("invalid");
		expect(request.getReader()).andReturn(new BufferedReader(new StringReader("body")));
		replay();
		
		assertEquals("isValid", false, filter.validate(request, secretKey));
		
		verify();
	}
	
	public void testValidateResetsReader() throws Exception {
		filter.setSignatureHeaderName("X-hmac");
		BufferedReader reader = new BufferedReader(new StringReader("body"));
		
		expect(request.getHeader("X-hmac")).andReturn("invalid");
		expect(request.getReader()).andReturn(reader);
		replay();
		
		filter.validate(request, secretKey);
		
		verify();
		
		assertEquals("body", IOUtils.toString(reader));
	}
	
	public void testHashRequestBody() throws Exception {
		replay();
		
		assertEquals("14f2baafbf2441af0d057b1aa1db012a", filter.hashRequestBody("the body".getBytes(), secretKey));
		
		verify();
	}

	protected void setUpRequestBody(String body) throws IOException {
	}

	private static HttpServletRequest matchRequestWithPrincipal(final HttpServletRequest expected, final String principalName) {
		EasyMock.reportMatcher(new IArgumentMatcher() {
			private String message;
			
			public boolean matches(Object argument) {
				if (!(argument instanceof HttpServletRequest)) {
					message = "instanceof HttpServletRequest";
					return false;
				}
				
				final HttpServletRequest w = (HttpServletRequest) argument;
				
				final Principal userPrincipal = w.getUserPrincipal();
				final String actualName = userPrincipal != null ? userPrincipal.getName() : null;
				if (!StringUtils.equals(actualName, principalName)) {
					message = "userPrincipal " + principalName;
					return false;
				}
				
				return true;
			}
			
			public void appendTo(StringBuffer buffer) {
				buffer.append(message);
			}
			
		});
		return null;
	}
}
