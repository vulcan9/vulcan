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
using System;
using System.Collections.Generic;
using System.IO;
using Microsoft.Build.Framework;

namespace SourceForge.Vulcan.DotNet {
	public class MsBuildListener : ILogger	{
		private string parameters;

		private LoggerVerbosity verbosity;
		private IBuildMessageReporter reporter;

		private readonly IDictionary<int, Stack<FileInfo>> projectFiles = new Dictionary<int, Stack<FileInfo>>();

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

		public void TargetStarted(object sender, TargetStartedEventArgs e)
		{
			reporter.SendMessage("TARGET_STARTED", e.ProjectFile, e.TargetName, null, e.Message);
			PushProjectFile(e.ThreadId, e.ProjectFile);
		}

		public void TargetFinished(object sender, TargetFinishedEventArgs e)
		{
			reporter.SendMessage("TARGET_FINISHED", e.ProjectFile, e.TargetName, null, e.Message);
			PopProjectFile(e.ThreadId);
		}

		public void TaskStarted(object sender, TaskStartedEventArgs e)
		{
			reporter.SendMessage("TASK_STARTED", e.ProjectFile, null, e.TaskName, e.Message);
			PushProjectFile(e.ThreadId, e.ProjectFile);
		}

		public void TaskFinished(object sender, TaskFinishedEventArgs e)
		{
			reporter.SendMessage("TASK_FINISHED", e.ProjectFile, null, e.TaskName, e.Message);
			PopProjectFile(e.ThreadId);
		}

		public void BuildErrorRaised(object sender, BuildErrorEventArgs e)
		{
			SendMessageWithPriority(MessagePriority.ERROR, e.Message, e.File, e.LineNumber, e.Code, e.ThreadId);
		}

		public void BuildWarningRaised(object sender, BuildWarningEventArgs e)
		{
			SendMessageWithPriority(MessagePriority.WARNING, e.Message, e.File, e.LineNumber, e.Code, e.ThreadId);
		}

		private void SendMessageWithPriority(MessagePriority prio, string message, string file, int lineNumber, string code, int threadId)
		{
			Stack<FileInfo> stack;
			if (!string.IsNullOrEmpty(file) && !Path.IsPathRooted(file) && projectFiles.TryGetValue(threadId, out stack) && stack.Count > 0)
			{
				file = Path.Combine(stack.Peek().DirectoryName, file);
			}
			reporter.SendMessage("MESSAGE_LOGGED", prio, message,
													 file, lineNumber, code);
		}

		private void PushProjectFile(int threadId, string projectFile)
		{
			Stack<FileInfo> stack;
			if (!projectFiles.TryGetValue(threadId, out stack))
			{
				stack = new Stack<FileInfo>();
				projectFiles[threadId] = stack;
			}

			stack.Push(new FileInfo(projectFile));
		}

		private void PopProjectFile(int threadId)
		{
			Stack<FileInfo> stack;
			if (projectFiles.TryGetValue(threadId, out stack))
			{
				stack.Pop();
			}
		}
	}
}
