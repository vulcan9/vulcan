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
package net.sourceforge.vulcan.core.support;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.core.BuildPhase;
import net.sourceforge.vulcan.core.BuildStatusListener;
import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.MetricDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.MetricDto.MetricType;

public class DelegatingBuildDetailCallbackTest extends EasyMockTestCase {
	final BuildDetailCallback delegate = createStrictMock(BuildDetailCallback.class);
	final Object eventSource = new Object();
	final ProjectStatusDto status = new ProjectStatusDto();
	
	DelegatingBuildDetailCallback callback = new DelegatingBuildDetailCallback(status, delegate, eventSource, false, false);
	
	final List<BuildMessageDto> listenedErrors = new ArrayList<BuildMessageDto>();
	final List<BuildMessageDto> listenedWarnings = new ArrayList<BuildMessageDto>();
	final List<BuildPhase> listenedPhases = new ArrayList<BuildPhase>();

	BuildStatusListener listener = new BuildStatusListener() {
		public void onBuildPhaseChanged(Object source, BuildPhase phase) {
			listenedPhases.add(phase);
		}
		public void onErrorLogged(Object source, BuildMessageDto error) {
			listenedErrors.add(error);
		}
		public void onWarningLogged(Object source, BuildMessageDto warning) {
			listenedWarnings.add(warning);
		}
	};
	
	public void testRaisesPhaseChanged() throws Exception {
		doBuildListenerTest();
		
		assertEquals(1, listenedPhases.size());
		assertEquals(BuildPhase.Build, listenedPhases.get(0));
	}
	
	public void testCapturesMetrics() throws Exception {
		doBuildListenerTest();
		
		assertEquals(1, status.getMetrics().size());
	}

	public void testCapturesErrorsAndWarnings() throws Exception {
		doBuildListenerTest();
		
		assertEquals(1, status.getErrors().size());
		assertEquals(1, listenedErrors.size());
		assertEquals(1, status.getWarnings().size());
		assertEquals(1, listenedWarnings.size());
	}

	public void testSuppressErrors() throws Exception {
		callback = new DelegatingBuildDetailCallback(status, delegate, eventSource, true, false);
		
		doBuildListenerTest();
		
		assertEquals(0, status.getErrors().size());
		assertEquals(0, listenedErrors.size());
		assertEquals(1, status.getWarnings().size());
		assertEquals(1, listenedWarnings.size());
	}

	public void testSuppressWarnings() throws Exception {
		callback = new DelegatingBuildDetailCallback(status, delegate, eventSource, false, true);
		
		doBuildListenerTest();
		
		assertEquals(1, status.getErrors().size());
		assertEquals(1, listenedErrors.size());
		assertEquals(0, status.getWarnings().size());
		assertEquals(0, listenedWarnings.size());
	}

	public void testEventWithNoListeners() throws Exception {
		callback.reportError("no one is listening", "file", 2, "code");
	}
	
	public void testRemoveListenerWhenNonePresent() throws Exception {
		assertEquals("removeBuildStatusListener()", false, callback.removeListener(null));
	}
	
	public void testRemoveListener() throws Exception {
		callback.addListener(listener);
		
		assertEquals("removeBuildStatusListener()", true, callback.removeListener(listener));
	}
	
	public void testAddRemoveMultipleListeners() throws Exception {
		BuildStatusListener other = createMock(BuildStatusListener.class);
		
		callback.addListener(listener);
		callback.addListener(other);
		
		assertEquals("removeBuildStatusListener()", true, callback.removeListener(other));
		assertEquals("removeBuildStatusListener()", true, callback.removeListener(listener));
	}
	
	private void doBuildListenerTest() throws Exception {
		delegate.setPhaseMessageKey(BuildPhase.Build.getMessageKey());
		
		if (!callback.isSuppressErrors()) {
			delegate.reportError("an error occurred.", "file", 0, "code");	
		}
		
		if (!callback.isSuppressWarnings()) {
			delegate.reportWarning("you should not do that.", "file", 0, "code");
		}
		delegate.addMetric(new MetricDto("message.key", "1", MetricType.PERCENT));
		
		replay();
		
		callback.addListener(listener);
		
		callback.setPhase(BuildPhase.Build);
		callback.reportError("an error occurred.", "file", 0, "code");
		callback.reportWarning("you should not do that.", "file", 0, "code");
		callback.addMetric(new MetricDto("message.key", "1", MetricType.PERCENT));
		
		verify();
	}
}
