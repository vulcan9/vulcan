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

import java.util.Locale;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.WarningEvent;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;


public class SpringEventHandlerTest extends EasyMockTestCase {
	SpringEventHandler handler = new SpringEventHandler();
	ApplicationContext ctx;
	
	@Override
	public void setUp() throws Exception {
		ctx = createMock(ApplicationContext.class);
		
		handler.setApplicationContext(ctx);
	}
	
	public void testReportsEvent() throws Exception {
		ctx.publishEvent((ApplicationEvent) anyObject());
		
		replay();
		
		handler.reportEvent(new WarningEvent(this, null, null));
		
		verify();
	}

	public void testLogsErrorEvent() throws Exception {
		expect(ctx.getMessage(eq("foo"), aryEq(new Object[0]), EasyMockTestCase.<Locale>isNull())).andReturn("bar");
		
		ctx.publishEvent((ApplicationEvent) anyObject());
		expectLastCall();
		replay();
		
		handler.reportEvent(new ErrorEvent(this, "foo", null));
		
		verify();
	}
}
