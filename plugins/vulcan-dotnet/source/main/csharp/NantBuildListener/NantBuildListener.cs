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
using NAnt.Core;

namespace SourceForge.Vulcan.DotNet {
	public class NAntListener : IBuildListener {
		const string PROP_HOST = "net.sourceforge.vulcan.ant.buildlistener.Constants.HOST";
		const string PROP_PORT = "net.sourceforge.vulcan.ant.buildlistener.Constants.PORT";
		
		const string DEFAULT_HOST = "localhost";
		const string DEFAULT_PORT = "7123";
		
		const int MAX_MESSAGE_LENGTH = 4096;
		const string MESSAGE_TRUNCATED_SUFFIX = "...";

		private UdpBuildReporter reporter;
		
		public void BuildStarted(object sender, BuildEventArgs e) {
			string host = e.Project.Properties[PROP_HOST];
			
			if (host == null) {
				host = DEFAULT_HOST;
			}
			
			string port = e.Project.Properties[PROP_PORT];
			
			if (port == null) {
				port = DEFAULT_PORT;
			}

			int portInt = int.Parse(port);

			reporter = new UdpBuildReporter(host, portInt);
			
			report("BUILD_STARTED", e);
		}
		public void BuildFinished(object sender, BuildEventArgs e) {
			report("BUILD_FINISHED", e);
		}
		public void TargetStarted(object sender, BuildEventArgs e) {
			report("TARGET_STARTED", e);
		}
		public void TargetFinished(object sender, BuildEventArgs e) {
			report("TARGET_FINISHED", e);
		}
		public void TaskStarted(object sender, BuildEventArgs e) {
			report("TASK_STARTED", e);
		}
		public void TaskFinished(object sender, BuildEventArgs e) {
			report("TASK_FINISHED", e);
		}
		public void MessageLogged(object sender, BuildEventArgs e) {
			report("MESSAGE_LOGGED", e);
		}
		
		internal void report(string eventType, BuildEventArgs e) {
			string projectName = null;
			
			if (e.Project != null) {
				projectName = e.Project.ProjectName;
			}

			string targetName = null;
			
			if (e.Target != null) {
				targetName = e.Target.Name;
			}
			
			string taskName = null;
			
			if (e.Task != null) {
				taskName = e.Task.Name;
			}
			
			string message = e.Message;
			if (message == null && e.Exception != null) {
				message = e.Exception.Message;
			}
			
			if ("MESSAGE_LOGGED".Equals(eventType)) {
				MessagePriority priority;
				
				switch(e.MessageLevel) {
					case Level.Warning: priority = MessagePriority.WARNING; break;
					case Level.Error: priority = MessagePriority.ERROR; break;
					default: priority = MessagePriority.INFO; break;
				}
				
				reporter.SendMessage(eventType, priority, message, null, 0, null);
			} else {
				reporter.SendMessage(eventType, projectName, targetName, taskName, message);
			}

		}
	}
}
