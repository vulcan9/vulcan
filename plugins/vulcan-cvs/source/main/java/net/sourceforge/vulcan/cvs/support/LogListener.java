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

import org.netbeans.lib.cvsclient.command.FileInfoContainer;
import org.netbeans.lib.cvsclient.command.log.LogInformation;
import org.netbeans.lib.cvsclient.commandLine.BasicListener;
import org.netbeans.lib.cvsclient.event.FileInfoEvent;
import org.netbeans.lib.cvsclient.event.MessageEvent;

public abstract class LogListener extends BasicListener {
	@Override
	public void messageSent(MessageEvent me) {
		final String message = me.getMessage();
		
		processMessage(message);
	}

	@Override
	public void fileInfoGenerated(FileInfoEvent e) {
		final FileInfoContainer info = e.getInfoContainer();
		
		if (info instanceof LogInformation) {
			final LogInformation logInfo = (LogInformation) info;
			processLogInfo(logInfo);
		}
	}
	
	protected abstract void processMessage(String message);
	protected abstract void processLogInfo(LogInformation logInfo);
}
