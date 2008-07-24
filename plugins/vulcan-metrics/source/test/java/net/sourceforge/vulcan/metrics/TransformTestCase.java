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

import java.io.IOException;
import java.util.List;

import javax.xml.transform.TransformerFactory;

import junit.framework.TestCase;
import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventHandler;

import org.jdom.Document;
import org.jdom.Element;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

public abstract class TransformTestCase extends TestCase implements ResourcePatternResolver {

	protected final XmlMetricsPlugin plugin = new XmlMetricsPlugin();
	protected final Document doc = new Document(new Element("merged-root"));

	@Override
	public void setUp() throws Exception {
		super.setUp();
		plugin.setEventHandler(new Handler());
		plugin.setTransfomSourcePath("source/main/config/xsl/*.xsl");
		plugin.setResourceResolver(this);
		plugin.setTransformerFactory(TransformerFactory.newInstance());
		
		plugin.init();
	}

	public Resource getResource(String filename) {
		return new FileSystemResource(TestUtils.resolveRelativeFile("source/main/config/xsl/" + filename));
	}
	
	public Resource[] getResources(String pattern) throws IOException {
		assertEquals("source/main/config/xsl/*.xsl", pattern);
		
		return new Resource[] {
				 getResource("unit-test.xsl"),
				 getResource("code-coverage.xsl"),
				 getResource("static-analysis.xsl"),
				 getResource("identity.xsl")
		};
	}

	protected static void assertContainsMetric(Document doc, String key, MetricType type, String value, boolean uniqueKey) {
		assertEquals("metrics", doc.getRootElement().getName());
		
		final List<Element> children = getChildren(doc);
		for (Element e : children) {
			if (key.equals(e.getAttributeValue("key"))) {
				if (uniqueKey) {
					if (type != null) {
						assertEquals("<metric key='" + key + "'/> type ", type.name().toLowerCase(), e.getAttributeValue("type").toLowerCase());
					}
					assertEquals("<metric key='" + key + "'/> value ", value, e.getAttributeValue("value"));
					return;
				} else if (value.equals(e.getAttributeValue("value"))) {
					return;
				}
			}
		}
		
		fail("Did not find <metric key='" + key + "' type='" + type + "' value='" + value + "'/>");
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
