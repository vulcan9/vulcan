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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;
import net.sourceforge.vulcan.core.support.ProjectRebuildExpert.FullBuildAfterIncrementalBuildStrategy;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto.UpdateStrategy;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;
import net.sourceforge.vulcan.metadata.SvnRevision;

@SvnRevision(id="$Id$", url="$HeadURL$")
public class ProjectRebuildExpertTest extends TestCase {
	ProjectConfigDto currentTarget = new ProjectConfigDto();
	RevisionTokenDto currentRevision = new RevisionTokenDto();
	ProjectStatusDto buildStatus = new ProjectStatusDto();
	RevisionTokenDto previousRevision = new RevisionTokenDto();
	ProjectStatusDto previousStatus = new ProjectStatusDto();

	boolean isDailyFullBuildRequired = false;
	
	WorkingCopyUpdateExpert wce = new WorkingCopyUpdateExpert() {
		@Override
		boolean isDailyFullBuildRequired(ProjectConfigDto currentTarget, ProjectStatusDto previousStatus) {
			return isDailyFullBuildRequired;
		}
	};

	ProjectRebuildExpertStub expert = new ProjectRebuildExpertStub(wce);
	
	Date day1;
	Date day2;
	Date now;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	
		DateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");
		day1 = fmt.parse("1/1/2000");
		day2 = fmt.parse("2/1/2000");
		
		now = day2;
	}

	public void testDoesNotRebuildWhenPreviousWasFull() throws Exception {
		final FullBuildAfterIncrementalBuildStrategy strategy = expert.createFullBuildAfterIncrementalBuildStrategy();
		
		previousStatus.setUpdateType(UpdateType.Full);
		currentTarget.setUpdateStrategy(UpdateStrategy.CleanAlways);
		
		assertFalse(strategy.shouldBuild(currentTarget, currentRevision, buildStatus, previousRevision, previousStatus));
	}

	public void testDoesNotRebuildOnIncrementalBuildRequested() throws Exception {
		final FullBuildAfterIncrementalBuildStrategy strategy = expert.createFullBuildAfterIncrementalBuildStrategy();
		
		previousStatus.setUpdateType(UpdateType.Incremental);
		currentTarget.setUpdateStrategy(UpdateStrategy.IncrementalAlways);
		
		assertFalse(strategy.shouldBuild(currentTarget, currentRevision, buildStatus, previousRevision, previousStatus));
	}
	
	public void testRebuildOnFullBuildRequestedPreviousIncremental() throws Exception {
		final FullBuildAfterIncrementalBuildStrategy strategy = expert.createFullBuildAfterIncrementalBuildStrategy();
		
		previousStatus.setUpdateType(UpdateType.Incremental);
		currentTarget.setUpdateStrategy(UpdateStrategy.CleanAlways);
		
		assertTrue(strategy.shouldBuild(currentTarget, currentRevision, buildStatus, previousRevision, previousStatus));
	}
	
	public void testRebuildOnDailyFullBuildRequestedPreviousIncremental() throws Exception {
		final FullBuildAfterIncrementalBuildStrategy strategy = expert.createFullBuildAfterIncrementalBuildStrategy();
		
		previousStatus.setUpdateType(UpdateType.Incremental);
		currentTarget.setUpdateStrategy(UpdateStrategy.CleanDaily);
		
		isDailyFullBuildRequired = true;
		
		assertTrue(strategy.shouldBuild(currentTarget, currentRevision, buildStatus, previousRevision, previousStatus));
	}
	
	public void testDoesNotRebuildOnDailyFullBuildRequestedPreviousIncrementalSameDay() throws Exception {
		final FullBuildAfterIncrementalBuildStrategy strategy = expert.createFullBuildAfterIncrementalBuildStrategy();
		
		previousStatus.setUpdateType(UpdateType.Incremental);
		currentTarget.setUpdateStrategy(UpdateStrategy.CleanDaily);
		
		isDailyFullBuildRequired = false;
		
		assertFalse(strategy.shouldBuild(currentTarget, currentRevision, buildStatus, previousRevision, previousStatus));
	}
	
	public void testDoesNotRebuildOnDailyFullBuildRequestedPreviousIncrementalPreviousWasFull() throws Exception {
		final FullBuildAfterIncrementalBuildStrategy strategy = expert.createFullBuildAfterIncrementalBuildStrategy();
		
		previousStatus.setUpdateType(UpdateType.Full);
		currentTarget.setUpdateStrategy(UpdateStrategy.CleanDaily);
		
		isDailyFullBuildRequired = true;
		
		assertFalse(strategy.shouldBuild(currentTarget, currentRevision, buildStatus, previousRevision, previousStatus));
	}
	
	class ProjectRebuildExpertStub extends ProjectRebuildExpert {
		public ProjectRebuildExpertStub(WorkingCopyUpdateExpert wc) {
			super(wc);
		}

		FullBuildAfterIncrementalBuildStrategy createFullBuildAfterIncrementalBuildStrategy() {
			return new FullBuildAfterIncrementalBuildStrategy();
		}
	}
}
