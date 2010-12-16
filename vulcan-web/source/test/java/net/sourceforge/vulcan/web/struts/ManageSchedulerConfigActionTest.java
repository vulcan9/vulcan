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

import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.struts.forms.SchedulerConfigForm;

import org.easymock.EasyMock;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class ManageSchedulerConfigActionTest extends MockApplicationContextStrutsTestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		setRequestPathInfo("/admin/setup/manageSchedulerConfig.do");
	}
	
	public void testCreateScheduler() throws Exception {
		SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("a name");
		config.setInterval(100);
		config.setCronExpression("");
		
		addRequestParameter("action", "create");
		addRequestParameter("config.name", "a name");
		addRequestParameter("config.cronExpression", "");
		addRequestParameter("intervalScalar", "10");
		addRequestParameter("intervalMultiplier", "10");
		
		
		manager.addSchedulerConfig(config);
		replay();
		
		actionPerform();
		
		verify();
		
		verifyActionMessages(new String[] {"messages.save.success"});
	}
	
	public void testCreateSchedulerCron() throws Exception {
		SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("a name");
		config.setInterval(0);
		config.setCronExpression("* * 8-19/3 ? * *");
		
		addRequestParameter("action", "create");
		addRequestParameter("config.name", "a name");
		addRequestParameter("config.cronExpression", "* * 8-19/3 ? * *");
		addRequestParameter("intervalScalar", "100");
		addRequestParameter("intervalMultiplier", "10");
		
		manager.addSchedulerConfig(config);
		replay();
		
		actionPerform();
		
		verify();
		
		verifyActionMessages(new String[] {"messages.save.success"});
	}
	
	public void testCreateSchedulerCronBadSyntax() throws Exception {
		SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("a name");
		config.setInterval(0);
		config.setCronExpression("* * * * * *");
		
		addRequestParameter("action", "create");
		addRequestParameter("config.name", "a name");
		addRequestParameter("config.cronExpression", "* * */Febtembruary * * *");
		addRequestParameter("intervalScalar", "0");
		addRequestParameter("intervalMultiplier", "0");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError("config.cronExpression", "errors.cron.syntax");
		verifyInputForward();
	}
	
	public void testCreateSchedulerCronBadSyntaxUnsupportedOperation() throws Exception {
		SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("a name");
		config.setInterval(0);
		config.setCronExpression("* * * * * *");
		
		addRequestParameter("action", "create");
		addRequestParameter("config.name", "a name");
		addRequestParameter("config.cronExpression", "0 5 8-19/3 * * *");
		addRequestParameter("intervalScalar", "0");
		addRequestParameter("intervalMultiplier", "0");
		
		replay();
		
		actionPerform();
		
		verify();
		
		assertPropertyHasError("config.cronExpression", "errors.cron.syntax.detailed");
		verifyInputForward();
	}
	
	public void testEditNewScheduler() throws Exception {
		SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("a name");
		config.setInterval(100);
		
		addRequestParameter("action", "edit");
		addRequestParameter("createNew", "true");
		
		assertNull(request.getSession().getAttribute("schedulerConfigForm"));
		
		replay();
		
		actionPerform();
		
		verify();

		assertNotNull(request.getSession().getAttribute("schedulerConfigForm"));
	}
	
	public void testEditScheduler() throws Exception {
		SchedulerConfigDto config = new SchedulerConfigDto();
		config.setName("a name");
		config.setInterval(10000);
		
		addRequestParameter("action", "edit");
		addRequestParameter("config.name", "a name");
		
		assertNull(request.getSession().getAttribute("schedulerConfigForm"));
		
		EasyMock.expect(manager.getSchedulerConfig("a name")).andReturn(config);
		replay();
		
		actionPerform();
		
		verify();

		final SchedulerConfigForm form = (SchedulerConfigForm) request.getSession().getAttribute("schedulerConfigForm");
		assertNotNull(form);
		assertEquals("10", form.getIntervalScalar());
		assertEquals("1000", form.getIntervalMultiplier());
	}
}
