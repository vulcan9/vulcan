/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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
package net.sourceforge.vulcan.spring;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import junit.framework.TestCase;
import net.sourceforge.vulcan.metadata.Transient;
import net.sourceforge.vulcan.spring.SpringBeanXmlEncoderTest.Bean.Status;

import org.jdom.Element;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.FieldRetrievingFactoryBean;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;


public class SpringBeanXmlEncoderTest extends TestCase {
	SpringBeanXmlEncoder enc = new SpringBeanXmlEncoder();
	
	public static class Prim {
		private boolean flag;
		
		public boolean isFlag() {
			return flag;
		}
		public void setFlag(boolean flag) {
			this.flag = flag;
		}
	}
	public static class Bean {
		public enum Status { ACTIVE, PASSIVE {} };
		
		private String value;
		private File filename;
		private Bean innerBean;
		private List<?> children;
		private Set<?> set;
		private Map<?,?> map;
		private Status status;
		private Integer trans;
		private UUID uuid;
		
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public File getFilename() {
			return filename;
		}
		public void setFilename(File filename) {
			this.filename = filename;
		}
		public Bean getInnerBean() {
			return innerBean;
		}
		public void setInnerBean(Bean innerBean) {
			this.innerBean = innerBean;
		}
		public List<?> getChildren() {
			return children;
		}
		public void setChildren(List<?> children) {
			this.children = children;
		}
		public Set<?> getSet() {
			return set;
		}
		public void setSet(Set<?> set) {
			this.set = set;
		}
		public Map<?,?> getMap() {
			return map;
		}
		public void setMap(Map<?,?> map) {
			this.map = map;
		}
		public Status getStatus() {
			return status;
		}
		public void setStatus(Status status) {
			this.status = status;
		}
		@Transient
		public Integer getTrans() {
			return trans;
		}
		public void setTrans(Integer trans) {
			this.trans = trans;
		}
		public UUID getUuid() {
			return uuid;
		}
		public void setUuid(UUID uuid) {
			this.uuid = uuid;
		}
	}
	public static class Array {
		private String[] values;
		public String[] getValues() {
			return values;
		}
		public void setValues(String[] values) {
			this.values = values;
		}
	}
	public void testCreatesBeanAsRoot() {
		assertNotNull(enc.root);
		assertEquals("beans", enc.root.getName());
	}
	public void testSimple() {
		final Element root = new Element("beans");
		
		Bean s = new Bean();
		s.setValue("a value");
		
		assertEquals(0, root.getContentSize());
		
		enc.encodeBean(root, "mySimple", s);
		
		assertEquals(1, root.getContentSize());
		
		final Element beanNode = root.getChild("bean");
		assertNotNull("wrong element type", beanNode);
		assertEquals("mySimple", beanNode.getAttributeValue("name"));
		assertEquals(s.getClass().getName(), beanNode.getAttributeValue("class"));
		
		assertEquals(1, beanNode.getContentSize());
		
		final Element propNode = beanNode.getChild("property");
		assertNotNull("wrong element type", propNode);
		assertPropertyValue(propNode, "value", s.getValue());

	}
	public void testUUID() {
		final Element root = new Element("beans");
		
		Bean s = new Bean();
		s.setUuid(UUID.randomUUID());
		
		assertEquals(0, root.getContentSize());
		
		enc.encodeBean(root, "mySimple", s);
		
		assertEquals(1, root.getContentSize());
		
		Element beanNode = root.getChild("bean");
		assertNotNull("wrong element type", beanNode);
		assertEquals("mySimple", beanNode.getAttributeValue("name"));
		assertEquals(s.getClass().getName(), beanNode.getAttributeValue("class"));
		
		assertEquals(1, beanNode.getContentSize());
		
		final Element propNode = beanNode.getChild("property");
		assertNotNull("wrong element type", propNode);
		assertEquals("uuid", propNode.getAttributeValue("name"));
		
		assertEquals(1, propNode.getContentSize());
		
		beanNode = propNode.getChild("bean");
		assertNotNull("wrong element type", beanNode);
		assertEquals(UUID.class.getName(), beanNode.getAttributeValue("class"));
		assertEquals("fromString", beanNode.getAttributeValue("factory-method"));
		
		Element argNode = beanNode.getChild("constructor-arg");
		assertEquals("java.lang.String", argNode.getAttributeValue("type"));
		assertEquals(s.getUuid().toString(), argNode.getAttributeValue("value"));
	}
	public void testUUIDInMap() {
		final Element root = new Element("beans");
		
		Bean s = new Bean();
		s.setMap(Collections.singletonMap("a", UUID.randomUUID()));
		
		assertEquals(0, root.getContentSize());
		
		enc.encodeBean(root, "mySimple", s);
		
		assertEquals(1, root.getContentSize());
		
		Element beanNode = root.getChild("bean");
		assertNotNull("wrong element type", beanNode);
		assertEquals("mySimple", beanNode.getAttributeValue("name"));
		assertEquals(s.getClass().getName(), beanNode.getAttributeValue("class"));
		
		assertEquals(1, beanNode.getContentSize());
		
		final Element propNode = beanNode.getChild("property");
		assertNotNull("wrong element type", propNode);
		assertEquals("map", propNode.getAttributeValue("name"));
		
		assertEquals(1, propNode.getContentSize());
		
		final Element entryNode = propNode.getChild("map").getChild("entry");
		
		beanNode = entryNode.getChild("bean");
		assertNotNull("wrong element type", beanNode);
		assertEquals(UUID.class.getName(), beanNode.getAttributeValue("class"));
		assertEquals("fromString", beanNode.getAttributeValue("factory-method"));
		
		Element argNode = beanNode.getChild("constructor-arg");
		assertEquals("java.lang.String", argNode.getAttributeValue("type"));
		assertEquals(s.getMap().get("a").toString(), argNode.getAttributeValue("value"));
	}
	public void testEncodesInnerBean() {
		final Element root = new Element("beans");
		
		Bean s = new Bean();
		s.setValue("a value");
		Bean is = new Bean();
		is.setValue("inner value");
		s.setInnerBean(is);
		
		assertEquals(0, root.getContentSize());
		
		enc.encodeBean(root, "mySimple", s);
		
		assertEquals(1, root.getContentSize());
		
		final List<?> children = root.getChild("bean").getChildren("property");
		
		Element child = (Element) children.get(0);
		assertEquals("innerBean", child.getAttributeValue("name"));
		final Element beanNode = (Element)child.getChildren().get(0);
		
		assertEquals("bean", beanNode.getName());
		assertEquals("inner value", beanNode.getChild("property").getChild("value").getText());
		
		child = (Element) children.get(1);
		assertEquals("value", child.getAttributeValue("name"));

	}
	public void testHandlesPrimitives() {
		Prim p = new Prim();
		p.setFlag(true);
		
		final Element root = new Element("beans");
		enc.encodeBean(root, "mine", p);
		
		assertEquals("true", root.getChild("bean").getChild("property").getChild("value").getText());
	}
	public void testIgnoresTransient() {
		final Bean b = new Bean();
		b.setTrans(new Integer(3));
		
		final Element root = new Element("beans");
		enc.encodeBean(root, "mine", b);
		
		assertNotNull(root.getChild("bean"));
		assertEquals(0, root.getChild("bean").getContentSize());
	}
	public void testHandlesArrays() {
		Array a = new Array();
		a.setValues(new String[] {"a", "2", null, "four"});
		
		final Element root = new Element("beans");
		enc.encodeBean(root, "a", a);
		
		final Element listNode = root.getChild("bean").getChild("property").getChild("list");
		assertNotNull("Did not convert array to <list>", listNode);
		
		final List<?> nodes = listNode.getChildren();
		assertEquals("value", ((Element)nodes.get(0)).getName());
		assertEquals("a", ((Element)nodes.get(0)).getText());

		assertEquals("value", ((Element)nodes.get(1)).getName());
		assertEquals("2", ((Element)nodes.get(1)).getText());

		assertEquals("null", ((Element)nodes.get(2)).getName());
		
		assertEquals("value", ((Element)nodes.get(3)).getName());
		assertEquals("four", ((Element)nodes.get(3)).getText());
	}
	@SuppressWarnings("unchecked")
	public void testHandlesEnum() {
		final Element root = new Element("beans");
		
		Bean s = new Bean();

		s.setStatus(Status.ACTIVE);
		
		enc.encodeBean(root, "bean", s);
		
		assertEquals(1, root.getContentSize());
		
		final Element beanNode = root.getChild("bean");
		assertEquals(s.getClass().getName(), beanNode.getAttributeValue("class"));
		
		assertEquals(1, beanNode.getContentSize());
		
		final Element propNode = beanNode.getChild("property");
		assertNotNull("wrong element type", propNode);
		assertEquals("status", propNode.getAttributeValue("name"));
		
		assertEquals(1, propNode.getContentSize());
		
		final Element inner = propNode.getChild("bean");
		assertNotNull(inner);
		assertEquals(FieldRetrievingFactoryBean.class.getName(), inner.getAttributeValue("class"));
		
		final List<Element> props = inner.getChildren();
		assertEquals(2, props.size());
		assertEquals("targetClass", props.get(0).getAttributeValue("name"));
		assertEquals(1, props.get(0).getContentSize());
		assertEquals(Status.class.getName(), props.get(0).getChild("value").getText());
		assertEquals("targetField", props.get(1).getAttributeValue("name"));
		assertEquals(1, props.get(1).getContentSize());
		assertEquals(Status.ACTIVE.name(), props.get(1).getChild("value").getText());
	}
	@SuppressWarnings("unchecked")
	public void testHandlesEnumSubclass() {
		final Element root = new Element("beans");
		
		enc.encodeEnum(root, Status.PASSIVE);
		
		assertEquals(1, root.getContentSize());

		final Element bean = root.getChild("bean");
		assertNotNull(bean);
		assertEquals(FieldRetrievingFactoryBean.class.getName(), bean.getAttributeValue("class"));
		
		final List<Element> props = bean.getChildren();
		assertEquals(2, props.size());
		assertEquals("targetClass", props.get(0).getAttributeValue("name"));
		assertEquals(1, props.get(0).getContentSize());
		assertEquals(Status.class.getName(), props.get(0).getChild("value").getText());
		assertEquals("targetField", props.get(1).getAttributeValue("name"));
		assertEquals(1, props.get(1).getContentSize());
		assertEquals(Status.PASSIVE.name(), props.get(1).getChild("value").getText());
	}
	public void testHandlesFile() {
		final Element root = new Element("beans");
		
		Bean s = new Bean();
		s.setValue("a value");
		s.setFilename(new File("."));
		
		assertEquals(0, root.getContentSize());
		
		enc.encodeBean(root, "mySimple", s);
		
		assertEquals(1, root.getContentSize());
		
		final Element beanNode = root.getChild("bean");
		assertNotNull("wrong element type", beanNode);
		assertEquals("mySimple", beanNode.getAttributeValue("name"));
		assertEquals(s.getClass().getName(), beanNode.getAttributeValue("class"));
		
		assertEquals(2, beanNode.getContentSize());
		final List<?> props = beanNode.getChildren("property");

		assertPropertyValue((Element)props.get(0), "filename", ".");
		assertPropertyValue((Element)props.get(1), "value", "a value");
	}
	public void testHandlesList() {
		final Element root = new Element("beans");
		Bean b = new Bean();
		Bean child1 = new Bean();
		child1.setValue("child value");
		Date child2 = new Date();
		
		b.setChildren(Arrays.asList(new Object[] {child1, child2}));
		
		enc.encodeBean(root, "foo", b);
		
		assertEquals(1, root.getContentSize());
		Element beanNode = root.getChild("bean");
		assertNotNull(beanNode);
		
		Element propertyNode = beanNode.getChild("property");
		assertNotNull(propertyNode);

		Element listNode = propertyNode.getChild("list");
		assertNotNull(listNode);
		
		List<?> beansInList = listNode.getChildren();
		assertEquals(2, beansInList.size());
		
		Element beanInList = (Element) beansInList.get(0);
		assertNotNull(beanInList);
		assertEquals("bean", beanInList.getName());
		assertNull(beanInList.getAttribute("name"));
		assertEquals(child1.getClass().getName(), beanInList.getAttributeValue("class"));
		assertEquals(1, beanInList.getContentSize());
		assertEquals(child1.getValue(), beanInList.getChild("property").getChild("value").getText());

		beanInList = (Element) beansInList.get(1);
		assertNotNull(beanInList);
		assertEquals("value", beanInList.getName());
		assertEquals(Long.toString(child2.getTime()), beanInList.getText());
		
	}
	
	public void testHandlesSet() {
		final Element root = new Element("beans");
		Bean b = new Bean();
		
		b.setSet(new HashSet<String>(Arrays.asList(new String[] {"a", "b"})));
		
		enc.encodeBean(root, "foo", b);
		
		assertEquals(1, root.getContentSize());
		Element beanNode = root.getChild("bean");
		assertNotNull(beanNode);
		
		Element propertyNode = beanNode.getChild("property");
		assertNotNull(propertyNode);

		Element setNode = propertyNode.getChild("set");
		assertNotNull(setNode);
		
		List<?> beansInList = setNode.getChildren();
		assertEquals(2, beansInList.size());
		
		Element beanInList = (Element) beansInList.get(0);
		assertNotNull(beanInList);
		assertEquals("value", beanInList.getName());
		assertTrue("a".equals(beanInList.getText()) || "b".equals(beanInList.getText()));

		beanInList = (Element) beansInList.get(1);
		assertNotNull(beanInList);
		assertEquals("value", beanInList.getName());
		assertTrue("a".equals(beanInList.getText()) || "b".equals(beanInList.getText()));
	}
	@SuppressWarnings("unchecked")
	public void testEncodesMap() {
		final Element root = new Element("beans");
		Bean b = new Bean();
		final Map<String, Object> m = new HashMap<String, Object>();
		m.put("null", null);
		m.put("prim", new Integer(1));
		final Bean nested = new Bean();
		nested.setValue("a value");
		m.put("bean", nested);
		
		b.setMap(m);
		
		enc.encodeBean(root, "foo", b);
		
		assertEquals(1, root.getContentSize());
		Element beanNode = root.getChild("bean");
		assertNotNull(beanNode);
		
		Element propertyNode = beanNode.getChild("property");
		assertNotNull(propertyNode);
		assertEquals("map", propertyNode.getAttributeValue("name"));
		
		Element mapNode = propertyNode.getChild("map");
		assertNotNull(mapNode);
		
		List<Element> nodesInMap = new ArrayList<Element>(mapNode.getChildren());
		assertEquals(3, nodesInMap.size());
		
		Collections.sort(nodesInMap, new Comparator<Element>() {
			public int compare(Element o1, Element o2) {
				final String t1 = o1.getChild("key").getValue();
				final String t2 = o2.getChild("key").getValue();
				return t1.compareTo(t2);
			}
		});
		
		Element entry = nodesInMap.get(0);
		assertEquals(2, entry.getContentSize());
		Element key = entry.getChild("key");
		assertNotNull(key);
		assertNotNull(key.getChild("value"));
		assertEquals("bean", key.getChild("value").getText());
		
		beanNode = entry.getChild("bean");
		assertNotNull(beanNode);
		assertEquals(Bean.class.getName(), beanNode.getAttributeValue("class"));
		assertEquals(1, beanNode.getContentSize());
		assertNotNull(beanNode.getChild("property"));
		assertEquals("value", beanNode.getChild("property").getAttributeValue("name"));
		assertNotNull(beanNode.getChild("property").getChild("value"));
		assertEquals("a value", beanNode.getChild("property").getChild("value").getText());

		entry = nodesInMap.get(1);
		assertNotNull(entry);
		assertEquals("entry", entry.getName());
		
		assertEquals(2, entry.getContentSize());
		key = entry.getChild("key");
		assertNotNull(key);
		assertNotNull(key.getChild("value"));
		assertEquals("null", key.getChild("value").getText());
		assertNotNull(entry.getChild("null"));

		entry = nodesInMap.get(2);
		assertNotNull(entry);
		assertEquals("entry", entry.getName());
		
		assertEquals(2, entry.getContentSize());
		key = entry.getChild("key");
		assertNotNull(key);
		assertNotNull(key.getChild("value"));
		assertEquals("prim", key.getChild("value").getText());
		assertNotNull(entry.getChild("value"));
		assertEquals("1", entry.getChild("value").getText());

		entry = nodesInMap.get(0);
		assertNotNull(entry);
		assertEquals("entry", entry.getName());
		
	}
	public void testEncodesMapKeyAsBean() {
		final Element root = new Element("beans");
		Bean b = new Bean();
		final Map<Bean, String> m = new HashMap<Bean, String>();
		Bean key = new Bean();
		key.setValue("haha");
		m.put(key, "\t  the\t value  ");
		
		b.setMap(m);
		
		enc.encodeBean(root, "foo", b);
		
		assertEquals(1, root.getContentSize());
		Element beanNode = root.getChild("bean");
		assertNotNull(beanNode);
		
		Element propertyNode = beanNode.getChild("property");
		assertNotNull(propertyNode);
		assertEquals("map", propertyNode.getAttributeValue("name"));
		
		Element mapNode = propertyNode.getChild("map");
		assertNotNull(mapNode);
		
		assertEquals(1, mapNode.getContentSize());
		Element entry = mapNode.getChild("entry");
		assertNotNull(entry);
		assertEquals(2, entry.getContentSize());
		
		beanNode = entry.getChild("key").getChild("bean");
		assertNotNull(beanNode);
		
		assertEquals(Bean.class.getName(), beanNode.getAttributeValue("class"));
		assertEquals(1, beanNode.getContentSize());
		assertEquals("haha", beanNode.getChild("property").getChild("value").getText());
		assertEquals("\t  the\t value  ", entry.getChild("value").getText());
	}
	public void testIsPrimitive() {
		assertTrue(enc.isPrimitive(String.class));
		assertTrue(enc.isPrimitive(Integer.class));
		assertTrue(enc.isPrimitive(java.util.Date.class));
		assertTrue(enc.isPrimitive(java.sql.Date.class));
		assertTrue(enc.isPrimitive(File.class));
		
		assertFalse(enc.isPrimitive(List.class));
		assertFalse(enc.isPrimitive(Bean.class));
	}
	public void testAddBeanAddsToRoot() {
		assertEquals(0, enc.root.getContentSize());
		enc.addBean("aSimple", new Bean());
		assertEquals(1, enc.root.getContentSize());
	}
	public void testWritesOutput() throws Exception {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final Writer writer = new OutputStreamWriter(os);
		
		Bean b = new Bean();
		b.setValue("  \thello\t  \t  there  \t  ");
		enc.addBean("foo", b);
		enc.write(writer);
		
		String asXml = os.toString().replaceAll("\n", "").replaceAll("\r", "");
		
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN//EN\" \"http://www.springframework.org/dtd/spring-beans.dtd\"><beans>  <bean name=\"foo\" class=\"net.sourceforge.vulcan.spring.SpringBeanXmlEncoderTest$Bean\">    <property name=\"value\">      <value>  \thello\t  \t  there  \t  </value>    </property>  </bean></beans>", asXml);
	}
	public void testLoadDefaultConfig() throws Exception {
		final BeanFactory factory = new XmlBeanFactory(new ClassPathResource("/net/sourceforge/vulcan/spring/default-config.xml"));
		factory.getBean("configuration");
	}
	public void testUseFactoryBean() {
		enc.setFactoryExpert(new SpringBeanXmlEncoder.FactoryExpert() {
			public boolean needsFactory(Object bean) {
				return true;
			}
			public String getFactoryBeanName(Object bean) {
				return "bertha";
			}
			public String getFactoryMethod(Object bean) {
				return "createThingy";
			}
			public List<String> getConstructorArgs(Object bean) {
				return Arrays.asList(new String[] {"pacific"});
			}
			public void registerPlugin(ClassLoader classLoader, String id) {
			}
		});
		
		final Element root = new Element("beans");

		Bean cfg = new Bean();
		cfg.setValue("a value");
		
		enc.encodeBeanOrValue(root, cfg);
		
		final Element bean = root.getChild("bean");
		assertEquals(null, bean.getAttributeValue("class"));
		assertEquals("bertha", bean.getAttributeValue("factory-bean"));
		assertEquals("createThingy", bean.getAttributeValue("factory-method"));
		
		assertEquals("value", bean.getChild("property").getAttributeValue("name"));
		assertEquals("a value", bean.getChild("property").getChild("value").getText());
		
		assertNotNull(bean.getChild("constructor-arg"));
		assertEquals("pacific", bean.getChild("constructor-arg").getChild("value").getText());
	}
	public void testUseFactoryBeanForEnum() {
		enc.setFactoryExpert(new SpringBeanXmlEncoder.FactoryExpert() {
			public boolean needsFactory(Object bean) {
				return true;
			}
			public String getFactoryBeanName(Object bean) {
				return "bertha";
			}
			public String getFactoryMethod(Object bean) {
				return "createEnum";
			}
			public List<String> getConstructorArgs(Object bean) {
				return Arrays.asList(new String[] {"pacific", "atlantic"});
			}
			public void registerPlugin(ClassLoader classLoader, String id) {
			}
		});
		
		final Element root = new Element("beans");

		Bean b = new Bean();
		b.setStatus(Status.ACTIVE);
		
		enc.encodeBeanOrValue(root, b);
		
		final Element prop = root.getChild("bean").getChild("property");
		assertNotNull(prop);
		assertEquals("status", prop.getAttributeValue("name"));
		
		assertEquals(1, prop.getContentSize());
		
		final Element bean = prop.getChild("bean");
		assertNotNull(bean.getAttributeValue("name"));
		assertEquals("bertha", bean.getAttributeValue("factory-bean"));
		assertEquals("createEnum", bean.getAttributeValue("factory-method"));
	}
	protected void assertPropertyValue(Element propNode, String expectedName, String expectedValue) {
		assertEquals(expectedName, propNode.getAttributeValue("name"));
		
		assertEquals(1, propNode.getContentSize());
		final Element valNode = propNode.getChild("value");
		assertNotNull("wrong element type", valNode);
		assertEquals(expectedValue, valNode.getText());
	}
}
