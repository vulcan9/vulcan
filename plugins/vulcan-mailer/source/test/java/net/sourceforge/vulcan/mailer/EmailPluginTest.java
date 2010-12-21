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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
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
	public void testSubscribedAddresses() throws Exception {
		ProjectStatusDto status = new ProjectStatusDto();
		status.setName("b");
		status.setStatus(ProjectStatusDto.Status.PASS);
		status.setStatusChanged(false);

		Map<Locale, List<String>> all = plugin.getSubscribedAddresses(status);
		assertNotNull(all);
		assertEquals(1, all.size());
		assertNotNull(all.get(null));
		assertEquals(1, all.get(null).size());

		status.setStatus(ProjectStatusDto.Status.FAIL);
		all = plugin.getSubscribedAddresses(status);
		assertNotNull(all);
		assertEquals(3, all.get(null).size());
	}
	public void testGetSubscribedAddressesTruncatesBlank() throws Exception {
		ProjectStatusDto status = new ProjectStatusDto();
		status.setName("nonesuch");
		status.setStatus(ProjectStatusDto.Status.PASS);
		status.setStatusChanged(false);

		assertNull(plugin.getSubscribedAddresses(status));

		status.setName("d");
		final Map<Locale, List<String>> all = plugin.getSubscribedAddresses(status);
		assertNull(all);
	}
	public void testOnlyOnChange() throws Exception {
		profiles[1].setOnlyOnChange(true);
		plugin.setConfiguration(config);

		ProjectStatusDto status = new ProjectStatusDto();
		status.setName("c");
		status.setStatus(ProjectStatusDto.Status.FAIL);
		status.setStatusChanged(false);

		Map<Locale, List<String>> all = plugin.getSubscribedAddresses(status);
		assertNull(all);

		status.setStatusChanged(true);
		all = plugin.getSubscribedAddresses(status);
		assertNotNull(all);
		assertEquals(1, all.size());
		assertEquals(2, all.get(null).size());
	}

	public void testGetEmailAddresses0Match() throws Exception {
		config.setRepositoryEmailMappings(new String[]{"thedude=dude@dudes.com", "thedudette=dudette@dudettes.com", "dog=dog@dog.com"});

		ChangeSetDto changeSetDto = new ChangeSetDto();
		ChangeSetDto changeSetDto2 = new ChangeSetDto();
		ChangeSetDto changeSetDto3 = new ChangeSetDto();
		changeSetDto.setAuthor("thedude "); //just in case of spaces
		changeSetDto2.setAuthor(" dog"); //just in case of spaces
		changeSetDto3.setAuthor(" thedudette"); //just in case of spaces
		ArrayList<ChangeSetDto> changeSets = new ArrayList<ChangeSetDto>();
		changeSets.add(changeSetDto);
		changeSets.add(changeSetDto2);
		changeSets.add(changeSetDto3);

		ChangeLogDto changeLogDto = new ChangeLogDto();
		changeLogDto.setChangeSets(changeSets);

		ProjectStatusDto statusDto = new ProjectStatusDto();
		statusDto.setChangeLog(changeLogDto);

		profiles[0].setOnlyEmailChangeAuthors(true);
		profiles[0].setEmailAddresses(new String[]{"nomatch@dudes.com", "nomatch@dudettes.com", "nomatch@dog.com"});
		List<String> emailAddresses = plugin.getEmailAddresses(statusDto, profiles[0]);

		assertEquals(0, emailAddresses.size());
	}

	public void testGetEmailAddresses2Match() throws Exception {
		config.setRepositoryEmailMappings(new String[]{"thedude=dude@dudes.com", "thedudette=dudette@dudettes.com", "dog=dog@dog.com"});

		ChangeSetDto changeSetDto = new ChangeSetDto();
		ChangeSetDto changeSetDto2 = new ChangeSetDto();
		changeSetDto.setAuthor("thedude "); //just in case of spaces
		changeSetDto2.setAuthor(" dog"); //just in case of spaces
		ArrayList<ChangeSetDto> changeSets = new ArrayList<ChangeSetDto>();
		changeSets.add(changeSetDto);
		changeSets.add(changeSetDto2);

		ChangeLogDto changeLogDto = new ChangeLogDto();
		changeLogDto.setChangeSets(changeSets);

		ProjectStatusDto statusDto = new ProjectStatusDto();
		statusDto.setChangeLog(changeLogDto);

		profiles[0].setOnlyEmailChangeAuthors(true);
		profiles[0].setEmailAddresses(new String[]{"dude@dudes.com", "dudette@dudettes.com", "dog@dog.com"});
		List<String> emailAddresses = plugin.getEmailAddresses(statusDto, profiles[0]);

		assertEquals(2, emailAddresses.size());
		assertEquals("dude@dudes.com", emailAddresses.get(0));
		assertEquals("dog@dog.com", emailAddresses.get(1));
	}

	public void testGetEmailAddresses3Match() throws Exception {
		config.setRepositoryEmailMappings(new String[]{"thedude=dude@dudes.com", "thedudette=dudette@dudettes.com", "dog=dog@dog.com"});

		ChangeSetDto changeSetDto = new ChangeSetDto();
		ChangeSetDto changeSetDto2 = new ChangeSetDto();
		ChangeSetDto changeSetDto3 = new ChangeSetDto();
		changeSetDto.setAuthor("thedude "); //just in case of spaces
		changeSetDto2.setAuthor(" dog"); //just in case of spaces
		changeSetDto3.setAuthor(" thedudette"); //just in case of spaces
		ArrayList<ChangeSetDto> changeSets = new ArrayList<ChangeSetDto>();
		changeSets.add(changeSetDto);
		changeSets.add(changeSetDto2);
		changeSets.add(changeSetDto3);

		ChangeLogDto changeLogDto = new ChangeLogDto();
		changeLogDto.setChangeSets(changeSets);

		ProjectStatusDto statusDto = new ProjectStatusDto();
		statusDto.setChangeLog(changeLogDto);

		profiles[0].setOnlyEmailChangeAuthors(true);
		profiles[0].setEmailAddresses(new String[]{"dude@dudes.com", "dudette@dudettes.com", "dog@dog.com"});
		List<String> emailAddresses = plugin.getEmailAddresses(statusDto, profiles[0]);

		assertEquals(3, emailAddresses.size());
		assertEquals("dude@dudes.com", emailAddresses.get(0));
		assertEquals("dog@dog.com", emailAddresses.get(1));
		assertEquals("dudette@dudettes.com", emailAddresses.get(2));
	}

	public void testGetEmailAddressesRemovesDuplicateAuthors() throws Exception {
		config.setRepositoryEmailMappings(new String[]{"thedude=dude@dudes.com", "thedudette=dudette@dudettes.com", "dog=dog@dog.com"});

		ChangeSetDto changeSetDto = new ChangeSetDto();
		ChangeSetDto changeSetDto2 = new ChangeSetDto();
		ChangeSetDto changeSetDto3 = new ChangeSetDto();
		changeSetDto.setAuthor("thedude "); //just in case of spaces
		changeSetDto2.setAuthor(" dog"); //just in case of spaces
		changeSetDto3.setAuthor(" thedude"); //just in case of spaces
		ArrayList<ChangeSetDto> changeSets = new ArrayList<ChangeSetDto>();
		changeSets.add(changeSetDto);
		changeSets.add(changeSetDto2);
		changeSets.add(changeSetDto3);

		ChangeLogDto changeLogDto = new ChangeLogDto();
		changeLogDto.setChangeSets(changeSets);

		ProjectStatusDto statusDto = new ProjectStatusDto();
		statusDto.setChangeLog(changeLogDto);

		profiles[0].setOnlyEmailChangeAuthors(true);
		profiles[0].setEmailAddresses(new String[]{"dude@dudes.com", "dudette@dudettes.com", "dog@dog.com"});
		List<String> emailAddresses = plugin.getEmailAddresses(statusDto, profiles[0]);

		assertEquals(2, emailAddresses.size());
		assertEquals("dude@dudes.com", emailAddresses.get(0));
		assertEquals("dog@dog.com", emailAddresses.get(1));
	}

	public void testGetEmailAddressesNoRepoMappings() throws Exception {
		ChangeSetDto changeSetDto = new ChangeSetDto();
		ChangeSetDto changeSetDto2 = new ChangeSetDto();
		ChangeSetDto changeSetDto3 = new ChangeSetDto();
		changeSetDto.setAuthor("thedude "); //just in case of spaces
		changeSetDto2.setAuthor(" dog"); //just in case of spaces
		changeSetDto3.setAuthor(" thedudette"); //just in case of spaces
		ArrayList<ChangeSetDto> changeSets = new ArrayList<ChangeSetDto>();
		changeSets.add(changeSetDto);
		changeSets.add(changeSetDto2);
		changeSets.add(changeSetDto3);

		ChangeLogDto changeLogDto = new ChangeLogDto();
		changeLogDto.setChangeSets(changeSets);

		ProjectStatusDto statusDto = new ProjectStatusDto();
		statusDto.setChangeLog(changeLogDto);

		profiles[0].setOnlyEmailChangeAuthors(true);
		profiles[0].setEmailAddresses(new String[]{"thedude@dudes.com", "thedudette@dudettes.com", "dog@dog.com"});
		List<String> emailAddresses = plugin.getEmailAddresses(statusDto, profiles[0]);

		assertEquals(0, emailAddresses.size());
	}

	public void testGetEmailAddressesOnlyEmailChangeAuthorsIsFalse() throws Exception {
		ProjectStatusDto statusDto = new ProjectStatusDto();

		profiles[0].setOnlyEmailChangeAuthors(false);
		profiles[0].setEmailAddresses(new String[]{"thedude@dudes.com", "thedudette@dudettes.com", "dog@dog.com"});
		List<String> emailAddresses = plugin.getEmailAddresses(statusDto, profiles[0]);

		assertEquals(3, emailAddresses.size());
	}

	public void testGetEmailAddressesChangeLogsIsNull() throws Exception {
	    ProjectStatusDto statusDto = new ProjectStatusDto();

	    profiles[0].setOnlyEmailChangeAuthors(true);
	    List<String> emailAddresses = plugin.getEmailAddresses(statusDto, profiles[0]);

	    assertEquals(0, emailAddresses.size());
	}

	public void testGetChangeAuthorEmailMap() throws Exception {
		config.setRepositoryEmailMappings(new String[]{"thedude=dude@dudes.com ", " thedudette=dudette@dudettes.com"}); //added some spaces
		Map<String, String> changeAuthorEmailMap = plugin.getChangeAuthorEmailMap();
		assertEquals("dude@dudes.com", changeAuthorEmailMap.get("thedude"));
		assertEquals("dudette@dudettes.com", changeAuthorEmailMap.get("thedudette"));
	}
}
