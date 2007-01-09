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
package net.sourceforge.vulcan.cvs.dto;

import net.sourceforge.vulcan.dto.RevisionTokenDto;

/**
 * Token representing the date revision which points to the most
 * recent modification.  Uses a digest of all individual file
 * revisions to determine if anything has changed when comparing
 * to another aggregate revision.
 */
public class CvsAggregateRevisionTokenDto extends RevisionTokenDto {
	private String digest;

	public CvsAggregateRevisionTokenDto() {
	}
	public CvsAggregateRevisionTokenDto(String digest, String newestModificationDateString) {
		super(toLong(newestModificationDateString), newestModificationDateString);
		this.digest = digest;
	}

	@Override
	public boolean equals(Object o) {
		final CvsAggregateRevisionTokenDto other = (CvsAggregateRevisionTokenDto) o;
		
		return digest.equals(other.digest);
	}
	
	@Override
	public int hashCode() {
		return digest.hashCode();
	}
	
	public String getDigest() {
		return digest;
	}
	public void setDigest(String digest) {
		this.digest = digest;
	}
	
	private static Long toLong(String newestModificationDateString) {
		return Long.valueOf(newestModificationDateString.replaceAll("\\D", ""));
	}
}
