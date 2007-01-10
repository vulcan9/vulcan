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

import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.quartz.CronTrigger;


@SvnRevision(id="$Id$", url="$HeadURL$")
public final class SchedulerConfigForm extends ConfigForm {
	private static long[] multipliers = {86400000, 3600000, 60000, 1000};
	private boolean daemon;
	private String intervalScalar;
	private String intervalMultiplier;
	private String timeoutScalar;
	private String timeoutMultiplier;
	
	public SchedulerConfigForm() {
		super(new SchedulerConfigDto());
	}
	public String getTargetType() {
		return daemon ? "build daemon" : "scheduler";
	}
	@Override
	protected void doAfterValidate() {
		final SchedulerConfigDto config = this.getSchedulerConfig();
		config.setInterval(Long.parseLong(intervalScalar) * Long.parseLong(intervalMultiplier));
		config.setTimeout(Long.parseLong(timeoutScalar) * Long.parseLong(timeoutMultiplier));
	}
	@Override
	protected void doAfterPopulate() {
		final long interval = this.getSchedulerConfig().getInterval();
		long mult = calculateMultiplier(interval);
		intervalMultiplier = Long.toString(mult);
		if (mult > 0) {
			intervalScalar = Long.toString(interval / mult);
		} else {
			intervalScalar = "0";
		}

		final long timout = this.getSchedulerConfig().getTimeout();
		mult = calculateMultiplier(timout);
		timeoutMultiplier = Long.toString(mult);
		if (mult > 0) {
			timeoutScalar = Long.toString(timout / mult);
		} else {
			timeoutScalar = "0";
		}
	}
	private long calculateMultiplier(long interval) {
		if (interval == 0) {
			return 0;
		}
		for (int i=0; i<multipliers.length; i++) {
			if (interval % multipliers[i] == 0) {
				return multipliers[i];
			}
		}
		return 1;
	}
	@Override
	public void resetInternal(ActionMapping mapping, HttpServletRequest request) {
		daemon = false;
		timeoutScalar = "0";
		timeoutMultiplier = "0";
	}
	public SchedulerConfigDto getSchedulerConfig() {
		return (SchedulerConfigDto) getConfig();
	}
	public boolean isDaemon() {
		return daemon;
	}
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
	}
	public String getIntervalMultiplier() {
		return intervalMultiplier;
	}
	public void setIntervalMultiplier(String multiplier) {
		this.intervalMultiplier = multiplier;
	}
	public String getIntervalScalar() {
		return intervalScalar;
	}
	public void setIntervalScalar(String scalar) {
		this.intervalScalar = scalar;
	}
	public String getTimeoutMultiplier() {
		return timeoutMultiplier;
	}
	public void setTimeoutMultiplier(String timeoutMultiplier) {
		this.timeoutMultiplier = timeoutMultiplier;
	}
	public String getTimeoutScalar() {
		return timeoutScalar;
	}
	public void setTimeoutScalar(String timeoutScalar) {
		this.timeoutScalar = timeoutScalar;
	}
	@Override
	protected boolean shouldValidate() {
		return isCreateAction() || isUpdateAction();
	}
	@Override
	protected void customValidate(ActionMapping mapping, HttpServletRequest request, ActionErrors errors) {
		final String cronExpr = getSchedulerConfig().getCronExpression();
		
		if (!StringUtils.isBlank(cronExpr)) {
			try {
				new CronTrigger("test", "group", cronExpr).computeFirstFireTime(null);
				this.intervalMultiplier = "0";
				this.intervalScalar = "0";
			} catch (ParseException e) {
				errors.add("config.cronExpression", new ActionMessage("errors.cron.syntax"));
			} catch (NumberFormatException e) {
				errors.add("config.cronExpression", new ActionMessage("errors.cron.syntax"));
			} catch (UnsupportedOperationException e) {
				errors.add("config.cronExpression", new ActionMessage(
						"errors.cron.syntax.detailed",
						new String[] { e.getMessage() }));
			}
		}
	}
}
