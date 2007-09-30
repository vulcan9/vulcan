/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2006 Chris Eldredge
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
package net.sourceforge.vulcan.core.conversion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.core.BuildOutcomeStore;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.spring.SpringFileStore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class for loading old file based XML build outcomes and storing
 * them into the new JDBC based persistence provider.
 */
public class BuildOutcomeConverter {
	private SpringFileStore fileStore;
	private BuildOutcomeStore buildOutcomeStore;
	private StateManager stateManager;
	private EventHandler eventHandler;
	
	private Converter converter;
	
	public void convertOldBuildOutcomes() {
		final Map<String, List<UUID>> oldOutcomeIds = fileStore.getBuildOutcomeIDs();
		
		if (oldOutcomeIds.isEmpty()) {
			startStateManager();
			return;
		}
		
		final Map<UUID, String> idsToProjectNames = new HashMap<UUID, String>();
		final List<UUID> allIds = new ArrayList<UUID>();
		
		for (Map.Entry<String, List<UUID>> e : oldOutcomeIds.entrySet()) {
			final String projectName = e.getKey();
			final List<UUID> projectBuildIds = e.getValue();

			allIds.addAll(projectBuildIds);
			for (UUID id : projectBuildIds) {
				idsToProjectNames.put(id, projectName);
			}
		}
		
		Collections.sort(allIds, new Comparator<UUID>() {
			public int compare(UUID o1, UUID o2) {
				return ((Integer)o1.clockSequence()).compareTo(o2.clockSequence());
			}
		});
		
		converter = new Converter(allIds, idsToProjectNames, fileStore, buildOutcomeStore);
		
		converter.start();
	}
	
	public boolean isRunning() {
		return converter != null && converter.isRunning();
	}
	
	public int getConvertedCount() {
		if (converter == null) {
			return 0;
		}
		return converter.getConvertedCount();
	}
	
	public int getTotalCount() {
		if (converter == null) {
			return 0;
		}
		
		return converter.getTotalCount();
	}
	
	public SpringFileStore getFileStore() {
		return fileStore;
	}

	public void setFileStore(SpringFileStore fileStore) {
		this.fileStore = fileStore;
	}

	public BuildOutcomeStore getBuildOutcomeStore() {
		return buildOutcomeStore;
	}

	public void setBuildOutcomeStore(BuildOutcomeStore buildOutcomeStore) {
		this.buildOutcomeStore = buildOutcomeStore;
	}

	public StateManager getStateManager() {
		return stateManager;
	}

	public void setStateManager(StateManager stateManager) {
		this.stateManager = stateManager;
	}

	public EventHandler getEventHandler() {
		return eventHandler;
	}

	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	private void startStateManager() {
		try {
			stateManager.start();
		} catch (Exception e) {
			eventHandler.reportEvent(new ErrorEvent(this, "errors.load.failure", new String[] {e.getMessage()}, e));
		}
	}

	private class Converter extends Thread {
		private final Log log = LogFactory.getLog(Converter.class);
		private final List<UUID> allIds;
		private final Map<UUID, String> idsToProjectNames;
		private final SpringFileStore fileStore;
		private final BuildOutcomeStore buildOutcomeStore;
		private final Set<UUID> converted = new HashSet<UUID>();
		
		public Converter(List<UUID> allIds, Map<UUID, String> idsToProjectNames, SpringFileStore fileStore, BuildOutcomeStore buildOutcomeStore) {
			this.allIds = allIds;
			this.idsToProjectNames = idsToProjectNames;
			this.fileStore = fileStore;
			this.buildOutcomeStore = buildOutcomeStore;
		}

		@Override
		public void run() {
			int i = 0;
			for (UUID id : allIds) {
				convert(id);
				i++;
			}
			
			startStateManager();
		}

		public boolean isRunning() {
			return isAlive();
		}
		
		public int getConvertedCount() {
			return converted.size();
		}
		
		public int getTotalCount() {
			return allIds.size();
		}
		
		private void convert(UUID id) {
			final String projectName = idsToProjectNames.get(id);
			try {
				final ProjectStatusDto buildOutcome = fileStore.loadBuildOutcome(projectName, id);
				
				fixProblems(buildOutcome);
				
				buildOutcomeStore.storeBuildOutcome(buildOutcome);
				
				converted.add(id);
				
				fileStore.archiveBuildOutcome(projectName, id, true);
			} catch (Exception e) {
				log.error("Failed to convert build outcome " + id + " for project " + projectName, e);
				fileStore.archiveBuildOutcome(projectName, id, false);
			}
		}

		private void fixProblems(final ProjectStatusDto buildOutcome) {
			if (buildOutcome.getStartDate() == null) {
				// old logs didn't have a start date.
				buildOutcome.setStartDate(buildOutcome.getCompletionDate());
			}
			
			final Map<String, UUID> deps = buildOutcome.getDependencyIds();
			
			for (Iterator<Entry<String, UUID>> i = deps.entrySet().iterator(); i.hasNext(); ) {
				final Entry<String, UUID> e = i.next();
				final String depName = e.getKey();
				final UUID id = e.getValue();
				if (!converted.contains(id)) {
					i.remove();
					log.info("Pruning missing dependency " + depName + "/" + id + " from project "+ buildOutcome.getName() + "/" + buildOutcome.getId());
				}
			}
		}
	}
}
