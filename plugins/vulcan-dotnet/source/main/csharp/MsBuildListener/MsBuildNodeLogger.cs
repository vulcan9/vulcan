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
using System.Collections.Generic;
using System.IO;
using Microsoft.Build.Framework;

namespace SourceForge.Vulcan.DotNet {
	public class MsBuildNodeLogger : MsBuildListener, INodeLogger
	{
		private readonly Dictionary<BuildEventContext, string> projectStartedEvents = new Dictionary<BuildEventContext, string>();

		public void Initialize(IEventSource eventSource, int nodeCount)
		{
			Initialize(eventSource);
			eventSource.ProjectStarted += ProjectStarted;
		}

		protected override FileInfo GetCurrentProjectFile(BuildEventArgs e)
		{
			var fileName = GetProjectFile(e.BuildEventContext);
			if (fileName == null || string.IsNullOrEmpty(fileName))
			{
				return null;
			}

			return new FileInfo(fileName);
		}

		public void ProjectStarted(object sender, ProjectStartedEventArgs e)
		{
			AddProjectStartedEvent(e.ProjectFile, e.BuildEventContext);
		}

		public override void TargetStarted(object sender, TargetStartedEventArgs e)
		{
			base.TargetStarted(sender, e);
			AddProjectStartedEvent(e.ProjectFile, e.BuildEventContext);
		}

		public override void TaskStarted(object sender, TaskStartedEventArgs e)
		{
			base.TaskStarted(sender, e);
			AddProjectStartedEvent(e.ProjectFile, e.BuildEventContext);
		}

		internal string GetProjectFile(BuildEventContext e)
		{
			string file;

			lock (projectStartedEvents)
			{
				projectStartedEvents.TryGetValue(e, out file);
			}

			return file;
		}

		internal void AddProjectStartedEvent(string projectFile, BuildEventContext context)
		{
			lock (this.projectStartedEvents)
			{
				if (!this.projectStartedEvents.ContainsKey(context))
				{
					this.projectStartedEvents.Add(context, projectFile);
				}
			}
		}
	}
}
