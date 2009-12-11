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
package net.sourceforge.vulcan.core.support;

import static net.sourceforge.vulcan.dto.ProjectStatusDto.Status.ERROR;
import static net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType.Full;
import static net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType.Incremental;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto.UpdateStrategy;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SvnRevision(id = "$Id$", url = "$HeadURL$")
class WorkingCopyUpdateExpert {
	Log log = LogFactory.getLog(getClass());
	
	UpdateType determineUpdateStrategy(ProjectConfigDto currentTarget, ProjectStatusDto previousStatus) {
		if (UpdateStrategy.CleanAlways == currentTarget.getUpdateStrategy()) {
			return Full;
		}
		
		final File workDir = new File(currentTarget.getWorkDir());
		
		if (!workDir.exists()) {
			log.info("Performing full build of " + currentTarget.getName() + " even though incremental " +
					"build was requested because work directory does not exist.");
			
			return Full;
		}
		
		final File[] files = workDir.listFiles();
		
		if (files == null) {
			log.error("Failed to list contents of " + workDir.getAbsolutePath() +
					" probably due to insufficient permissions.  Attempting to perform a full build of " +
					currentTarget.getName());
			return Full;
		}
		
		if (files.length == 0) {
			log.info("Performing full build of " + currentTarget.getName() + " even though incremental " +
					"build was requested because work directory is empty.");
			
			return Full;
		}
		
		if (previousStatus == null) {
			log.info("Performing full build of " + currentTarget.getName() + " even though incremental " +
					"build was requested because previous build is not available.");
	
			return Full;
		}
	
		if (ERROR == previousStatus.getStatus() && !previousStatus.isWorkDirSupportsIncrementalUpdate()) {
			log.info("Performing full build of " + currentTarget.getName() + " even though incremental " +
					"build was requested because previous build resulted in ERROR before or during checkout or update.");

			return Full;
		}
		
		if (!currentTarget.getRepositoryTagName().equals(previousStatus.getTagName())) {
			log.info("Performing full build of " + currentTarget.getName() + " even though incremental " +
					"build was requested because previous build was performed from a different tag/branch.");

			return Full;
		}
		
		if (isDailyFullBuildRequired(currentTarget, previousStatus)) {
			log.info("Performing first daily full build of " + currentTarget.getName() + ".");

			return Full;
		}
		
		return Incremental;
	}

	boolean isDailyFullBuildRequired(ProjectConfigDto currentTarget, ProjectStatusDto previousStatus) {
		if (currentTarget.getUpdateStrategy() != UpdateStrategy.CleanDaily) {
			return false;
		}
		
		final Date previousBuildDate = previousStatus.getCompletionDate();
		
		final Calendar cal = new GregorianCalendar();
		cal.setTime(previousBuildDate);
		
		final int previousBuildDay = cal.get(Calendar.DAY_OF_YEAR);
		
		cal.setTime(getNow());
		final int currentDay = cal.get(Calendar.DAY_OF_YEAR);
		
		return previousBuildDay != currentDay;
	}

	Date getNow() {
		return new Date();
	}
}
