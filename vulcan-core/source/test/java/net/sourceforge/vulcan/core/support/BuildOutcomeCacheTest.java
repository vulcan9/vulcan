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
import java.util.List;
import java.util.Map;
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
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		cache.setStore(store);
		cache.setCacheSize(10);
		
		
		storedOutcomes.put(zero, new ProjectStatusDto());
		storedOutcomes.put(one, new ProjectStatusDto());
		
		storedOutcomes.get(one).setBuildNumber(0);
		
		map.put("myProject", Arrays.asList(zero, one));
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
}
