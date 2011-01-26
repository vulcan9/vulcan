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
package net.sourceforge.vulcan.core.support;

import java.util.ArrayList;
import java.util.List;


/**
 * This class facilitates selecting default metrics for plotting
 * in the build history report.  It selects two metrics to plot
 * from the list of metrics present in the build history.
 */
public class MetricSelector {
	private List<String> preferredMetrics;

	public List<String> getPreferredMetrics() {
		return preferredMetrics;
	}

	public void setPreferredMetrics(List<String> preferredMetrics) {
		this.preferredMetrics = preferredMetrics;
	}
	
	/**
	 * Returns a list of one or two metrics that are selected.
	 */
	public List<String> selectDefaultMetrics(List<String> availableMetrics) {
		final List<String> selected = new ArrayList<String>();
		
		for (String s : preferredMetrics) {
			if (availableMetrics.contains(s)) {
				selected.add(s);
				if (selected.size() == 2) {
					break;
				}
			}
		}
		
		if (selected.size() != 2) {
			for (String s : availableMetrics) {
				if (!selected.contains(s)) {
					selected.add(s);
					if (selected.size() == 2) {
						break;
					}
				}
			}
		}
		
		return selected;
	}
}
