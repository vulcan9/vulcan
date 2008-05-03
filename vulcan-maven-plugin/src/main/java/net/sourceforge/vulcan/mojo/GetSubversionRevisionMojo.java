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
	 * @parameter expression="${project.revision.numeric}"
	 */
	private String predefinedSubversionRevision;
	
	/**
	 * @parameter expression="${project}"
	 * @required
	 * @readonly 
	 */
	private MavenProject mavenProject;
	
	private Exception exception;
	
	public void execute() throws MojoExecutionException	{
		if (StringUtils.isNotBlank(predefinedSubversionRevision) && !"0".equals(predefinedSubversionRevision)) {
			mavenProject.getProperties().setProperty(
				"net.sourceforge.vulcan.maven-vulcan-plugin.revision",
				predefinedSubversionRevision);
			return;
		}
		
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
