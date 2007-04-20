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
package net.sourceforge.vulcan;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public interface RepositoryAdaptor {
	
	/**
	 * Obtain a token representing the HEAD revision for the overall project tree.
	 * For a transaction based repository this may simply be the "revision of last change" on
	 * the project path.  However, for file based repositories, a more complex operation must
	 * be carried out to dertermine if any one member has been updated, or if new members have
	 * been added.
	 * @param previousRevision If this project has been checked out before, the instance of
	 * RevisionTokenDto previously returned.  This is provided to allow implementations to
	 * optimize the range in which changes are searched.
	 */
	RevisionTokenDto getLatestRevision(RevisionTokenDto previousRevision) throws RepositoryException, InterruptedException;

	/**
	 * Populate the changeLog with ChangeSetDtos occurring between previousRevision, exclusive
	 * and currentRevision, inclusive.  Write a "diff -u" to the output stream and close when finished.
	 */
	ChangeLogDto getChangeLog(RevisionTokenDto previousRevision, RevisionTokenDto currentRevision, OutputStream diffOutputStream) throws RepositoryException, InterruptedException;
	
	/**
	 * Perform a "checkout" operation, creating a local "working copy" or "sandbox"
	 * containing the source files for the given project.
	 */
	void createWorkingCopy(File absolutePath, BuildDetailCallback buildDetailCallback) throws RepositoryException, InterruptedException;

	/**
	 * Perform an "update" operation on an existing "working copy" or "sandbox"
	 * given the path to the existing location.
	 */
	void updateWorkingCopy(File absolutePath, BuildDetailCallback buildDetailCallback) throws RepositoryException;
	
	/**
	 * Return a logical name which identifies the tag/branch or other line of development
	 * that is being used to create the working copy.  The identifier returned will be
	 * passed into the BuildTool to identify the line of development.  If no branch or tag
	 * applies, "HEAD" or "trunk" may be used as a default value.
	 */
	String getTagName();
	
	/**
	 * Set the name of the tag/branch to use.  When performing a "managed build", 
	 * this will be called before getLatestRevision, getChangeLog,
	 * and createWorkingCopy.  When no tag is selected, this method will
	 * not be called.  In that case, HEAD/trunk should be used.
	 */
	void setTagName(String tagName);
	
	/**
	 * Return a list of tags that may be used to build the project.  This list
	 * should always contain at least one tag representing the HEAD/trunk
	 * development path.  If other tags or branches exist, they should be
	 * included as well.
	 * <br><br>
	 * This list will be presented to a user performing a "managed build" to
	 * allow the user to select an alternate tag or branch to build from,
	 * whereas normally vulcan would build the project using HEAD/trunk.
	 */
	List<RepositoryTagDto> getAvailableTags() throws RepositoryException;
	
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
