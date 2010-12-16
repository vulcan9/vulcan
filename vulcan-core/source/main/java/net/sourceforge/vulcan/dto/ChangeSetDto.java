/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2010 Chris Eldredge
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

import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ChangeSetDto extends BaseDto {
	private String revisionLabel;
	private String author;
	private Date timestamp;
	private String message;
	private List<String> modifiedPaths = Collections.emptyList();

	public ChangeSetDto() {
	}
	
	public ChangeSetDto(String revisionLabel, String author, Date timestamp, String message, List<String> modifiedPaths) {
		this.revisionLabel = revisionLabel;
		this.author = author;
		this.timestamp = timestamp;
		this.message = message;
		this.modifiedPaths = modifiedPaths;
	}
	
	public String getAuthor() {
		return author;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public List<String> getModifiedPaths() {
		return modifiedPaths;
	}
	
	public void setModifiedPaths(List<String> modifiedPaths) {
		this.modifiedPaths = modifiedPaths;
	}
	
	public void addModifiedPath(String path) {
		if (modifiedPaths == Collections.<String>emptyList()) {
			modifiedPaths = new ArrayList<String>();
		}
		
		modifiedPaths.add(path);
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
