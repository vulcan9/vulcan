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
namespace SourceForge.Vulcan.Tray
{
	public class Preferences
	{
		private string url;
		private string selectedLabels;
		private int interval;
		private bool bubbleFailures;
		private bool bubbleSuccess;
		
		public string Url
		{
			get { return url; }
			set { url = value; }
		}

		public string SelectedLabels
		{
			get { return selectedLabels; }
			set { selectedLabels = value; }
		}

		public int Interval
		{
			get { return interval; }
			set { interval = value; }
		}

		public bool BubbleFailures
		{
			get { return bubbleFailures; }
			set { bubbleFailures = value; }
		}

		public bool BubbleSuccess
		{
			get { return bubbleSuccess; }
			set { bubbleSuccess = value; }
		}

		public Preferences Clone()
		{
			return (Preferences) MemberwiseClone();
		}
	}
}