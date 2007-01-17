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
using System.Text;

using Microsoft.Build.Framework;

namespace SourceForge.Vulcan.DotNet {
	public class MsBuildListener : ILogger	{
		string parameters;
		LoggerVerbosity verbosity;
		UdpBuildReporter reporter;

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

			reporter = new UdpBuildReporter(map["hostname"], int.Parse((string)map["port"]));

			eventSource.BuildStarted += new BuildStartedEventHandler(buildStarted);
			eventSource.BuildFinished += new BuildFinishedEventHandler(buildFinished);
			eventSource.TargetStarted += new TargetStartedEventHandler(targetStarted);
			eventSource.TargetFinished += new TargetFinishedEventHandler(targetFinished);
			eventSource.TaskStarted += new TaskStartedEventHandler(taskStarted);
			eventSource.TaskFinished += new TaskFinishedEventHandler(taskFinished);
			eventSource.ErrorRaised += new BuildErrorEventHandler(buildErrorRaised);
			eventSource.WarningRaised += new BuildWarningEventHandler(buildWarningRaised);
		}

		public void Shutdown()
		{
			
		}

		void buildStarted(object sender, BuildStartedEventArgs e)
		{
			reporter.SendMessage("BUILD_STARTED", null, null, null, e.Message);
		}
		void buildFinished(object sender, BuildFinishedEventArgs e)
		{
			reporter.SendMessage("BUILD_FINISHED", null, null, null, e.Message);
		}
		void targetStarted(object sender, TargetStartedEventArgs e)
		{
			reporter.SendMessage("TARGET_STARTED", e.ProjectFile, e.TargetName, null, e.Message);
		}
		void targetFinished(object sender, TargetFinishedEventArgs e)
		{
			reporter.SendMessage("TARGET_FINISHED", e.ProjectFile, e.TargetName, null, e.Message);
		}
		void taskStarted(object sender, TaskStartedEventArgs e)
		{
			reporter.SendMessage("TASK_STARTED", e.ProjectFile, null, e.TaskName, e.Message);
		}
		void taskFinished(object sender, TaskFinishedEventArgs e)
		{
			reporter.SendMessage("TASK_FINISHED", e.ProjectFile, null, e.TaskName, e.Message);
		}
		void buildErrorRaised(object sender, BuildErrorEventArgs e)
		{
			reporter.SendMessage("MESSAGE_LOGGED", MessagePriority.ERROR, e.Message,
			                     e.File, e.LineNumber, e.Code);
		}
		void buildWarningRaised(object sender, BuildWarningEventArgs e)
		{
			reporter.SendMessage("MESSAGE_LOGGED", MessagePriority.WARNING, e.Message,
													 e.File, e.LineNumber, e.Code);
		}

	}
}
