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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.subversion.dto.CheckoutDepth;
import net.sourceforge.vulcan.subversion.dto.SparseCheckoutDto;
import net.sourceforge.vulcan.subversion.dto.SubversionConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionProjectConfigDto;
import net.sourceforge.vulcan.subversion.dto.SubversionRepositoryProfileDto;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryImpl;
import org.tmatesoft.svn.core.wc.SVNPropertyData;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class SubversionRepositoryAdaptorTest extends TestCase {
	SubversionRepositoryAdaptor r;
	SubversionConfigDto globalConfig = new SubversionConfigDto();
	SubversionProjectConfigDto repoConfig = new SubversionProjectConfigDto();
	ProjectConfigDto projectConfig = new ProjectConfigDto();
	SubversionRepositoryProfileDto profile = new SubversionRepositoryProfileDto();
	
	
	RevisionTokenDto r1 = new RevisionTokenDto(100l, "r100");
	RevisionTokenDto r2 = new RevisionTokenDto(200l, "r200");
	
	SVNURL fakeURL;
	SVNDirEntry fakeSVNDirEntry;
	List<ChangeSetDto> fakeChangeSets = new ArrayList<ChangeSetDto>();
	long fakeMostRecentLogRevision;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		fakeURL = SVNURL.parseURIEncoded("http://localhost");
		
		repoConfig.setRepositoryProfile("a");
		repoConfig.setPath("");
		
		profile.setDescription("a");
		profile.setRootUrl("http://localhost/svn");
		
		globalConfig.setProfiles(new SubversionRepositoryProfileDto[] {profile});
		
		r = new SubversionRepositoryAdaptor(globalConfig, projectConfig, repoConfig, null, false);
	}
	
	public void testSortsTags() throws Exception {
		final String path = "/myProject";
		final List<SVNDirEntry> unsorted = new ArrayList<SVNDirEntry>();
		
		unsorted.add(new SVNDirEntry(fakeURL, fakeURL, "b", SVNNodeKind.DIR, 1, false, 1, new Date(), ""));
		unsorted.add(new SVNDirEntry(fakeURL, fakeURL, "a", SVNNodeKind.DIR, 1, false, 1, new Date(), ""));
		
		repoConfig.setPath(path);
		
		r = new SubversionRepositoryAdaptor(globalConfig, projectConfig, repoConfig, null, globalConfig.getProfiles()[0], new SVNRepositoryImpl(fakeURL, null) {
			@Override
			@SuppressWarnings("unchecked")
			public Collection getDir(String path, long arg1, SVNProperties arg2, Collection arg3) throws SVNException {
				if (path.equals(path)) {
					return Collections.singletonList(new SVNDirEntry(fakeURL, fakeURL, "tags", SVNNodeKind.DIR, 1, false, 1, new Date(), "tags"));
				}
				
				return unsorted;
			}
		});
		
		r.lineOfDevelopment.setPath(path);
		
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
		repoConfig.setPath("a");
		
		r = new TestableSubversionRepositoryAdaptor();
		
		try {
			r.getLatestRevision(null);
			fail("expected exception");
		} catch (RepositoryException e) {
			assertEquals("svn.path.not.exist", e.getKey());
			assertEquals("a", e.getArgs()[0]);
		}
	}
	
	public void testGetLatestRevisionUsesLogRevision() throws Exception {
		fakeSVNDirEntry = new SVNDirEntry(fakeURL, fakeURL, "trunk", SVNNodeKind.DIR, 0, false, 100l, new Date(), "author");
		
		fakeMostRecentLogRevision = 101l;
		r = new TestableSubversionRepositoryAdaptor();
		
		assertEquals(fakeMostRecentLogRevision, r.getLatestRevision(r1).getRevision().longValue());
	}
	
	public void testGetLatestRevisionFiltersSparseLogs() throws Exception {
		fakeSVNDirEntry = new SVNDirEntry(fakeURL, fakeURL, "trunk", SVNNodeKind.DIR, 0, false, 100l, new Date(), "author");
		
		repoConfig.setCheckoutDepth(CheckoutDepth.Empty);
		
		ChangeSetDto change = new ChangeSetDto();
		change.setModifiedPaths(new String[] {"/some/included/path"});
		change.setRevisionLabel("r125");
		fakeChangeSets.add(change);
		
		change = new ChangeSetDto();
		change.setModifiedPaths(new String[] {"/some/excluded/path"});
		change.setRevisionLabel("r150");
		fakeChangeSets.add(change);		
		
		repoConfig.setPath("/");
		repoConfig.setCheckoutDepth(CheckoutDepth.Empty);
		repoConfig.setFolders(new SparseCheckoutDto[] {new SparseCheckoutDto("some/included", CheckoutDepth.Infinity)});

		fakeMostRecentLogRevision = 200l;
		
		r = new TestableSubversionRepositoryAdaptor();
		
		assertEquals(125l, r.getLatestRevision(r1).getRevision().longValue());
	}
	
	public void testGetLatestRevisionFiltersSparseLogsNoneMatch() throws Exception {
		fakeSVNDirEntry = new SVNDirEntry(fakeURL, fakeURL, "trunk", SVNNodeKind.DIR, 0, false, 100l, new Date(), "author");
		
		repoConfig.setCheckoutDepth(CheckoutDepth.Empty);
		
		ChangeSetDto change = new ChangeSetDto();
		change.setModifiedPaths(new String[] {"/some/excluded/path"});
		change.setRevisionLabel("r125");
		fakeChangeSets.add(change);
		
		repoConfig.setPath("/");
		repoConfig.setCheckoutDepth(CheckoutDepth.Empty);
		repoConfig.setFolders(new SparseCheckoutDto[] {new SparseCheckoutDto("some/included", CheckoutDepth.Infinity)});

		fakeMostRecentLogRevision = 200l;
		
		r = new TestableSubversionRepositoryAdaptor();
		
		assertEquals(r1.getRevision(), r.getLatestRevision(r1).getRevision());
	}
	
	public void testGetLatestRevisionSparseIgnoresException() throws Exception {
		fakeSVNDirEntry = new SVNDirEntry(fakeURL, fakeURL, "trunk", SVNNodeKind.DIR, 0, false, 100l, new Date(), "author");
		
		repoConfig.setCheckoutDepth(CheckoutDepth.Empty);
		fakeChangeSets = null;
		
		repoConfig.setPath("/");
		repoConfig.setCheckoutDepth(CheckoutDepth.Empty);
		repoConfig.setFolders(new SparseCheckoutDto[] {new SparseCheckoutDto("some/included", CheckoutDepth.Infinity)});

		fakeMostRecentLogRevision = 200l;
		
		r = new TestableSubversionRepositoryAdaptor();
		
		assertEquals(fakeMostRecentLogRevision, r.getLatestRevision(r1).getRevision().longValue());
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
	
	public void testCombineLogRegexAndMessageCaseInsensitive() throws Exception {
		assertEquals("Bug-ID: (\\d+)", r.combinePatterns(null, "Bug-ID: %bugid%"));
	}
	
	public void testConvertsPropertyToStringIfNecessary() throws Exception {
		final String value = "hello world";
		final SVNPropertyValue data = SVNPropertyValue.create("proprietary", value.getBytes());
		final SVNPropertyData prop = new SVNPropertyData("proprietary", data, null);
		
		assertEquals(value, r.getValueIfNotNull(prop));
	}
	
	public void testFiltersSparseChangeSets() throws Exception {
		final ChangeSetDto change = new ChangeSetDto();
		change.setModifiedPaths(new String[] {"some/excluded/path"});
		repoConfig.setCheckoutDepth(CheckoutDepth.Empty);
		
		fakeChangeSets.add(change);
		
		final ChangeLogDto changeLog = new TestableSubversionRepositoryAdaptor().getChangeLog(r1, r2, null);
		
		assertEquals(Collections.emptyList(), changeLog.getChangeSets());
	}
	
	public void testGetChangeLogCachesResult() throws Exception {
		final ChangeSetDto change = new ChangeSetDto();
		change.setModifiedPaths(new String[] {"some/excluded/path"});
		repoConfig.setCheckoutDepth(CheckoutDepth.Empty);
		
		fakeChangeSets.add(change);
		
		r = new TestableSubversionRepositoryAdaptor();
		
		final ChangeLogDto cl1 = r.getChangeLog(r1, r2, null);
		
		fakeChangeSets = new ArrayList<ChangeSetDto>();
		
		final ChangeLogDto cl2 = r.getChangeLog(r1, r2, null);
		
		assertSame(cl1.getChangeSets(), cl2.getChangeSets());
	}

	class TestableSubversionRepositoryAdaptor extends SubversionRepositoryAdaptor {
		
		public TestableSubversionRepositoryAdaptor() throws ConfigException {
			super(globalConfig, projectConfig, repoConfig, null, SubversionRepositoryAdaptorTest.this.profile, new SVNRepositoryImpl(fakeURL, null) {
				@Override
				public SVNDirEntry info(String path, long revision) throws SVNException {
					return fakeSVNDirEntry;
				}
			});
		}

		@Override
		protected List<ChangeSetDto> fetchChangeSets(SVNRevision r1, SVNRevision r2) throws RepositoryException {
			if (fakeChangeSets == null) {
				throw new RepositoryException("the path doesn't exist at that revision", new SVNException(SVNErrorMessage.UNKNOWN_ERROR_MESSAGE));
			}
			return fakeChangeSets;
		}
		
		@Override
		protected void fetchDifferences(SVNRevision r1, SVNRevision r2,
				OutputStream os) throws RepositoryException {
		}
		
		@Override
		protected long getMostRecentLogRevision(long lastChangedRevision) throws RepositoryException {
			return fakeMostRecentLogRevision;
		}
	}
}
