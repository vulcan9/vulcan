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
	public class DashboardStatus
	{
		private readonly bool failuresPresent;
		private readonly bool currentlyBuilding;
		
		internal DashboardStatus(bool failuresPresent, bool currentlyBuilding)
		{
			this.failuresPresent = failuresPresent;
			this.currentlyBuilding = currentlyBuilding;
		}

		internal bool CurrentlyBuilding
		{
			get { return currentlyBuilding; }
		}

		internal bool FailuresPresent
		{
			get { return failuresPresent; }
		}
		
		internal DashboardStatus Clone()
		{
			return (DashboardStatus) MemberwiseClone();
		}
		
		public override bool Equals(object o)
		{
			if (this == o)
			{
				return true;
			}
			
			DashboardStatus other = o as DashboardStatus;
			
			if (other == null)
			{
				return false;
			}

			return failuresPresent == other.failuresPresent && currentlyBuilding == other.currentlyBuilding;
		}

		public override int GetHashCode()
		{
			return failuresPresent.GetHashCode() * 37 + currentlyBuilding.GetHashCode();
		}
	}
}