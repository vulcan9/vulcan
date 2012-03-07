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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ChangeSetDto extends BaseDto {
	private String revisionLabel;
	private String authorName;
	private String authorEmail;
	private Date timestamp;
	private String message;
	private List<ModifiedPathDto> modifiedPaths = Collections.emptyList();

	public ChangeSetDto() {
	}
	
	public ChangeSetDto(String revisionLabel, String authorName, String authorEmail, Date timestamp, String message, List<ModifiedPathDto> modifiedPaths) {
		this.revisionLabel = revisionLabel;
		this.authorName = authorName;
		this.authorEmail = authorEmail;
		this.timestamp = timestamp;
		this.message = message;
		this.modifiedPaths = modifiedPaths;
	}
	
	public String getAuthorName() {
		return authorName;
	}
	
	public void setAuthorName(String author) {
		this.authorName = author;
	}
	
	public String getAuthorEmail() {
		return authorEmail;
	}
	
	public void setAuthorEmail(String authorEmail) {
		this.authorEmail = authorEmail;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public List<ModifiedPathDto> getModifiedPaths() {
		return modifiedPaths;
	}
	
	public void setModifiedPaths(List<ModifiedPathDto> modifiedPaths) {
		this.modifiedPaths = modifiedPaths;
	}
	
	public void addModifiedPath(String path, PathModification action) {
		if (modifiedPaths == Collections.<ModifiedPathDto>emptyList()) {
			modifiedPaths = new ArrayList<ModifiedPathDto>();
		}
		
		modifiedPaths.add(new ModifiedPathDto(path, action));
	}
	
	@Deprecated
	public RevisionTokenDto getRevision() {
		return new RevisionTokenDto(0l, revisionLabel);
	}
	
	@Deprecated
	public void setRevision(RevisionTokenDto revision) {
		if (revision == null) {
			this.revisionLabel = null;
		} else {
			this.revisionLabel = revision.getLabel();
		}
	}
	
	public String getRevisionLabel() {
		return revisionLabel;
	}
	
	public void setRevisionLabel(String revisionLabel) {
		this.revisionLabel = revisionLabel;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(Date timestamp) {
		this.timestamp = (Date) timestamp;
	}
}
