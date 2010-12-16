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
using System.Reflection;
using System.Windows.Forms;

namespace SourceForge.Vulcan.Tray
{
	public partial class ConfigForm : Form
	{
		private readonly Preferences preferences;

		public ConfigForm(Preferences preferences)
		{
			this.preferences = preferences.Clone();

                        // .NET 1.1 compat
                        EventInfo formClosingEvent = GetType().GetEvent("FormClosing");
                        if (formClosingEvent != null)
                        {
                                formClosingEvent.AddEventHandler(this, new FormClosingEventHandler(this.onClosing));
                        }
			
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
