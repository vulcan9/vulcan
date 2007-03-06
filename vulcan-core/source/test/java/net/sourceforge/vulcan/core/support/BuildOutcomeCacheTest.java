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

import static org.easymock.EasyMock.getCurrentArguments;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.core.Store;
import net.sourceforge.vulcan.dto.ProjectStatusDto;

import org.easymock.IAnswer;

public class BuildOutcomeCacheTest extends EasyMockTestCase {
	BuildOutcomeCache cache = new BuildOutcomeCache();
	
	Store store = createMock(Store.class);
	
	Map<String, List<UUID>> map = new HashMap<String, List<UUID>>();
	Map<UUID, ProjectStatusDto> storedOutcomes = new HashMap<UUID, ProjectStatusDto>();
	
	final UUID zero = UUID.randomUUID();
	final UUID one = UUID.randomUUID();
	final UUID two = UUID.randomUUID();
	final UUID three = UUID.randomUUID();
	final UUID four = UUID.randomUUID();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		cache.setStore(store);
		cache.setCacheSize(10);
		
		storedOutcomes.put(zero, new ProjectStatusDto());
		storedOutcomes.put(one, new ProjectStatusDto());
		storedOutcomes.put(two, new ProjectStatusDto());
		storedOutcomes.put(three, new ProjectStatusDto());
		storedOutcomes.put(four, new ProjectStatusDto());
		
		storedOutcomes.get(zero).setBuildNumber(0);
		storedOutcomes.get(one).setBuildNumber(1);
		storedOutcomes.get(two).setBuildNumber(2);
		storedOutcomes.get(three).setBuildNumber(3);
		storedOutcomes.get(four).setBuildNumber(4);
		
		map.put("myProject", Arrays.asList(zero, one, two, three, four));
	}
	
	public void trainInit() throws Exception {
		expect(store.getBuildOutcomeIDs()).andReturn(map);
		expect(store.loadBuildOutcome((String)notNull(), (UUID)notNull())).andAnswer(new IAnswer<ProjectStatusDto>() {
			public ProjectStatusDto answer() throws Throwable {
				final UUID id = (UUID) getCurrentArguments()[1];
				
				return storedOutcomes.get(id);
			}
		}).anyTimes();
	}
	@TrainingMethod("trainInit")
	public void testInit() throws Exception {
		cache.init();		
	}
	
	@TrainingMethod("trainInit")
	public void testSetsProjectNameOnStatus() throws Exception {
		// If a project is renamed, outcomes stored with the old name
		// should have the name updated to the new name.
		cache.init();
		
		final ProjectStatusDto status = cache.getOutcome(zero);
		assertEquals("myProject", status.getName());
	}
	
	@TrainingMethod("trainInit")
	public void testDoesNotOverrideBuildNumber() throws Exception {
		cache.init();
		
		storedOutcomes.get(zero).setBuildNumber(99);
		
		final ProjectStatusDto status = cache.getOutcome(zero);
		assertEquals((Integer)99, status.getBuildNumber());
	}

	@TrainingMethod("trainInit")
	public void testSetsBuildNumberWhenNull() throws Exception {
		cache.init();
		
		storedOutcomes.get(zero).setBuildNumber(null);
		storedOutcomes.get(one).setBuildNumber(null);
		
		assertEquals((Integer)0, cache.getOutcome(zero).getBuildNumber());
		assertEquals((Integer)1, cache.getOutcome(one).getBuildNumber());
	}
	
	@TrainingMethod("trainInit")
	public void testGetOutcomeByBuildNumberOneToOne() throws Exception {
		cache.init();
		
		assertSame(storedOutcomes.get(zero), cache.getOutcomeByBuildNumber("myProject", 0));
		assertSame(storedOutcomes.get(one), cache.getOutcomeByBuildNumber("myProject", 1));
	}
	
	@TrainingMethod("trainInit")
	public void testGetOutcomeByBuildNumberOutOfBounds() throws Exception {
		cache.init();
		
		assertEquals(null, cache.getOutcomeByBuildNumber("myProject", 5));
	}
	
	@TrainingMethod("trainInit")
	public void testGetOutcomeByBuildNumberInvalidProject() throws Exception {
		cache.init();
		
		assertEquals(null, cache.getOutcomeByBuildNumber("noSuch", 5));
	}
	
	@TrainingMethod("trainInit")
	public void testGetOutcomeByBuildNumberOffset() throws Exception {
		cache.init();
		
		offsetBuildNumbers();
		
		assertSame(storedOutcomes.get(zero), cache.getOutcomeByBuildNumber("myProject", 757));
		assertSame(storedOutcomes.get(one), cache.getOutcomeByBuildNumber("myProject", 758));
		assertSame(storedOutcomes.get(two), cache.getOutcomeByBuildNumber("myProject", 759));
		assertSame(storedOutcomes.get(three), cache.getOutcomeByBuildNumber("myProject", 760));
		assertSame(storedOutcomes.get(four), cache.getOutcomeByBuildNumber("myProject", 761));
	}

	@TrainingMethod("trainInit")
	public void testGetOutcomeByBuildNumberOffsetLowerBound() throws Exception {
		cache.init();
		
		offsetBuildNumbers();
		
		assertEquals(null, cache.getOutcomeByBuildNumber("myProject", 756));
	}
	
	@TrainingMethod("trainInit")
	public void testGetOutcomeByBuildNumberOffsetUpperBound() throws Exception {
		cache.init();
		
		offsetBuildNumbers();
		
		assertEquals(null, cache.getOutcomeByBuildNumber("myProject", 762));
	}
	
	@TrainingMethod("trainInit")
	public void testGetOutcomeByBuildNumberIncongruous() throws Exception {
		cache.init();
		
		setBuildNumbers(7, 8, 100, 101, 102);
		
		assertSame(storedOutcomes.get(zero), cache.getOutcomeByBuildNumber("myProject", 7));
		assertSame(storedOutcomes.get(one), cache.getOutcomeByBuildNumber("myProject", 8));
		assertSame(storedOutcomes.get(two), cache.getOutcomeByBuildNumber("myProject", 100));
		assertSame(storedOutcomes.get(three), cache.getOutcomeByBuildNumber("myProject", 101));
		assertSame(storedOutcomes.get(four), cache.getOutcomeByBuildNumber("myProject", 102));
	}
	
	@TrainingMethod("trainInit")
	public void testGetOutcomeByBuildNumberIncongruous2() throws Exception {
		cache.init();
		
		setBuildNumbers(7, 81, 90, 101, 120);
		
		assertSame(storedOutcomes.get(zero), cache.getOutcomeByBuildNumber("myProject", 7));
		assertSame(storedOutcomes.get(one), cache.getOutcomeByBuildNumber("myProject", 81));
		assertSame(storedOutcomes.get(two), cache.getOutcomeByBuildNumber("myProject", 90));
		assertSame(storedOutcomes.get(three), cache.getOutcomeByBuildNumber("myProject", 101));
		assertSame(storedOutcomes.get(four), cache.getOutcomeByBuildNumber("myProject", 120));
	}

	@TrainingMethod("trainInit")
	public void testGetOutcomeByBuildNumberIncongruous3() throws Exception {
		cache.init();
		
		setBuildNumbers(0, 2, 4, 6, 8);
		
		assertSame(storedOutcomes.get(one), cache.getOutcomeByBuildNumber("myProject", 2));
	}

	@TrainingMethod("trainInit")
	public void testGetOutcomeByBuildNumberIncongruous4() throws Exception {
		cache.init();
		
		setBuildNumbers(-2, 0, 2, 4, 6);
		
		assertSame(storedOutcomes.get(three), cache.getOutcomeByBuildNumber("myProject", 4));
	}
	
	@TrainingMethod("trainInit")
	public void testGetOutcomeByBuildNumberIncongruousNullMissingBuilds() throws Exception {
		cache.init();
		
		setBuildNumbers(7, 81, 90, 101, 120);
		Set<Integer> valid = new HashSet<Integer>(Arrays.asList(7, 81, 90, 101, 120));
		
		for (int i=0; i<200; i++) {
			if (!valid.contains(i)) {
				assertEquals(null, cache.getOutcomeByBuildNumber("myProject", i));
			}
		}
	}
	
	private void setBuildNumbers(int i, int j, int k, int l, int m) {
		storedOutcomes.get(zero).setBuildNumber(i);
		storedOutcomes.get(one).setBuildNumber(j);
		storedOutcomes.get(two).setBuildNumber(k);
		storedOutcomes.get(three).setBuildNumber(l);
		storedOutcomes.get(four).setBuildNumber(m);
	}

	private void offsetBuildNumbers() {
		setBuildNumbers(757, 758, 759, 760, 761);
	}
}
