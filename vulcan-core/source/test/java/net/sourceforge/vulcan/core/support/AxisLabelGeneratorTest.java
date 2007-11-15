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
package net.sourceforge.vulcan.core.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.transform.Transformer;

import junit.framework.TestCase;
import net.sourceforge.vulcan.core.support.AbstractProjectDomBuilder.AxisLabel;
import net.sourceforge.vulcan.exception.NoSuchTransformFormatException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.time.DateUtils;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class AxisLabelGeneratorTest extends TestCase {
	private AbstractProjectDomBuilder domBuilder = new AbstractProjectDomBuilder() {
		@Override
		protected Transformer createTransformer(String format)
				throws NoSuchTransformFormatException {
			return null;
		}
		@Override
		protected String formatMessage(String key, Object[] args, Locale locale) {
			final String mkey = locale + ":" + key;
			if (fakeDateFormats.containsKey(mkey)) {
				return fakeDateFormats.get(mkey);
			}
			fail("No such key " + mkey);
			return null;
		}
	};
	private AbstractProjectDomBuilder.AxisLabelGenerator gen;
	private Calendar cal = new GregorianCalendar(2001, 0, 6, 11, 34);
	
	private Map<String,String> fakeDateFormats = new HashMap<String,String>();
	
	private Locale locale = Locale.US;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		cal.set(Calendar.MONTH, Calendar.OCTOBER);
		
		fakeDateFormats.put("en_US:axis.by.month", "MMM yyyy");
		fakeDateFormats.put("en_US:axis.by.week", "MMM d");
		fakeDateFormats.put("en_US:axis.by.day", "EEE MMM d");
	}
	
	// if delta ~ 3 days [Oct 1, 6:00 AM, 12:00 PM, 18:00 AM, Oct 2, 6:00 AM ...]

	public void testMinMaxThreeMonths() throws Exception {
		setParams(DateUtils.MILLIS_PER_DAY * 90);
	
		final Calendar c = DateUtils.truncate(cal, Calendar.MONTH);
		assertEquals(c.getTimeInMillis(), gen.getMin());
		
		c.add(Calendar.MONTH, 4);
		
		assertEquals(c.getTimeInMillis(), gen.getMax());
	}

	public void testLabelThreeMonths() throws Exception {
		assertLabels(DateUtils.MILLIS_PER_DAY * 90, "Oct 2001", "Nov 2001", "Dec 2001", "Jan 2002");
	}
	
	public void testLabelThreeMonthsAlternateLocale() throws Exception {
		fakeDateFormats.put("fr:axis.by.month", "MMM yyyy");
		locale = Locale.FRENCH;
		assertLabels(DateUtils.MILLIS_PER_DAY * 90, "oct. 2001", "nov. 2001", "déc. 2001", "janv. 2002");
	}
	
	public void testLabelThreeWeeks() throws Exception {
		assertLabels(DateUtils.MILLIS_PER_DAY * 20, "Oct 6", "Oct 13", "Oct 20");
	}
	
	public void testLabelFiveWeeks() throws Exception {
		assertLabels(DateUtils.MILLIS_PER_DAY * 35, "Oct 6", "Oct 13", "Oct 20", "Oct 27", "Nov 3", "Nov 10");
	}
	
	public void testLabelNineDays() throws Exception {
		assertLabels(DateUtils.MILLIS_PER_DAY * 9, "Sat Oct 6", "Sun Oct 7", "Mon Oct 8", "Tue Oct 9",
				"Wed Oct 10", "Thu Oct 11", "Fri Oct 12", "Sat Oct 13", "Sun Oct 14", "Mon Oct 15");
	}
	
	private void assertLabels(long delta, String... expectedLabels) {
		setParams(delta);
		
		final List<AxisLabel> axisLabels = gen.getLabels();
		final List<String> labels = new ArrayList<String>();
		
		for (AxisLabel axisLabel : axisLabels) {
			labels.add(axisLabel.getLabel());
		}
		assertEquals(Arrays.asList(expectedLabels), labels);
	}

	private void setParams(long delta) {
		gen = domBuilder.createAxisLabelGenerator(cal.getTimeInMillis(), cal.getTimeInMillis()+delta, locale);
	}
}
