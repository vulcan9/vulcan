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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.vulcan.core.BuildTarget;
import net.sourceforge.vulcan.core.DependencyException;
import net.sourceforge.vulcan.core.DependencyGroup;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


public final class DependencyGroupImpl implements DependencyGroup {
	private enum DependencyStatus { PASS, FAIL, CYCLE, PENDING };
	
	final List<BuildTarget> targets = new ArrayList<BuildTarget>();
	final Map<String, Integer> dependencyCounts = new HashMap<String, Integer>();
	
	final Map<String, DependencyStatus> results = new HashMap<String, DependencyStatus>();
	
	String name;
	boolean manualBuild;
	
	public void initializeBuildResults(Map<String, ProjectStatusDto> statusMap) {
		for (ProjectStatusDto projectStatus : statusMap.values()) {
			DependencyStatus status;
			
			try {
				status = DependencyStatus.valueOf(projectStatus.getStatus().name());
			} catch (IllegalArgumentException e) {
				status = DependencyStatus.FAIL;
			}
			
			results.put(projectStatus.getName(), status);
		}
	}

	public void addTarget(ProjectConfigDto config) {
		addTarget(config, new ProjectStatusDto());
	}
	
	public void addTarget(ProjectConfigDto config, ProjectStatusDto buildStatus) {
		final BuildTarget target = new BuildTargetImpl(config, buildStatus);
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
		
		sort();
	}

	public boolean isEmpty() {
		return targets.isEmpty();
	}

	public boolean isBlocked() throws DependencyException {
		if (isEmpty()) {
			throw new IllegalStateException();
		}
		
		// TODO: this method should not have side effects
		
		boolean pending = false;
		
		final BuildTarget target = targets.get(0);
		for (String name : target.getPendingDependencies()) {
			final DependencyStatus status = results.get(name);
			if (status == null) {
				checkMissing(target, name);
				continue;
			}
			switch (status) {
				case FAIL:
					if (target.isBuildOnDependencyFailure()) {
						continue;
					}
					targets.remove(0);
					results.put(target.getProjectName(), DependencyStatus.FAIL);
					throw new DependencyFailureException(target.getProjectConfig(), name);
				case CYCLE:
					targets.remove(0);
					for (String dependency : target.getAllDependencies()) {
						if (DependencyStatus.CYCLE.equals(results.get(dependency))) {
							throw new DependencyCycleException(target.getProjectConfig(), dependency);		
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

	public BuildTarget getNextTarget() throws DependencyException {
		if (isEmpty()) {
			throw new IllegalStateException();
		} else if (isBlocked()) {
			throw new PendingDependencyException();
		}
		
		final BuildTarget target = targets.remove(0);
		
		results.put(target.getProjectName(), DependencyStatus.PENDING);
		
		return target; 
	}

	public List<ProjectConfigDto> getPendingProjects() {
		List<ProjectConfigDto> projects = new ArrayList<ProjectConfigDto>();
		
		for (BuildTarget target : targets) {
			projects.add(target.getProjectConfig());
		}
		
		return Collections.unmodifiableList(projects);
	}
	
	public void targetCompleted(ProjectConfigDto config, boolean success) {
		final String name = config.getName();
		
		if (success) {
			results.put(name, DependencyStatus.PASS);
			
			for (final BuildTarget target : targets) {
				target.removePendingDependency(name);
			}
		} else {
			results.put(name, DependencyStatus.FAIL);
		}

		sort();
	}

	public List<ProjectStatusDto> getPendingTargets() {
		final List<ProjectStatusDto> list = new ArrayList<ProjectStatusDto>(targets.size());
		
		for (BuildTarget target : targets) {
			list.add(target.getStatus());
		}
		
		return list;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isManualBuild() {
		return manualBuild;
	}
	
	public void setManualBuild(boolean manualBuild) {
		this.manualBuild = manualBuild;
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
	
	private void checkMissing(final BuildTarget target, String name) throws DependencyException {
		for (final BuildTarget tgt : targets) {
			if (name.equals(tgt.getProjectName())) {
				targets.remove(0);
				results.put(target.getProjectName(), DependencyStatus.CYCLE);
				results.put(name, DependencyStatus.CYCLE);
				throw new DependencyCycleException(target.getProjectConfig(), name);
			}
		}
		if (!target.isBuildOnDependencyFailure()) {
			targets.remove(0);
			throw new DependencyMissingException(target.getProjectConfig(), name);
		}
	}

	private void sort() {
		Collections.sort(targets, new Comparator<BuildTarget>() {
			public int compare(BuildTarget o1, BuildTarget o2) {
				return compareTargets(o1, o2);
			}
		});
	}

	private int compareTargets(BuildTarget lhs, BuildTarget rhs) {
		final Integer numProjectsDependingOnLhs = dependencyCounts.get(lhs.getProjectName());
		final Integer numProjectsDependingOnRhs = dependencyCounts.get(rhs.getProjectName());
		
		int signum = numProjectsDependingOnRhs.compareTo(numProjectsDependingOnLhs);
		
		if (signum != 0) {
			return signum;
		}
		
		if (lhs.dependsOn(rhs)) {
			return 1;
		}
		
		if (rhs.dependsOn(lhs)) {
			return -1;
		}
		
		signum = ((Integer)lhs.getNumberOfPendingDependencies()).compareTo(rhs.getNumberOfPendingDependencies());
		
		return signum;
	}
}
