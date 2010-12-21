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
using System.Drawing;
using System.Reflection;
using System.Timers;
using System.Windows.Forms;
using System.Xml;
using Timer=System.Timers.Timer;

namespace SourceForge.Vulcan.Tray
{
	public class VulcanNotifyIcon : IDisposable
	{
		private static readonly Icon[] Blank = { Icons.Blank };
		private static readonly Icon[] Pass = { Icons.Pass };
		private static readonly Icon[] Fail = { Icons.Fail };
		private static readonly Icon[] BuildingPassing = {
		 Icons.BuildPass01,
		 Icons.BuildPass02,
		 Icons.BuildPass03,
		 Icons.BuildPass04,
		 Icons.BuildPass05,
		 Icons.BuildPass06,
		 Icons.BuildPass07,
		 Icons.BuildPass08,
		 Icons.BuildPass09,
		 Icons.BuildPass10,
		 Icons.BuildPass11,
		 Icons.BuildPass12,
		 Icons.BuildPass13,
		 Icons.BuildPass14,
		 Icons.BuildPass15,
		 Icons.BuildPass16,
    };
		private static readonly Icon[] BuildingFailing = {
		 Icons.BuildFail01,
		 Icons.BuildFail02,
		 Icons.BuildFail03,
		 Icons.BuildFail04,
		 Icons.BuildFail05,
		 Icons.BuildFail06,
		 Icons.BuildFail07,
		 Icons.BuildFail08,
		 Icons.BuildFail09,
		 Icons.BuildFail10,
		 Icons.BuildFail11,
		 Icons.BuildFail12,
		 Icons.BuildFail13,
		 Icons.BuildFail14,
		 Icons.BuildFail15,
		 Icons.BuildFail16,
    };
		
		private const int AnimationInterval = 100;
		
		private readonly bool ballonsSupported;
		private readonly StatusForm statusForm;
		private readonly BrowserIntegration browserIntegration;
		private readonly NotifyIcon notifyIcon;
		private readonly Timer timer;
		
		private Icon[] activeIcons;
		private int currentIconIndex;
		
		private XmlNode latestProject;
		private string lastMessage;

		internal VulcanNotifyIcon(StatusForm statusForm, StatusMonitor statusMonitor, BrowserIntegration browserIntegration)
		{
			this.statusForm = statusForm;
			this.browserIntegration = browserIntegration;

			this.activeIcons = Blank;
			this.currentIconIndex = 0;

			this.timer = new Timer(AnimationInterval);
			this.timer.Elapsed += new ElapsedEventHandler(this.onTimerElapsed);
			this.timer.Start();
			
			MenuItem[] items = new MenuItem[] {
    		new MenuItem("Restore", new EventHandler(onMenuRestore)),
    		new MenuItem("Go to Dashboard", new EventHandler(onMenuOpenDashboard)),
    		new MenuItem("-"),
    		new MenuItem("Exit", new EventHandler(onMenuExit))
      };
			
			items[0].DefaultItem = true;
			
			notifyIcon = new NotifyIcon();

			notifyIcon.ContextMenu = new ContextMenu(items);
			notifyIcon.Icon = Icons.Blank;
			notifyIcon.Text = "Vulcan Tray Notifier";
			notifyIcon.Visible = true;
			notifyIcon.DoubleClick += new EventHandler(this.onDoubleClick);

			statusMonitor.DataLoadError += new StatusMonitor.DataLoadErrorHandler(this.onLoadError);
			statusMonitor.NewBuildAvailable += new StatusMonitor.NewBuildHandler(this.onNewBuild);
			statusMonitor.DashboardStatusChanged += new StatusMonitor.DashboardStatusChangedHandler(this.onDashboardStatusChanged);

			// .NET 1.1 compat
			EventInfo balloonTipClickedEvent = notifyIcon.GetType().GetEvent("BalloonTipClicked");
			if (balloonTipClickedEvent != null)
			{
				balloonTipClickedEvent.AddEventHandler(notifyIcon, new EventHandler(this.onBalloonTipClicked));
				ballonsSupported = true;
			}
			else
			{
				ballonsSupported = false;
			}
		}

		public void Dispose()
		{
			timer.Stop();
			notifyIcon.Dispose();
			statusForm.Dispose();
		}

		private void onTimerElapsed(object sender, ElapsedEventArgs e)
		{
			Icon icon = Icons.Blank;
			
			lock(activeIcons)
			{
				if (activeIcons.Length > 0)
				{
					currentIconIndex++;
					if (currentIconIndex >= activeIcons.Length)
					{
						currentIconIndex = 0;
					}

					icon = activeIcons[currentIconIndex];
				}
			}

			if (notifyIcon.Icon != icon)
			{
				notifyIcon.Icon = icon;
			}
		}

		private void onDashboardStatusChanged(object source, DashboardStatusChangedEventArgs e)
		{
			lock(activeIcons)
			{
				if (e.Status.FailuresPresent)
				{
					if (e.Status.CurrentlyBuilding)
					{
						activeIcons = BuildingFailing;
					}
					else
					{
						activeIcons = Fail;
					}
				}
				else
				{
					if (e.Status.CurrentlyBuilding)
					{
						activeIcons = BuildingPassing;
					}
					else
					{
						activeIcons = Pass;	
					}
				}
			}
		}

		private void onLoadError(object source, DataLoadErrorEventArgs e)
		{
			latestProject = null;
			lock (activeIcons)
			{
				activeIcons = Blank;
			}
			
			displayBubble("Data Load Failure", e.Cause.Message, ToolTipIcon.Warning);
		}

		private void onNewBuild(object source, NewBuildEventArgs e)
		{
			latestProject = e.ProjectNode;

			string projectName = latestProject.Attributes["name"].Value;
			string status = latestProject.SelectSingleNode("status").InnerText;
			string message = latestProject.SelectSingleNode("message").InnerText;

			bool isPass = "PASS".Equals(status);

			bool shouldBubble = (isPass && statusForm.Preferences.BubbleSuccess)
				|| (!isPass && statusForm.Preferences.BubbleFailures);

			if (!shouldBubble)
			{
				return;
			}

			string title = string.Format(
				"{0}: {1}",
				projectName,
				status);

			string text = string.Format(
				"{0}",
				message);

			ToolTipIcon icon = ToolTipIcon.Error;
			
			if (isPass)
			{
				icon = ToolTipIcon.Info;
			}

			displayBubble(title, text, icon);	
		}

		private void onDoubleClick(object sender, EventArgs e)
		{
			statusForm.Restore();
		}

		private void onMenuRestore(object sender, EventArgs e)
		{
			statusForm.Restore();
		}

		private void onMenuExit(object sender, EventArgs e)
		{
			Dispose();
		}

		private void onMenuOpenDashboard(object sender, EventArgs e)
		{
			browserIntegration.OpenDashboard();
		}

		private void onBalloonTipClicked(object sender, EventArgs e)
		{
			if (latestProject == null)
			{
				return;
			}

			string projectName = latestProject.Attributes["name"].Value;
			string buildNumber = latestProject.SelectSingleNode("build-number").InnerText;

			browserIntegration.OpenBuildReport(projectName, buildNumber);
		}

		private void displayBubble(string title, string text, ToolTipIcon icon)
		{
			if (!ballonsSupported || (lastMessage != null && lastMessage.Equals(text)))
			{
				return;
			}

			lastMessage = text;

			// Use reflection to avoid runtime errors in Mono/.NET 1.1.
			Type type = notifyIcon.GetType();
			type.GetProperty("BalloonTipTitle").SetValue(notifyIcon, title, null);
			type.GetProperty("BalloonTipText").SetValue(notifyIcon, text, null);
			type.GetProperty("BalloonTipIcon").SetValue(notifyIcon, icon, null);
			
			MethodInfo method = type.GetMethod("ShowBalloonTip", new Type[] {typeof(int)});
			
			method.Invoke(notifyIcon, new object[] { 10000 });
		}
	}
}