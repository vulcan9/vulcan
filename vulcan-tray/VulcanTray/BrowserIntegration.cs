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
					"{0}viewProjectStatus.do?transform=xhtml&projectName={1}&index={2}",
					baseUrl,
					projectName,
					buildNumber);
			}

			Process.Start(reportUrl);

		}

	}
}