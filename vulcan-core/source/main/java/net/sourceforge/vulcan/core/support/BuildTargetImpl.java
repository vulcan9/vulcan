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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sourceforge.vulcan.core.BuildTarget;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Represents a project to be built. 
 */
public class BuildTargetImpl implements BuildTarget {
	private final ProjectConfigDto config;
	private final ProjectStatusDto status;
	private final List<String> pendingDependencies;
	private final List<String> pendingDependenciesReadOnly;
	
	public BuildTargetImpl(ProjectConfigDto config, ProjectStatusDto buildStatus) {
		this.config = config;
		this.status = buildStatus;
		this.status.setName(config.getName());
		this.status.setStatus(Status.IN_QUEUE);
		
		this.pendingDependencies = new ArrayList<String>(Arrays.asList(config.getDependencies()));
		this.pendingDependenciesReadOnly = Collections.unmodifiableList(pendingDependencies);
	}
	
	public String getProjectName() {
		return config.getName();
	}

	public ProjectConfigDto getProjectConfig() {
		return config;
	}

	public ProjectStatusDto getStatus() {
		return status;
	}
	
	public Iterable<String> getAllDependencies() {
		return Arrays.asList(config.getDependencies());
	}

	public Iterable<String> getPendingDependencies() {
		return pendingDependenciesReadOnly;
	}
	
	public void removePendingDependency(String name) {
		pendingDependencies.remove(name);
	}

	public int getNumberOfPendingDependencies() {
		return pendingDependencies.size();
	}

	public boolean dependsOn(BuildTarget other) {
		return pendingDependencies.contains(other.getProjectName());
	}

	public boolean isBuildOnDependencyFailure() {
		return config.isBuildOnDependencyFailure();
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