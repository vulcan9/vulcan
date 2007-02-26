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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletOutputStream;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.Keys;
import net.sourceforge.vulcan.ProjectManager;
import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import servletunit.HttpServletRequestSimulator;
import servletunit.HttpServletResponseSimulator;
import servletunit.ServletConfigSimulator;
import servletunit.ServletContextSimulator;
import servletunit.ServletOutputStreamSimulator;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class ServletTestCase extends EasyMockTestCase {
	ServletContextSimulator servletContext = new ServletContextSimulator() {
		@Override
		public String getMimeType(String path) {
			return mimeType;
		}
		@Override
		public RequestDispatcher getRequestDispatcher(String path) {
			requestedPath = path;
			return super.getRequestDispatcher(path);
		}
		@Override
		public void log(String message, Throwable throwable) {
			loggedMessage = message;
			loggedThrowable = throwable;
		}
	};
	ServletConfigSimulator servletConfig = new ServletConfigSimulator() {
		@Override
		public ServletContextSimulator getServletContext() {
			return servletContext;
		}
	};
	HttpServletRequestSimulator request = new HttpServletRequestSimulator(servletContext);
	HttpServletResponseSimulator response = new HttpServletResponseSimulator() {
		@Override
		public int getStatusCode() {
			return httpErrorCode;
		}
		@Override
		public void sendError(int error) throws IOException {
			httpErrorCode = error;
		}
		@Override
		public void sendRedirect(String path) throws IOException {
			redirect = path;
		}
		@Override
		public void setStatus(int sc) {
			super.setStatus(sc);
			httpErrorCode = sc;
		}
		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return new ServletOutputStreamSimulator(os) {
				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					if (ioe != null) {
						throw ioe;
					}
					super.write(b, off, len);
				}
				@Override
				public void close() throws IOException {
					closeCalled = true;
					super.close();
				}
			};
			
		}
	};
	
	int httpErrorCode;
	String mimeType;
	String redirect;
	String requestedPath;
	
	IOException ioe;
	boolean closeCalled;
	
	ByteArrayOutputStream os = new ByteArrayOutputStream();
	
	StaticWebApplicationContext wac;
	StateManager mgr;
	EventHandler eventHandler;
	
	Throwable loggedThrowable;
	String loggedMessage;
	
	@Override
	public void setUp() throws Exception {
		wac = new StaticWebApplicationContext();
		
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		
		mgr = createStrictMock(StateAndProjectManager.class);
		wac.getBeanFactory().registerSingleton(Keys.STATE_MANAGER, mgr);
		
		wac.getBeanFactory().registerSingleton(Keys.EVENT_POOL, Boolean.TRUE);
		
		eventHandler = createStrictMock(EventHandler.class);
		wac.getBeanFactory().registerSingleton(Keys.EVENT_HANDLER, eventHandler);
	}

	public interface StateAndProjectManager extends StateManager, ProjectManager {}
}
