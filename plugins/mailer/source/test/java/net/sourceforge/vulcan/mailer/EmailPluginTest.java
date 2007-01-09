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
package net.sourceforge.vulcan.mailer;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.mailer.dto.ConfigDto;
import net.sourceforge.vulcan.mailer.dto.ProfileDto;

public class EmailPluginTest extends TestCase {
	EmailPlugin plugin = new EmailPlugin();
	ConfigDto config = new ConfigDto();
	
	ProfileDto[] profiles = new ProfileDto[] {
			new ProfileDto(),
			new ProfileDto(),
			new ProfileDto()
		};
	
	@Override
	public void setUp() throws Exception {
		
		
		profiles[0].setPolicy(new ProfileDto.Policy[] {ProfileDto.Policy.ALWAYS});
		profiles[1].setPolicy(new ProfileDto.Policy[] {ProfileDto.Policy.FAIL});
		profiles[2].setPolicy(new ProfileDto.Policy[] {ProfileDto.Policy.ALWAYS});
		
		profiles[0].setProjects(new String[] {"a", "b"});
		profiles[1].setProjects(new String[] {"c", "b"});
		profiles[2].setProjects(new String[] {"d"});
		
		profiles[0].setEmailAddresses(new String[] {"Bugs Bunny <bugs@wb.com>"});
		profiles[1].setEmailAddresses(new String[] {"Garfield <gman@home.net>", "Snoopy <snoops@example.com>"});
		profiles[2].setEmailAddresses(new String[] {"", "  ", "\t"});
		
		config.setProfiles(profiles);
		
		plugin.setMessageAssembler(new MessageAssembler());
		
		plugin.setConfiguration(config);
	}
	public void testHashes() throws Exception {
		assertTrue(plugin.subscribers.containsKey("a"));
		assertTrue(plugin.subscribers.containsKey("b"));
		assertTrue(plugin.subscribers.containsKey("c"));
		
		assertEquals(1, plugin.subscribers.get("a").size());
		assertEquals(2, plugin.subscribers.get("b").size());
		assertEquals(1, plugin.subscribers.get("c").size());
	}
	public void testGetEmailAddresses() throws Exception {
		Map<Locale, List<String>> all = plugin.getSubscribedAddresses("b", ProjectStatusDto.Status.PASS, false);
		assertNotNull(all);
		assertEquals(1, all.size());
		assertNotNull(all.get(null));
		assertEquals(1, all.get(null).size());

		all = plugin.getSubscribedAddresses("b", ProjectStatusDto.Status.FAIL, false);
		assertNotNull(all);
		assertEquals(3, all.get(null).size());
	}
	public void testGetEmailAddressesTruncatesBlank() throws Exception {
		assertNull(plugin.getSubscribedAddresses("nonesuch", ProjectStatusDto.Status.PASS, false));
		final Map<Locale, List<String>> all = plugin.getSubscribedAddresses("d", ProjectStatusDto.Status.PASS, false);
		assertNull(all);
	}
	public void testOnlyOnChange() throws Exception {
		profiles[1].setOnlyOnChange(true);
		plugin.setConfiguration(config);
		
		Map<Locale, List<String>> all = plugin.getSubscribedAddresses("c", ProjectStatusDto.Status.FAIL, false);
		assertNull(all);

		all = plugin.getSubscribedAddresses("c", ProjectStatusDto.Status.FAIL, true);
		assertNotNull(all);
		assertEquals(1, all.size());
		assertEquals(2, all.get(null).size());
	}
}
