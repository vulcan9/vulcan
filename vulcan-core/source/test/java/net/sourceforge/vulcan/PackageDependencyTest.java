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
package net.sourceforge.vulcan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;
import junit.framework.TestCase;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.lang.StringUtils;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class PackageDependencyTest extends TestCase {
	JDepend jdepend;
	
	Collection<JavaPackage> pkgs;
	
	@SuppressWarnings("unchecked")
	@Override
	public void setUp() throws IOException {
		jdepend = new JDepend();

		jdepend.addDirectory(TestUtils.resolveRelativePath("target/classes"));
		
		pkgs = jdepend.analyze();
	}

	public void testNoCycles() {
		final List<String> cycles = new ArrayList<String>();
		
		for (JavaPackage pkg : pkgs) {
			if (pkg.containsCycle()) {
				cycles.add(pkg.getName());
			}
		}
		
		if (cycles.size() > 0) {
			fail("The following packages have dependency cycles: " + StringUtils.join(cycles.iterator(), ", "));
		}
	}

	
	public void testAbstractPackages() {
		final String[] abstractPackages = {
				"net.sourceforge.vulcan",
				"net.sourceforge.vulcan.core",
				"net.sourceforge.vulcan.metadata",
				"net.sourceforge.vulcan.scheduler"};
		
		for (String name : abstractPackages) {
			final JavaPackage pkg = jdepend.getPackage(name);
			
			if (pkg.getConcreteClassCount() == 4 && name.equals("net.sourceforge.vulcan.core")) {
				/*
				 * Waiver for ProjectBuilder.BuildPhase, DependencyBuildPolicy,
				 * WorkingCopyUpdateStrategy and NameCollisionResolutionMode
				 * which are enums (not concrete classes) 
				 */
				continue;
			}
			assertEquals(name + " should be abstract but has " + pkg.getConcreteClassCount() + " concrete classes", 1.0, pkg.abstractness(), 0.01);
		}
	}
	
	public void testNoDepsOnSupport() {
		final String[] supportsPackages = {
				"net.sourceforge.vulcan.core.support",
				"net.sourceforge.vulcan.scheduler.thread",
				"net.sourceforge.vulcan.spring"};
		
		for (String name : supportsPackages) {
			assertNoAfferents(name);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void assertNoAfferents(String packageName) {
		final JavaPackage support = jdepend.getPackage(packageName);
		final Collection<JavaPackage> deps = support.getAfferents();
		final List<String> names = new ArrayList<String>();
		
		for (JavaPackage pkg : deps) {
			final String name = pkg.getName();
			if (isSupportPackage(name)) {
				continue;
			}
			names.add(name);
		}
		if (names.size() > 0) {
			fail("The following packages should not depend on " + packageName + ": " + StringUtils.join(names.iterator(), ", "));
		}
	}

	/**
	 * At this time it is considered OK to have one support package depend on another,
	 * as long as no cycles exist (which would be caught by the cycles test.
	 */
	private boolean isSupportPackage(final String name) {
		return name.indexOf("support") > 0 || name.indexOf("spring") > 0;
	}
}
