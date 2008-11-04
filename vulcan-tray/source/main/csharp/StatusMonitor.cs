/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2007 Chris Eldredge
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
using System.IO;
using System.Net;
using System.Xml;

namespace SourceForge.Vulcan.Tray
{
	internal class StatusMonitor
	{
		public delegate void DataLoadedHandler(object source, DataLoadedEventArgs e);
		public event DataLoadedHandler DataLoaded;

		public delegate void DataLoadErrorHandler(object source, DataLoadErrorEventArgs e);
		public event DataLoadErrorHandler DataLoadError;
		
		public delegate void NewBuildHandler(object source, NewBuildEventArgs e);
		public event NewBuildHandler NewBuildAvailable;

		public delegate void DashboardStatusChangedHandler(object source, DashboardStatusChangedEventArgs e);
		public event DashboardStatusChangedHandler DashboardStatusChanged;

		private readonly CookieContainer cookieContainer = new CookieContainer();

		private string url;
		private DashboardStatus lastStatus;
		private DateTime lastUpdate = DateTime.MinValue;

		public string Url
		{
			get { return url; }
			set
			{
				if (url != null && !url.Equals(value))
				{
					lastUpdate = DateTime.MinValue;	
				}
				url = value;
			}
		}

		public void Reload()
		{
			XmlDocument doc;

			try
			{
				doc = LoadXml();	
			}
			catch (WebException e)
			{
				if (DataLoadError != null)
				{
					DataLoadError(this, new DataLoadErrorEventArgs(e));
				}

				return;
			}
			
			if (DataLoaded != null)
			{
				DataLoaded(this, new DataLoadedEventArgs(doc));
			}

			DetectStateChanges(doc);
		}

		internal virtual XmlDocument LoadXml()
		{
			HttpWebRequest webreq = ((HttpWebRequest)(WebRequest.Create(url)));
			webreq.CookieContainer = cookieContainer;

			XmlDocument doc = new XmlDocument();

			using (WebResponse response = webreq.GetResponse())
			{
				doc.Load(new StreamReader(response.GetResponseStream()));
			}

			return doc;
		}

		private void DetectStateChanges(XmlNode doc)
		{
			XmlNodeList projects = doc.SelectNodes("/projects/project");
			DateTime newestUpdate = DateTime.MinValue;

			bool failuresPresent = false;
			bool currentlyBuilding = false;
			
			foreach (XmlNode project in projects)
			{
				string status = project.SelectSingleNode("status").InnerText;

				if ("BUILDING".Equals(status))
				{
					currentlyBuilding = true;
					XmlNode previousStatusNode = project.SelectSingleNode("previous-status");
					string previousStatusText = "";
					
					if (previousStatusNode != null)
					{
						previousStatusText = previousStatusNode.InnerText;
					}
					ParseFailure(previousStatusText, ref failuresPresent);
				}
				else
				{
					ParseFailure(status, ref failuresPresent);
				}

				XmlNode timestampNode = project.SelectSingleNode("timestamp");
				
				if (timestampNode == null)
				{
					continue;
				}
				
				string dtStr = timestampNode.InnerText;
				
				if (string.IsNullOrEmpty(dtStr))
				{
					continue;
				}
				
				DateTime dt = DateTime.ParseExact(dtStr, "yyyy-MM-dd HH:mm:ss", null);
				
				if (dt > this.lastUpdate)
				{
					FireNewBuildAvailable(project);
				}
				
				if (dt > newestUpdate)
				{
					newestUpdate = dt;
				}
			}

			this.lastUpdate = newestUpdate;

			DashboardStatus newStatus = new DashboardStatus(failuresPresent, currentlyBuilding);
			if (!newStatus.Equals(lastStatus))
			{
				FireDashboardStatusChanged(newStatus);
			}

			lastStatus = newStatus;
		}

		private void FireDashboardStatusChanged(DashboardStatus status)
		{
			if (DashboardStatusChanged != null)
			{
				DashboardStatusChanged(this, new DashboardStatusChangedEventArgs(status));
			}
		}

		private void FireNewBuildAvailable(XmlNode project)
		{
			if (NewBuildAvailable != null)
			{
				NewBuildAvailable(this, new NewBuildEventArgs(project));
			}
		}

		private static void ParseFailure(string statusText, ref bool failuresPresent)
		{
			if ("FAIL".Equals(statusText) || "SKIP".Equals(statusText) || "ERROR".Equals(statusText))
			{
				failuresPresent = true;
			}
		}
	}

	internal class DashboardStatusChangedEventArgs : EventArgs
	{
		private readonly DashboardStatus status;

		public DashboardStatusChangedEventArgs(DashboardStatus status)
		{
			this.status = status;
		}

		public DashboardStatus Status
		{
			get { return status; }
		}
	}

	internal class DataLoadErrorEventArgs : EventArgs 
	{
		private readonly Exception cause;

		public Exception Cause
		{
			get { return cause; }
		}
		
		public DataLoadErrorEventArgs(Exception cause)
		{
			this.cause = cause;
		}
	}

	internal class NewBuildEventArgs : EventArgs
	{
		private readonly XmlNode projectNode;
		
		public XmlNode ProjectNode
		{
			get { return projectNode; }
		}
		
		public NewBuildEventArgs(XmlNode projectNode)
		{
			this.projectNode = projectNode;
		}
	}

	internal class DataLoadedEventArgs : EventArgs
	{
		private readonly XmlDocument document;

		public XmlDocument Document
		{
			get { return document; }
		}

		public DataLoadedEventArgs(XmlDocument document)
		{
			this.document = document;
		}
	}
}
