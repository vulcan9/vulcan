/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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
package net.sourceforge.vulcan.dto;


public class PreferencesDto extends BaseDto {
	private String sortColumn;
	private String sortOrder;
	private String popupMode;
	private String stylesheet;
	private int reloadInterval;
	private boolean groupByLabel;
	private boolean showBuildDaemons;
	private boolean showBuildQueue;
	private boolean showSchedulers;
	private String[] labels;
	private String[] dashboardColumns;
	private String[] buildHistoryColumns = {};
	
	public String getSortColumn() {
		return sortColumn;
	}
	public void setSortColumn(String sortColumn) {
		this.sortColumn = sortColumn;
	}
	public String getSortOrder() {
		return sortOrder;
	}
	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}
	public String getPopupMode() {
		return popupMode;
	}
	public void setPopupMode(String popupMode) {
		this.popupMode = popupMode;
	}
	public String getStylesheet() {
		return stylesheet;
	}
	public void setStylesheet(String stylesheet) {
		this.stylesheet = stylesheet;
	}
	public String[] getLabels() {
		return labels;
	}
	public void setLabels(String[] labels) {
		this.labels = labels;
	}
	public boolean isGroupByLabel() {
		return groupByLabel;
	}
	public void setGroupByLabel(boolean groupByLabel) {
		this.groupByLabel = groupByLabel;
	}
	public int getReloadInterval() {
		return reloadInterval;
	}
	public void setReloadInterval(int reloadInterval) {
		this.reloadInterval = reloadInterval;
	}
	public boolean isShowBuildDaemons() {
		return showBuildDaemons;
	}
	public void setShowBuildDaemons(boolean showBuildDaemons) {
		this.showBuildDaemons = showBuildDaemons;
	}
	public boolean isShowBuildQueue() {
		return showBuildQueue;
	}
	public void setShowBuildQueue(boolean showBuildQueue) {
		this.showBuildQueue = showBuildQueue;
	}
	public boolean isShowSchedulers() {
		return showSchedulers;
	}
	public void setShowSchedulers(boolean showSchedulers) {
		this.showSchedulers = showSchedulers;
	}
	public String[] getDashboardColumns() {
		return dashboardColumns;
	}
	public void setDashboardColumns(String[] dashboardColumns) {
		this.dashboardColumns = dashboardColumns;
	}
	public String[] getBuildHistoryColumns() {
		return buildHistoryColumns;
	}
	public void setBuildHistoryColumns(String[] buildHistoryColumns) {
		this.buildHistoryColumns = buildHistoryColumns;
	}
}
