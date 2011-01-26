/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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

import static net.sourceforge.vulcan.dto.ProjectStatusDto.Status.*;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto.UpdateStrategy;
import net.sourceforge.vulcan.dto.ProjectStatusDto.UpdateType;

import org.apache.commons.io.FileUtils;

public class WorkingCopyUpdateExpertTest extends TestCase {
	WorkingCopyUpdateExpert expert = new WorkingCopyUpdateExpert() {
		@Override
		Date getNow() {
			if (now != null) {
				return now;
			}
			return super.getNow();
		}
	};
	
	ProjectConfigDto config = new ProjectConfigDto();
	ProjectStatusDto previousStatus = new ProjectStatusDto();
	
	File workDir = new File("target/WorkingCopyUpdateExpertTest");
	
	Date day1;
	Date day2;
	Date now;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	
		config.setWorkDir(workDir.getAbsolutePath());
		
		DateFormat fmt = new SimpleDateFormat("MM/dd/yyyy");
		day1 = fmt.parse("1/1/2000");
		day2 = fmt.parse("2/1/2000");
		
		now = day2;
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
		FileUtils.deleteDirectory(workDir);
	}
	
	public void testNotRequested() throws Exception {
		config.setUpdateStrategy(UpdateStrategy.CleanAlways);
		
		assertEquals(UpdateType.Full, expert.determineUpdateStrategy(config, null));
	}
	
	public void testRequestedButWorkdirNotPresent() throws Exception {
		config.setUpdateStrategy(UpdateStrategy.IncrementalAlways);
		
		assertEquals(UpdateType.Full, expert.determineUpdateStrategy(config, null));
	}

	public void testRequestedButWorkdirEmpty() throws Exception {
		assertTrue("Cannot create test directory", workDir.mkdirs());
		
		config.setUpdateStrategy(UpdateStrategy.IncrementalAlways);
		
		assertEquals(UpdateType.Full, expert.determineUpdateStrategy(config, null));
	}
	
	public void testRequestedButPreviousBuildNotPresent() throws Exception {
		assertTrue("Cannot create test directory", workDir.mkdirs());
		FileUtils.touch(new File(workDir, "foo"));
		
		config.setUpdateStrategy(UpdateStrategy.IncrementalAlways);
		
		assertEquals(UpdateType.Full, expert.determineUpdateStrategy(config, null));
	}

	public void testRequestedButPreviousBuildError() throws Exception {
		assertTrue("Cannot create test directory", workDir.mkdirs());
		FileUtils.touch(new File(workDir, "foo"));
		
		config.setUpdateStrategy(UpdateStrategy.IncrementalAlways);
		previousStatus.setStatus(ERROR);
		
		assertEquals(UpdateType.Full, expert.determineUpdateStrategy(config, previousStatus));
	}

	public void testAllowedOnPreviousBuildErrorWithVcsFlag() throws Exception {
		assertTrue("Cannot create test directory", workDir.mkdirs());
		FileUtils.touch(new File(workDir, "foo"));
		
		config.setUpdateStrategy(UpdateStrategy.IncrementalAlways);
		config.setRepositoryTagName("trunk");
		previousStatus.setStatus(ERROR);
		previousStatus.setWorkDirSupportsIncrementalUpdate(true);
		previousStatus.setTagName("trunk");
		
		assertEquals(UpdateType.Incremental, expert.determineUpdateStrategy(config, previousStatus));
	}

	public void testRequestedButPreviousBuildDifferentTag() throws Exception {
		assertTrue("Cannot create test directory", workDir.mkdirs());
		FileUtils.touch(new File(workDir, "foo"));
		
		config.setUpdateStrategy(UpdateStrategy.IncrementalAlways);
		config.setRepositoryTagName("trunk");
		previousStatus.setStatus(PASS);
		previousStatus.setTagName("branches/1.0");
		
		assertEquals(UpdateType.Full, expert.determineUpdateStrategy(config, previousStatus));
	}
	
	public void testRequestedAllCriteriaMet() throws Exception {
		assertTrue("Cannot create test directory", workDir.mkdirs());
		FileUtils.touch(new File(workDir, "foo"));
		
		config.setUpdateStrategy(UpdateStrategy.IncrementalAlways);
		config.setRepositoryTagName("trunk");
		previousStatus.setStatus(PASS);
		previousStatus.setTagName("trunk");
		
		assertEquals(UpdateType.Incremental, expert.determineUpdateStrategy(config, previousStatus));
	}
	
	public void testDaily() throws Exception {
		assertTrue("Cannot create test directory", workDir.mkdirs());
		FileUtils.touch(new File(workDir, "foo"));
		
		config.setUpdateStrategy(UpdateStrategy.CleanDaily);
		config.setRepositoryTagName("trunk");
		previousStatus.setStatus(PASS);
		previousStatus.setTagName("trunk");
		previousStatus.setCompletionDate(new Date(day1.getTime()));
		
		assertEquals(UpdateType.Full, expert.determineUpdateStrategy(config, previousStatus));
	}
	public void testDailyAlreadyBuilt() throws Exception {
		assertTrue("Cannot create test directory", workDir.mkdirs());
		FileUtils.touch(new File(workDir, "foo"));
		
		config.setUpdateStrategy(UpdateStrategy.CleanDaily);
		config.setRepositoryTagName("trunk");
		previousStatus.setStatus(PASS);
		previousStatus.setTagName("trunk");
		previousStatus.setCompletionDate(new Date(day2.getTime()));
		
		assertEquals(UpdateType.Incremental, expert.determineUpdateStrategy(config, previousStatus));
	}
}
