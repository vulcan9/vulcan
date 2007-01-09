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
package net.sourceforge.vulcan.mojo;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.war.WarArchiver;

/**
 * @goal bundle
 * @phase install
 */
public class BundleMojo	extends AbstractMojo {
	
	/**
	 * The directory for the generated WAR.
	 *
	 * @parameter expression="${vulcan.war.target}"
	 * @required
	 */
	private File warFile;

	/**
	 * @parameter expression="${project.groupId}.${project.artifactId}"
	 * @required
	 */
	private String vulcanPluginId;
	
	/**
	 * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#war}"
	 * @required
	 * @readonly
	 */
	private WarArchiver archiver;
	
	/**
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	public void execute() throws MojoExecutionException	{
		
		archiver.setDestFile(warFile);
		
		try {
			archiver.addArchivedFileSet(warFile);
			final File zipFile = new File(project.getBuild().getDirectory(), vulcanPluginId + ".zip");
			archiver.addFile(zipFile, "WEB-INF/plugins/" + zipFile.getName());
		
			findManifest();
			
			archiver.createArchive();
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void findManifest() throws ArchiverException {
		final Collection files = archiver.getFiles().values();
		
		for (Iterator itr = files.iterator(); itr.hasNext();) {
			final ArchiveEntry entry = (ArchiveEntry) itr.next();
			final File file = entry.getFile();
			
			if ("MANIFEST.MF".equals(file.getName())) {
				getLog().info("Adding manifest back");
				archiver.setManifest(file);
			} else if ("web.xml".equals(file.getName())) {
				getLog().info("Adding web.xml back");
				archiver.setWebxml(file);
			}
		}
	}
}
