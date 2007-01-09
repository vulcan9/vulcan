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
