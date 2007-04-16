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
package net.sourceforge.vulcan.web.struts;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.InvalidCancelException;
import org.apache.struts.action.RequestProcessor;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ExceptionConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class RestRequestProcessor extends RequestProcessor {
	private RestRequestDetector restRequestDetector;
	
	@Override
	public void init(ActionServlet servlet, ModuleConfig moduleConfig) throws ServletException {
		super.init(servlet, moduleConfig);
		
		final WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(
				servlet.getServletContext());
		
		this.restRequestDetector = (RestRequestDetector) wac.getBean(
				"restRequestDetector", RestRequestDetector.class);
	}
	
	@Override
	protected boolean processValidate(HttpServletRequest request, HttpServletResponse response, ActionForm form, ActionMapping mapping) throws IOException, ServletException, InvalidCancelException {
		if (restRequestDetector.isRestRequest(request)) {
			final String restForwardName = restRequestDetector.getRestResponseForwardName();
			mapping = new ActionMappingWrapper(mapping, restForwardName);
		}
		return super.processValidate(request, response, form, mapping);
	}
	
	@Override
	protected ActionForward processActionPerform(HttpServletRequest request, HttpServletResponse response, Action action, ActionForm form, ActionMapping mapping) throws IOException, ServletException {
		final ActionForward actionForward = processActionPerformInternal(request, response, action, form, mapping);
		
		if (restRequestDetector.isRestRequest(request)) {
			return mapping.findForward(restRequestDetector.getRestResponseForwardName());
		}
		return actionForward;
	}
	
	protected ActionForward processActionPerformInternal(HttpServletRequest request, HttpServletResponse response, Action action, ActionForm form, ActionMapping mapping) throws IOException, ServletException {
		return super.processActionPerform(request, response, action, form, mapping);
	}
	
	/**
	 * Delegating wrapper which decorates <code>getInputForward</code>.
	 */
	private static class ActionMappingWrapper extends ActionMapping {
		private final ActionMapping delegate;
		private final String restForwardName;
		
		public ActionMappingWrapper(ActionMapping delegate, String restForwardName) {
			this.delegate = delegate;
			this.restForwardName = restForwardName;
		}

		@Override
		public String getInput() {
			return delegate.findForward(restForwardName).getPath();
		}
		@Override
		public ActionForward getInputForward() {
			return delegate.findForward(restForwardName);
		}
		@Override
		public void addExceptionConfig(ExceptionConfig config) {
			delegate.addExceptionConfig(config);
		}
		@Override
		public void addForwardConfig(ForwardConfig config) {
			delegate.addForwardConfig(config);
		}
		@Override
		public ExceptionConfig findException(Class type) {
			return delegate.findException(type);
		}
		@Override
		public ExceptionConfig findExceptionConfig(String type) {
			return delegate.findExceptionConfig(type);
		}
		@Override
		public ExceptionConfig[] findExceptionConfigs() {
			return delegate.findExceptionConfigs();
		}
		@Override
		public ActionForward findForward(String forwardName) {
			return delegate.findForward(forwardName);
		}
		@Override
		public ForwardConfig findForwardConfig(String name) {
			return delegate.findForwardConfig(name);
		}
		@Override
		public ForwardConfig[] findForwardConfigs() {
			return delegate.findForwardConfigs();
		}
		@Override
		public String[] findForwards() {
			return delegate.findForwards();
		}
		@Override
		public void freeze() {
			delegate.freeze();
		}
		@Override
		public String getActionId() {
			return delegate.getActionId();
		}
		@Override
		public String getAttribute() {
			return delegate.getAttribute();
		}
		@Override
		public boolean getCancellable() {
			return delegate.getCancellable();
		}
		@Override
		public String getCatalog() {
			return delegate.getCatalog();
		}
		@Override
		public String getCommand() {
			return delegate.getCommand();
		}
		@Override
		public String getExtends() {
			return delegate.getExtends();
		}
		@Override
		public String getForward() {
			return delegate.getForward();
		}
		@Override
		public String getInclude() {
			return delegate.getInclude();
		}
		@Override
		public ModuleConfig getModuleConfig() {
			return delegate.getModuleConfig();
		}
		@Override
		public String getMultipartClass() {
			return delegate.getMultipartClass();
		}
		@Override
		public String getName() {
			return delegate.getName();
		}
		@Override
		public String getParameter() {
			return delegate.getParameter();
		}
		@Override
		public String getPath() {
			return delegate.getPath();
		}
		@Override
		public String getPrefix() {
			return delegate.getPrefix();
		}
		@Override
		public String getProperty(String key) {
			return delegate.getProperty(key);
		}
		@Override
		public String[] getRoleNames() {
			return delegate.getRoleNames();
		}
		@Override
		public String getRoles() {
			return delegate.getRoles();
		}
		@Override
		public String getScope() {
			return delegate.getScope();
		}
		@Override
		public String getSuffix() {
			return delegate.getSuffix();
		}
		@Override
		public String getType() {
			return delegate.getType();
		}
		@Override
		public boolean getUnknown() {
			return delegate.getUnknown();
		}
		@Override
		public boolean getValidate() {
			return delegate.getValidate();
		}
		@Override
		public void inheritFrom(ActionConfig config) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
			delegate.inheritFrom(config);
		}
		@Override
		public boolean isExtensionProcessed() {
			return delegate.isExtensionProcessed();
		}
		@Override
		public void processExtends(ModuleConfig moduleConfig) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
			delegate.processExtends(moduleConfig);
		}
		@Override
		public void removeExceptionConfig(ExceptionConfig config) {
			delegate.removeExceptionConfig(config);
		}
		@Override
		public void removeForwardConfig(ForwardConfig config) {
			delegate.removeForwardConfig(config);
		}
		@Override
		public void setActionId(String actionId) {
			delegate.setActionId(actionId);
		}
		@Override
		public void setAttribute(String attribute) {
			delegate.setAttribute(attribute);
		}
		@Override
		public void setCancellable(boolean cancellable) {
			delegate.setCancellable(cancellable);
		}
		@Override
		public void setCatalog(String catalog) {
			delegate.setCatalog(catalog);
		}
		@Override
		public void setCommand(String command) {
			delegate.setCommand(command);
		}
		@Override
		public void setExtends(String inherit) {
			delegate.setExtends(inherit);
		}
		@Override
		public void setForward(String forward) {
			delegate.setForward(forward);
		}
		@Override
		public void setInclude(String include) {
			delegate.setInclude(include);
		}
		@Override
		public void setInput(String input) {
			delegate.setInput(input);
		}
		@Override
		public void setModuleConfig(ModuleConfig moduleConfig) {
			delegate.setModuleConfig(moduleConfig);
		}
		@Override
		public void setMultipartClass(String multipartClass) {
			delegate.setMultipartClass(multipartClass);
		}
		@Override
		public void setName(String name) {
			delegate.setName(name);
		}
		@Override
		public void setParameter(String parameter) {
			delegate.setParameter(parameter);
		}
		@Override
		public void setPath(String path) {
			delegate.setPath(path);
		}
		@Override
		public void setPrefix(String prefix) {
			delegate.setPrefix(prefix);
		}
		@Override
		public void setProperty(String key, String value) {
			delegate.setProperty(key, value);
		}
		@Override
		public void setRoles(String roles) {
			delegate.setRoles(roles);
		}
		@Override
		public void setScope(String scope) {
			delegate.setScope(scope);
		}
		@Override
		public void setSuffix(String suffix) {
			delegate.setSuffix(suffix);
		}
		@Override
		public void setType(String type) {
			delegate.setType(type);
		}
		@Override
		public void setUnknown(boolean unknown) {
			delegate.setUnknown(unknown);
		}
		@Override
		public void setValidate(boolean validate) {
			delegate.setValidate(validate);
		}
		@Override
		public void throwIfConfigured() {
			delegate.throwIfConfigured();
		}
		
	}
}
