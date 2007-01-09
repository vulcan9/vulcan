package org.apache.maven;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import net.sourceforge.vulcan.ant.buildlistener.UdpBuildEventPublisher;

import org.apache.commons.grant.GrantProject;
import org.apache.commons.jelly.tags.ant.AntTagLibrary;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.jelly.JellyBuildListener;
import org.apache.maven.jelly.JellyPropsHandler;
import org.apache.maven.jelly.MavenJellyContext;
import org.apache.maven.project.Project;
import org.apache.tools.ant.types.Path;

/**
 * A class to help create Ant projects.
 * 
 * Modified by Chris Eldredge to add custom BuildListener used by Vulcan to monitor
 * build activity.
 */
public class AntProjectBuilder
{
    /**
     * Initialize Ant.
     * @param project a Maven project
     * @param context the maven context whose properties will be available to the Ant Project
     * @return an Ant project
     */
    public static GrantProject build( final Project project, final MavenJellyContext context )
    {
        // Create the build listener.
        JellyBuildListener buildListener = new JellyBuildListener( context.getXMLOutput() );
        buildListener.setDebug( context.getDebugOn().booleanValue() );
        buildListener.setEmacsMode( context.getEmacsModeOn().booleanValue() );

        // Create our ant project.
        GrantProject antProject = new GrantProject();
        antProject.setPropsHandler( new JellyPropsHandler( context ) );
        antProject.init();
        antProject.setBaseDir( project.getFile().getParentFile().getAbsoluteFile() );
        antProject.addBuildListener( buildListener );
        
        try {
			antProject.addBuildListener(new UdpBuildEventPublisher());
		} catch (Exception e) {
			LogFactory.getLog("net.sourceforge.vulcan.maven")
				.error("Failed to add Vulcan BuildListener to Ant Project", e);
		}
        
        context.setAntProject( antProject );
        AntTagLibrary.setProject( context, antProject );

        Path p = new Path( antProject );
        p.setPath( project.getDependencyClasspath() );
        antProject.addReference( MavenConstants.DEPENDENCY_CLASSPATH, p );

        return antProject;
    }
}
