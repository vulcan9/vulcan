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

import java.io.File;

import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.integration.support.PluginSupport;
import net.sourceforge.vulcan.integration.support.PluginSupport.ProgressUnit;

import org.netbeans.lib.cvsclient.commandLine.BasicListener;
import org.netbeans.lib.cvsclient.event.FileAddedEvent;

public class CheckoutListener extends BasicListener {
	private final BuildDetailCallback buildDetailCallback;
	private final long previousBytesCounted;
	private long bytesCounted = 0;
	
	public CheckoutListener(BuildDetailCallback buildDetailCallback, long previousBytesCounted) {
		this.buildDetailCallback = buildDetailCallback;
		this.previousBytesCounted = previousBytesCounted;
	}

	@Override
	public void fileAdded(FileAddedEvent e) {
		final File file = new File(e.getFilePath());
		
		bytesCounted += file.length();
		
		PluginSupport.setWorkingCopyProgress(buildDetailCallback, bytesCounted, previousBytesCounted, ProgressUnit.Bytes);
	}

	public long getBytesCounted() {
		return bytesCounted;
	}
}
