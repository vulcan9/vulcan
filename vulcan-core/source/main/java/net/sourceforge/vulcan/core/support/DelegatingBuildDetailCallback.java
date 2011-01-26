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
package net.sourceforge.vulcan.core.support;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.core.BuildPhase;
import net.sourceforge.vulcan.core.BuildStatusListener;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;

public class DelegatingBuildDetailCallback implements BuildDetailCallback {
	private final ProjectStatusDto buildStatus;
	private final BuildDetailCallback delegate;
	private final Object eventSource;
	private final boolean suppressErrors;
	private final boolean suppressWarnings;

	private BuildPhase currentPhase;
	
	private final Object listenerLock = new Object();
	private List<BuildStatusListener> buildListeners;
	
	public interface BuildStatusListenerVisitor {
		void visit(BuildStatusListener node);
	}
	
	public DelegatingBuildDetailCallback(ProjectStatusDto buildStatus, BuildDetailCallback delegate, Object eventSource, boolean suppressErrors, boolean suppressWarnings) {
		this.buildStatus = buildStatus;
		this.delegate = delegate;
		this.eventSource = eventSource;
		this.suppressErrors = suppressErrors;
		this.suppressWarnings = suppressWarnings;
	}

	public boolean isSuppressErrors() {
		return suppressErrors;
	}
	
	public boolean isSuppressWarnings() {
		return suppressWarnings;
	}
	
	public BuildPhase getCurrentPhase() {
		return currentPhase;
	}

	public void setPhase(final BuildPhase phase) {
		currentPhase = phase;
		
		setPhaseMessageKey(phase.getMessageKey());
		
		raise(new BuildStatusListenerVisitor() {
			public void visit(BuildStatusListener listener) {
				listener.onBuildPhaseChanged(this, phase);
			}
		});
	}
	
	public void clearPhase() {
		currentPhase = null;
		setPhaseMessageKey(null);
		setDetail(null);
	}

	public void reportError(String message, String file, Integer lineNumber, String code) {
		if (suppressErrors) {
			return;
		}
		
		delegate.reportError(message, file, lineNumber, code);
		final BuildMessageDto error = new BuildMessageDto(message, file, lineNumber, code);
		buildStatus.addError(error);
		
		raise(new BuildStatusListenerVisitor() {
			public void visit(BuildStatusListener listener) {
				listener.onErrorLogged(eventSource, error);
			}
		});
	}

	public void reportWarning(String message, String file, Integer lineNumber, String code) {
		if (suppressWarnings) {
			return;
		}
		
		delegate.reportWarning(message, file, lineNumber, code);
		final BuildMessageDto warning = new BuildMessageDto(message, file, lineNumber, code);
		buildStatus.addWarning(warning);
		
		raise(new BuildStatusListenerVisitor() {
			public void visit(BuildStatusListener listener) {
				listener.onWarningLogged(eventSource, warning);
			}
		});
	}
	
	public void addMetric(MetricDto metric) {
		buildStatus.addMetric(metric);
		delegate.addMetric(metric);
	}
	
	public void setDetail(String detail) {
		delegate.setDetail(detail);
	}

	public void setDetailMessage(String messageKey, Object[] args) {
		delegate.setDetailMessage(messageKey, args);
	}
	
	public void setPhaseMessageKey(String key) {
		delegate.setPhaseMessageKey(key);
	}

	public void addListener(BuildStatusListener listener) {
		synchronized(listenerLock) {
			List<BuildStatusListener> newList = new ArrayList<BuildStatusListener>();
			if (buildListeners != null) {
				newList.addAll(buildListeners);
			}
			newList.add(listener);
			buildListeners = newList;
		}
	}

	public boolean removeListener(BuildStatusListener listener) {
		synchronized(listenerLock) {
			if (buildListeners == null) {
				return false;
			}
			
			List<BuildStatusListener> newList = new ArrayList<BuildStatusListener>(buildListeners);
			boolean flag = newList.remove(listener);
			buildListeners = newList.isEmpty() ? null : newList;
			return flag;
		}
	}

	private void raise(BuildStatusListenerVisitor visitor) {
		// copy to local variable to avoid concurrent modification
		final List<BuildStatusListener> listeners = buildListeners;

		if (listeners == null) {
			return;
		}
		
		for (BuildStatusListener listener : listeners) {
			visitor.visit(listener);
		}
	}
}