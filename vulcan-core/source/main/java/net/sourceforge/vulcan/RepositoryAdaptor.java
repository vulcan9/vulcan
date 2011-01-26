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
package net.sourceforge.vulcan;

import java.io.OutputStream;
import java.util.List;

import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.RepositoryException;

public interface RepositoryAdaptor {
	
	/**
	 * Determine if changes are available in the repository since the previous build.
	 * This method is only called when a working copy is known to already exist and
	 * the previous build result is available.
	 * @param mostRecentBuildInSameWorkDir The result of the previous build in the same directory.
	 */
	boolean hasIncomingChanges(ProjectStatusDto mostRecentBuildInSameWorkDir) throws RepositoryException;
	
	/**
	 * Prepare for subsequent operations.  This method will be called before calling other methods like
	 * getLatestRevision, getChangeLog, etc.
	 */
	void prepareRepository(BuildDetailCallback buildDetailCallback) throws RepositoryException, InterruptedException;
	
	/**
	 * Obtain a token representing the most recent revision.
	 * For a transaction based repository this may simply be the "revision of last change".
	 * However, for file based repositories, a more complex operation must be carried out
	 * to determine if any child paths have been updated, or if new paths have been added.
	 * @param previousRevision If this project has been checked out before, the instance of
	 * RevisionTokenDto previously returned.  This is provided to allow implementations to
	 * optimize the range in which changes are searched.
	 * TODO: push this into CVS, the only thing that wants it.
	 */
	RevisionTokenDto getLatestRevision(RevisionTokenDto previousRevision) throws RepositoryException, InterruptedException;

	/**
	 * Populate the changeLog with ChangeSetDtos occurring between previousRevision, exclusive
	 * and currentRevision, inclusive.  Write a "diff -u" to the output stream and close when finished.
	 * This method will only be called when:
	 * 
	 * <ul>
	 * <li>previousRevision is not null</li>
	 * <li>previousRevision.equals(currentRevision) is false</li>
	 * <li>previousRevision is from the same tag/branch as current configuration</li>
	 * </ul>
	 * 
	 * @param diffOutputStream OutputStream to write differences to.  This parameter may be null if
	 * diffs have been disabled by configuration.
	 */
	ChangeLogDto getChangeLog(RevisionTokenDto previousRevision, RevisionTokenDto currentRevision, OutputStream diffOutputStream) throws RepositoryException, InterruptedException;

	/**
	 * Either create a working copy from scratch or restore an existing working copy
	 * to a pristine state where uncommitted changes are reverted, non-versioned files
	 * and directories are removed and the working copy is updated to the latest revision.
	 */
	void createPristineWorkingCopy(BuildDetailCallback buildDetailCallback) throws RepositoryException, InterruptedException;

	/**
	 * Perform an update operation on an existing working copy.  Unlike createPristineWorkingCopy,
	 * this method should not remove non-versioned files or revert uncommitted changes.
	 */
	void updateWorkingCopy(BuildDetailCallback buildDetailCallback) throws RepositoryException;
	
	/**
	 * Verify that a given path is a valid working copy.
	 * @param path Location to verify
	 * @return true if the path contains a working copy, false if not
	 */
	boolean isWorkingCopy() throws RepositoryException;
	
	/**
	 * Return a logical name which identifies the tag/branch or other line of development
	 * that is being used to create the working copy.
	 */
	String getTagOrBranch();
	
	/**
	 * Set the name of the tag/branch to use.  When performing a "managed build", 
	 * this will be called before getLatestRevision, getChangeLog,
	 * and createPristineWorkingCopy.  When no tag is selected, this method will
	 * not be called.
	 */
	void setTagOrBranch(String tagOrBranchName);
	
	/**
	 * Return a list of tags and branches that may be used to populate
	 * the working copy.  This list should always contain at least one tag
	 * representing the default setting (default, trunk, HEAD, etc.).
	 * If other tags or branches exist, they should be included as well.
	 * <br><br>
	 * This list will be presented to a user performing a "managed build" to
	 * allow the user to select an alternate tag or branch to build from,
	 * whereas normally vulcan would build the project using the default.
	 */
	List<RepositoryTagDto> getAvailableTagsAndBranches() throws RepositoryException;
	
	/**
	 * Return URL where project source code can be accessed.  If applicable,
	 * the protocol should be http in order that a user may browse the source
	 * in a standard web browser.  Examples include Subversion/Apache2, ViewCVS,
	 * FishEye, etc.  If not applicable, return null.
	 * <br><br>
	 * This link will be displayed in the user interface to allow a user to
	 * navigate from a build outcome summary to the source code easily.
	 */
	String getRepositoryUrl();
}
