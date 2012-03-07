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
package net.sourceforge.vulcan.core.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sourceforge.vulcan.core.BuildOutcomeStore;
import net.sourceforge.vulcan.core.ProjectNameChangeListener;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.StoreException;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BuildOutcomeCache implements ProjectNameChangeListener {
	private static final Log log = LogFactory.getLog(BuildOutcomeCache.class);
	
	private final Lock readLock;
	private final Lock writeLock;
	
	final Map<String, UUID> latestOutcomes = new HashMap<String, UUID>();
	final Map<String, UUID> latestOutcomesReadonly = Collections.unmodifiableMap(latestOutcomes);
	final Map<String, Integer> workDirBuildNumbers = new HashMap<String, Integer>();
	
	int cacheSize;
	
	Map<UUID, ProjectStatusDto> outcomes;
	
	/**
	 * Mapping of project name to all build outcome ids in store, chronological.
	 */
	Map<String, List<UUID>> outcomeIDs;
	
	/**
	 * Mapping of UUID to projectName.
	 */
	final Map<UUID, String> idToProjects = new HashMap<UUID, String>();
	
	BuildOutcomeStore buildOutcomeStore;
	EventHandler eventHandler;
	
	public BuildOutcomeCache() {
		final ReadWriteLock lock = new ReentrantReadWriteLock();
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}
	
	public void init() {
		if (cacheSize <= 0) {
			throw new IllegalStateException("Must set cacheSize > 0.");
		}
		
		outcomes = createLRUMap(cacheSize);
		
		writeLock.lock();
		
		try {
			outcomeIDs = buildOutcomeStore.getBuildOutcomeIDs();
			
			for (Entry<String, List<UUID>> e : outcomeIDs.entrySet()) {
				final List<UUID> ids = e.getValue();
				if (ids.isEmpty()) {
					continue;
				}
				latestOutcomes.put(e.getKey(), ids.get(ids.size()-1));
				
				for (UUID id : ids) {
					idToProjects.put(id, e.getKey());
				}
			}
		} finally { 
			writeLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<UUID, ProjectStatusDto> createLRUMap(int cacheSize) {
		return new LRUMap(cacheSize);
	}
	
	/**
	 * Saves a new build outcome.
	 */
	public void store(ProjectStatusDto statusDto) throws StoreException {
		writeLock.lock();
		try {
			store(statusDto.getName(), statusDto);
		} finally {
			writeLock.unlock();
		}
	}
	
	public void claimBrokenBuild(ProjectStatusDto outcome, String claimUser) {
		final Date date = new Date();
		writeLock.lock();
		try {
			buildOutcomeStore.claimBrokenBuild(outcome.getId(), claimUser, date);
			
			outcome.setBrokenBy(claimUser);
			outcome.setClaimDate(date);
		} finally {
			writeLock.unlock();
		}
	}

	public List<UUID> getOutcomeIds(String projectName) {
		try {
			readLock.lock();
			return outcomeIDs.get(projectName);
		} finally {
			readLock.unlock();
		}
	}
	
	public UUID getLatestOutcomeId(String projectName) {
		try {
			readLock.lock();
			return latestOutcomes.get(projectName);
		} finally {
			readLock.unlock();
		}
	}

	public ProjectStatusDto getLatestOutcome(String projectName) {
		final UUID uuid;
		
		try {
			readLock.lock();
			uuid = latestOutcomes.get(projectName);
			if (uuid == null) {
				return null;
			}
		} finally {
			readLock.unlock();
		}
		
		return getOutcome(uuid);
	}
	

	/**
	 * @return Mapping of project name to latest build outcome.
	 */
	public Map<String, ProjectStatusDto> getLatestOutcomes() {
		final Map<String, ProjectStatusDto> map = new HashMap<String, ProjectStatusDto>();
		final Set<Entry<String, UUID>> outcomes;
		
		readLock.lock();
		try {
			outcomes = new HashSet<Entry<String, UUID>>(latestOutcomes.entrySet());
		} finally {
			readLock.unlock();
		}
		
		for (Entry<String, UUID> e : outcomes) {
			final String name = e.getKey();
			map.put(name, getOutcome(e.getValue()));
		}
		return map;
	}

	public ProjectStatusDto getOutcome(UUID id) {
		readLock.lock();
		
		try {
			if (outcomes.containsKey(id)) {
				return outcomes.get(id);
			}
		} finally {
			readLock.unlock();
		}
		
		writeLock.lock();
		
		try {
			final ProjectStatusDto status = buildOutcomeStore.loadBuildOutcome(id);
			
			outcomes.put(id, status);
			
			return status;
		} catch (StoreException e) {
			final String projectName = idToProjects.get(id);
			
			eventHandler.reportEvent(new ErrorEvent(
					this, "errors.load.build.outcome",
					new Object[] {id, projectName}, e));
			
			return null;
		} finally {
			writeLock.unlock();
		}
	}
	
	public ProjectStatusDto getOutcomeByBuildNumber(String projectName, int buildNumber) {
		final List<UUID> outcomeIds = getOutcomeIds(projectName);
		
		if (outcomeIds == null || outcomeIds.isEmpty()) {
			return null;
		}
		
		return findOutcomeByNumber(outcomeIds, buildNumber, buildNumber, null);
	}

	public Integer getMostRecentBuildNumberByWorkDir(String workDir) {
		try {
			readLock.lock();
			if (workDirBuildNumbers.containsKey(workDir)) {
				return workDirBuildNumbers.get(workDir);
			}
		} finally {
			readLock.unlock();
		}

		try {
			writeLock.lock();
			final Integer buildNumber = buildOutcomeStore.findMostRecentBuildNumberByWorkDir(workDir);
			workDirBuildNumbers.put(workDir, buildNumber);
			
			return buildNumber;
		} finally {
			writeLock.unlock();
		}
	}

	public void projectNameChanged(String oldName, String newName) {
		try {
			writeLock.lock();
			
			final List<UUID> ids = outcomeIDs.remove(oldName);
			
			if (ids == null || ids.isEmpty()) {
				return;
			}
			
			outcomeIDs.put(newName, ids);
			
			for (UUID uuid : ids) {
				idToProjects.put(uuid, newName);
			}
			
			for (ProjectStatusDto cachedOutcome : outcomes.values()) {
				if (oldName.equals(cachedOutcome.getName())) {
					cachedOutcome.setName(newName);
				}
			}
			
			latestOutcomes.put(newName, latestOutcomes.remove(oldName));
		} finally {
			writeLock.unlock();
		}
	}

	public BuildOutcomeStore getBuildOutcomeStore() {
		return buildOutcomeStore;
	}
	
	public void setBuildOutcomeStore(BuildOutcomeStore buildOutcomeStore) {
		this.buildOutcomeStore = buildOutcomeStore;
	}
	
	public EventHandler getEventHandler() {
		return eventHandler;
	}
	
	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	
	public int getCacheSize() {
		return cacheSize;
	}
	
	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}
	
	void mergeOutcomes(Map<String, ProjectStatusDto> outcomes) throws StoreException {
		writeLock.lock();
		try {
			for (Entry<String, ProjectStatusDto> e : outcomes.entrySet()) {
				store(e.getKey(), e.getValue());
			}
		} finally {
			writeLock.unlock();
		}
	}
	
	private void store(String name, ProjectStatusDto statusDto) throws StoreException {
		final UUID id = buildOutcomeStore.storeBuildOutcome(statusDto);
		
		if (log.isDebugEnabled()) {
			log.debug("Latest build for project " + name + " is " + statusDto.getBuildNumber());
		}
		
		latestOutcomes.put(name, id);
		outcomes.put(id, statusDto);
		workDirBuildNumbers.put(statusDto.getWorkDir(), statusDto.getBuildNumber());
		
		final List<UUID> ids;
		
		if (outcomeIDs.containsKey(name)) {
			ids = outcomeIDs.get(name);
		} else {
			ids = new ArrayList<UUID>();
			outcomeIDs.put(name, ids);
		}
		
		ids.add(id);
		statusDto.setId(id);
	}
	
	private ProjectStatusDto findOutcomeByNumber(final List<UUID> outcomeIds, int buildNumber, int guess, Set<Integer> visitedIndexes) {
		final int numOutcomes = outcomeIds.size();

		int delta = 0;
		
		if (guess >= numOutcomes) {
			guess = numOutcomes - 1;
		} else if (guess < 0) {
			guess = 0;
		}
		
		if (visitedIndexes != null && visitedIndexes.contains(guess)) {
			return null;
		}
		
		final ProjectStatusDto outcome = getOutcome(outcomeIds.get(guess));
		final int actualBuildNumber = outcome.getBuildNumber();
		
		delta = buildNumber - actualBuildNumber;

		if (delta == 0) {
			return outcome;
		}
		
		if (visitedIndexes == null) {
			visitedIndexes = new HashSet<Integer>();
		}
		
		visitedIndexes.add(guess);
		
		if (buildNumber > actualBuildNumber && guess >= numOutcomes - 1) {
			return null;
		}
		
		if (guess == 0 && delta < 0) {
			return null;
		}
		
		int nextGuess = guess + delta;
		
		if (delta < 0 && guess + delta < 0) {
			nextGuess = guess - 1;
		} else if (delta > 0 && guess + delta >= numOutcomes) {
			nextGuess = guess + 1;
		}
		
		while (visitedIndexes.contains(nextGuess)) {
			nextGuess--;
		}
		
		return findOutcomeByNumber(outcomeIds, buildNumber, nextGuess, visitedIndexes);
	}
}
