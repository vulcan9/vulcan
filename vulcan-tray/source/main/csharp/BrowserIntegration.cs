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
using System.Diagnostics;

namespace SourceForge.Vulcan.Tray
{
	public class BrowserIntegration
	{
		private string url;

		public string Url
		{
			get { return url;  }
			set { url = value; }
		}
		
		public void OpenDashboard()
		{
			Process.Start(url);
		}
		
		public void OpenBuildReport(string projectName, string buildNumber)
		{
			string baseUrl = url;

			if (!url.EndsWith("/"))
			{
				baseUrl += "/";
			}

			string reportUrl;

			if (string.IsNullOrEmpty(buildNumber))
			{
				reportUrl = string.Format(
					"{0}viewProjectStatus.do?transform=xhtml&projectName={1}",
					baseUrl,
					projectName);
			}
			else
			{
				reportUrl = string.Format(
					"{0}viewProjectStatus.do?transform=xhtml&projectName={1}&buildNumber={2}",
					baseUrl,
					projectName,
					buildNumber);
			}

			Process.Start(reportUrl);

		}

	}
}