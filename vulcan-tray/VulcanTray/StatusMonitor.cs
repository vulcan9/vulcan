using System;
using System.Net;
using System.Xml;

namespace SourceForge.Vulcan.Tray
{
	internal class DashboardStatus
	{
		private readonly bool failuresPresent;
		private readonly bool currentlyBuilding;
		
		internal DashboardStatus(bool failuresPresent, bool currentlyBuilding)
		{
			this.failuresPresent = failuresPresent;
			this.currentlyBuilding = currentlyBuilding;
		}

		internal bool CurrentlyBuilding
		{
			get { return currentlyBuilding; }
		}

		internal bool FailuresPresent
		{
			get { return failuresPresent; }
		}
		
		internal DashboardStatus Clone()
		{
			return (DashboardStatus) MemberwiseClone();
		}
		
		public override bool Equals(object o)
		{
			if (this == o)
			{
				return true;
			}
			
			DashboardStatus other = o as DashboardStatus;
			
			if (other == null)
			{
				return false;
			}

			return failuresPresent == other.failuresPresent && currentlyBuilding == other.currentlyBuilding;
		}
	}
	
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
			XmlDocument doc = new XmlDocument();

			try
			{
				doc.Load(url);	
			}
			catch (WebException e)
			{
				if (DataLoadError != null)
				{
					DataLoadError(this, new DataLoadErrorEventArgs(e));
				}
			}
			
			
			if (DataLoaded != null)
			{
				DataLoaded(this, new DataLoadedEventArgs(doc));
			}

			detectStateChanges(doc);
		}
		
		private void detectStateChanges(XmlDocument doc)
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
					parseFailure(previousStatusText, ref failuresPresent);
				}
				else
				{
					parseFailure(status, ref failuresPresent);
				}
						
				string dtStr = project.SelectSingleNode("timestamp").InnerText;
				
				if (string.IsNullOrEmpty(dtStr))
				{
					continue;
				}
				
				DateTime dt = DateTime.ParseExact(dtStr, "yyyy-MM-dd HH:mm:ss", null);
				
				if (dt > this.lastUpdate)
				{
					fireStatusUpdated(project);
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
				fireDashboardStatusChanged(newStatus);
			}

			lastStatus = newStatus;
		}

		private void fireDashboardStatusChanged(DashboardStatus status)
		{
			if (DashboardStatusChanged != null)
			{
				DashboardStatusChanged(this, new DashboardStatusChangedEventArgs(status));
			}
		}

		private void fireStatusUpdated(XmlNode project)
		{
			if (NewBuildAvailable != null)
			{
				NewBuildAvailable(this, new NewBuildEventArgs(project));
			}
		}

		private static void parseFailure(string statusText, ref bool failuresPresent)
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
