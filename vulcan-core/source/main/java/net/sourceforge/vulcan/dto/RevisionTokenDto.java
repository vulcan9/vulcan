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
package net.sourceforge.vulcan.dto;

import org.apache.commons.lang.builder.EqualsBuilder;

import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class RevisionTokenDto extends BaseDto {
	protected Long revision;
	protected String label;
	
	public RevisionTokenDto() {
	}
	public RevisionTokenDto(Long revision) {
		this(revision, revision.toString());
	}
	public RevisionTokenDto(Long revision, String label) {
		this.revision = revision;
		this.label = label;
	}

	@Override
	public boolean equals(Object o) {
		return new EqualsBuilder().append(revision, ((RevisionTokenDto)o).revision).isEquals();
	}
	
	@Override
	public int hashCode() {
		if (revision == null) {
			return 0;
		}
		return revision.hashCode();
	}
	
	@Override
	public String toString() {
		return label;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Long getRevision() {
		return revision;
	}

	public void setRevision(Long revision) {
		this.revision = revision;
	}
}
