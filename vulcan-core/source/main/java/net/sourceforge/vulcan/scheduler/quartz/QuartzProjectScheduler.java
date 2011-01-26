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
package net.sourceforge.vulcan.scheduler.quartz;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.scheduler.ProjectScheduler;

public class QuartzProjectScheduler implements ProjectScheduler {
	private SchedulerConfigDto config;
	private Trigger trigger;

	/* Injected Dependencies: */
	private JobDetail jobDetail;
	private Scheduler scheduler;

	public void init(SchedulerConfigDto config) {
		this.config = config;
		
		jobDetail.setName(config.getName());
		
		final String cronExpression = config.getCronExpression();
		
		if (config.isPaused()) {
			trigger = null;
		} else if (!StringUtils.isBlank(cronExpression)) {
			try {
				trigger = new CronTrigger(
					jobDetail.getName(),
					jobDetail.getGroup(),
					cronExpression);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		} else if (config.getInterval() > 0){
			trigger = new SimpleTrigger(
					jobDetail.getName(),
					jobDetail.getGroup(),
					SimpleTrigger.REPEAT_INDEFINITELY,
					config.getInterval());
		} else {
			trigger = null;
		}
		
		if (trigger != null) {
			trigger.setStartTime(new Date(System.currentTimeMillis() + config.getInterval()));
		}
	}

	public void configurationChanged(SchedulerConfigDto config) {
		stopIfScheduled();
		
		init(config);

		start();
	}

	public void start() {
		if (trigger == null) {
			return;
		}
		
		try {
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			throw new RuntimeException(e);
		}
	}

	public void wakeUp() {
		throw new UnsupportedOperationException("not implemented");
	}
	
	public void stop() {
		stopIfScheduled();
	}

	public boolean isRunning() {
		return getNextExecutionDate() != null;
	}

	public Date getNextExecutionDate() {
		if (trigger == null) {
			return null;
		}
		return trigger.getNextFireTime();
	}
	
	public String getName() {
		return config.getName();
	}

	public JobDetail getJobDetail() {
		return jobDetail;
	}

	public void setJobDetail(JobDetail jobDetail) {
		this.jobDetail = jobDetail;
	}

	public Scheduler getScheduler() {
		return scheduler;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public Trigger getTrigger() {
		return trigger;
	}
	
	private boolean stopIfScheduled() {
		try {
			return scheduler.deleteJob(jobDetail.getName(), jobDetail.getGroup());
		} catch (SchedulerException e) {
			throw new RuntimeException(e);
		}
	}
}
