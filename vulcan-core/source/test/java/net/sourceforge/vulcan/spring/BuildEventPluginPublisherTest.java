/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.event.BrokenBuildClaimedEvent;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.BuildStartingEvent;
import net.sourceforge.vulcan.integration.BuildManagerObserverPlugin;
import net.sourceforge.vulcan.integration.MetricsPlugin;

public class BuildEventPluginPublisherTest extends EasyMockTestCase {
	BuildEventPluginPublisher pub = new BuildEventPluginPublisher();
	BuildManagerObserverPlugin d1 = createMock(BuildManagerObserverPlugin.class);
	BuildManagerObserverPlugin d2 = createMock(BuildManagerObserverPlugin.class);
	
	BuildStartingEvent startingEvent = new BuildStartingEvent(this, null, null, null);
	BuildCompletedEvent completeEvent = new BuildCompletedEvent(this, null, null, null);
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		checkOrder(true);
	}
	
	public void train() throws Exception {
		d1.onBuildCompleted(completeEvent);
		d2.onBuildCompleted(completeEvent);
	}
	
	@TrainingMethod("train")
	public void testCallsInOrder() throws Exception {
		pub.add(new PublishPlugin(d1));
		pub.add(new PublishPlugin(d2));
		pub.onApplicationEvent(new EventBridge(completeEvent));
	}
	
	@TrainingMethod("train")
	public void testCallsReportersFirst() throws Exception {
		pub.add(new PublishPlugin(d2));
		pub.add(new ReportPlugin(d1));
		pub.onApplicationEvent(new EventBridge(completeEvent));
	}
	
	public void trainStarting() throws Exception {
		d1.onBuildStarting(startingEvent);
	}
	
	@TrainingMethod("trainStarting")
	public void testCallsBuildStarting() throws Exception {
		pub.add(new PublishPlugin(d1));
		pub.onApplicationEvent(new EventBridge(startingEvent));
	}

	public static class PublishPlugin implements BuildManagerObserverPlugin {
		private final BuildManagerObserverPlugin delegate;
		
		PublishPlugin(BuildManagerObserverPlugin delegate) {
			this.delegate = delegate;
		}
		public String getId() {
			return null;
		}
		public String getName() {
			return null;
		}
		public void onBuildStarting(BuildStartingEvent event) {
			delegate.onBuildStarting(event);
		}
		public void onBuildCompleted(BuildCompletedEvent event) {
			delegate.onBuildCompleted(event);
		}
		public void onBrokenBuildClaimed(
				BrokenBuildClaimedEvent event) {
			delegate.onBrokenBuildClaimed(event);
		}
	}
	
	@MetricsPlugin
	public static class ReportPlugin extends PublishPlugin {
		ReportPlugin(BuildManagerObserverPlugin delegate) {
			super(delegate);
		}
	}
}
