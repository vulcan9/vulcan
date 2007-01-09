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
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * @goal zip
 * @phase package
 * @requiresDependencyResolution runtime
 */
public class ZipMojo extends AbstractMojo {
	
	/**
	 * @parameter expression="${project.groupId}.${project.artifactId}"
	 * @required
	 */
	private String vulcanPluginId;

	/**
	 * The location of the jar file to include.
	 *
	 * @parameter expression="${project.build.directory}/${project.build.finalName}.jar"
	 * @required
	 */
	private File jarFile;
	
	/**
	 * @parameter expression="${project.build.directory}/plugin-version.xml"
	 * @required
	 */
	private File versionFile;

	/**
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;
	
	/**
	 * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#zip}"
	 * @required
	 * @readonly
	 */
	private ZipArchiver zipArchiver;
	
	/**
	 * @parameter
	 */
	private Set bundledDependencies = Collections.EMPTY_SET;
	
	public void execute() throws MojoExecutionException	{
		final File zipFile = new File(project.getBuild().getDirectory(), vulcanPluginId + ".zip");
		
		final String prefix = vulcanPluginId + "/";
		
		zipArchiver.setDestFile(zipFile);
		try {
			zipArchiver.addFile(versionFile, prefix + "plugin-version.xml");
			zipArchiver.addFile(jarFile, prefix + jarFile.getName());
			
			addDependencies(prefix);
			
			zipArchiver.createArchive();
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void addDependencies(String prefix) throws ArchiverException {
		if (bundledDependencies.isEmpty()) {
			return;
		}
		
		final Set artifacts = project.getArtifacts();

		for (Iterator itr = artifacts.iterator(); itr.hasNext();) {
			final Artifact dep = (Artifact) itr.next();
			
			final String versionlessKey = ArtifactUtils.versionlessKey(dep);
			
			if (bundledDependencies.remove(versionlessKey)) {
				final File file = dep.getFile();
				zipArchiver.addFile(file, prefix + file.getName());
			}
		}

		for (Iterator itr =  bundledDependencies.iterator(); itr.hasNext();) {
			final String unmatched = (String) itr.next();
			getLog().error("No dependency found matches " + unmatched);
		}
	}
}
