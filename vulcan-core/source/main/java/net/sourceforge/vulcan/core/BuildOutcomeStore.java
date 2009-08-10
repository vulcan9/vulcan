/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2007 Chris Eldredge
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
package net.sourceforge.vulcan.core;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly=true)
@SvnRevision(id="$Id$", url="$HeadURL$")
public interface BuildOutcomeStore {
	UUID storeBuildOutcome(ProjectStatusDto outcome) throws StoreException;
	
	void claimBrokenBuild(UUID id, String userName, Date claimDate);
	
	ProjectStatusDto loadBuildOutcome(UUID id) throws StoreException;
	
	List<ProjectStatusDto> loadBuildSummaries(BuildOutcomeQueryDto query);

	List<BuildMessageDto> loadTopBuildErrors(BuildOutcomeQueryDto query, int maxResultCount);
	
	List<TestFailureDto> loadTopTestFailures(BuildOutcomeQueryDto query, int maxResultCount);
	
	Long loadAverageBuildTimeMillis(String name, UpdateType updateType);
	
	Map<String, List<UUID>> getBuildOutcomeIDs();

	Integer findMostRecentBuildNumberByWorkDir(String workDir);

	List<String> getBuildUsers();

	List<String> getBuildSchedulers();
}
