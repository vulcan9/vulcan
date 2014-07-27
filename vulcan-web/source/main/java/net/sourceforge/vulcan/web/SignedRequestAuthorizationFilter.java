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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public class SignedRequestAuthorizationFilter extends OncePerRequestFilter {
	private Log log = LogFactory.getLog(getClass());
	
	private String algorithm = "HmacMD5";
	private String signatureHeaderName;
	private String sharedSecret;
	private String principalParameterName;
	
	private SecretKey secretKey;
	
	public SignedRequestAuthorizationFilter() {
		super.addRequiredProperty("signatureHeaderName");
		super.addRequiredProperty("sharedSecret");
	}
	
	@Override
	protected void initFilterBean() throws ServletException {
		if (StringUtils.isBlank(sharedSecret)) {
			secretKey = null;
			return;
		}
		
		try {
			secretKey = new SecretKeySpec(sharedSecret.getBytes(), algorithm);
			
			// Initialize a mac instance to fail fast on NoSuchAlgorithmException or InvalidKeyException.
			// This way any configuration errors will prevent the application from starting instead of causing
			// problems later.
			final Mac mac = Mac.getInstance(algorithm);
			
			mac.init(secretKey);
		} catch (NoSuchAlgorithmException e) {
			throw new ServletException(e);
		} catch (InvalidKeyException e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest request,	HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
		if (!validate(request, secretKey)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		request = decorateRequestWithFormData(request);
		request = decorateRequestWithPrincipal(request);
		
		chain.doFilter(request, response);
	}

	protected HttpServletRequest decorateRequestWithFormData(HttpServletRequest request) {
		return new ParameterParsingHttpServletRequestWrapper(request);
	}

	protected HttpServletRequest decorateRequestWithPrincipal(HttpServletRequest request) {
		Principal principal = request.getUserPrincipal();
		if (StringUtils.isBlank(principalParameterName) || principal != null) {
			return request;
		}
		
		final String requestBy = request.getParameter(principalParameterName);
		if (StringUtils.isBlank(requestBy)) {
			return request;
		}
		
		return new HttpServletRequestWrapper(request) {
			@Override
			public Principal getUserPrincipal() {
				return new SignedRequestPrincipal(requestBy);
			}
		};
	}
	
	protected boolean validate(HttpServletRequest request, SecretKey secretKey) throws IOException, ServletException {
		if (secretKey == null) {
			log.warn("Forbidding access to " + request.getContextPath() + " because no shared secret is configured in web.xml");
			return false;
		}
		
		final String signature = request.getHeader(signatureHeaderName);
		
		if (StringUtils.isBlank(signature)) {
			log.warn("Forbidding access to " + request.getContextPath() + " because request does not contain header " + signatureHeaderName);
			return false;
		}

		BufferedReader reader = request.getReader();
		reader.mark(8192);
		byte[] buffer = IOUtils.toByteArray(reader);
		reader.reset();

		final String resultStr = hashRequestBody(buffer, secretKey);

		if (!resultStr.equals(signature)) {
			log.warn("Forbidding access to " + request.getContextPath() + " because signature does not match request body");
			return false;
		}
		
		return true;
	}

	protected String hashRequestBody(byte[] buffer, SecretKey secretKey) throws IOException, ServletException {
		final byte[] result;
		try {
			final Mac mac = Mac.getInstance(algorithm);
			mac.init(secretKey);
			result = mac.doFinal(buffer);
		} catch (NoSuchAlgorithmException e) {
			throw new ServletException(e);
		} catch (InvalidKeyException e) {
			throw new ServletException(e);
		}

		return new String(Hex.encodeHex(result));
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}
	
	public void setSignatureHeaderName(String signatureHeaderName) {
		this.signatureHeaderName = signatureHeaderName;
	}
	
	public void setSharedSecret(String sharedSecret) {
		this.sharedSecret = sharedSecret;
	}
	
	public void setPrincipalParameterName(String principalParameterName) {
		this.principalParameterName = principalParameterName;
	}
	
	protected SecretKey getSecretKey() {
		return secretKey;
	}
	
	static class SignedRequestPrincipal implements Principal {
		private final String name;
		
		public SignedRequestPrincipal(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
