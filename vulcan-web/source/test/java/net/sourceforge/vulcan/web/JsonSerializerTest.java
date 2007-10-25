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

import java.util.Locale;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.spring.jdbc.JdbcMetricDto;

import org.springframework.context.MessageSource;

public class JsonSerializerTest extends EasyMockTestCase {
	JsonSerializer ser;
	MessageSource messageSource = createStrictMock(MessageSource.class);
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		ser = new JsonSerializer();
		ser.setMessageSource(messageSource);
	}
	
	public void testSerializeMetric() throws Exception {
		final MetricDto m = new MetricDto();
		m.setMessageKey("a.key");
		m.setValue("45");
		
		final Locale locale = Locale.CANADA_FRENCH;
		
		ser.setMessageSource(messageSource);
		expect(messageSource.getMessage("a.key", null, locale)).andReturn("Some Label");
		
		replay();
		
		String s = ser.toJSON(m, locale);
		
		verify();
		
		assertEquals("[{\"value\":\"45\",\"label\":\"Some Label\",\"key\":\"a.key\"}]", s);
	}
	
	public void testSerializeMetricSubclass() throws Exception {
		final MetricDto m = new JdbcMetricDto();
		m.setMessageKey("a.key");
		m.setValue("45");
		
		final Locale locale = Locale.CANADA_FRENCH;
		
		ser.setMessageSource(messageSource);
		expect(messageSource.getMessage("a.key", null, locale)).andReturn("Some Label");
		
		replay();
		
		String s = ser.toJSON(m, locale);
		
		verify();
		
		assertEquals("[{\"value\":\"45\",\"label\":\"Some Label\",\"key\":\"a.key\"}]", s);
	}
}
