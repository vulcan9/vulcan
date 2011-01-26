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