/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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
package net.sourceforge.vulcan.subversion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.subversion.dto.CheckoutDepth;
import net.sourceforge.vulcan.subversion.dto.SparseCheckoutDto;
import net.sourceforge.vulcan.subversion.dto.SubversionProjectConfigDto;

public class SparseChangeLogFilter {
	private final List<SparseCheckoutDto> folders;
	
	public SparseChangeLogFilter(SubversionProjectConfigDto config, LineOfDevelopment lineOfDevelopment)
	{
		String rootPath = lineOfDevelopment.getComputedRelativePath();
		
		if (!rootPath.startsWith("/")) {
			rootPath = "/" + rootPath;
		}
		
		if (!rootPath.endsWith("/")) {
			rootPath += "/";
		}
		
		folders = new ArrayList<SparseCheckoutDto>();
		
		for (SparseCheckoutDto folder : config.getFolders()) {
			final SparseCheckoutDto copy = (SparseCheckoutDto) folder.copy();
			String dir = copy.getDirectoryName();
			if (!dir.endsWith("/")) {
				dir += "/";
			}
			copy.setDirectoryName(rootPath + dir);
			
			folders.add(copy);
		}
		
		// sort longest to shortest / reverse alphabetical order
		Collections.reverse(folders);
		
		folders.add(new SparseCheckoutDto(rootPath, config.getCheckoutDepth()));
	}
	
	public void removeIrrelevantChangeSets(List<ChangeSetDto> result) {
		for (Iterator<ChangeSetDto> itr = result.iterator(); itr.hasNext();) {
			if (!isMatch(itr.next())) {
				itr.remove();
			}
		}
	}

	private boolean isMatch(ChangeSetDto changeSet) {
		for (String path : changeSet.getModifiedPaths()) {
			for (SparseCheckoutDto folder : folders) {
				final String sparseDir = folder.getDirectoryName();
				
				// If the root path matches the folder (without trailing slash), we matched.
				if (sparseDir.substring(0, sparseDir.length()-1).equals(path)) {
					return true;
				}
				
				if (!path.startsWith(sparseDir)) {
					// Not even close.
					continue;
				}
			
				final CheckoutDepth depth = folder.getCheckoutDepth();
			
				if (depth == CheckoutDepth.Infinity) {
					return true;
				}
			
				String ext = path.substring(sparseDir.length());
				
				if (ext.indexOf("/") < 0 && (depth == CheckoutDepth.Files || depth == CheckoutDepth.Immediates)) {
					return true;
				}
			}
		}
		return false;
	}

}
