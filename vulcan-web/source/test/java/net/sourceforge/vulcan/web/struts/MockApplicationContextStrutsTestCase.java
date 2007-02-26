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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.AssertionFailedError;
import net.sourceforge.vulcan.Keys;
import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.ProjectDomBuilder;
import net.sourceforge.vulcan.core.Store;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.event.EventPool;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.ServletTestCase;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.RequestProcessor;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.upload.CommonsMultipartRequestHandler;
import org.apache.struts.upload.FormFile;
import org.apache.struts.util.ModuleUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import servletunit.struts.ExceptionDuringTestError;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class MockApplicationContextStrutsTestCase extends EasyMockStrutsTestCase {
	protected ActionForward resultForward;
	protected final static Hashtable<String, Object> multipartElements = new Hashtable<String, Object>();
	protected final StaticWebApplicationContext wac = new StaticWebApplicationContext();

	protected ServletTestCase.StateAndProjectManager manager;
	protected BuildManager buildManager;
	protected ProjectDomBuilder projectDomBuilder;
	protected EventPool eventPool;
	protected EventHandler eventHandler;
	protected Store store;
	
	private boolean multipart;
	
	public MockApplicationContextStrutsTestCase() {
		initWac();
	}
	
	public static final class UncaughtExceptionError extends Error {
		public UncaughtExceptionError(Throwable cause) {
			super(cause);
		}
	}
	public static final class MultipartRequestHandlerStub extends CommonsMultipartRequestHandler {
		@Override
		public void handleRequest(HttpServletRequest request)
				throws ServletException {
		}
		@Override
		public Hashtable<String, Object> getAllElements() {
			return multipartElements;
		}
		@Override
		public void rollback() {
		}
	}
	
	@Override
	public void setUp() throws Exception {
		setUp(true);
	}

	protected void setUp(boolean callSuper) throws Exception, ServletException {
		if (callSuper) {
			super.setUp();
		}

		context.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		
		multipartElements.clear();
		
		setContextDirectory(TestUtils.resolveRelativeFile("source/main/docroot"));
		
		setRequestProcessor(new RequestProcessor() {
			@Override
			protected ActionForward processException(
					HttpServletRequest request, HttpServletResponse response,
					Exception exception, ActionForm form, ActionMapping mapping)
					throws IOException, ServletException {
				
				throw new UncaughtExceptionError(exception);
			}
			@Override
			protected void processForwardConfig(HttpServletRequest request,
					HttpServletResponse response, ForwardConfig forward)
					throws IOException, ServletException {
		
				resultForward = (ActionForward) forward;
				
				super.processForwardConfig(request, response, forward);
			}
			
			@Override
			protected boolean processValidate(HttpServletRequest request,
					HttpServletResponse response, ActionForm form,
					ActionMapping mapping) throws IOException, ServletException {
				
				boolean result = super.processValidate(request, response, form, mapping);
				
				if (result == false) {
					// validation failed, use ActionMapping "input" param as forward
					resultForward = mapping.getInputForward();
				}
				return result;
			}
		});

		context.setAttribute(Keys.STATE_MANAGER, manager);
		
		context.setAttribute("javax.servlet.context.tempdir", new File(System.getProperty("java.io.tmpdir")));
	}
	
	@Override
	public final void actionPerform() {
		resultForward = null;
		try {
			super.actionPerform();
		} catch (UncaughtExceptionError e) {
			throwCause(e.getCause());
		} catch (ExceptionDuringTestError e) {
			final Throwable t;
			
			try {
				final Field field = e.getClass().getDeclaredField("rootCause");
				field.setAccessible(true);
				t = (Throwable) field.get(e);
			} catch (Exception reflectionException) {
				throw new RuntimeException(reflectionException);
			}
			
			throwCause(t);
		}
	}

	@Override
	public final void verifyForward(String forwardName) throws AssertionFailedError {
		if (resultForward == null) {
			throw new AssertionFailedError("actionForward is null");
		}
		assertEquals(forwardName, resultForward.getName());
	}
	@Override
	public final void verifyForwardPath(String forwardPath) throws AssertionFailedError {
		if (resultForward == null) {
			throw new AssertionFailedError("actionForward is null");
		}
		assertEquals(forwardPath, resultForward.getPath());
	}
	@Override
	public void addRequestParameter(String name, String value) {
		if (!multipart) {
			super.addRequestParameter(name, value);
		}
		multipartElements.put(name, value);
	}
	@Override
	public void addRequestParameter(String name, String[] value) {
		if (!multipart) {
			super.addRequestParameter(name, value);
		} else {
			throw new UnsupportedOperationException("not implemented.");
		}
	}
	public FormFile uploadFile(String paramName, final String fileName, final String contents) {
		final InputStream instream = new ByteArrayInputStream(contents.getBytes());
		
		final FormFile ff = new FormFile() {
			public void destroy() {
			}
			public String getContentType() {
				return "text/plain";
			}
			public byte[] getFileData() throws FileNotFoundException,
					IOException {
				return contents.getBytes();
			}
			public String getFileName() {
				return fileName;
			}
			public int getFileSize() {
				return contents.length();
			}
			public InputStream getInputStream() throws FileNotFoundException,
					IOException {
				return instream;
			}
			public void setContentType(String contentType) {
			}
			public void setFileName(String fileName) {
			}
			public void setFileSize(int fileSize) {
			}
		};
		
		multipartElements.put(paramName, ff);
		return ff;
	}
	protected final void assertErrorPresent(final String propertyName) {
		final ActionMessages errors = getActionErrors();

		assertTrue("expected " + propertyName + " to have error", errors.get(propertyName).hasNext());
	}

	protected final void assertPropertyHasErrors(final String propertyName, final String[] messageKeys) {
		assertPropertyHasErrors(propertyName, messageKeys, getActionErrors());
	}
	@SuppressWarnings("unchecked")
	protected final void assertPropertyHasErrors(final String propertyName, final String[] messageKeys, final ActionMessages messages) {
		final List<String> missingKeys = new ArrayList<String>(Arrays.asList(messageKeys));

		for (Iterator<ActionMessage> itr = messages.get(propertyName); itr.hasNext(); ) {
				ActionMessage next = itr.next();

				if (!missingKeys.remove(next.getKey())) {
						fail("unexpected validation error " + next.getKey());
				}
		}

		if (!missingKeys.isEmpty()) {
				fail("expected property " + propertyName + " to have validation errors: "
								+ StringUtils.join(missingKeys.iterator(), ','));
		}
	}
	
	protected final void assertPropertyHasError(final String propertyName, final String messageKey) {
		assertPropertyHasErrors(propertyName, new String[] {messageKey});
	}
	protected final void assertPropertyHasError(final String propertyName, final String messageKey, final ActionMessages messages) {
		assertPropertyHasErrors(propertyName, new String[] {messageKey}, messages);
	}
	
	protected final ActionMessages getActionErrors() {
		return getActionMessages(Globals.ERROR_KEY);
	}
	protected final ActionMessages getActionMessages(String key) {
		final ActionMessages errors = (ActionMessages) request.getAttribute(key);
		assertNotNull("expected ActionMessages but was null", errors);
		return errors;
	}
	protected final void setMultipartRequestHandlerStub() {
		multipart = true;
		request.setContentType("multipart/form-data");
		request.setAttribute(Globals.MULTIPART_KEY, MultipartRequestHandlerStub.class.getName());
	}
	protected final <T> T defineWacSingleton(String beanName, Class<T> type) {
		final T t = createMock(type);
		
		wac.getBeanFactory().registerSingleton(beanName, t);
		
		return t;
	}
	private final void setRequestProcessor(RequestProcessor rp) throws ServletException {
		ModuleConfig mc = ModuleUtils.getInstance().getModuleConfig(getRequest(), getActionServlet().getServletContext());
		
		rp.init(getActionServlet(), mc);
		
		getActionServlet().getServletContext().setAttribute(Globals.REQUEST_PROCESSOR_KEY, rp);
	}

	private void initWac() {
		manager = defineWacSingleton("stateManager",
				ServletTestCase.StateAndProjectManager.class);
		
		wac.getBeanFactory().registerAlias("stateManager", "projectManager");
		
		buildManager = defineWacSingleton("buildManager", BuildManager.class);
		projectDomBuilder = defineWacSingleton("projectDomBuilder", ProjectDomBuilder.class);
		eventPool = defineWacSingleton("eventPool", EventPool.class);
		eventHandler = defineWacSingleton("eventHandler", EventHandler.class);
		store = defineWacSingleton("store", Store.class);
		
		wac.refresh();
	}


	private void throwCause(Throwable cause) {
		if (cause instanceof ServletException) {
			final Throwable seCause = cause.getCause();
			if (seCause instanceof RuntimeException) {
				throw (RuntimeException) seCause;
			} else if (seCause != null) {
				throw new RuntimeException(seCause);
			}
		} else if (cause instanceof RuntimeException) {
			throw (RuntimeException) cause;
		}
		
		throw new RuntimeException(cause);
	}
}
