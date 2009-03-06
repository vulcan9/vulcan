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
		private readonly Dictionary<int, Stack<FileInfo>> projectsByNodeId = new Dictionary<int, Stack<FileInfo>>();

		public void Initialize(IEventSource eventSource, int nodeCount)
		{
			Initialize(eventSource);
		}

		public override void TargetStarted(object sender, TargetStartedEventArgs e)
		{
			base.TargetStarted(sender, e);
			PushProjectFile(e.BuildEventContext.NodeId, e.ProjectFile);
		}

		public override void TargetFinished(object sender, TargetFinishedEventArgs e)
		{
			base.TargetFinished(sender, e);
			PopProjectFile(e.BuildEventContext.NodeId);
		}

		public override void TaskStarted(object sender, TaskStartedEventArgs e)
		{
			base.TaskStarted(sender, e);
			PushProjectFile(e.BuildEventContext.NodeId, e.ProjectFile);
		}

		public override void TaskFinished(object sender, TaskFinishedEventArgs e)
		{
			base.TaskFinished(sender, e);
			PopProjectFile(e.BuildEventContext.NodeId);
		}

		protected override FileInfo GetCurrentProjectFile(BuildEventArgs e)
		{
			Stack<FileInfo> stack;

			lock (projectsByNodeId)
			{
				if (!projectsByNodeId.TryGetValue(e.BuildEventContext.NodeId, out stack) || stack.Count == 0)
				{
					return null;
				}
				return stack.Peek();	
			}

		}

		private void PushProjectFile(int nodeId, string projectFile)
		{
			lock(projectsByNodeId)
			{
				Stack<FileInfo> stack;
				if (!projectsByNodeId.TryGetValue(nodeId, out stack))
				{
					stack = new Stack<FileInfo>();
					projectsByNodeId[nodeId] = stack;
				}
				stack.Push(new FileInfo(projectFile));
			}
		}

		private void PopProjectFile(int nodeId)
		{
			lock (projectsByNodeId)
			{
				projectsByNodeId[nodeId].Pop();
			}
		}
	}
}
