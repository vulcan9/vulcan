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
package net.sourceforge.vulcan.scheduler.quartz;

import java.util.Date;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.dto.SchedulerConfigDto;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.scheduling.quartz.JobDetailBean;

public class QuartzProjectSchedulerTest extends EasyMockTestCase {
	SchedulerConfigDto config = new SchedulerConfigDto();
	QuartzProjectScheduler projectScheduler = new QuartzProjectScheduler();
	
	Scheduler sched = createStrictMock(Scheduler.class);
	
	public static class MockJob implements Job {
		public void execute(JobExecutionContext arg0) throws JobExecutionException {
		}
	}
	
	JobDetailBean detail;
	
	@Override
	protected void setUp() throws Exception {
		projectScheduler.setScheduler(sched);
		
		JobDetailBean detail = new JobDetailBean();
		detail.setJobClass(MockJob.class);
		detail.setBeanName("name");
		detail.setGroup("ProjectSchedulers");
		projectScheduler.setJobDetail(detail);

		config.setName("Quartz Mock Test");
		config.setInterval(100000);
		
		projectScheduler.init(config);

		this.detail = new JobDetailBean();
		this.detail.setJobClass(MockJob.class);
		this.detail.setName(config.getName());
		this.detail.setBeanName("name");
		this.detail.setGroup("ProjectSchedulers");
	}
	
	public void testInitZeroInterval() throws Exception {
		config.setInterval(0);
		
		replay();
		
		projectScheduler.init(config);
		projectScheduler.start();
	
		assertNull(projectScheduler.getNextExecutionDate());
		assertFalse(projectScheduler.isRunning());
		
		verify();
	}

	public void testStart() throws Exception {
		sched.scheduleJob(
				(JobDetail) reflectionEq(detail),
				triggerEq(new SimpleTrigger(
						detail.getName(),
						detail.getGroup(),
						SimpleTrigger.REPEAT_INDEFINITELY,
						config.getInterval())));
		
		expectLastCall().andReturn(new Date());
		
		replay();
		
		projectScheduler.start();
		
		verify();
	}

	public void testStop() throws Exception {
		sched.deleteJob(detail.getName(),
				detail.getGroup());
		
		expectLastCall().andReturn(true);
		
		replay();
		
		projectScheduler.stop();
		
		verify();
	}
	
	public void testConfigurationChanged() throws Exception {
		final SchedulerConfigDto newConfig = (SchedulerConfigDto) config.copy();
		newConfig.setInterval(1234);
		newConfig.setName("new name");
		
		sched.deleteJob(detail.getName(), detail.getGroup());
		
		expectLastCall().andReturn(false);
		
		detail.setName("new name");
		
		sched.scheduleJob(
				(JobDetail) reflectionEq(detail),
				triggerEq(new SimpleTrigger(
						detail.getName(),
						detail.getGroup(),
						SimpleTrigger.REPEAT_INDEFINITELY,
						1234)));
		
		expectLastCall().andReturn(new Date());
		
		replay();
		
		projectScheduler.configurationChanged(newConfig);
		
		verify();
	}

	public void testConfigurationChangedDisable() throws Exception {
		final SchedulerConfigDto newConfig = (SchedulerConfigDto) config.copy();
		newConfig.setInterval(0);
		
		sched.deleteJob(detail.getName(), detail.getGroup());
		
		expectLastCall().andReturn(true);
		
		replay();
		
		projectScheduler.configurationChanged(newConfig);
		
		verify();
	}
	
	public void testConfigurationChangedPause() throws Exception {
		final SchedulerConfigDto newConfig = (SchedulerConfigDto) config.copy();
		newConfig.setPaused(true);
		
		sched.deleteJob(detail.getName(), detail.getGroup());
		
		expectLastCall().andReturn(true);
		
		replay();
		
		projectScheduler.configurationChanged(newConfig);
		
		verify();
	}
	
	public void testConfigurationChangedSetCronExpr() throws Exception {
		final SchedulerConfigDto newConfig = (SchedulerConfigDto) config.copy();
		newConfig.setInterval(0);
		newConfig.setCronExpression("* * * ? * *");
		
		sched.deleteJob(detail.getName(), detail.getGroup());
		expectLastCall().andReturn(true);
		
		sched.scheduleJob(
				(JobDetail) reflectionEq(detail),
				triggerEq(new CronTrigger(
						detail.getName(),
						detail.getGroup(),
						newConfig.getCronExpression())));
		expectLastCall().andReturn(new Date());
		
		replay();
		
		projectScheduler.configurationChanged(newConfig);
		
		verify();
	}
	
	public void testGetNextExecutionDateNotStarted() throws Exception {
		replay();
		
		assertEquals(null, projectScheduler.getNextExecutionDate());
		
		verify();
	}

	public void testGetNextExecutionDate() throws Exception {
		config.setInterval(10000);
		
		projectScheduler.setScheduler(StdSchedulerFactory.getDefaultScheduler());
		projectScheduler.init(config);
		
		long now = System.currentTimeMillis();
		
		assertFalse(projectScheduler.isRunning());
		
		projectScheduler.start();
		
		assertTrue(projectScheduler.isRunning());
		
		final Date d = projectScheduler.getNextExecutionDate();
		assertNotNull(d);
		
		assertTrue("Expected next execution time to be 10 seconds from now but is "
				+ (d.getTime() - now),
				now + config.getInterval() - 100 < d.getTime());
	}
	
	public void testCronTrigger() throws Exception {
		config.setCronExpression("* * * * * * *");
		
		projectScheduler.init(config);
		
		assertTrue(projectScheduler.getTrigger() instanceof CronTrigger);
	}
	
	public static Trigger triggerEq(Trigger trigger) {
		EasyMock.reportMatcher(new TriggerMatcher(trigger));
		return null;
	}
	static class TriggerMatcher implements IArgumentMatcher {
		private final Trigger expected;

		public TriggerMatcher(final Trigger expected) {
			this.expected = expected;
		}
		
		public boolean matches(Object argument) {
			return checkTrigger(expected, (Trigger) argument);
		}
		public void appendTo(StringBuffer buffer) {
			buffer.append(expected);
		}
		private boolean checkTrigger(Trigger t1, Trigger t2) {
			return new EqualsBuilder()
				.append(t1.getName(), t2.getName())
				.append(t1.getGroup(), t2.getGroup())
				.isEquals();
		}
	}
}
