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
package net.sourceforge.vulcan.metrics;

import org.jdom.Document;
import org.jdom.Element;

public class IdentityTransformTest extends TransformTestCase {
	final Element report = new Element("report");
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		doc.getRootElement().addContent(report);
	}

	public void testIdentity() throws Exception {
		final Element metric = new Element("metric");
		
		final String key = "a metric";
		final String value = "eleventy-billion";
		
		metric.setAttribute("key", key);
		metric.setAttribute("value", value);
		
		report.addContent(metric);
		
		final Document t = plugin.transform(doc);
		
		assertEquals(1, t.getRootElement().getContentSize());
		
		final Element transformedMetric = t.getRootElement().getChild("metric");
		assertEquals(key, transformedMetric.getAttributeValue("key"));
		assertEquals(value, transformedMetric.getAttributeValue("value"));
	}
	public void testSkipsOnMissingKey() throws Exception {
		final Element metric = new Element("metric");
		
		final String value = "eleventy-billion";
		
		metric.setAttribute("value", value);
		
		report.addContent(metric);
		
		final Document t = plugin.transform(doc);
		
		assertEquals(0, t.getRootElement().getContentSize());
	}
	public void testSkipsOnMissingValue() throws Exception {
		final Element metric = new Element("metric");
		
		final String key = "k";
		
		metric.setAttribute("key", key);
		
		report.addContent(metric);
		
		final Document t = plugin.transform(doc);
		
		assertEquals(0, t.getRootElement().getContentSize());
	}
}