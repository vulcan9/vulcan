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
package net.sourceforge.vulcan.cvs;

import java.util.HashMap;
import java.util.Map;

import java.io.File;

import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.cvs.dto.CvsConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsProjectConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsRepositoryProfileDto;
import net.sourceforge.vulcan.exception.RepositoryException;

public class Checkout {
	public static void main(String[] args) throws RepositoryException {
		final CvsRepositoryProfileDto profile = new CvsRepositoryProfileDto();
		
		profile.setHost("gtkpod.cvs.sourceforge.net");
		profile.setProtocol("pserver");
		profile.setRepositoryPath("/cvsroot/gtkpod");
		profile.setUsername("anonymous");
		profile.setPassword("");
		
		final CvsProjectConfigDto projectConfig = new CvsProjectConfigDto();
		projectConfig.setModule("gtkpod");
		
		final CvsConfigDto cvsConfigDto = new CvsConfigDto();
		final Map<String, Long> map = new HashMap<String,Long>();
		map.put("gtkpod", 3000000l);
		cvsConfigDto.setWorkingCopyByteCounts(map);
		
		final CvsRepositoryAdaptor repo = new CvsRepositoryAdaptor(cvsConfigDto, profile, projectConfig, "gtkpod");

		final File file = new File("/home/chris/workspace/cvs-test");
		
		repo.createWorkingCopy(file, new BuildDetailCallback() {
			public void setDetail(String msg) {
			}
			public void setDetailMessage(String messageKey, Object[] args) {
			}
			public void setPhaseMessageKey(String arg0) {
			}
			public void reportError(String arg0, String arg1, Integer arg2, String arg3) {
			}
			public void reportWarning(String arg0, String arg1, Integer arg2, String arg3) {
			}
		});
	}
}
