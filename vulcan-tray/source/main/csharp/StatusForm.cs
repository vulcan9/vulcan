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
using System.Reflection;
using System.Runtime.InteropServices;
using System.Security;
using System.Windows.Forms;
using System.Xml;

namespace SourceForge.Vulcan.Tray
{
	public partial class StatusForm : Form
	{
		private readonly BrowserIntegration browserIntegration;
		private readonly PreferenceStore preferenceStore;
		private readonly StatusMonitor statusMonitor;
		private Preferences preferences;
		
		internal StatusForm(StatusMonitor statusMonitor, BrowserIntegration browserIntegration, PreferenceStore preferenceStore)
		{
			this.browserIntegration = browserIntegration;
			this.preferenceStore = preferenceStore;

			this.statusMonitor = statusMonitor;
			
			statusMonitor.DataLoaded += onXmlDataLoaded;

			preferences = preferenceStore.Load();

			MenuItem settingsMenuItem = new MenuItem("Settings...", new EventHandler(onSettingsClicked));
			MenuItem separatorMenuItem = new MenuItem("-");
			MenuItem exitMenuItem = new MenuItem("Exit", new EventHandler(onMenuExitClick));
			MenuItem fileMenu = new MenuItem("File", new MenuItem[] { settingsMenuItem, separatorMenuItem, exitMenuItem });
			MainMenu mainMenu = new MainMenu(new MenuItem[] {fileMenu});
			this.Menu = mainMenu;

			// .NET 1.1 compat
                        EventInfo formClosingEvent = GetType().GetEvent("FormClosing");
                        if (formClosingEvent != null)
                        {
                                formClosingEvent.AddEventHandler(this, new FormClosingEventHandler(this.onClosing));
			}
			
			InitializeComponent();

			configure();
			
			timer.Enabled = true;
		}

		public Preferences Preferences
		{
			get { return preferences; }
		}

		public void Restore()
		{
			if (!Visible)
			{
				Show();
			}

			if (WindowState == FormWindowState.Minimized)
			{
				WindowState = FormWindowState.Normal;
			}

			NativeMethods.SetForegroundWindow(Handle);
		}

		#region Event Handlers
		
		private void onClosing(object sender, FormClosingEventArgs e)
		{
			if (e.CloseReason == CloseReason.UserClosing)
			{
				e.Cancel = true;
				Hide();
			}
		}

		private void onMenuExitClick(object sender, EventArgs e)
		{
			Dispose();
		}

		private void onRefreshClick(object sender, EventArgs e)
		{
			updateProjectStatus();
		}

		private void onLoad(object sender, EventArgs e)
		{
			updateProjectStatus();
		}

		private void onXmlDataLoaded(object source, DataLoadedEventArgs e)
		{
			/* Filthy dirty hack:  I can't figure out how to get the
			 * DataGridView to display an attribute on a nested element.
			 * Instead, hack the nested element to have text equal to
			 * the attribute that I wan't to display.
			 */
			foreach (XmlNode node in e.Document.SelectNodes("//timestamp"))
			{
				if (node.Attributes["age"] == null)
				{
					continue;
				}

				XmlNode age = e.Document.CreateNode(XmlNodeType.Element, "age", "");
				age.InnerText = node.Attributes["age"].Value;

				node.ParentNode.AppendChild(age);
			}

			xmlDataSet.Clear();
			xmlDataSet.ReadXml(new XmlNodeReader(e.Document));

			try
			{
				dataGrid.DataSource = xmlDataSet;
				dataGrid.DataMember = "project";
			}
			catch (NotImplementedException)
			{
				// mono throws this
			}
		}
		
		private void onTick(object sender, EventArgs e)
		{
			statusMonitor.Reload();
		}
		
		private void onCellDoubleClick(object sender, DataGridViewCellEventArgs e)
		{
			string text = (string)dataGrid.Rows[e.RowIndex].Cells[0].Value;
			browserIntegration.OpenBuildReport(text, null);
		}

		private void onSettingsClicked(object sender, EventArgs e)
		{
			ConfigForm configForm = new ConfigForm(Preferences);

			configForm.ShowDialog(this);

			if (configForm.DialogResult == DialogResult.OK)
			{
				this.preferences = configForm.Preferences;
				configure();
				this.preferenceStore.Save(this.Preferences);
			}
		}

		#endregion

		#region Private Methods
		
		private void configure()
		{
			browserIntegration.Url = Preferences.Url;
			statusMonitor.Url = Preferences.Url;
			timer.Interval = Preferences.Interval;
		}

		private void updateProjectStatus()
		{
			string baseUrl = Preferences.Url;
			
			if (!baseUrl.EndsWith("/"))
			{
				baseUrl += "/";
			}

			statusMonitor.Url = baseUrl + "projects.jsp";
			statusMonitor.Reload();
		}
		#endregion
	}

	[SuppressUnmanagedCodeSecurity]
	internal sealed class NativeMethods
	{
		[DllImport("user32.dll")]
		[return: MarshalAs(UnmanagedType.Bool)]
		public static extern bool SetForegroundWindow(IntPtr handle);
	}
}
