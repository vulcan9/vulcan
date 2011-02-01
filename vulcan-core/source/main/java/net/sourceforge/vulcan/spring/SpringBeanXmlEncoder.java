/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;

import net.sourceforge.vulcan.core.BeanEncoder;
import net.sourceforge.vulcan.metadata.Transient;

import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format.TextMode;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.config.FieldRetrievingFactoryBean;


public final class SpringBeanXmlEncoder implements BeanEncoder {
	Element root;
	FactoryExpert factoryExpert = new NoOpFactoryExpert();
	int factoryObjectCount;
	
	public SpringBeanXmlEncoder() {
		reset();
	}
	public FactoryExpert getFactoryExpert() {
		return factoryExpert;
	}
	public void setFactoryExpert(FactoryExpert factoryExpert) {
		this.factoryExpert = factoryExpert;
	}

	public void addBean(String beanName, Object bean) {
		encodeBean(root, beanName, bean);
	}
	public void write(Writer writer) throws IOException {
		final Document doc = new Document(root, getDocType());
		final Format format = Format.getPrettyFormat();
		format.setTextMode(TextMode.PRESERVE);
		
		XMLOutputter out = new XMLOutputter(format);
		
		out.output(doc, writer);
	}
	public void reset() {
		root = new Element("beans");
		factoryObjectCount = 0;
	}

	void encodeBean(Element root, String beanName, Object bean) {
		final BeanWrapper bw = new BeanWrapperImpl(bean);
		
		final PropertyDescriptor[] pds = bw.getPropertyDescriptors();
		final Element beanNode = new Element("bean");
		
		root.addContent(beanNode);
		
		if (beanName != null) {
			beanNode.setAttribute("name", beanName);
		}
		
		if (factoryExpert.needsFactory(bean)) {
			encodeBeanByFactory(beanNode, bean);
		} else {
			beanNode.setAttribute("class", bean.getClass().getName());
		}
		
		for (PropertyDescriptor pd : pds) {
			if (pd.getWriteMethod() == null) continue;
			
			final Method readMethod = pd.getReadMethod();
			
			if (readMethod == null) continue;
			
			if (readMethod.getAnnotation(Transient.class) != null) {
				continue;
			}
			final String name = pd.getName();
			
			final Object value = bw.getPropertyValue(name);
			if (value == null) continue;
			
			final Element propertyNode = new Element("property");
			propertyNode.setAttribute("name", name);
			beanNode.addContent(propertyNode);

			encodeObject(propertyNode, value);
		}
	}
	void encodeBeanByFactory(final Element beanNode, Object bean) {
		final String factoryBeanName = factoryExpert.getFactoryBeanName(bean);
		
		if (factoryBeanName != null) {
			beanNode.setAttribute("factory-bean", factoryBeanName);
		}

		if (beanNode.getAttributeValue("name") == null) {
			beanNode.setAttribute("name", "factoryObject" + factoryObjectCount++);
		}
		
		beanNode.setAttribute("factory-method", factoryExpert.getFactoryMethod(bean));
		
		final List<String> ctrArgs = factoryExpert.getConstructorArgs(bean);
		for (String arg : ctrArgs) {
			final Element ctrNode = new Element("constructor-arg");
			final Element valueNode = new Element("value");
			
			valueNode.setText(arg);
			
			ctrNode.addContent(valueNode);
			beanNode.addContent(ctrNode);
		}
	}
	void encodeObject(final Element node, final Object object) {
		if (object instanceof List<?>) {
			encodeList(node, "list", (List<?>) object);
		} else if (object instanceof Object[]) {
			encodeList(node, "list", Arrays.asList((Object[])object));
		} else if (object instanceof Set<?>) {
			encodeList(node, "set", (Collection<?>) object);
		} else if (object instanceof Map<?,?>) {
			encodeMap(node, (Map<?,?>)object);
		} else if (object instanceof UUID) {
			encodeUUID(node, (UUID)object);
		} else {
			encodeBeanOrValue(node, object);
		}
	}
	void encodeUUID(Element propertyNode, UUID uuid) {
		final Element bean = new Element("bean");
		
		bean.setAttribute("class", UUID.class.getName());
		bean.setAttribute("factory-method", "fromString");
		
		final Element arg = new Element("constructor-arg");
		arg.setAttribute("type", "java.lang.String");
		arg.setAttribute("value", uuid.toString());
		
		bean.addContent(arg);
		propertyNode.addContent(bean);
	}
	void encodeList(Element root, String collectionType, Collection<? extends Object> collection) {
		final Element listNode = new Element(collectionType);
		root.addContent(listNode);
		
		for (final Object bean : collection) {
			if (bean == null) {
				listNode.addContent(new Element("null"));
			} else {
				encodeBeanOrValue(listNode, bean);
			}
		}
	}
	void encodeMap(Element root, Map<?,?> map) {
		final Element mapNode = new Element("map");
		root.addContent(mapNode);
		
		for (Iterator<?> itr = map.keySet().iterator(); itr.hasNext();) {
			final Element entryNode = new Element("entry");
			final Object key = itr.next();
			final Element keyNode = new Element("key");
			encodeObject(keyNode, key);
			
			entryNode.addContent(keyNode);
			encodeObject(entryNode, map.get(key));
			
			mapNode.addContent(entryNode);
		}
	}
	void encodeBeanOrValue(Element root, Object object) {
		if (object == null) {
			encodeAsValue(root, object);
		} else if (isPrimitive(object.getClass())) {
			encodeAsValue(root, object);
		} else {
			encodeBean(root, null, object);
		}
	}
	<T> boolean isPrimitive(Class<T> cls) {
		if ("java.lang".equals(cls.getPackage().getName())) {
			return true;
		} else if (Date.class.isAssignableFrom(cls)) {
			return true;
		} else if (File.class.isAssignableFrom(cls)) {
			return true;
		} else if (Enum.class.isAssignableFrom(cls)) {
			return true;
		}
		
		return false;
	}
	void encodeAsValue(final Element propertyNode, final Object object) {
		if (object == null) {
			propertyNode.addContent(new Element("null"));
			return;
		} else if (object instanceof Enum<?>) {
			encodeEnum(propertyNode, (Enum<?>)object);
			return;
		}
		
		final Element valueNode = new Element("value");

		PropertyEditor editor = PropertyEditorManager.findEditor(object.getClass());

		if (editor == null && object instanceof Date) {
			editor = new DateEditor();
		}
		
		if (editor != null) {
			editor.setValue(object);
			valueNode.setText(editor.getAsText());
		} else {
			valueNode.setText(object.toString());
		}
		propertyNode.addContent(valueNode);
	}
	void encodeEnum(Element propertyNode, Enum<?> e) {
		final Element factory = new Element("bean");
		
		propertyNode.addContent(factory);
		
		if (factoryExpert.needsFactory(e)) {
			encodeBeanByFactory(factory, e);
			return;
		}

		factory.setAttribute("class", FieldRetrievingFactoryBean.class.getName());
		
		final Element targetClass = new Element("property");
		targetClass.setAttribute("name", "targetClass");
		encodeAsValue(targetClass, e.getDeclaringClass().getName());
		
		final Element targetField = new Element("property");
		targetField.setAttribute("name", "targetField");
		encodeAsValue(targetField, e.name());
		
		factory.addContent(targetClass);
		factory.addContent(targetField);
	}
	DocType getDocType() {
		return new DocType("beans", "-//SPRING//DTD BEAN//EN", "http://www.springframework.org/dtd/spring-beans.dtd");
	}
	private static class NoOpFactoryExpert implements FactoryExpert {
		public boolean needsFactory(Object arg0) {
			return false;
		}
		public List<String> getConstructorArgs(Object arg0) {
			return null;
		}
		public String getFactoryBeanName(Object arg0) {
			return null;
		}
		public String getFactoryMethod(Object arg0) {
			return null;
		}
		public void registerPlugin(ClassLoader classLoader, String id) {
		}
	}
}
