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
package net.sourceforge.vulcan.core.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.vulcan.core.DependencyException;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


@SvnRevision(id="$Id$", url="$HeadURL$")
public final class DependencyGroupImpl implements DependencyGroup {
	private enum Status { PASS, FAIL, CYCLE, PENDING };
	
	final List<Target> targets = new ArrayList<Target>();
	final Map<String, Integer> dependencyCounts = new HashMap<String, Integer>();
	
	final Map<String, Status> results = new HashMap<String, Status>();
	
	String name;
	boolean manualBuild;
	
	class Target implements Comparable<Target> {
		private final ProjectConfigDto config;
		private final List<String> pendingDependencies;
		
		private Target(ProjectConfigDto config) {
			this.config = config;
			this.pendingDependencies = new ArrayList<String>(Arrays.asList(config.getDependencies()));
		}
		public int compareTo(Target o) {
			final Integer numDependentProjects = dependencyCounts.get(config.getName());
			final Integer otherNumDependentProjects = dependencyCounts.get(o.config.getName());
			
			int signum = otherNumDependentProjects.compareTo(numDependentProjects);
			
			if (signum != 0) {
				return signum;
			}
			
			if (pendingDependencies.contains(o.config.getName())) {
				return 1;
			}
			
			if (o.pendingDependencies.contains(config.getName())) {
				return -1;
			}
			
			signum = Integer.valueOf(pendingDependencies.size()).compareTo(Integer.valueOf(o.pendingDependencies.size()));
			
			return signum;
		}
		@Override
		public boolean equals(Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
		@Override
		public int hashCode() {
			return HashCodeBuilder.reflectionHashCode(this);
		}
		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}
	}
	
	public void initializeBuildResults(Map<String, ProjectStatusDto> statusMap) {
		for (ProjectStatusDto projectStatus : statusMap.values()) {
			Status status;
			
			try {
				status = Status.valueOf(projectStatus.getStatus().name());
			} catch (IllegalArgumentException e) {
				status = Status.FAIL;
			}
			
			results.put(projectStatus.getName(), status);
		}
	}

	public void addTarget(ProjectConfigDto config) {
		final Target target = new Target(config);
		targets.add(target);
		
		final String targetName = config.getName();
		if (!dependencyCounts.containsKey(targetName)) {
			dependencyCounts.put(targetName, 0);
		}
		
		final String[] dependencies = config.getDependencies();
		
		for (String dependency : dependencies) {
			Integer count = dependencyCounts.get(dependency);
			if (count == null) {
				count = 1;
			} else {
				count++;
			}
			
			dependencyCounts.put(dependency, count);
		}
		
		Collections.sort(targets);
	}

	public boolean isEmpty() {
		return targets.isEmpty();
	}

	public boolean isBlocked() throws DependencyException {
		if (isEmpty()) {
			throw new IllegalStateException();
		}
		
		boolean pending = false;
		
		final Target target = targets.get(0);
		for (String name : target.pendingDependencies) {
			final Status status = results.get(name);
			if (status == null) {
				checkMissing(target, name);
				continue;
			}
			switch (status) {
				case FAIL:
					if (target.config.isBuildOnDependencyFailure()) {
						continue;
					}
					targets.remove(0);
					results.put(target.config.getName(), Status.FAIL);
					throw new DependencyFailureException(target.config, name);
				case CYCLE:
					targets.remove(0);
					final String[] dependencies = target.config.getDependencies();
					for (int i = 0; i < dependencies.length; i++) {
						if (Status.CYCLE.equals(results.get(dependencies[i]))) {
							throw new DependencyCycleException(target.config, dependencies[i]);		
						}
					}
					throw new IllegalStateException();
				case PENDING:
					pending = true;
					break;
			}
		}
		
		return pending;
	}

	public ProjectConfigDto getNextTarget() throws DependencyException {
		if (isEmpty()) {
			throw new IllegalStateException();
		} else if (isBlocked()) {
			throw new PendingDependencyException();
		}
		
		final Target target = targets.remove(0);
		
		results.put(target.config.getName(), Status.PENDING);
		
		return target.config; 
	}

	public List<ProjectConfigDto> getPendingProjects() {
		List<ProjectConfigDto> projects = new ArrayList<ProjectConfigDto>();
		
		for (Target target : targets) {
			projects.add(target.config);
		}
		
		return Collections.unmodifiableList(projects);
	}
	
	public void targetCompleted(ProjectConfigDto config, boolean success) {
		final String name = config.getName();
		
		if (success) {
			results.put(name, Status.PASS);
			
			for (final Target target : targets) {
				target.pendingDependencies.remove(name);
			}
		} else {
			results.put(name, Status.FAIL);
		}
		
	}

	public ProjectStatusDto[] getPendingTargets() {
		final ProjectStatusDto[] ps = new ProjectStatusDto[targets.size()];
		
		for (int i=0; i<targets.size(); i++) {
			final Target target = targets.get(i);
			ps[i] = new ProjectStatusDto();
			ps[i].setName(target.config.getName());
			ps[i].setStatus(ProjectStatusDto.Status.IN_QUEUE);
		}
		return ps;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	private void checkMissing(final Target target, String name) throws DependencyException {
		for (final Target tgt : targets) {
			if (name.equals(tgt.config.getName())) {
				targets.remove(0);
				results.put(target.config.getName(), Status.CYCLE);
				results.put(name, Status.CYCLE);
				throw new DependencyCycleException(target.config, name);
			}
		}
		if (!target.config.isBuildOnDependencyFailure()) {
			targets.remove(0);
			throw new DependencyMissingException(target.config, name);
		}
	}

	public boolean isManualBuild() {
		return manualBuild;
	}
	
	public void setManualBuild(boolean manualBuild) {
		this.manualBuild = manualBuild;
	}
}
