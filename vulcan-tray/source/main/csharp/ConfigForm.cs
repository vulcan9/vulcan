using System.Windows.Forms;

namespace SourceForge.Vulcan.Tray
{
	public partial class ConfigForm : Form
	{
		private readonly Preferences preferences;

		public ConfigForm(Preferences preferences)
		{
			this.preferences = preferences.Clone();
			
			InitializeComponent();

			populate();
		}

		public Preferences Preferences
		{
			get { return preferences; }
		}

		private void populate()
		{
			url.Text = Preferences.Url;
			pollingInterval.Value = Preferences.Interval/1000;

			chkBubbleFailures.Checked = preferences.BubbleFailures;
			chkBubbleSuccess.Checked = preferences.BubbleSuccess;
		}

		private void onClosing(object sender, FormClosingEventArgs e)
		{
			Preferences.Url = url.Text;
			Preferences.Interval = (int)pollingInterval.Value*1000;
			preferences.BubbleFailures = chkBubbleFailures.Checked;
			preferences.BubbleSuccess = chkBubbleSuccess.Checked;
		}
	}
}