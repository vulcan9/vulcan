/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2010 Chris Eldredge
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
package net.sourceforge.vulcan.scheduler.thread;

import java.net.InetAddress;

import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.core.BuildManager;
import net.sourceforge.vulcan.core.BuildTarget;
import net.sourceforge.vulcan.core.ProjectBuilder;
import net.sourceforge.vulcan.dto.BuildDaemonInfoDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.scheduler.BuildDaemon;

public abstract class BuildDaemonImpl extends AbstractScheduler implements BuildDaemon {
	private BuildManager buildManager;
	
	protected BuildTarget currentTarget;
	private ProjectBuilder builder;
	private String phaseMessageKey;
	private String detail;
	private Object detailArgs[];
	
	BuildDetailCallback callback = new BuildDetailCallback() {
		public void setPhaseMessageKey(String phase) {
			BuildDaemonImpl.this.phaseMessageKey = phase;
		}
		public void setDetail(String detail) {
			BuildDaemonImpl.this.detail = detail;
			detailArgs = null;
		}
		public void setDetailMessage(String messageKey, Object[] args) {
			detail = messageKey;
			detailArgs = args;
		}
		public void reportError(String message, String file, Integer lineNumber, String code) {
		}
		public void reportWarning(String message, String file, Integer lineNumber, String code) {
		}
		public void addMetric(MetricDto metric) {
		}
	};
	
	@Override
	protected void execute() throws Exception {
		final BuildDaemonInfoDto info = new BuildDaemonInfoDto();
		info.setHostname(InetAddress.getLocalHost());
		info.setName(getConfig().getName());
		
		currentTarget = buildManager.getTarget(info);
		
		if (currentTarget == null) {
			return;
		}
		
		builder = createBuilder();
		
		final Thread watchdog;
		if (getConfig().getTimeout() > 0) {
			watchdog = new Thread("Watchdog[" + getName() + "]") {
				@Override
				public void run() {
					synchronized(BuildDaemonImpl.this) {
						if (currentTarget == null) {
							return;
						}
						try {
							BuildDaemonImpl.this.wait(getConfig().getTimeout());
						} catch (InterruptedException e) {
							log.warn("Unexpected interruption in " + getName() + "; build will be aborted.");
						}
						if (builder != null && builder.isBuilding()) {
							builder.abortCurrentBuild(true, null);
						}
					}
				}
			};
			watchdog.setUncaughtExceptionHandler(this);
			watchdog.start();
		} else {
			watchdog = null;
		}

		try {
			builder.build(info, currentTarget, callback);
		} finally {
			synchronized(this) {
				currentTarget = null;
				builder = null;
				phaseMessageKey = null;
				detail = null;
				detailArgs = null;
				notifyAll();
			}
			if (watchdog != null) {
				watchdog.join();
			}
		}
	}
	protected abstract ProjectBuilder createBuilder();
	
	public synchronized void abortCurrentBuild(String requestUsername) {
		if (builder != null) {
			builder.abortCurrentBuild(false, requestUsername);
		}
	}
	public String getPhaseMessageKey() {
		return phaseMessageKey;
	}
	public String getDetail() {
		return detail;
	}
	public Object[] getDetailArgs() {
		return detailArgs;
	}
	public synchronized boolean isBuilding() {
		if (builder != null) {
			return builder.isBuilding();
		}
		return false;
	}
	public synchronized boolean isKilling() {
		if (builder != null) {
			return builder.isKilling();
		}
		return false;
	}
	public synchronized ProjectConfigDto getCurrentTarget() {
		return currentTarget == null ? null : currentTarget.getProjectConfig();
	}
	public BuildManager getBuildManager() {
		return buildManager;
	}
	public void setBuildManager(BuildManager buildManager) {
		this.buildManager = buildManager;
	}
	@Override
	protected void shutdown() {
		abortCurrentBuild("BuildDaemon " + getName() + " shutting down");
	}
}
