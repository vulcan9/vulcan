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

import java.util.Date;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.processors.JsonBeanProcessor;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.spring.jdbc.JdbcMetricDto;

public class JsonSerializer implements MessageSourceAware {
	private final ThreadLocal<Locale> locale;
	private MessageSource messageSource;
	
	public JsonSerializer() {
		locale = new ThreadLocal<Locale>();

		JsonConfig.getInstance().reset();
		JsonConfig.getInstance().registerJsonBeanProcessor(Date.class, new DateProcessor());
		final MetricProcessor metricProcessor = new MetricProcessor();
		
		JsonConfig.getInstance().registerJsonBeanProcessor(MetricDto.class, metricProcessor);
		JsonConfig.getInstance().registerJsonBeanProcessor(JdbcMetricDto.class, metricProcessor);
	}
	
	class MetricProcessor implements JsonBeanProcessor {
		public JSONObject processBean(Object obj) {
			final Locale locale = JsonSerializer.this.locale.get();
			
			final MetricDto metric = (MetricDto)obj;
			final JSONObject jobj = new JSONObject();
			
			jobj.accumulate("value", metric.getValue());
			jobj.accumulate("key", metric.getMessageKey());
			jobj.accumulate("label", messageSource.getMessage(metric.getMessageKey(), null, locale));
			
			return jobj;
		}
	}
	
	class DateProcessor implements JsonBeanProcessor {
		public JSONObject processBean(Object obj) {
			final JSONObject jobj = new JSONObject();
			jobj.accumulate("time", ((Date)obj).getTime());
			
			return jobj;
		}
	}
	
	public String toJSON(Object o, Locale locale) {
		this.locale.set(locale);
		
		try {
			return JSONArray.fromObject(o).toString();
		} finally {
			this.locale.set(null);
		}
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
}
