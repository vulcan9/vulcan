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

using System;
using System.Net;
using System.Xml;
using NUnit.Framework;

namespace SourceForge.Vulcan.Tray
{
	[TestFixture]
	public class StatusMonitorTest
	{
		private StatusMonitorStub monitor;
		private XmlDocument fakeStatus;
		private XmlElement projectNodes;
		private DataLoadErrorEventArgs errorEventArgs;
		private DataLoadedEventArgs loadEventArgs;
		private DashboardStatusChangedEventArgs statusChangedEventArgs;
		private NewBuildEventArgs newBuildEventArgs;

		[SetUp]
		public void SetUp()
		{
			monitor = new StatusMonitorStub();
			monitor.DataLoadError += monitor_DataLoadError;
			monitor.DataLoaded += monitor_DataLoaded;
			monitor.DashboardStatusChanged += monitor_DashboardStatusChanged;
			monitor.NewBuildAvailable += monitor_NewBuildAvailable;
			fakeStatus = new XmlDocument();

			projectNodes = fakeStatus.CreateElement("projects");
			
			fakeStatus.AppendChild(projectNodes);
			
			monitor.Document = fakeStatus;

			errorEventArgs = null;
			loadEventArgs = null;
			statusChangedEventArgs = null;
			newBuildEventArgs = null;
		}

		[Test]
		public void ReloadHandlesWebException()
		{
			monitor.Exception = new WebException();
			
			monitor.Reload();

			Assert.IsNotNull(errorEventArgs, "DataLoadError was not fired.");
			Assert.AreSame(monitor.Exception, errorEventArgs.Cause);

			Assert.IsNull(loadEventArgs, "Should not fire DataLoaded on error");
			Assert.IsNull(statusChangedEventArgs, "Should not fire DashboardStatusChanged on error");
			Assert.IsNull(newBuildEventArgs, "Should not fire NewBuildAvailable on error");
		}

		[Test]
		public void DoesNotFireEventsOnUnhandledException()
		{
			monitor.Exception = new Exception("bug in program");

			try
			{
				monitor.Reload();
				Assert.Fail("Expected Exception.");
			}
			catch (Exception e)
			{
				Assert.AreSame(monitor.Exception, e);
			}
			
			Assert.IsNull(errorEventArgs, "Should not fire DataLoadError on unhandled exception.");
			Assert.IsNull(loadEventArgs, "Should not fire DataLoaded on error");
			Assert.IsNull(statusChangedEventArgs, "Should not fire DashboardStatusChanged on error");
			Assert.IsNull(newBuildEventArgs, "Should not fire NewBuildAvailable on error");
		}
		
		[Test]
		public void FiresLoadEvent()
		{
			monitor.Reload();

			Assert.IsNotNull(loadEventArgs, "DataLoaded was not fired.");
			Assert.AreSame(fakeStatus, loadEventArgs.Document);
		}

		[Test]
		public void DoesNotFireNewBuildOnNoTimestamp()
		{
			AddFakeProjectStatus("a", "Not Built", null);
			AddFakeProjectStatus("b", "Not Built", null);

			monitor.Reload();

			Assert.IsNull(newBuildEventArgs, "Should not fire NewBuildAvailable on missing timestamp");
		}
		
		[Test]
		public void FiresNewBuildAvailable()
		{
			XmlElement proj = AddFakeProjectStatus("a", "PASS", "2006-12-31 12:00:00");
			
			monitor.Reload();

			Assert.IsNotNull(newBuildEventArgs, "Did not fire NewBuildAvailable event");
			Assert.AreSame(proj, newBuildEventArgs.ProjectNode);
		}

		[Test]
		public void DoesNotFiresNewBuildAvailableOnSameStatus()
		{
			AddFakeProjectStatus("a", "PASS", "2006-12-31 12:00:00");

			monitor.Reload();

			newBuildEventArgs = null;

			monitor.Reload();
			
			Assert.IsNull(newBuildEventArgs, "Should not fire NewBuildAvailable when timestamp does not change.");
		}

		[Test]
		public void DoesNotFiresNewBuildAvailableOnSameStatusAfterLoadError()
		{
			AddFakeProjectStatus("a", "PASS", "2006-12-31 12:00:00");

			monitor.Reload();

			newBuildEventArgs = null;

			monitor.Exception = new WebException();
			monitor.Reload();

			monitor.Exception = null;
			monitor.Reload();
			
			Assert.IsNull(newBuildEventArgs, "Should not fire NewBuildAvailable when timestamp does not change.");
		}
		
		XmlElement AddFakeProjectStatus(string name, string status, string timestamp)
		{
			XmlElement projectNode = fakeStatus.CreateElement("project");
			
			projectNode.SetAttribute("name", name);

			XmlElement statusNode = fakeStatus.CreateElement("status");
			
			statusNode.InnerText = status;
			
			projectNode.AppendChild(statusNode);

			if (timestamp != null)
			{
				XmlElement timestampNode = fakeStatus.CreateElement("timestamp");
				projectNode.AppendChild(timestampNode);
				timestampNode.InnerText = timestamp;
			}

			projectNodes.AppendChild(projectNode);
			
			return projectNode;
		}
		
		void monitor_DataLoadError(object source, DataLoadErrorEventArgs e)
		{
			errorEventArgs = e;
		}

		void monitor_DataLoaded(object source, DataLoadedEventArgs e)
		{
			loadEventArgs = e;
		}

		void monitor_DashboardStatusChanged(object source, DashboardStatusChangedEventArgs e)
		{
			statusChangedEventArgs = e;
		}

		void monitor_NewBuildAvailable(object source, NewBuildEventArgs e)
		{
			newBuildEventArgs = e;
		}
	}
	
	internal class StatusMonitorStub : StatusMonitor
	{
		private XmlDocument document;
		private Exception exception;
		
		public XmlDocument Document
		{
			get { return document; }
			set { document = value; }
		}

		public Exception Exception
		{
			get { return exception; }
			set { exception = value; }
		}

		protected override XmlDocument LoadXml()
		{
			if (exception != null)
			{
				throw exception;
			}
			
			return document;
		}
	}
}
