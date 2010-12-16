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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.StateManager;

import org.apache.commons.lang.time.DateUtils;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.mock.MockPageContext;
import org.springframework.web.context.WebApplicationContext;

import servletunit.HttpServletRequestSimulator;
import servletunit.ServletContextSimulator;

public class JstlFunctionsTest extends EasyMockTestCase {
	final HttpServletRequestSimulator request = new HttpServletRequestSimulator(new ServletContextSimulator());
	final WebApplicationContext wac = createStrictMock(WebApplicationContext.class);
	final StateManager stateManager = createStrictMock(StateManager.class);
	
	final PageContext pageContext = new MockPageContext() {
		@Override
		public ServletRequest getRequest() {
			return JstlFunctionsTest.this.request;
		}
	};;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		JstlFunctions.setWebApplicationContext(wac);
	}
	public void testMangleSpace() throws Exception {
		assertEquals("full_throttle", JstlFunctions.mangle("full throttle"));
	}
	public void testManglePlus() throws Exception {
		assertEquals("full___throttle", JstlFunctions.mangle("full + throttle"));
	}
	public void testMangleDos() throws Exception {
		assertEquals("full_throttle", JstlFunctions.mangle("full.throttle"));
	}
	public void testBrackets() throws Exception {
		assertEquals("fullThrottle_0_", JstlFunctions.mangle("fullThrottle[0]"));
	}
	public void testMangleLtGtNpsb() throws Exception {
		assertEquals("_rocky___bullwinkle_", JstlFunctions.mangle("<rocky & bullwinkle>"));
	}
	public void testGetPropList() throws Exception {
		ActionErrors errors = new ActionErrors();
		errors.add("a", new ActionMessage("foo"));
		errors.add("a", new ActionMessage("bar"));
		errors.add("b", new ActionMessage("baz"));
		
		request.setAttribute(Globals.ERROR_KEY, errors);
		
		assertEquals(Arrays.asList("a", "b"), JstlFunctions.getActionErrorPropertyList(request));
	}
	public void testGetPropListNull() throws Exception {
		assertEquals(Collections.emptyList(), JstlFunctions.getActionErrorPropertyList(request));
	}
	public void testEncode() throws Exception {
		assertEquals("a%20b%3F", JstlFunctions.encode("a b?"));
	}
	public void testElapsedTimeSeconds() throws Exception {
		expect(wac.getMessage("time.seconds", null, request.getLocale())).andReturn("seconds");
		replay();
		
		assertEquals("35 seconds", JstlFunctions.formatElapsedTime(pageContext, 35697, 2));
		
		verify();
	}
	public void testElapsedTimeMinutesAndSeconds() throws Exception {
		expect(wac.getMessage("time.minute", null, request.getLocale())).andReturn("minute");
		expect(wac.getMessage("time.second", null, request.getLocale())).andReturn("second");
		replay();
		
		assertEquals("1 minute, 1 second", JstlFunctions.formatElapsedTime(pageContext, 61297, 2));
		
		verify();
	}
	public void testElapsedTimeMinutesAndZeroSeconds() throws Exception {
		expect(wac.getMessage("time.minutes", null, request.getLocale())).andReturn("minutes");
		replay();
		
		assertEquals("2 minutes", JstlFunctions.formatElapsedTime(pageContext, 120297, 2));
		
		verify();
	}
	public void testElapsedTimeHoursAndMinutes() throws Exception {
		expect(wac.getMessage("time.hour", null, request.getLocale())).andReturn("hour");
		expect(wac.getMessage("time.minutes", null, request.getLocale())).andReturn("minutes");
		replay();
		
		assertEquals("1 hour, 2 minutes", JstlFunctions.formatElapsedTime(pageContext, DateUtils.MILLIS_PER_HOUR + 120297, 2));
		
		verify();
	}
	public void testElapsedTimeStopsAfterTwoUnits() throws Exception {
		expect(wac.getMessage("time.hour", null, request.getLocale())).andReturn("hour");
		expect(wac.getMessage("time.minutes", null, request.getLocale())).andReturn("minutes");
		replay();
		
		assertEquals("1 hour, 2 minutes", JstlFunctions.formatElapsedTime(pageContext, DateUtils.MILLIS_PER_HOUR + 122297, 2));
		
		verify();
	}
	public void testElapsedTimeDayAndHours() throws Exception {
		expect(wac.getMessage("time.days", null, request.getLocale())).andReturn("days");
		expect(wac.getMessage("time.hours", null, request.getLocale())).andReturn("hours");
		replay();
		
		assertEquals("3 days, 12 hours", JstlFunctions.formatElapsedTime(pageContext, DateUtils.MILLIS_PER_DAY * 3 + DateUtils.MILLIS_PER_HOUR * 12, 2));
		
		verify();
	}
	public void testElapsedTimeDayHoursMinutesAndSeconds() throws Exception {
		expect(wac.getMessage("time.days", null, request.getLocale())).andReturn("days");
		expect(wac.getMessage("time.hours", null, request.getLocale())).andReturn("hours");
		expect(wac.getMessage("time.minute", null, request.getLocale())).andReturn("minute");
		expect(wac.getMessage("time.seconds", null, request.getLocale())).andReturn("seconds");

		replay();
		
		assertEquals("3 days, 12 hours, 1 minute, 4 seconds", JstlFunctions.formatElapsedTime(pageContext, DateUtils.MILLIS_PER_DAY * 3 + DateUtils.MILLIS_PER_HOUR * 12 + DateUtils.MILLIS_PER_MINUTE + 4500, 4));
		
		verify();
	}
	public void testElapsedTimeDayAndHoursOne() throws Exception {
		expect(wac.getMessage("time.days", null, request.getLocale())).andReturn("days");
		replay();
		
		assertEquals("3 days", JstlFunctions.formatElapsedTime(pageContext, DateUtils.MILLIS_PER_DAY * 3 + DateUtils.MILLIS_PER_HOUR * 12, 1));
		
		verify();
	}
	public void testElapsedTimeDayAndZeroHours() throws Exception {
		expect(wac.getMessage("time.days", null, request.getLocale())).andReturn("days");
		expect(wac.getMessage("time.minutes", null, request.getLocale())).andReturn("minutes");
		replay();
		
		assertEquals("3 days, 29 minutes", JstlFunctions.formatElapsedTime(pageContext, DateUtils.MILLIS_PER_DAY * 3 + DateUtils.MILLIS_PER_MINUTE * 29, 2));
		
		verify();
	}
	public void testGetProjectNamesByLabelsNullLabels() throws Exception {
		expect(wac.getBean(Keys.STATE_MANAGER, StateManager.class)).andReturn(stateManager);
		
		final ArrayList<String> list = new ArrayList<String>();
		expect(stateManager.getProjectConfigNames()).andReturn(list);
		
		replay();
		
		assertSame(list, JstlFunctions.getProjectNamesByLabels(null));
		
		verify();
	}
	public void testGetProjectNamesByLabelsEmpty() throws Exception {
		expect(wac.getBean(Keys.STATE_MANAGER, StateManager.class)).andReturn(stateManager);
		
		final ArrayList<String> list = new ArrayList<String>();
		expect(stateManager.getProjectConfigNames()).andReturn(list);
		
		replay();
		
		assertSame(list, JstlFunctions.getProjectNamesByLabels(new String[0]));
		
		verify();
	}
	public void testGetProjectNamesByLabels() throws Exception {
		expect(wac.getBean(Keys.STATE_MANAGER, StateManager.class)).andReturn(stateManager);
		
		final List<String> one = Arrays.asList("a", "c");
		expect(stateManager.getProjectConfigNamesByLabel("x")).andReturn(one);
		final List<String> two = Arrays.asList("b", "c");
		expect(stateManager.getProjectConfigNamesByLabel("y")).andReturn(two);
		
		replay();
		
		assertEquals(Arrays.asList("a", "b", "c"), JstlFunctions.getProjectNamesByLabels(new String[] {"x", "y"}));
		
		verify();
	}
}
