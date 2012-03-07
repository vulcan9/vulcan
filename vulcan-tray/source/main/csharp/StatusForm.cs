/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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
using System.Collections.Generic;
using System.Xml;
using System.Text;
using SourceForge.Vulcan.Tray.source.main.csharp;

namespace SourceForge.Vulcan.Tray
{
	public partial class StatusForm : Form
	{
		private readonly BrowserIntegration browserIntegration;
		private readonly PreferenceStore preferenceStore;
		private readonly StatusMonitor statusMonitor;
		private Preferences preferences;

		private IList<string> availableLabels = new List<string>();
		private IList<string> selectedLabels = new List<string>();
		
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

			if (!string.IsNullOrEmpty(Preferences.SelectedLabels))
			{
				SetSelectedLabels(ConstructList(Preferences.SelectedLabels));
			}

			cbProjectLabels.SelectedValueChanged += cbProjectLabels_SelectedValueChanged;
		}

		private void onXmlDataLoaded(object source, DataLoadedEventArgs e)
		{
			/* Filthy dirty hack:  I can't figure out how to get the
			 * DataGridView to display an attribute on a nested element.
			 * Instead, hack the nested element to have text equal to
			 * the attribute that I want to display.
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

			XmlNode availableLabelsNode = e.Document.SelectSingleNode("//available-labels");
			XmlNode selectedLabelsNode = e.Document.SelectSingleNode("//selected-labels");

			if (availableLabelsNode != null && selectedLabelsNode != null)
			{
				BindProjectLabels(availableLabelsNode, selectedLabelsNode);	
			}
		}

		private void BindProjectLabels(XmlNode availableLabels, XmlNode selectedLabels)
		{
			this.availableLabels.Clear();
			this.selectedLabels.Clear();
			cbProjectLabels.Items.Clear();

			foreach (XmlNode currentLabel in availableLabels)
			{
				this.availableLabels.Add(currentLabel.InnerText);
				cbProjectLabels.Items.Add(currentLabel.InnerText);
			}

			foreach (XmlNode currentLabel in selectedLabels)
			{
				this.selectedLabels.Add(currentLabel.InnerText);
			}

			cbProjectLabels.Items.Add("All");
			if (this.availableLabels.Count > 1)
			{
				cbProjectLabels.Items.Add("Multiple...");
			}

			SetSelectedLabels(this.selectedLabels);
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
			
			string queryString = ConstructQueryString(this.selectedLabels);

			if(!string.IsNullOrEmpty(queryString))
			{
			  statusMonitor.Url = statusMonitor.Url + "?" + queryString;
			}

			statusMonitor.Reload();
		}
		#endregion

		private IList<string> ConstructList(string preferenceString)
		{
			if(string.IsNullOrEmpty(preferenceString))
			{
				return new List<string>();
			}

			return new List<string>(preferenceString.Split(','));
		}

		private string ConstructQueryString(IList<string> selectedLabels)
		{
			StringBuilder QueryStringBuilder = new StringBuilder();
			for (int i = 0; i < selectedLabels.Count; i++)
			{
				QueryStringBuilder.Append("label=");
				QueryStringBuilder.Append(selectedLabels[i]);

				//append an ampersand (query string argument delimiter) if we are not
				//at the end of the array
				if(i < selectedLabels.Count - 1)
				{
					QueryStringBuilder.Append("&");
				}
			}

			return QueryStringBuilder.ToString();
		}

		private string ConstructPreferencesString(IList<string> selectedLabels)
		{
			List<string> l = new List<string>(selectedLabels);
			return string.Join(",", l.ToArray());
		}

		private void clbProjectLabels_ItemCheck(object sender, ItemCheckEventArgs e)
		{
			updateProjectStatus();
		}

		private void cbProjectLabels_SelectedValueChanged(object sender, EventArgs e)
		{
			if(cbProjectLabels.SelectedItem as string == "Multiple...")
			{
				MultipleLabelSelectForm selectForm = new MultipleLabelSelectForm();
				IList<string> selectedLabels = selectForm.Show(this.availableLabels, this.selectedLabels);

				if(selectForm.DialogResult == DialogResult.OK)
				{
					SetSelectedLabels(selectedLabels);
				}
			}
			else if(cbProjectLabels.SelectedItem as string == "All")
			{
				this.SetSelectedLabels(new List<string>());
			}
			else
			{
				this.SetSelectedLabel(this.cbProjectLabels.SelectedItem as string);
			}
		}

		private void SetSelectedLabel(string newSelectedLabel)
		{
			List<string> l = new List<string>();
			l.Add(newSelectedLabel);
			this.SetSelectedLabels(l);
		}

		private void SetSelectedLabels(IList<string> newSelectedLabels)
		{
			cbProjectLabels.SelectedValueChanged -= cbProjectLabels_SelectedValueChanged;

			IList<string> oldSelectedLabels = this.selectedLabels;
			this.selectedLabels = newSelectedLabels;

			if (newSelectedLabels.Count == 0)
			{
				cbProjectLabels.SelectedItem = "All";
			}
			else if (selectedLabels.Count == 1)
			{
				cbProjectLabels.SelectedItem = selectedLabels[0];
			}
			else
			{
				cbProjectLabels.SelectedItem = "Multiple...";
			}

			if (SelectedLabelsChanged(oldSelectedLabels, newSelectedLabels))
			{
				updateProjectStatus();
			}

			cbProjectLabels.SelectedValueChanged += cbProjectLabels_SelectedValueChanged;
		}

		private bool SelectedLabelsChanged(IList<string> currentSelectedLabels, IList<string> newSelectedLabels)
		{
			//different counts, so selection changed
			if(currentSelectedLabels.Count != newSelectedLabels.Count)
			{
				return true;
			}

			//both are empty, selection didn't change
			if(currentSelectedLabels.Count == 0 && newSelectedLabels.Count == 0)
			{
				return false;
			}

			//one item per list, so compare the items directly
			if(currentSelectedLabels.Count == 1 && newSelectedLabels.Count == 1)
			{
				return currentSelectedLabels[0] != newSelectedLabels[0];
			}

			//construct a dictionary of one of the lists, then iterate through the other
			//to see if it contains all the items in the first list.
			IDictionary<string, string> currentSelectedLabelsDict = new Dictionary<string, string>();

			foreach (string currentLabel in currentSelectedLabels)
			{
				currentSelectedLabelsDict.Add(currentLabel, currentLabel);
			}

			foreach (string currentLabel in newSelectedLabels)
			{
				if(!currentSelectedLabelsDict.ContainsKey(currentLabel))
				{
					return true;
				}
			}

			return false;
		}

		private void StatusForm_FormClosing(object sender, FormClosingEventArgs e)
		{
			this.Preferences.SelectedLabels = ConstructPreferencesString(this.selectedLabels);
			this.preferenceStore.Save(this.Preferences);
		}


	}

	[SuppressUnmanagedCodeSecurity]
	internal sealed class NativeMethods
	{
		[DllImport("user32.dll")]
		[return: MarshalAs(UnmanagedType.Bool)]
		public static extern bool SetForegroundWindow(IntPtr handle);
	}
}
