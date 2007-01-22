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

import junit.framework.TestCase;

public class LineOfDevelopmentTest extends TestCase {
	LineOfDevelopment lod;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		lod = new LineOfDevelopment();
		lod.setRepositoryRoot("http://localhost/svn");
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
		lod.tagFolderNames.add("branches");
		lod.tagFolderNames.add("tags");
		
		assertTagName("foo/trunk/bar", "trunk");
		assertTagName("foo/branches/1.6/bar", "branches/1.6");
		assertTagName("/tags/1.6/bar/cat", "tags/1.6");
	}
	
	public void testBranch() throws Exception {
		assertTagName("/myProject/branches/1.1-bug-fix", "trunk");
		
		lod.tagFolderNames.add("branches");
		
		assertTagName("/myProject/branches/1.1-bug-fix", "branches/1.1-bug-fix");
	}
	
	public void testGoofy() throws Exception {
		assertTagName("/myProject/goofy/1.1-bug-fix", "trunk");
		
		lod.tagFolderNames.add("goofy");
		
		assertTagName("/myProject/goofy/1.1-bug-fix", "goofy/1.1-bug-fix");
	}
	
	public void testSetTagName() throws Exception {
		lod.setPath("/myProject/trunk");
		
		assertEquals("http://localhost/svn/myProject/trunk", lod.getAbsoluteUrl());
		assertEquals("/myProject/trunk", lod.getComputedRelativePath());
		
		lod.setAlternateTagName("branches/bug-fix");
		
		assertEquals("branches/bug-fix", lod.getComputedTagName());
		
		assertEquals("/myProject/branches/bug-fix", lod.getComputedRelativePath());
		assertEquals("http://localhost/svn/myProject/branches/bug-fix", lod.getAbsoluteUrl());
	}
	
	public void testSetTagNameOnVariantPath() throws Exception {
		lod.tagFolderNames.add("samples");
		
		lod.setPath("/myProject/samples/4.1");
		
		assertEquals("http://localhost/svn/myProject/samples/4.1", lod.getAbsoluteUrl());
		assertEquals("/myProject/samples/4.1", lod.getComputedRelativePath());
		
		lod.setAlternateTagName("branches/bug-fix");
		
		assertEquals("branches/bug-fix", lod.getComputedTagName());
		
		assertEquals("/myProject/branches/bug-fix", lod.getComputedRelativePath());
		assertEquals("http://localhost/svn/myProject/branches/bug-fix", lod.getAbsoluteUrl());
	}
	
	public void testDetectProjectRoot() throws Exception {
		assertEquals("/myProject", determineTagRoot("/myProject"));
		assertEquals("/myProject", determineTagRoot("/myProject/trunk"));
		assertEquals("/myProject", determineTagRoot("/myProject/trunk/"));
		
		assertEquals("/myProject/taggy/hello", determineTagRoot("/myProject/taggy/hello"));
		assertEquals("/myProject/branchy/howdy", determineTagRoot("/myProject/branchy/howdy"));
		
		lod.tagFolderNames.add("taggy");
		lod.tagFolderNames.add("branchy");
		
		assertEquals("/myProject", determineTagRoot("/myProject/taggy/hello"));
		assertEquals("/myProject", determineTagRoot("/myProject/branchy/howdy"));
	}
	
	public void testDetectProjectRootSubModule() throws Exception {
		assertEquals("/myProject", determineTagRoot("/myProject/trunk/submodule"));
	}
	
	public void testDetectProjectRootSubModuleBranch() throws Exception {
		assertEquals("/myProject/branches/1.5/submodule", determineTagRoot("/myProject/branches/1.5/submodule"));
		lod.tagFolderNames.add("branches");
		assertEquals("/myProject", determineTagRoot("/myProject/branches/1.5/submodule"));
	}

	public void testGetRelativePathSubModule() throws Exception {
		lod.tagFolderNames.add("tags");
		
		lod.setPath("/trunk/submodule");
		
		lod.setAlternateTagName("tags/1.0");
		
		assertEquals("/tags/1.0/submodule", lod.getComputedRelativePath());
		assertEquals("http://localhost/svn/tags/1.0/submodule", lod.getAbsoluteUrl());
		assertEquals("tags/1.0", lod.getComputedTagName());
	}
	
	private String determineTagRoot(String path) {
		lod.setPath(path);
		return lod.getComputedTagRoot();
	}
	private void assertTagName(String path, String expectedTagName) {
		lod.setPath(path);
		assertEquals("for path " + path, expectedTagName, lod.getComputedTagName());
	}
}
