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
package net.sourceforge.vulcan.subversion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.subversion.dto.SubversionConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionProjectConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionRepositoryProfileDto;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryImpl;

public class SubversionRepositoryAdaptorTest extends TestCase {
	SubversionRepositoryAdaptor r;
	SubversionConfigDto globalConfig = new SubversionConfigDto();
	SubversionProjectConfigDto repoConfig = new SubversionProjectConfigDto();
	ProjectConfigDto projectConfig = new ProjectConfigDto();
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		repoConfig.setRepositoryProfile("a");
		
		SubversionRepositoryProfileDto profile = new SubversionRepositoryProfileDto();
		profile.setDescription("a");
		profile.setRootUrl("http://localhost/svn");
		
		globalConfig.setProfiles(new SubversionRepositoryProfileDto[] {profile});
		
		r = new SubversionRepositoryAdaptor(globalConfig, projectConfig, repoConfig, null, false);
	}
	
	public void testDefaultIsTrunk() throws Exception {
		assertTagName("", "trunk");
		assertTagName("/", "trunk");
		assertTagName("/foo", "trunk");
		assertTagName("/trunk", "trunk");
		assertTagName("/foo/trunk", "trunk");
		assertTagName("foo/trunk", "trunk");
		assertTagName("foo/trunk/bar", "trunk");
		assertTagName("foo/head", "trunk");
	}

	public void testDetectsTagInNestedFolder() throws Exception {
		r.tagFolderNames.add("branches");
		r.tagFolderNames.add("tags");
		
		assertTagName("foo/trunk/bar", "trunk");
		assertTagName("foo/branches/1.6/bar", "branches/1.6");
		assertTagName("/tags/1.6/bar/cat", "tags/1.6");
	}
	
	public void testBranch() throws Exception {
		assertTagName("/myProject/branches/1.1-bug-fix", "trunk");
		
		r.tagFolderNames.add("branches");
		
		assertTagName("/myProject/branches/1.1-bug-fix", "branches/1.1-bug-fix");
	}
	
	public void testGoofy() throws Exception {
		assertTagName("/myProject/goofy/1.1-bug-fix", "trunk");
		
		r.tagFolderNames.add("goofy");
		
		assertTagName("/myProject/goofy/1.1-bug-fix", "goofy/1.1-bug-fix");
	}
	
	public void testSetTagName() throws Exception {
		repoConfig.setPath("/myProject/trunk");
		
		assertEquals("http://localhost/svn/myProject/trunk", r.getCompleteSVNURL().toString());
		assertEquals("/myProject/trunk", r.getRelativePath());
		
		r.setTagName("branches/bug-fix");
		
		assertEquals("branches/bug-fix", r.getTagName());
		
		assertEquals("/myProject/branches/bug-fix", r.getRelativePath());
		assertEquals("http://localhost/svn/myProject/branches/bug-fix", r.getCompleteSVNURL().toString());
	}
	
	public void testSetTagNameOnVariantPath() throws Exception {
		r.tagFolderNames.add("samples");
		
		repoConfig.setPath("/myProject/samples/4.1");
		
		assertEquals("http://localhost/svn/myProject/samples/4.1", r.getCompleteSVNURL().toString());
		assertEquals("/myProject/samples/4.1", r.getRelativePath());
		
		r.setTagName("branches/bug-fix");
		
		assertEquals("branches/bug-fix", r.getTagName());
		
		assertEquals("/myProject/branches/bug-fix", r.getRelativePath());
		assertEquals("http://localhost/svn/myProject/branches/bug-fix", r.getCompleteSVNURL().toString());
	}
	
	public void testDetectProjectRoot() throws Exception {
		assertEquals("/myProject", r.determineTagRoot("/myProject"));
		assertEquals("/myProject", r.determineTagRoot("/myProject/trunk"));
		assertEquals("/myProject", r.determineTagRoot("/myProject/trunk/"));
		
		assertEquals("/myProject/taggy/hello", r.determineTagRoot("/myProject/taggy/hello"));
		assertEquals("/myProject/branchy/howdy", r.determineTagRoot("/myProject/branchy/howdy"));
		
		r.tagFolderNames.add("taggy");
		r.tagFolderNames.add("branchy");
		
		assertEquals("/myProject", r.determineTagRoot("/myProject/taggy/hello"));
		assertEquals("/myProject", r.determineTagRoot("/myProject/branchy/howdy"));
	}
	
	public void testDetectProjectRootSubModule() throws Exception {
		assertEquals("/myProject", r.determineTagRoot("/myProject/trunk/submodule"));
	}
	
	public void testDetectProjectRootSubModuleBranch() throws Exception {
		assertEquals("/myProject/branches/1.5/submodule", r.determineTagRoot("/myProject/branches/1.5/submodule"));
		r.tagFolderNames.add("branches");
		assertEquals("/myProject", r.determineTagRoot("/myProject/branches/1.5/submodule"));
	}
	
	public void testSortsTags() throws Exception {
		final List<SVNDirEntry> unsorted = new ArrayList<SVNDirEntry>();
		
		final SVNURL fakeURL = SVNURL.parseURIEncoded("http://localhost");
		
		unsorted.add(new SVNDirEntry(fakeURL, "b", SVNNodeKind.DIR, 1, false, 1, new Date(), ""));
		unsorted.add(new SVNDirEntry(fakeURL, "a", SVNNodeKind.DIR, 1, false, 1, new Date(), ""));
		
		r = new SubversionRepositoryAdaptor(globalConfig, projectConfig, repoConfig, null, new SVNRepositoryImpl(fakeURL, null) {
			@Override
			public Collection getDir(String path, long arg1, Map arg2, Collection arg3) throws SVNException {
				if (repoConfig.getPath().equals(path)) {
					return Collections.singletonList(new SVNDirEntry(fakeURL, "tags", SVNNodeKind.DIR, 1, false, 1, new Date(), "tags"));
				}
				
				return unsorted;
			}
		});
		
		repoConfig.setPath("/myProject");
		
		final List<RepositoryTagDto> tags = r.getAvailableTags();
		
		final List<String> names = new ArrayList<String>();
		
		for (RepositoryTagDto tag : tags) {
			names.add(tag.getName());
		}
		
		final List<String> sortedNames = new ArrayList<String>(names);
		Collections.sort(sortedNames);
		
		assertEquals(sortedNames, names);
	}
	public void testGetLatestRevisionHandlesNullPathInfo() throws Exception {
		final SVNURL fakeURL = SVNURL.parseURIEncoded("http://localhost");
		repoConfig.setPath("a");
		
		r = new SubversionRepositoryAdaptor(globalConfig, projectConfig, repoConfig, null, new SVNRepositoryImpl(fakeURL, null) {
			@Override
			public SVNDirEntry info(String path, long revision) throws SVNException {
				return null;
			}
		});
		
		try {
			r.getLatestRevision();
			fail("expected exception");
		} catch (RepositoryException e) {
			assertEquals("svn.path.not.exist", e.getKey());
			assertEquals("a", e.getArgs()[0]);
		}
	}
	public void testNonFatalException() throws Exception {
		// Might be SVNErrorCode.CLIENT_UNRELATED_RESOURCES but not confirmed.
		final SVNException e = new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, 
				"svn: The location for 'http://example.com/svn/trunk/myProject' for revision" +
				"99 does not exist in the repository or refers to an unrelated object"));
		
		assertFalse(r.isFatal(e));
	}
	public void testFatalException() throws Exception {
		final SVNException e = new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN, ""));
		
		assertTrue(r.isFatal(e));
	}
	public void testCombineLogRegexAndMessage() throws Exception {
		assertEquals("bug (\\d+)", r.combinePatterns("bug (\\d+)", null));
		assertEquals("Bug-ID: (\\d+)", r.combinePatterns(null, "Bug-ID: %BUGID%"));
		assertEquals("bug (\\d+)|Bug-ID: (\\d+)", r.combinePatterns("bug (\\d+)", "Bug-ID: %BUGID%"));
	}
	private void assertTagName(String path, String expectedTagName) {
		repoConfig.setPath(path);
		assertEquals("for path " + path, expectedTagName, r.getTagName());
	}
}
