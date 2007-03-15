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

import java.util.List;

import javax.xml.transform.TransformerFactory;

import junit.framework.TestCase;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventHandler;

import org.jdom.Document;
import org.jdom.Element;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public abstract class TransformTestCase extends TestCase {

	protected final XmlMetricsPlugin plugin = new XmlMetricsPlugin();
	protected final Document doc = new Document(new Element("merged-root"));

	@Override
	public void setUp() throws Exception {
		super.setUp();
		plugin.setEventHandler(new Handler());
		plugin.setTransfomSources(new Resource[] {
				new ClassPathResource("net/sourceforge/vulcan/metrics/resources/unit-test.xsl"),
				new ClassPathResource("net/sourceforge/vulcan/metrics/resources/code-coverage.xsl"),
				new ClassPathResource("net/sourceforge/vulcan/metrics/resources/static-analysis.xsl"),
				new ClassPathResource("net/sourceforge/vulcan/metrics/resources/identity.xsl")});
		
		plugin.setTransformerFactory(TransformerFactory.newInstance());
		
		plugin.init();
	}

	protected static void assertContainsMetric(Document doc, String key, String value, boolean uniqueKey) {
		assertEquals("metrics", doc.getRootElement().getName());
		
		final List<Element> children = getChildren(doc);
		for (Element e : children) {
			if (key.equals(e.getAttributeValue("key"))) {
				if (value.equals(e.getAttributeValue("value"))) {
					return;
				}
				if (uniqueKey) {
					assertEquals("<metric key='" + key + "'/> value ", value, e.getAttributeValue("value"));
				}
			}
		}
		
		fail("Did not find <metric key='" + key + "' value='" + value + "'/>");
	}
	protected static void assertContainsTestFailure(Document doc, String name) {
		final List<Element> children = getChildren(doc);
		for (Element e : children) {
			if (name.equals(e.getText())) {
				return;
			}
		}
		
		fail("Did not find <test-failure>" + name + "</test-failure>");
	}
	@SuppressWarnings("unchecked")
	private static List<Element> getChildren(Document doc) {
		final List<Element> children = doc.getRootElement().getChildren();
		return children;
	}
	
	class Handler implements EventHandler {
		public void reportEvent(Event e) {
			if (e instanceof ErrorEvent) {
				throw new Error(((ErrorEvent)e).getError());
			}
			
		}
	}
}
