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
using System;
using System.Collections.Generic;
using System.IO;
using Microsoft.Build.Framework;

namespace SourceForge.Vulcan.DotNet {
	public class MsBuildListener : ILogger	{
		private string parameters;

		private LoggerVerbosity verbosity;
		private IBuildMessageReporter reporter;

		private readonly Stack<FileInfo> projectFiles = new Stack<FileInfo>();

		public string Parameters
		{
			get
			{
				return parameters;
			}
			set
			{
				parameters = value;
			}
		}

		public LoggerVerbosity Verbosity
		{
			get
			{
				return verbosity;
			}
			set
			{
				verbosity = value;
			}
		}

		public IBuildMessageReporter Reporter
		{
			set { reporter = value; }
		}

		public void Initialize(IEventSource eventSource)
		{
			if (parameters == null)
			{
				throw new LoggerException("must specify hostname and port");
			}

			String[] keyValues = parameters.Split(',');

			if (keyValues.Length != 2)
			{
				throw new LoggerException("must specify hostname and port");
			}

			SortedDictionary<string, string> map = new SortedDictionary<string, string>();

			foreach(String keyValue in keyValues) {
				String[] pair = keyValue.Split('=');

				if (pair.Length != 2)
				{
					throw new LoggerException("expected syntax key=value but got " + keyValue);
				}

				map.Add(pair[0], pair[1]);
			}

			if (!map.ContainsKey("hostname") || !map.ContainsKey("port"))
			{
				throw new LoggerException("must specify hostname and port");
			}

			reporter = new UdpBuildReporter(map["hostname"], int.Parse(map["port"]));

			eventSource.BuildStarted += BuildStarted;
			eventSource.BuildFinished += BuildFinished;
			eventSource.TargetStarted += TargetStarted;
			eventSource.TargetFinished += TargetFinished;
			eventSource.TaskStarted += TaskStarted;
			eventSource.TaskFinished += TaskFinished;
			eventSource.ErrorRaised += BuildErrorRaised;
			eventSource.WarningRaised += BuildWarningRaised;
		}

		public void Shutdown()
		{
		}

		public void BuildStarted(object sender, BuildStartedEventArgs e)
		{
			reporter.SendMessage("BUILD_STARTED", null, null, null, e.Message);
		}
		public void BuildFinished(object sender, BuildFinishedEventArgs e)
		{
			reporter.SendMessage("BUILD_FINISHED", null, null, null, e.Message);
		}
		public virtual void TargetStarted(object sender, TargetStartedEventArgs e)
		{
			reporter.SendMessage("TARGET_STARTED", e.ProjectFile, e.TargetName, null, e.Message);
			projectFiles.Push(new FileInfo(e.ProjectFile));
		}
		public virtual void TargetFinished(object sender, TargetFinishedEventArgs e)
		{
			reporter.SendMessage("TARGET_FINISHED", e.ProjectFile, e.TargetName, null, e.Message);
			projectFiles.Pop();
		}
		public virtual void TaskStarted(object sender, TaskStartedEventArgs e)
		{
			reporter.SendMessage("TASK_STARTED", e.ProjectFile, null, e.TaskName, e.Message);
			projectFiles.Push(new FileInfo(e.ProjectFile));
		}
		public virtual void TaskFinished(object sender, TaskFinishedEventArgs e)
		{
			reporter.SendMessage("TASK_FINISHED", e.ProjectFile, null, e.TaskName, e.Message);
			projectFiles.Pop();
		}
		public void BuildErrorRaised(object sender, BuildErrorEventArgs e)
		{
			SendMessageWithPriority(e, MessagePriority.ERROR, e.File, e.LineNumber, e.Code);
		}

		public void BuildWarningRaised(object sender, BuildWarningEventArgs e)
		{
			SendMessageWithPriority(e, MessagePriority.WARNING, e.File, e.LineNumber, e.Code);
		}

		protected virtual FileInfo GetCurrentProjectFile(BuildEventArgs e)
		{
			if (projectFiles.Count == 0)
			{
				return null;
			}
			return projectFiles.Peek();
		}

		private void SendMessageWithPriority(BuildEventArgs e, MessagePriority prio, string file, int lineNumber, string code)
		{
			if (!string.IsNullOrEmpty(file) && !Path.IsPathRooted(file))
			{
				FileInfo projectFile = GetCurrentProjectFile(e);
				if (projectFile != null)
				{
					file = Path.Combine(projectFile.DirectoryName, file);	
				}
			}
			reporter.SendMessage("MESSAGE_LOGGED", prio, e.Message,
													 file, lineNumber, code);
		}
	}
}
