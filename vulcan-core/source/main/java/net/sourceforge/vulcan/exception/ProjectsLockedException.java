package net.sourceforge.vulcan.exception;

import java.util.Collections;
import java.util.List;

public class ProjectsLockedException extends Exception {
	private final List<String> lockedProjects;

	public ProjectsLockedException(List<String> lockedProjects) {
		this.lockedProjects = Collections.unmodifiableList(lockedProjects);
	}
	
	public List<String> getLockedProjectNames() {
		return lockedProjects;
	}
}
