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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @goal get-subversion-revision
 * @phase generate-resources
 */
public class GetSubversionRevisionMojo	extends AbstractMojo {
	/**
	 * @parameter expression="${basedir}" default-value="/tmp"
	 */
	private String basedir;
	
	/**
	 * @parameter expression="${project}"
     * @required
     * @readonly 
	 */
	private MavenProject mavenProject;
	
	private Exception exception;
	
	public void execute() throws MojoExecutionException	{
		try {
			final Document contents = exec();
			final long revision = findRevision(contents);
			
			mavenProject.getProperties().setProperty(
					"net.sourceforge.vulcan.maven-vulcan-plugin.revision",
					Long.toString(revision));
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private long findRevision(Document contents) throws JDOMException {
		final XPath selector = XPath.newInstance("/info/entry/commit/@revision");
		
		final Attribute node = (Attribute) selector.selectSingleNode(contents);
		
		if (node == null) {
			throw new IllegalStateException("Failed to obtain Last-Changed-Revision from working copy");
		}
		
		return node.getLongValue();
	}

	private Document exec() throws Exception {
		final Process proc = Runtime.getRuntime().exec(
				new String[] {"svn", "info", "--xml"}, null, new File(basedir));
		final Document[] document = new Document[1];
		
		final Thread t = new Thread() {
			public void run() {
				try {
					document[0] = new SAXBuilder().build(proc.getInputStream());
				} catch (Exception e) {
					exception = e;
				}
			}
		};
		
		t.start();
		
		final int result = proc.waitFor();
		
		if (result != 0) {
			final String errorStream = IOUtils.toString(proc.getErrorStream());
			if (StringUtils.isNotBlank(errorStream)) {
				getLog().error(errorStream);
			}
		}
		
		t.join();
		
		if (exception != null) {
			throw exception;
		}
	
		return document[0];
	}
}
