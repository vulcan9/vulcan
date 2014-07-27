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
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * When HttpServletRequest.getReader() or getInputStream() is invoked,
 * the default behavior of parsing x-www-form-urlencoded form data
 * into parameters is skipped.
 * 
 * When any variant of HttpServletRequest.getParameter() is invoked,
 * attempting to read from getReader() or getInputStream() will read
 * zero bytes.
 * 
 * This class allows getReader() / getInputStream() to be used and still
 * parse form data into parameters.
 */
public class ParameterParsingHttpServletRequestWrapper extends HttpServletRequestWrapper {
	private MultiMap parameterMultiMap;
	private Map<String, String> parameterMap;
	
	public ParameterParsingHttpServletRequestWrapper(HttpServletRequest delegate) {
		super(delegate);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Enumeration getParameterNames() {
		getParameterMap();
		return new IteratorEnumeration(parameterMultiMap.keySet().iterator());
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public String[] getParameterValues(String name) {
		getParameterMap();
		List<String> values = (List<String>)parameterMultiMap.get(name);

		if (values == null) {
			return null;
		}
		
		return (String[])values.toArray(new String[values.size()]);
	}
	
	@Override
	public String getParameter(String name) {
		String[] values = getParameterValues(name);
		return (values != null && values.length > 0)
				? values[0]
				: null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map getParameterMap() {
		if (parameterMap != null) {
			return parameterMap;
		}
		
		parameterMap = new HashMap<String, String>(super.getParameterMap());
		parameterMultiMap = new MultiHashMap();
		
		for (Enumeration e = super.getParameterNames(); e.hasMoreElements();) {
			String name = (String)e.nextElement();
			for(String v : super.getParameterValues(name)) {
				parameterMultiMap.put(name, v);
			}
		}
		
		if (!StringUtils.equals(getContentType(), "application/x-www-form-urlencoded")) {
			return parameterMultiMap;
		}
		
		String encoding = getCharacterEncoding();
		
		if (StringUtils.isBlank(encoding)) {
			encoding = "UTF-8";
		}
		
		try {
			BufferedReader reader = getReader();
			reader.mark(8192);
			String formData = IOUtils.toString(reader);
			reader.reset();
			
			for (String kvp : formData.split("&")) {
				String[] kv = kvp.split("=");
				String key = URLDecoder.decode(kv[0], encoding);
				String value = kv.length > 1 ? URLDecoder.decode(kv[1], encoding) : StringUtils.EMPTY;
				parameterMultiMap.put(key, value);
				if (!parameterMap.containsKey(key)) {
					parameterMap.put(key,  value);
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		
		return parameterMap;
	}
	
}