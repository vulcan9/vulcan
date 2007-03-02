using System;
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
			
			InitializeComponent();

			configure();
			
			timer.Enabled = true;
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
			ConfigForm configForm = new ConfigForm(preferences);

			configForm.ShowDialog(this);

			if (configForm.DialogResult == DialogResult.OK)
			{
				this.preferences = configForm.Preferences;
				configure();
				this.preferenceStore.Save(this.preferences);
			}
		}

		#endregion

		#region Private Methods
		
		private void configure()
		{
			browserIntegration.Url = preferences.Url;
			statusMonitor.Url = preferences.Url;
			timer.Interval = preferences.Interval;
		}

		private void updateProjectStatus()
		{
			string baseUrl = preferences.Url;
			
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