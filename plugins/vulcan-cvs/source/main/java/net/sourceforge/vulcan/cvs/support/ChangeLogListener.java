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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.vulcan.dto.ChangeSetDto;

import org.netbeans.lib.cvsclient.command.log.LogInformation;

public class ChangeLogListener extends LogListener {
	private static final String LOG_MEMBER_DELIM = "=============================================================================";
	private static final String LOG_ENTRY_DELIM = "----------------------------";
	private final Pattern pattern = Pattern.compile("date: (.+);  author: (.+);  state:.*");
	
	private final List<ChangeSetDto> allEntries = new ArrayList<ChangeSetDto>();
	private final List<ChangeSetDto> currentEntries = new ArrayList<ChangeSetDto>();
	
	private ChangeSetDto currentEntry;
	private StringBuilder msgBuf;
	private boolean readingMessage;
	
	public List<ChangeSetDto> getEntries() {
		return allEntries;
	}
	
	@Override
	protected void processMessage(String message) {
		if (message.equals(LOG_ENTRY_DELIM)) {
			entryFinished();
			
			currentEntry = new ChangeSetDto();
			return;
		} else if (message.equals(LOG_MEMBER_DELIM)) {
			entryFinished();
			return;
		}
		
		if (currentEntry == null) {
			return;
		}
		
		if (message.startsWith("revision") && !readingMessage) {
			currentEntry.setRevisionLabel(message.substring(9));
		} else if (readingMessage) {
			if (msgBuf.length() > 0) {
				msgBuf.append("\n");
			}
			msgBuf.append(message);
		} else {
			final Matcher matcher = pattern.matcher(message);
			if (matcher.matches()) {
				matcher.start();
		
				currentEntry.setTimestamp(new Date(parseDate(matcher.group(1)).getTime()));
				currentEntry.setAuthor(matcher.group(2));
				
				readingMessage = true;
				this.msgBuf = new StringBuilder();
			}
		}
	}

	private void entryFinished() {
		if (currentEntry != null) {
			currentEntry.setMessage(msgBuf.toString());
			currentEntries.add(currentEntry);
		}
		currentEntry = null;
		readingMessage = false;
	}

	@Override
	protected void processLogInfo(LogInformation logInfo) {
		final String repositoryFilename = logInfo.getRepositoryFilename();
		
		for (ChangeSetDto e : currentEntries) {
			e.setModifiedPaths(new String[] {repositoryFilename});
		}
		
		allEntries.addAll(currentEntries);
		
		reset();
	}
	
	private void reset() {
		currentEntries.clear();
		currentEntry = null;
		msgBuf = null;
		readingMessage = false;
	}
}
