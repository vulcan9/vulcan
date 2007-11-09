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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.BuildOutcomeQueryDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.TestFailureDto;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

@Transactional(readOnly=true)
@SvnRevision(id="$Id$", url="$HeadURL$")
public interface BuildOutcomeStore {
	
	@Transactional(readOnly=false, rollbackFor=StoreException.class)
	UUID storeBuildOutcome(ProjectStatusDto outcome) throws StoreException;
	
	@Transactional(readOnly=true)
	ProjectStatusDto loadBuildOutcome(UUID id) throws StoreException;
	
	@Transactional(readOnly=true)
	List<ProjectStatusDto> loadBuildSummaries(BuildOutcomeQueryDto query);

	@Transactional(readOnly=true)
	List<BuildMessageDto> loadTopBuildErrors(BuildOutcomeQueryDto query, int maxResultCount);
	
	@Transactional(readOnly=true)
	List<TestFailureDto> loadTopTestFailures(BuildOutcomeQueryDto query, int maxResultCount);
	
	@Transactional(readOnly=true)
	Map<String, List<UUID>> getBuildOutcomeIDs();

}
