using System;
using System.Collections.Generic;
using System.Windows.Forms;

namespace SourceForge.Vulcan.Tray
{
	static class Program
	{
		/// <summary>
		/// The main entry point for the application.
		/// </summary>
		[STAThread]
		static void Main()
		{
			Application.EnableVisualStyles();
			Application.SetCompatibleTextRenderingDefault(false);

			StatusMonitor statusMonitor = new StatusMonitor();
			BrowserIntegration browserIntegration = new BrowserIntegration();
			
			StatusForm mainForm = new StatusForm(statusMonitor, browserIntegration, new RegistryPreferenceStore());
			VulcanNotifyIcon trayIcon = new VulcanNotifyIcon(mainForm, statusMonitor, browserIntegration);
			
			Application.Run(mainForm);

			trayIcon.Dispose();
		}
	}
}