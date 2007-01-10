package net.sourceforge.vulcan.mojo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 * @goal generate-plugin-version
 * @phase generate-resources
 */
public class GeneratePluginVersionMojo	extends AbstractMojo {
	/**
	 * @parameter expression="${project.build.directory}/plugin-version.xml"
	 * @required
	 */
	private File versionFile;
	
	/**
	 * @parameter expression="${net.sourceforge.vulcan.maven-vulcan-plugin.revision}"
	 * @required
	 */
	private long pluginRevision;

	/**
	 * @parameter
	 * @required
	 */
	private String versionLabel;
	
	/**
	 * @parameter expression="${line.separator}"
	 * @required
	 */
	private String lineSeparator;
	
	public void execute() throws MojoExecutionException	{
		final File dir = versionFile.getParentFile();
		if (!dir.exists() && !dir.mkdirs()) {
			throw new MojoExecutionException("Failed to create directory " + versionFile.getParent());
		}
		
		getLog().info("Writing plugin version info to " + versionFile.getAbsolutePath());
		
		final Element root = new Element("vulcan-version-descriptor");
		root.setAttribute("pluginRevision", Long.toString(pluginRevision));
		root.setAttribute("version", versionLabel);
		
		final XMLOutputter out = new XMLOutputter();

		Writer writer = null;
		
		try {
			writer = new FileWriter(versionFile);
		
			out.output(root, writer);
			
			writer.write(lineSeparator);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException ignore) {
				}
			}
		}
	}
}
