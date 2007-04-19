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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.util.ModuleUtils;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class RestRequestProcessorTest extends MockApplicationContextStrutsTestCase {
	RestRequestDetector rrd = createMock(RestRequestDetector.class);
	ActionMapping mapping = new ActionMapping() {
		@Override
		public ActionForward findForward(String name) {
			assertEquals("some-rest-forward", name);
			return restForward;
		}
		@Override
		public ActionForward getInputForward() {
			return defaultInputForward;
		}
	};
	
	ActionForward defaultForward = new ActionForward();
	ActionForward defaultInputForward = new ActionForward();
	ActionForward restForward = new ActionForward();
	ActionForward actualInputForward = new ActionForward();
	
	ActionForm form = new ActionForm() {
		@Override
		public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
			actualInputForward = mapping.getInputForward();
			
			final ActionErrors errors = new ActionErrors();
			if (!formIsValid) {
				errors.add("a", new ActionMessage("c"));
				errors.add("b", new ActionMessage("d"));
			}
			return errors;
		}
	};
	
	RestRequestProcessor proc = new RestRequestProcessor() {
		@Override
		protected ActionForward processActionPerformInternal(HttpServletRequest request, HttpServletResponse response, Action action, ActionForm form, ActionMapping mapping) throws IOException, ServletException {
			return defaultForward;
		}
	};
	
	boolean formIsValid = true;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		final ModuleConfig mc = ModuleUtils.getInstance().getModuleConfig(getRequest(), getActionServlet().getServletContext());
		wac.getBeanFactory().registerSingleton("restRequestDetector", rrd);
		proc.init(actionServlet, mc);
	}
	public void testIsNotRestRequest() throws Exception {
		expect(rrd.isRestRequest(request)).andReturn(false);
		
		replay();
		
		final ActionForward forward = proc.processActionPerform(
				request, response, null, null, mapping);
		
		verify();
		
		assertSame(defaultForward, forward);
	}
	public void testIsRestRequest() throws Exception {
		expect(rrd.isRestRequest(request)).andReturn(true);
		expect(rrd.getRestResponseForwardName()).andReturn("some-rest-forward");
		replay();
		
		final ActionForward forward = proc.processActionPerform(
				request, response, null, null, mapping);
		
		verify();
		
		assertSame(restForward, forward);
	}
	public void testDefaultInputForwardOnNonRestRequest() throws Exception {
		expect(rrd.isRestRequest(request)).andReturn(false);
		
		replay();

		proc.processValidate(request, response, form, mapping);
		
		verify();
		
		assertSame(defaultInputForward, actualInputForward);
	}
	public void testOverrideInputForwardOnRestRequest() throws Exception {
		expect(rrd.isRestRequest(request)).andReturn(true);
		expect(rrd.getRestResponseForwardName()).andReturn("some-rest-forward");
		
		replay();

		proc.processValidate(request, response, form, mapping);
		
		verify();
		
		assertSame(restForward, actualInputForward);
	}
}
