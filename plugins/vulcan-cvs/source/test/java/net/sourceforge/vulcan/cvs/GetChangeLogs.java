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
package net.sourceforge.vulcan.cvs;

import net.sourceforge.vulcan.cvs.dto.CvsConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsProjectConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsRepositoryProfileDto;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.RepositoryException;

import org.apache.commons.lang.StringUtils;

public class GetChangeLogs {
	public static void main(String[] args) throws RepositoryException {
		final CvsRepositoryProfileDto profile = new CvsRepositoryProfileDto();
		final CvsProjectConfigDto projectConfig = new CvsProjectConfigDto();
		
		profile.setHost("gtkpod.cvs.sourceforge.net");
		profile.setProtocol("pserver");
		profile.setRepositoryPath("/cvsroot/gtkpod");
		profile.setUsername("anonymous");
		profile.setPassword("");
		projectConfig.setModule("gtkpod");
		
		final CvsRepositoryAdaptor repo = new CvsRepositoryAdaptor(new ProjectConfigDto(), new CvsConfigDto(), profile, projectConfig);
		
		RevisionTokenDto first = new RevisionTokenDto(20061022052422l, "2006/10/22 05:24:52");
		RevisionTokenDto head = new RevisionTokenDto(20061109074924l, "2006/11/09 07:49:24");
		
		final ChangeLogDto changeLog = repo.getChangeLog(first, head, System.err);
		
		printChangeLog(changeLog);
	}

	private static void printChangeLog(ChangeLogDto changeLog) {
		for (ChangeSetDto c : changeLog.getChangeSets()) {
			System.out.println("Revision: " + c.getRevisionLabel() + " Author: " + c.getAuthorName()
					+ " Timestamp: " + c.getTimestamp());
			System.out.println("Modified paths:");
			System.out.println(StringUtils.join(c.getModifiedPaths(), "\n"));
			System.out.println();
			System.out.println("Message:");
			System.out.println(c.getMessage());
			System.out.println();
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		}
	}
}
