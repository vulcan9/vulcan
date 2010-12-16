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
package net.sourceforge.vulcan.cvs.support;

import static net.sourceforge.vulcan.cvs.support.CvsDateFormat.parseDate;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.netbeans.lib.cvsclient.command.log.LogInformation;

public class NewestRevisionsLogListener extends LogListener {
	private final List<String> revisions;
	private final Set<String> symbolicNames = new HashSet<String>();
	
	private Date newestModificationDate;
	private String newestModificationString;

	public NewestRevisionsLogListener(List<String> revisions) {
		this.revisions = revisions;
	}

	public String getNewestModificationString() {
		return newestModificationString;
	}
	
	public Set<String> getSymbolicNames() {
		return symbolicNames;
	}
	
	@Override
	protected void processMessage(String message) {
		if (message.startsWith("date:")) {
			final String dateString = message.substring(6, 25);
			final Date date = parseDate(dateString);
			
			if (newestModificationDate == null || date.after(newestModificationDate)) {
				newestModificationDate = date;
				newestModificationString = dateString;
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void processLogInfo(LogInformation logInfo) {
		revisions.add(logInfo.getRepositoryFilename() + ":" + logInfo.getHeadRevision());

		final List<LogInformation.SymName> symNames = logInfo.getAllSymbolicNames();
		
		for (LogInformation.SymName name : symNames) {
			symbolicNames.add(name.getName());
		}
	}
}
