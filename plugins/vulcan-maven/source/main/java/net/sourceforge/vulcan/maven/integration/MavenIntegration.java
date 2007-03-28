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
package net.sourceforge.vulcan.maven.integration;

import java.io.File;
import java.io.IOException;

import net.sourceforge.vulcan.core.ProjectBuildConfigurator;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.maven.MavenProjectConfigurator;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MavenIntegration {
	private final Embedder embedder = new Embedder();
	final ArtifactRepository artifactRepository;
	
	public MavenIntegration() throws PlexusContainerException, ComponentLookupException, IOException, XmlPullParserException {
		final ClassLoader prev = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			embedder.start();
		} finally {
			Thread.currentThread().setContextClassLoader(prev);
		}
		
		final MavenSettingsBuilder settingsBuilder = (MavenSettingsBuilder) embedder.lookup( MavenSettingsBuilder.ROLE );
		final Settings settings = settingsBuilder.buildSettings();
		settings.setOffline(true);
		settings.setInteractiveMode(false);
		
		artifactRepository = createLocalRepository(embedder, settings);
	}
	
	public ProjectBuildConfigurator createProjectConfigurator(File buildSpecFile) throws ConfigException {
		try {
			final MavenProjectBuilder builder = (MavenProjectBuilder) embedder.lookup(MavenProjectBuilder.ROLE);

			final MavenProject project = builder.build(buildSpecFile, artifactRepository, null);
			
			return new MavenProjectConfigurator(project);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private ArtifactRepository createLocalRepository( Embedder embedder, Settings settings) throws ComponentLookupException
	{
		final ArtifactRepositoryLayout repositoryLayout =
		(ArtifactRepositoryLayout) embedder.lookup( ArtifactRepositoryLayout.ROLE, "default" );
		
		String url = settings.getLocalRepository();
		
		if ( !url.startsWith( "file:" ) )
		{
			url = "file://" + url;
		}
		
		return new DefaultArtifactRepository( "local", url, repositoryLayout );
	}
}
