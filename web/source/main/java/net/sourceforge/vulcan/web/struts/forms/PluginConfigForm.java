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
package net.sourceforge.vulcan.web.struts.forms;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.PluginConfigDto.Widget;
import net.sourceforge.vulcan.integration.ConfigChoice;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;
import org.apache.struts.validator.ValidatorForm;

//TODO: resolve serialization problems
@SvnRevision(id="$Id$", url="$HeadURL$")
public final class PluginConfigForm extends ValidatorForm implements DispatchForm {
	private static final Log log = LogFactory.getLog(PluginConfigForm.class);
	
	private static final class EnumConverter implements Converter {
		@SuppressWarnings("unchecked")
		public Object convert(Class target, Object arg) {
			return Enum.valueOf(target, (String)arg);
		}
	};

	public static final String HIDDEN_PASSWORD_VALUE = "****************";
	
	private transient FormFile pluginFile;
	private transient PluginConfigDto pluginConfig;
	private transient List<PropertyDescriptor> propertyDescriptors = new ArrayList<PropertyDescriptor>();
	
	private boolean projectPlugin;
	private String projectName;
	
	private String action;
	private String pluginId;
	private String focus = "pluginConfig";
	private String target;
	
	private Map<String, String> types = new HashMap<String, String>();
	private Map<String, List<String>> choices = new HashMap<String, List<String>>();
	private Map<String, String> hiddenPasswords = new HashMap<String, String>();
	
	private List<String> availableProjects;
	private List<String> location = new ArrayList<String>();
	
	public String getName() {
		return pluginId;
	}
	public String getOriginalName() {
		return null;
	}
	public String getTargetType() {
		return "plugin";
	}
	public void introspect(HttpServletRequest request) throws IntrospectionException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		Class<?> cls = null;
		
		if ("pluginConfig".equals(focus)) {
			cls = pluginConfig.getClass();
			this.location.clear();
			this.location.add("Setup");
			
			if (isProjectPlugin()) {
				this.location.add("Projects");
				this.location.add(projectName);
				this.location.add(this.pluginConfig.getPluginName());
			} else {
				this.location.add("Plugins");
				this.location.add(this.pluginConfig.getPluginName());
			}
		} else {
			cls = PropertyUtils.getPropertyType(this, focus);
			if (cls.isArray()) {
				cls = cls.getComponentType();
			}
		}
		
		final String prefix = focus + ".";
		final PropertyDescriptor[] pds;
		
		if (PluginConfigDto.class.isAssignableFrom(cls)) {
			final PluginConfigDto pluginConfig = (PluginConfigDto) PropertyUtils.getProperty(this, focus);
			final List<PropertyDescriptor> tmp = pluginConfig.getPropertyDescriptors(request.getLocale());
			pds = tmp.toArray(new PropertyDescriptor[tmp.size()]);
		} else {
			final BeanInfo beanInfo = Introspector.getBeanInfo(cls);
			Introspector.flushFromCaches(cls);
			pds = beanInfo.getPropertyDescriptors();
		}
		
		if (isNested()) {
			for (PropertyDescriptor pd : propertyDescriptors) {
				if (focus.startsWith(pd.getName())) {
					location.add(pd.getDisplayName());
				}
			}
		}
		
		types.clear();
		choices.clear();
		propertyDescriptors.clear();
		hiddenPasswords.clear();
		
		for (PropertyDescriptor pd : pds) {
			final String name = prefix + pd.getName();
			final PropertyDescriptor cp = new PropertyDescriptor(pd.getName(), pd.getReadMethod(), pd.getWriteMethod());
			cp.setShortDescription(pd.getShortDescription());
			cp.setDisplayName(pd.getDisplayName());
			cp.setName(name);
			propertyDescriptors.add(cp);
			types.put(name, getTypeAndPrepare(name, pd));
		}
		
		putLocationInRequest(request);
	}

	public void putLocationInRequest(HttpServletRequest request) {
		request.setAttribute("location", StringUtils.join(this.location.iterator(), " -> "));
	}

	@Override
	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
		setAction(ConfigForm.translateAction(getAction()));
		restoreUnmodifiedPasswords();
		return super.validate(mapping, request);
	}

	@Override
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		projectPlugin = false;
		pluginFile = null;
		target = null;
		focus = "pluginConfig";
		
		if (request == null) {
			return;
		}
		
		final String action = request.getParameter("action");
		if (action != null) {
			final String lower = action.toLowerCase();
			if (lower.equals("back") || lower.equals("update")) {
				resetPrimitiveArraysAndBooleans();
			}
		}
		
		loadProjectChoices(request);
	}

	public String getPluginId() {
		return pluginId;
	}
	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}
	public void setPluginConfig(HttpServletRequest request, PluginConfigDto pluginConfig) throws IntrospectionException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		this.pluginConfig = (PluginConfigDto) pluginConfig.copy();
		this.pluginId = this.pluginConfig.getPluginId();
		
		introspect(request);
	}
	public PluginConfigDto getPluginConfig() {
		return pluginConfig;
	}
	public FormFile getPluginFile() {
		return pluginFile;
	}
	public void setPluginFile(FormFile pluginFile) {
		this.pluginFile = pluginFile;
	}
	public List<PropertyDescriptor> getAllProperties() {
		return propertyDescriptors;
	}
	public Map<String, String> getTypes() {
		return types;
	}
	public void setTypes(Map<String, String> types) {
		this.types = types;
	}
	public Map<String, List<String>> getChoices() {
		return choices;
	}
	public void setChoices(Map<String, List<String>> choices) {
		this.choices = choices;
	}
	public boolean isProjectPlugin() {
		return projectPlugin;
	}
	public String getProjectName() {
		return projectName;
	}
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public void setProjectPlugin(boolean projectPlugin) {
		this.projectPlugin = projectPlugin;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getFocus() {
		return focus;
	}
	public void setFocus(String focus) {
		this.focus = focus;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String removeObject) {
		this.target = removeObject;
	}
	public boolean isNested() {
		return !"pluginConfig".equals(focus);
	}
	@SuppressWarnings("unchecked")
	private String getTypeAndPrepare(String propertyName, PropertyDescriptor pd) {
		final Class c = pd.getPropertyType();
		final ConfigChoice choicesType = 
			(ConfigChoice) pd.getValue(PluginConfigDto.ATTR_CHOICE_TYPE);
		
		if (choicesType != null) {
			switch (choicesType) {
				case PROJECTS:
					choices.put(propertyName, availableProjects);
					break;
				case INLINE:
					choices.put(propertyName,
							(List<String>) pd.getValue(PluginConfigDto.ATTR_AVAILABLE_CHOICES));
			}
			if (c.isArray()) {
				return "choice-array";
			}
			return "enum";
		}
		
		final Widget widget =
			(Widget) pd.getValue(PluginConfigDto.ATTR_WIDGET_TYPE);
		
		if (Widget.PASSWORD.equals(widget)) {
			hidePassword(propertyName);
			return "password";
		}
		
		if (c.isArray()) {
			final Class componentType = c.getComponentType();
			if (isPrimitive(componentType)) {
				return "primitive-array";
			} else if (Enum.class.isAssignableFrom(componentType)) {
				populateEnumChoices(propertyName, componentType);
				return "choice-array";
			}
			return "object-array";
		} else if (isBoolean(c)) {
			return "boolean";
		} else if (isPrimitive(c)) {
			return "primitive";
		} else if (Enum.class.isAssignableFrom(c)) {
			populateEnumChoices(propertyName, c);
			return "enum";
		}
		return "object";
	}
	private void hidePassword(String propertyName) {
		try {
			final String password = (String) PropertyUtils.getProperty(this, propertyName);
			if (!StringUtils.isBlank(password)) {
				hiddenPasswords.put(propertyName, password);
				PropertyUtils.setProperty(this, propertyName, HIDDEN_PASSWORD_VALUE);
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		}
	}
	private void restoreUnmodifiedPasswords() {
		for (String propertyName : hiddenPasswords.keySet()) {
			restorePasswordIfNecessary(propertyName);
		}
	}

	private void restorePasswordIfNecessary(String propertyName) {
		try {
			final String submittedPassword = (String) PropertyUtils.getProperty(this, propertyName);
			if (HIDDEN_PASSWORD_VALUE.equals(submittedPassword)) {
				PropertyUtils.setProperty(this, propertyName, hiddenPasswords.get(propertyName));
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			log.error("Exception accessing configuration data", e);
		}
	}
	private boolean isBoolean(Class<?> c) {
		return Boolean.class.equals(c) || Boolean.TYPE.equals(c);
	}

	private boolean isPrimitive(Class<?> cls) {
		if (cls.isPrimitive()) {
			return true;
		} else if ("java.lang".equals(cls.getPackage().getName())) {
			return true;
		} else if (Date.class.isAssignableFrom(cls)) {
			return true;
		}
		
		return false;
	}
	private void populateEnumChoices(String propertyName, Class<?> c) {
		final Enum[] values;
		
		try {
			final Method m = c.getMethod("values", (Class[])null);
			values = (Enum[]) m.invoke(null, (Object[])null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		ConvertUtils.register(new EnumConverter(), c);
		
		final List<String> list = new ArrayList<String>();
		
		for (Enum<?> e : values) {
			list.add(e.name());
		}
		
		choices.put(propertyName, list);
	}
	private void loadProjectChoices(HttpServletRequest request) {
		final StateManager mgr = ConfigForm.getStateManager(getServlet());
		availableProjects = mgr.getProjectConfigNames();
	}
	private void resetPrimitiveArraysAndBooleans() {
		final List<PropertyDescriptor> props = getAllProperties();
		
		for (PropertyDescriptor pd : props) {
			final String name = pd.getName();
			final String type = types.get(name);
			if (type.equals("primitive-array") ||
					type.equals("choice-array")) {
				final Object emptyArray = Array.newInstance(pd.getPropertyType().getComponentType(), 0);
				
				try {
					BeanUtils.setProperty(this, name, emptyArray);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else if (type.equals("boolean")) {
				try {
					BeanUtils.setProperty(this, name, Boolean.FALSE);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		propertyDescriptors = new ArrayList<PropertyDescriptor>();
	}
}
